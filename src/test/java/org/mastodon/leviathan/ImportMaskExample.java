package org.mastodon.leviathan;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.collection.RefSet;
import org.mastodon.leviathan.algorithms.JunctionGraphUtils;
import org.mastodon.leviathan.algorithms.MaskImporter;
import org.mastodon.leviathan.app.LeviathanAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.JunctionModel;
import org.mastodon.leviathan.model.MembranePart;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanViewBdv;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImportMaskExample
{
	public static < T extends RealType< T > > void main( final String[] args ) throws SpimDataException

	{
		final KeymapManager keymapManager = new KeymapManager();
		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		final KeyPressedManager keyPressedManager = new KeyPressedManager();
		final ViewerOptions options = ViewerOptions.options().shareKeyPressedEvents( keyPressedManager );
		final FeatureColorModeManager featureColorModeManager = new FeatureColorModeManager();
		final LeviathanPlugins plugins = new LeviathanPlugins( keymap );
		final Actions globalAppActions = new Actions( keymap.getConfig(), LeviathanKeyConfigContexts.LEVIATHAN );

		final String maskPath = "samples/Segmentation-2.xml";
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( maskPath );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				maskPath,
				spimData,
				options,
				() -> System.out.println( "Repaint" ) );

		final JunctionModel model = new JunctionModel( "pixel", "frame" );

		final int boundSourceID = 0;
		final int timepoint = 0;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( boundSourceID ).getSpimSource().getSource( timepoint, 0 ) );

		MaskImporter.importMask( img, model.getGraph(), timepoint );

		final LeviathanAppModel appModel =
				new LeviathanAppModel(
						model,
						sharedBdvData,
						keyPressedManager,
						featureColorModeManager,
						keymapManager,
						plugins,
						globalAppActions );

		final SelectionModel< Junction, MembranePart > selectionModel = appModel.getSelectionModel();
		selectionModel.listeners().add( () -> {
			final RefSet< MembranePart > edges = appModel.getSelectionModel().getSelectedEdges();
			if ( edges.size() != 1 )
				return;

			final JunctionGraph graph = model.getGraph();
			final MembranePart eref1 = graph.edgeRef();
			final MembranePart eref2 = graph.edgeRef();
			final Junction vref1 = graph.vertexRef();
			final Junction vref2 = graph.vertexRef();

			final MembranePart start = edges.iterator().next();
			final Junction other = start.getSource( vref1 );
			final MembranePart next = JunctionGraphUtils.nextCounterClockWiseEdge( graph, start, other, eref1 );
			selectionModel.setSelected( other, true );
			selectionModel.setSelected( next, true );

			final RefList< MembranePart > face = RefCollections.createRefList( graph.edges() );
			face.add( start );
			selectionModel.pauseListeners();
			while ( !next.equals( start ) )
			{
				face.add( next );
				JunctionGraphUtils.junctionAcross( next, other, vref2 );
				JunctionGraphUtils.nextCounterClockWiseEdge( graph, next, vref2, eref2 );
				selectionModel.setSelected( eref2, true );
				selectionModel.setSelected( vref2, true );
				other.refTo( vref2 );
				next.refTo( eref2 );
			}
			selectionModel.resumeListeners();

			final StringBuilder str = new StringBuilder( "Selected face made of " );
			str.append( face.get( 0 ).getInternalPoolIndex() );
			for ( int i = 1; i < face.size(); i++ )
				str.append( ", " + face.get( i ).getInternalPoolIndex() );
			System.out.println( str );

			graph.releaseRef( eref1 );
			graph.releaseRef( eref2 );
			graph.releaseRef( vref1 );
			graph.releaseRef( vref2 );
		} );

		new LeviathanViewBdv( appModel );
	}
}
