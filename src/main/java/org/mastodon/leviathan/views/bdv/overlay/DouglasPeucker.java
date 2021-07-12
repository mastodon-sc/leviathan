package org.mastodon.leviathan.views.bdv.overlay;

import org.scijava.util.IntArray;

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

	private static final int getX( final IntArray arr, final int i )
	{
		return arr.getValue( 2 * i );
	}

	private static final int getY( final IntArray arr, final int i )
	{
		return arr.getValue( 2 * i + 1 );
	}

	private static final void add( final IntArray from, final int index, final IntArray to )
	{
		to.addValue( getX( from, index ) );
		to.addValue( getY( from, index ) );
	}

	private static int size( final IntArray list )
	{
		return list.size() / 2;
	}

	private static final void douglasPeucker( final IntArray list, final int s, final int e, final double epsilon, final IntArray resultList )
	{
		// Find the point with the maximum distance
		double dmax = 0;
		int index = 0;

		final int start = s;
		final int end = e - 1;
		for ( int i = start + 1; i < end; i++ )
		{
			// Point
			final double px = getX( list, i );
			final double py = getY( list, i );
			// Start
			final double vx = getX( list, start );
			final double vy = getY( list, start );
			// End
			final double wx = getX( list, end );
			final double wy = getY( list, end );
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
			douglasPeucker( list, s, index, epsilon, resultList );
			douglasPeucker( list, index, e, epsilon, resultList );
		}
		else
		{
			if ( ( end - start ) > 0 )
			{
				add( list, start, resultList );
				add( list, end, resultList );
			}
			else
			{
				add( list, start, resultList );
			}
		}
	}

	public static final void douglasPeucker( final IntArray list, final IntArray resultList, final double epsilon )
	{
		douglasPeucker( list, 0, size( list ), epsilon, resultList );
	}
}
