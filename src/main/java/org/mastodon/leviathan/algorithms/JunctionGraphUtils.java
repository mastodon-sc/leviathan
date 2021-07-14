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
	public static final MembranePart nextCCWEdge( final JunctionGraph graph, final MembranePart edge, final Junction junction, final MembranePart eref )
	{
		return nextEdge( graph, edge, junction, eref, 1 );
	}

	public static final MembranePart nextCWEdge( final JunctionGraph graph, final MembranePart edge, final Junction junction, final MembranePart eref )
	{
		return nextEdge( graph, edge, junction, eref, -1 );
	}

	static final MembranePart nextEdge( final JunctionGraph graph, final MembranePart edge, final Junction junction, final MembranePart eref, final int signCCW )
	{
		final Junction ref1 = graph.vertexRef();
		final Junction ref2 = graph.vertexRef();

		final Junction source = junctionAcross( edge, junction, ref1 );
		final double dx0 = source.getDoublePosition( 0 ) - junction.getDoublePosition( 0 );
		final double dy0 = source.getDoublePosition( 1 ) - junction.getDoublePosition( 1 );
		final double alpha0 = Math.atan2( dy0, dx0 );

		double thetaBound = signCCW > 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		for ( final MembranePart candidate : junction.edges() )
		{
			if ( candidate.equals( edge ) )
				continue;

			final Junction target = junctionAcross( candidate, junction, ref2 );
			final double dx1 = target.getDoublePosition( 0 ) - junction.getDoublePosition( 0 );
			final double dy1 = target.getDoublePosition( 1 ) - junction.getDoublePosition( 1 );
			final double alpha1 = Math.atan2( dy1, dx1 );
			double theta = ( ( alpha1 - alpha0 ) ) % ( 2. * Math.PI );
			if ( theta < 0. )
				theta += 2. * Math.PI;
			if ( signCCW > 0 ? theta > thetaBound : theta < thetaBound )
			{
				thetaBound = theta;
				eref.refTo( candidate );
			}
		}

		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
		return eref;
	}

	/**
	 * Returns the junction of an edge which is at the top (lowest Y value).
	 * 
	 * @param edge
	 *            the edge.
	 * @param ref
	 *            a {@link Junction} reference.
	 * @return the top junction.
	 */
	public static final Junction topJunction( final MembranePart edge, final Junction ref )
	{
		final double ys = edge.getSource( ref ).getDoublePosition( 1 );
		final double yt = edge.getTarget( ref ).getDoublePosition( 1 );
		if ( yt < ys )
			return ref;
		else
			return edge.getSource( ref );
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
