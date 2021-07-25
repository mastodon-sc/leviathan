package org.mastodon.leviathan.algorithms;

import org.mastodon.leviathan.TestUtils;
import org.mastodon.leviathan.TestUtils.GraphBundle;
import org.mastodon.leviathan.app.LeviathanMainWindow;
import org.mastodon.leviathan.app.LeviathanWM;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class SplitMergeCellExample
{
	public static void main( final String[] args ) throws SpimDataException
	{
		final GraphBundle bundle = TestUtils.getExampleGraph();
		final JunctionModel junctionModel = bundle.junctionModel;
		final CellModel cellModel = bundle.cellModel;

		final FindFaces faceFinder = FindFaces.create( junctionModel.getGraph(), cellModel.getGraph() );
		final MembranePart eref = junctionModel.getGraph().edgeRef();

		// Link A to D.
		final Junction vA = bundle.junctions.get( 0 );
		final Junction vD = bundle.junctions.get( 3 );
		faceFinder.split( vA, vD, eref );

		final LeviathanWM wm = new LeviathanWM( new Context() );
		wm.setImagePath( LeviathanWM.dummyBdvData( junctionModel.getGraph() ) );
		wm.setJunctionModel( junctionModel );
		wm.setCellModel( cellModel );

		new LeviathanMainWindow( wm ).setVisible( true );
	}
}
