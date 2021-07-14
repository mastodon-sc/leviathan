package org.mastodon.leviathan.algorithms;

import org.mastodon.leviathan.algorithms.FaceIteratorGen.FaceIterator;
import org.mastodon.leviathan.app.LeviathanJunctionAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.JunctionModel;
import org.mastodon.leviathan.model.MembranePart;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanJunctionViewBdv;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.util.DummySpimData;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import bdv.viewer.ViewerOptions;

public class CwIteratorExample
{

	public static void main( final String[] args )
	{
		// Create example graph. 3 touching hexs.
		final JunctionModel model = new JunctionModel( "pixel", "frame" );
		final JunctionGraph graph = model.getGraph();

		final Junction A = graph.addVertex().init( 0, new double[] { 10, 0 } );
		final Junction B = graph.addVertex().init( 0, new double[] { 20, 10 } );
		final Junction C = graph.addVertex().init( 0, new double[] { 20, 20 } );
		final Junction D = graph.addVertex().init( 0, new double[] { 10, 30 } );
		final Junction E = graph.addVertex().init( 0, new double[] { 0, 20 } );
		final Junction F = graph.addVertex().init( 0, new double[] { 0, 10 } );

		final MembranePart ab = graph.addEdge( A, B ).init();
		final MembranePart bc = graph.addEdge( B, C ).init();
		final MembranePart cd = graph.addEdge( C, D ).init();
		final MembranePart de = graph.addEdge( D, E ).init();
		final MembranePart ef = graph.addEdge( E, F ).init();
		final MembranePart fa = graph.addEdge( F, A ).init();

		final Junction G = graph.addVertex().init( 0, new double[] { 30, 0 } );
		final Junction H = graph.addVertex().init( 0, new double[] { 40, 10 } );
		final Junction I = graph.addVertex().init( 0, new double[] { 40, 20 } );
		final Junction J = graph.addVertex().init( 0, new double[] { 30, 30 } );

		final MembranePart bg = graph.addEdge( B, G ).init();
		final MembranePart gh = graph.addEdge( G, H ).init();
		final MembranePart hi = graph.addEdge( H, I ).init();
		final MembranePart ij = graph.addEdge( I, J ).init();
		final MembranePart jc = graph.addEdge( J, C ).init();

		final Junction K = graph.addVertex().init( 0, new double[] { 50, 0 } );
		final Junction L = graph.addVertex().init( 0, new double[] { 60, 10 } );
		final Junction M = graph.addVertex().init( 0, new double[] { 60, 20 } );
		final Junction N = graph.addVertex().init( 0, new double[] { 50, 30 } );

		final MembranePart hk = graph.addEdge( H, K ).init();
		final MembranePart kl = graph.addEdge( K, L ).init();
		final MembranePart lm = graph.addEdge( L, M ).init();
		final MembranePart mn = graph.addEdge( M, N ).init();
		final MembranePart ni = graph.addEdge( N, I ).init();

		final LeviathanJunctionAppModel appModel = toAppModel( model );
		new LeviathanJunctionViewBdv( appModel );

		final FaceIteratorGen itgen = new FaceIteratorGen( model.getGraph() );
		System.out.println( "Clockwise:" );
		FaceIterator it = itgen.iterateCW( bc );
		while ( it.hasNext() )
		{
			final MembranePart next = it.next();
			System.out.println( next + " is CW ? " + it.isCW() );
		}
		System.out.println( "Counter Clockwise:" );
		it = itgen.iterateCCW( bc );
		while ( it.hasNext() )
		{
			final MembranePart next = it.next();
			System.out.println( next + " is CW ? " + it.isCW() );
		}

	}

	public static final LeviathanJunctionAppModel toAppModel( final JunctionModel model )
	{

		final KeymapManager keymapManager = new KeymapManager();
		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		final KeyPressedManager keyPressedManager = new KeyPressedManager();
		final ViewerOptions options = ViewerOptions.options()
				.shareKeyPressedEvents( keyPressedManager )
				.width( 400 )
				.height( 300 );

		final FeatureColorModeManager featureColorModeManager = new FeatureColorModeManager();
		final LeviathanPlugins plugins = new LeviathanPlugins( keymap );
		final Actions globalAppActions = new Actions( keymap.getConfig(), LeviathanKeyConfigContexts.LEVIATHAN );

		final String path = "x=100 y=100 z=1 sx=1 sy=1 sz=1 t=10.dummy";
		final SpimDataMinimal spimData = DummySpimData.tryCreate( path );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				path,
				spimData,
				options,
				() -> System.out.println( "Repaint" ) );

		final LeviathanJunctionAppModel appModel =
				new LeviathanJunctionAppModel(
						model,
						sharedBdvData,
						keyPressedManager,
						featureColorModeManager,
						keymapManager,
						plugins,
						globalAppActions );
		return appModel;
	}

}
