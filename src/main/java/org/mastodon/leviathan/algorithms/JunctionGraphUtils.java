package org.mastodon.leviathan.algorithms;

import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.MembranePart;

public class JunctionGraphUtils
{

	/**
	 * Finds the next edge of the specified junction, which is counter-clockwise
	 * with respect to the specified edge. Of course, the specified edge must be
	 * an edge of the specified junction.
	 * 
	 * @param graph
	 *            the junction graph.
	 * @param edge
	 *            the edge.
	 * @param junction
	 *            the junction.
	 * @param eref
	 *            a reference to be set to the next CCW edge.
	 * @return the next edge counter clock-wise.
	 */
	public static final MembranePart nextCounterClockWiseEdge( final JunctionGraph graph, final MembranePart edge, final Junction junction, final MembranePart eref )
	{
		final Junction ref1 = graph.vertexRef();
		final Junction ref2 = graph.vertexRef();

		final Junction source = junctionAcross( edge, junction, ref1 );
		final double dx0 = source.getDoublePosition( 0 ) - junction.getDoublePosition( 0 );
		final double dy0 = source.getDoublePosition( 1 ) - junction.getDoublePosition( 1 );
		final double alpha0 = Math.atan2( dy0, dx0 );

		double thetaMax = Double.NEGATIVE_INFINITY;
		for ( final MembranePart candidate : junction.edges() )
		{
			if ( candidate.equals( edge ) )
				continue;

			final Junction target = junctionAcross( candidate, junction, ref2 );
			final double dx1 = target.getDoublePosition( 0 ) - junction.getDoublePosition( 0 );
			final double dy1 = target.getDoublePosition( 1 ) - junction.getDoublePosition( 1 );
			final double alpha1 = Math.atan2( dy1, dx1 );
			double theta = ( alpha1 - alpha0 ) % ( 2. * Math.PI );
			if ( theta < 0. )
				theta += 2. * Math.PI;
			if ( theta > thetaMax )
			{
				thetaMax = theta;
				eref.refTo( candidate );
			}
		}

		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
		return eref;
	}

	public static final Junction junctionAcross( final MembranePart edge, final Junction junction, final Junction ref )
	{
		final Junction source = edge.getSource( ref );
		if ( !source.equals( junction ) )
			return source;

		return edge.getTarget( ref );
	}

	private JunctionGraphUtils()
	{}
}
