package org.mastodon.leviathan.algorithms;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.leviathan.model.cell.Cell;

import net.imglib2.RealLocalizable;

public class GraphUtils
{

	public static final double signedArea( final Cell cell )
	{
		return signedArea( cell.getBoundary() );
	}

	public static final double signedArea( final double[] boundary )
	{
		double a = 0.0;
		for ( int i = 0; i < boundary.length - 3; i = i + 2 )
		{
			final double x0 = boundary[ i ];
			final double y0 = boundary[ i + 1 ];
			final double x1 = boundary[ i + 2 ];
			final double y1 = boundary[ i + 3 ];
			a += x0 * y1 - x1 * y0;
		}
		final double x0 = boundary[ 0 ];
		final double y0 = boundary[ 1 ];
		final double x1 = boundary[ boundary.length - 2 ];
		final double y1 = boundary[ boundary.length - 1 ];

		return ( a + x1 * y0 - x0 * y1 ) / 2.0;
	}

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

	private GraphUtils()
	{}
}
