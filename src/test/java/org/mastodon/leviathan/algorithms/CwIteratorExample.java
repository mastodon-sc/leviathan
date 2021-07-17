package org.mastodon.leviathan.algorithms;

import org.mastodon.leviathan.TestUtils;
import org.mastodon.leviathan.TestUtils.GraphBundle;
import org.mastodon.leviathan.app.LeviathanWM;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class CwIteratorExample
{

	public static void main( final String[] args ) throws SpimDataException
	{
		final GraphBundle bundle = TestUtils.getExampleGraph();
		final JunctionModel model = bundle.junctionModel;
		final MembranePart bc = bundle.membranes.get( 1 );

		final LeviathanWM wm = new LeviathanWM( new Context() );
		wm.setJunctionModel( model );
		wm.setImagePath( LeviathanWM.dummyBdvData( model.getGraph() ) );

		final FaceIteratorGen< Junction, MembranePart > itgen = new FaceIteratorGen<>( model.getGraph() );
		System.out.println( "Clockwise:" );
		FaceIteratorGen< Junction, MembranePart >.FaceIterator it = itgen.iterateCW( bc );
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

		TestUtils.kickstart( model, null );
	}
}
