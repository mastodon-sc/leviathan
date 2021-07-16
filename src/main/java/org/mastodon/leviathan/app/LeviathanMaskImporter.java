package org.mastodon.leviathan.app;

import java.io.File;
import java.io.IOException;

import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.algorithms.MaskImporter;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.overlay.cell.ui.CellRenderSettingsManager;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.util.DummySpimData;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class LeviathanMaskImporter
{

	public static final < T extends RealType< T > > LeviathanWM importMask( final String maskPath, final Context context ) throws IOException, SpimDataException
	{
		/*
		 * Prepare window manager.
		 */

		final LeviathanWM wm = new LeviathanWM( context );
		final KeyPressedManager keyPressedManager = wm.getKeyPressedManager();
		final FeatureColorModeManager featureColorModeManager = wm.getFeatureColorModeManager();
		final CellRenderSettingsManager cellRenderSettingsManager = wm.getCellRenderSettingsManager();
		final KeymapManager keymapManager = wm.getKeymapManager();
		final LeviathanPlugins plugins = wm.getPlugins();
		final Actions globalAppActions = wm.getGlobalAppActions();
		final ViewerOptions options = ViewerOptions.options()
				.shareKeyPressedEvents( keyPressedManager )
				.width( 600 )
				.height( 400 );

		/*
		 * Create shared image data structure.
		 */

		SpimDataMinimal spimData = DummySpimData.tryCreate( new File( maskPath ).getName() );
		if ( spimData == null )
		{
			try
			{
				spimData = new XmlIoSpimDataMinimal().load( maskPath );
			}
			catch ( final SpimDataIOException e )
			{
				e.printStackTrace();
				System.err.println( "Could not open image data file. Opening with dummy dataset. Please fix dataset path!" );
				spimData = DummySpimData.tryCreate( "x=100 y=100 z=100 sx=1 sy=1 sz=1 t=10.dummy" );
			}
		}
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				maskPath,
				spimData,
				options,
				() -> {} );

		/*
		 * Physical units.
		 */

		final String spaceUnits = spimData.getSequenceDescription().getViewSetupsOrdered().stream()
				.filter( BasicViewSetup::hasVoxelSize )
				.map( setup -> setup.getVoxelSize().unit() )
				.findFirst()
				.orElse( "pixel" );
		final String timeUnits = "frame";

		/*
		 * Create junction graph from image data.
		 */

		final JunctionModel junctionModel = new JunctionModel( spaceUnits, timeUnits );

		final int boundSourceID = 0;
		final int timepoint = 0; // TODO
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( boundSourceID ).getSpimSource().getSource( timepoint, 0 ) );

		MaskImporter.importMask( img, junctionModel.getGraph(), timepoint );

		/*
		 * Create cell graph from junction graph.
		 */

		final CellModel cellModel = new CellModel( spaceUnits, timeUnits );
		FindFaces.findFaces( junctionModel.getGraph(), cellModel.getGraph() );

		/*
		 * Pass results to window manager and return it.
		 */

		final LeviathanJunctionAppModel junctionAppModel = new LeviathanJunctionAppModel(
				junctionModel,
				sharedBdvData,
				keyPressedManager,
				featureColorModeManager,
				keymapManager,
				plugins,
				globalAppActions );

		final LeviathanCellAppModel cellAppModel = new LeviathanCellAppModel(
				cellModel,
				junctionAppModel.getModel(),
				sharedBdvData,
				keyPressedManager,
				cellRenderSettingsManager,
				featureColorModeManager,
				keymapManager,
				plugins,
				globalAppActions );
		wm.setAppModels( cellAppModel, junctionAppModel );
		return wm;
	}
}
