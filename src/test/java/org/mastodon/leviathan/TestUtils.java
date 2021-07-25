package org.mastodon.leviathan;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.app.LeviathanMainWindow;
import org.mastodon.leviathan.app.LeviathanWM;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class TestUtils
{
	public static class GraphBundle
	{

		public final CellModel cellModel;

		public final JunctionModel junctionModel;

		public final RefList< Junction > junctions;

		public final RefList< MembranePart > membranes;

		public GraphBundle( final CellModel cellModel, final JunctionModel junctionModel, final RefList< Junction > junctions, final RefList< MembranePart > membranes )
		{
			this.cellModel = cellModel;
			this.junctionModel = junctionModel;
			this.junctions = junctions;
			this.membranes = membranes;
		}
	}

	public static GraphBundle getExampleGraph()
	{

		// Create example graph. 3 touching hexs.
		final JunctionModel junctionModel = new JunctionModel( "pixel", "frame" );
		final JunctionGraph junctionGraph = junctionModel.getGraph();
		final RefList< Junction > junctions = RefCollections.createRefList( junctionGraph.vertices() );
		final RefList< MembranePart > membranes = RefCollections.createRefList( junctionGraph.edges() );

		final Junction A = junctionGraph.addVertex().init( 0, new double[] { 10, 0 } );
		final Junction B = junctionGraph.addVertex().init( 0, new double[] { 20, 10 } );
		final Junction C = junctionGraph.addVertex().init( 0, new double[] { 20, 20 } );
		final Junction D = junctionGraph.addVertex().init( 0, new double[] { 10, 30 } );
		final Junction E = junctionGraph.addVertex().init( 0, new double[] { 0, 20 } );
		final Junction F = junctionGraph.addVertex().init( 0, new double[] { 0, 10 } );
		junctions.add( A );
		junctions.add( B );
		junctions.add( C );
		junctions.add( D );
		junctions.add( E );
		junctions.add( F );

		final MembranePart ab = junctionGraph.addEdge( A, B ).init();
		final MembranePart bc = junctionGraph.addEdge( B, C ).init();
		final MembranePart cd = junctionGraph.addEdge( C, D ).init();
		final MembranePart de = junctionGraph.addEdge( D, E ).init();
		final MembranePart ef = junctionGraph.addEdge( E, F ).init();
		final MembranePart fa = junctionGraph.addEdge( F, A ).init();
		membranes.add( ab );
		membranes.add( bc );
		membranes.add( cd );
		membranes.add( ab );
		membranes.add( de );
		membranes.add( ef );
		membranes.add( fa );

		final Junction G = junctionGraph.addVertex().init( 0, new double[] { 30, 0 } );
		final Junction H = junctionGraph.addVertex().init( 0, new double[] { 40, 10 } );
		final Junction I = junctionGraph.addVertex().init( 0, new double[] { 40, 20 } );
		final Junction J = junctionGraph.addVertex().init( 0, new double[] { 30, 30 } );
		junctions.add( G );
		junctions.add( H );
		junctions.add( I );
		junctions.add( J );

		final MembranePart bg = junctionGraph.addEdge( B, G ).init();
		final MembranePart gh = junctionGraph.addEdge( G, H ).init();
		final MembranePart hi = junctionGraph.addEdge( H, I ).init();
		final MembranePart ij = junctionGraph.addEdge( I, J ).init();
		final MembranePart jc = junctionGraph.addEdge( J, C ).init();
		membranes.add( bg );
		membranes.add( gh );
		membranes.add( hi );
		membranes.add( ij );
		membranes.add( jc );

		final Junction K = junctionGraph.addVertex().init( 0, new double[] { 70, -10 } );
		final Junction L = junctionGraph.addVertex().init( 0, new double[] { 80, 10 } );
		final Junction M = junctionGraph.addVertex().init( 0, new double[] { 80, 20 } );
		final Junction N = junctionGraph.addVertex().init( 0, new double[] { 70, 30 } );
		junctions.add( K );
		junctions.add( L );
		junctions.add( M );
		junctions.add( N );

		final MembranePart hk = junctionGraph.addEdge( H, K ).init();
		final MembranePart kl = junctionGraph.addEdge( K, L ).init();
		final MembranePart lm = junctionGraph.addEdge( L, M ).init();
		final MembranePart mn = junctionGraph.addEdge( M, N ).init();
		final MembranePart ni = junctionGraph.addEdge( N, I ).init();
		membranes.add( hk );
		membranes.add( kl );
		membranes.add( lm );
		membranes.add( mn );
		membranes.add( ni );

		final CellModel cellModel = FindFaces.findFaces( junctionModel );
		return new GraphBundle( cellModel, junctionModel, junctions, membranes );
	}

	public static final void kickstart( final JunctionModel junctionModel, final CellModel cellModel )
	{
		final Context context = new Context();
		final LeviathanWM wm = new LeviathanWM( context );
		try
		{
			wm.setImagePath( LeviathanWM.dummyBdvData( junctionModel.getGraph() ) );
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
		wm.setJunctionModel( junctionModel );
		wm.setCellModel( cellModel );
		new LeviathanMainWindow( wm ).setVisible( true );
	}
}
