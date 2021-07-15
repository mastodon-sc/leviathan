package org.mastodon.leviathan;

import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.algorithms.MaskImporter;
import org.mastodon.leviathan.app.LeviathanCellAppModel;
import org.mastodon.leviathan.app.LeviathanJunctionAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.app.LeviathanMainWindow;
import org.mastodon.leviathan.app.LeviathanWM;
import org.mastodon.leviathan.model.cell.Cell;
import org.mastodon.leviathan.model.cell.CellGraph;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;
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

		/*
		 * Junction graph.
		 */

		final String maskPath = "samples/Segmentation-2.xml";
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( maskPath );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				maskPath,
				spimData,
				options,
				() -> System.out.println( "Repaint" ) );

		final JunctionModel junctionModel = new JunctionModel( "pixel", "frame" );

		final int boundSourceID = 0;
		final int timepoint = 0;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( boundSourceID ).getSpimSource().getSource( timepoint, 0 ) );

		MaskImporter.importMask( img, junctionModel.getGraph(), timepoint );

		final LeviathanJunctionAppModel junctionAppModel =
				new LeviathanJunctionAppModel(
						junctionModel,
						sharedBdvData,
						keyPressedManager,
						featureColorModeManager,
						keymapManager,
						plugins,
						globalAppActions );

		/*
		 * Cell graph.
		 */

		final CellModel cellModel = new CellModel( junctionModel.getSpaceUnits(), junctionModel.getTimeUnits() );
		FindFaces.findFaces( junctionAppModel.getModel().getGraph(), cellModel.getGraph() );

		final LeviathanCellAppModel cellAppModel = new LeviathanCellAppModel(
				cellModel,
				junctionAppModel.getModel(),
				sharedBdvData,
				keyPressedManager,
				featureColorModeManager,
				keymapManager,
				plugins,
				globalAppActions );

		final LeviathanWM wm = new LeviathanWM( new Context() );
		wm.setAppModels( cellAppModel, junctionAppModel );
		new LeviathanMainWindow( wm ).setVisible( true );
	}

	public static void printCellGraph( final JunctionGraph junctionGraph, final CellGraph cellGraph )
	{
		final Junction jref = junctionGraph.vertexRef();
		final MembranePart mref = junctionGraph.edgeRef();
		System.out.println( "Cell graph from junction graph:" );
		for ( final Cell cell : cellGraph.vertices() )
		{
			final int[] mbids = cell.getMembranes();
			final MembranePart mb0 = junctionGraph.getGraphIdBimap().getEdge( mbids[ 0 ], mref );
			String mbstr = "" + mb0.getSource( jref ).getInternalPoolIndex();

			for ( int i = 1; i < mbids.length; i++ )
			{
				final MembranePart mb = junctionGraph.getGraphIdBimap().getEdge( mbids[ i ], mref );
				mbstr += ", " + mb.getSource( jref ).getInternalPoolIndex();
			}
			System.out.println( String.format( " - cell %3d: x = %5.1f y = %5.1f, from edges: %s" ,
					cell.getInternalPoolIndex(), cell.getDoublePosition( 0 ), cell.getDoublePosition( 1 ), mbstr ) );
		}
	}
}
