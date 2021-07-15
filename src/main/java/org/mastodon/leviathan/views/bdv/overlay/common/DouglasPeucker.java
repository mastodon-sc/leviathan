package org.mastodon.leviathan.views.bdv.overlay.common;

import org.scijava.util.DoubleArray;

public class DouglasPeucker
{

	private static final double sqr( final double x )
	{
		return x * x;
	}

	private static final double distanceBetweenPoints( final double vx, final double vy, final double wx, final double wy )
	{
		return sqr( vx - wx ) + sqr( vy - wy );
	}

	private static final double distanceToSegmentSquared( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		final double l2 = distanceBetweenPoints( vx, vy, wx, wy );
		if ( l2 == 0 )
			return distanceBetweenPoints( px, py, vx, vy );
		final double t = ( ( px - vx ) * ( wx - vx ) + ( py - vy ) * ( wy - vy ) ) / l2;
		if ( t < 0 )
			return distanceBetweenPoints( px, py, vx, vy );
		if ( t > 1 )
			return distanceBetweenPoints( px, py, wx, wy );
		return distanceBetweenPoints( px, py, ( vx + t * ( wx - vx ) ), ( vy + t * ( wy - vy ) ) );
	}

	private static final double perpendicularDistance( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		return Math.sqrt( distanceToSegmentSquared( px, py, vx, vy, wx, wy ) );
	}

	private static final double getX( final DoubleArray arr, final int i )
	{
		return arr.getValue( 2 * i );
	}

	private static final double getY( final DoubleArray arr, final int i )
	{
		return arr.getValue( 2 * i + 1 );
	}

	private static final void add( final DoubleArray from, final int index, final DoubleArray to )
	{
		to.addValue( getX( from, index ) );
		to.addValue( getY( from, index ) );
	}

	private static int size( final DoubleArray list )
	{
		return list.size() / 2;
	}

	private static final void douglasPeucker( final DoubleArray input, final int s, final int e, final double epsilon, final DoubleArray output )
	{
		// Find the point with the maximum distance
		double dmax = 0;
		int index = 0;

		final int start = s;
		final int end = e - 1;
		for ( int i = start + 1; i < end; i++ )
		{
			// Point
			final double px = getX( input, i );
			final double py = getY( input, i );
			// Start
			final double vx = getX( input, start );
			final double vy = getY( input, start );
			// End
			final double wx = getX( input, end );
			final double wy = getY( input, end );
			final double d = perpendicularDistance( px, py, vx, vy, wx, wy );
			if ( d > dmax )
			{
				index = i;
				dmax = d;
			}
		}
		// If max distance is greater than epsilon, recursively simplify
		if ( dmax > epsilon )
		{
			// Recursive call
			douglasPeucker( input, s, index, epsilon, output );
			douglasPeucker( input, index, e, epsilon, output );
		}
		else
		{
			if ( ( end - start ) > 0 )
			{
				add( input, start, output );
				add( input, end, output );
			}
			else
			{
				add( input, start, output );
			}
		}
	}

	public static final void douglasPeucker( final DoubleArray input, final DoubleArray output, final double epsilon )
	{
		douglasPeucker( input, 0, size( input ), epsilon, output );
	}
}
