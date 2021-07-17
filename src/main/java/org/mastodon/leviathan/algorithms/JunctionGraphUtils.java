package org.mastodon.leviathan.algorithms;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;

import net.imglib2.RealLocalizable;

public class JunctionGraphUtils
{

	public static final < V extends Vertex< E >, E extends Edge< V > > V vertexAcross( final E edge, final V vertex, final V ref )
	{
		final V source = edge.getSource( ref );
		if ( !source.equals( vertex ) )
			return source;

		return edge.getTarget( ref );
	}

	public static final < V extends Vertex< E > & RealLocalizable, E extends Edge< V > > double angle(
			final V pivot,
			final E v1,
			final E v2,
			final V vref1,
			final V vref2 )
	{
		final V s1 = vertexAcross( v1, pivot, vref1 );
		final double dx1 = pivot.getDoublePosition( 0 ) - s1.getDoublePosition( 0 );
		final double dy1 = pivot.getDoublePosition( 1 ) - s1.getDoublePosition( 1 );
		final double alpha1 = Math.atan2( dy1, dx1 );

		final V t2 = vertexAcross( v2, pivot, vref2 );
		final double dx2 = t2.getDoublePosition( 0 ) - pivot.getDoublePosition( 0 );
		final double dy2 = t2.getDoublePosition( 1 ) - pivot.getDoublePosition( 1 );
		final double alpha2 = Math.atan2( dy2, dx2 );

		// Normalize from -pi to pi;
		double theta = alpha2 - alpha1;
		theta = theta - 2. * Math.PI * Math.floor( ( theta + Math.PI ) / ( 2. * Math.PI ) );
		return theta;
	}

	private JunctionGraphUtils()
	{}
}
