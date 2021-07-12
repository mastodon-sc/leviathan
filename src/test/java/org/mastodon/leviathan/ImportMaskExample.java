package org.mastodon.leviathan;

import org.mastodon.leviathan.algorithms.MaskImporter;
import org.mastodon.leviathan.app.LeviathanAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.model.JunctionModel;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanViewBdv;
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

		new LeviathanViewBdv( appModel );
	}
}
