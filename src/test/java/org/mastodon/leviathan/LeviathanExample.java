package org.mastodon.leviathan;

import org.mastodon.leviathan.app.LeviathanAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.JunctionModel;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanViewBdv;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.util.DummySpimData;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import bdv.viewer.ViewerOptions;

public class LeviathanExample
{
	public static void main( final String[] args )
	{
		final JunctionModel model = new JunctionModel( "pixel", "frame" );
		final JunctionGraph graph = model.getGraph();

		double theta = 0.;
		double x = 500.;
		double y = 200.;
		final double length = 20.;
		final Junction v1 = graph.vertexRef();
		final Junction v2 = graph.vertexRef();
		Junction source = null;
		for ( int i = 0; i < 6; i++ )
		{
			final Junction target = graph.addVertex( v2 ).init( 0, new double[] { x, y } );
			if ( source != null )
				graph.addEdge( source, target );
			else
				source = graph.addVertex( v1 ).init( 0, new double[] { x, y } );
			theta += Math.toRadians( 60. );
			x += length * Math.cos( theta );
			y += length * Math.sin( theta );
			source.refTo( target );
		}
		final Junction first = graph.vertices().getRefPool().getObject( 0, v2 );
		graph.addEdge( source, first );

		final KeyPressedManager keyPressedManager = new KeyPressedManager();
		final FeatureColorModeManager featureColorModeManager = new FeatureColorModeManager();
		final KeymapManager keymapManager = new KeymapManager();
		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		final ViewerOptions options = ViewerOptions.options().shareKeyPressedEvents( keyPressedManager );
		final LeviathanPlugins plugins = new LeviathanPlugins( keymap );
		final Actions globalAppActions = new Actions( keymap.getConfig(), LeviathanKeyConfigContexts.LEVIATHAN );

		final String dummy = "x=1000 y=1000 z=1 sx=1 sy=1 sz=1 t=10.dummy";
		final SpimDataMinimal spimData = DummySpimData.tryCreate( dummy );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				dummy,
				spimData,
				options,
				() -> System.out.println( "Repaint" ) );

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
