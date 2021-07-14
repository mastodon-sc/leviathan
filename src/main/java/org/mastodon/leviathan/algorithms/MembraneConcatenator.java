package org.mastodon.leviathan.algorithms;

import org.mastodon.collection.RefList;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.MembranePart;
import org.scijava.util.DoubleArray;

import net.imglib2.RealPoint;

public class MembraneConcatenator
{

	public void getCentroid(
			final RefList< MembranePart > face,
			final double[] centroid,
			final Junction vref1,
			final Junction vref2 )
	{
		centroid[ 0 ] = 0.;
		centroid[ 1 ] = 0.;
		for ( final MembranePart mb : face )
		{
			final Junction source = mb.getSource( vref1 );
			centroid[ 0 ] += source.getDoublePosition( 0 );
			centroid[ 1 ] += source.getDoublePosition( 1 );
			final Junction target = mb.getTarget( vref2 );
			centroid[ 0 ] += target.getDoublePosition( 0 );
			centroid[ 1 ] += target.getDoublePosition( 1 );
		}
		centroid[ 0 ] /= ( 2. * face.size() );
		centroid[ 1 ] /= ( 2. * face.size() );
	}

	public double[] getBoundary( final RefList< MembranePart > face, final double[] centroid )
	{
		// Build pixel array.
		final DoubleArray arr = new DoubleArray();
		final DoubleArray tmp = new DoubleArray();
		final RealPoint p1 = new RealPoint( 2 );
		final RealPoint p2 = new RealPoint( 2 );
		final RealPoint p3 = new RealPoint( 2 );
		final RealPoint p4 = new RealPoint( 2 );
		boolean initialized = false;
		for ( final MembranePart mb : face )
		{
			if ( arr.isEmpty() )
			{
				final double[] pixels = mb.getPixels();
				for ( int i = 0; i < pixels.length; i++ )
					arr.addValue( pixels[ i ] );
				continue;
			}

			if ( !initialized )
			{
				// Concatenate first and second in proper order.
				getBegin( arr, p1 );
				getEnd( arr, p2 );
				copyPixelsTo( mb, tmp );
				getBegin( tmp, p3 );
				getEnd( tmp, p4 );

				final double a1b1 = sqDistance( p1, p3 );
				final double a1b2 = sqDistance( p1, p4 );
				final double a2b1 = sqDistance( p2, p3 );
				final double a2b2 = sqDistance( p2, p4 );
				final double smallest = Math.min( a1b1, Math.min( a1b2, Math.min( a2b1, a2b2 ) ) );

				if ( smallest == a1b1 )
				{
					// Reverse first then cat.
					reverse( arr );
				}
				else if ( smallest == a1b2 )
				{
					// Reverse both then cat.
					reverse( arr );
					reverse( tmp );
				}
				else if ( smallest == a2b1 )
				{
					// They already are in proper order.
				}
				else
				{
					// Reverse second.
					reverse( tmp );
				}
				cat( arr, tmp, p1, p2 );
				initialized = true;
				continue;
			}

			getEnd( arr, p2 );
			copyPixelsTo( mb, tmp );

			getBegin( tmp, p3 );
			getEnd( tmp, p4 );
			final double a2b1 = sqDistance( p2, p3 );
			final double a2b2 = sqDistance( p2, p4 );

			if ( a2b2 < a2b1 ) // Reverse new one.
				reverse( tmp );
			cat( arr, tmp, p1, p2 );
		}

		final double[] out = arr.copyArray();
		for ( int i = 0; i < out.length; i = i + 2 )
		{
			out[ i ] -= centroid[ 0 ];
			out[ i + 1 ] -= centroid[ 1 ];
		}
		return out;
	}

	private static final void cat(
			final DoubleArray arr1,
			final DoubleArray arr2,
			final RealPoint p1,
			final RealPoint p2 )
	{
		// Don't add first point if equals to the last one.
		getEnd( arr1, p1 );
		getBegin( arr2, p2 );
		final int start = p1.equals( p2 ) ? 2 : 0;

		for ( int i = start; i < arr2.size(); i++ )
			arr1.addValue( arr2.getValue( i ) );
	}

	private static final void reverse( final DoubleArray arr )
	{
		for ( int i = 0; i < arr.size() / 2; i = i + 2 )
		{
			final double tempX = arr.getValue( i );
			final double tempY = arr.getValue( i + 1 );
			arr.setValue( i, arr.getValue( arr.size() - i - 2 ) );
			arr.setValue( i + 1, arr.getValue( arr.size() - i - 1 ) );
			arr.setValue( arr.size() - i - 2, tempX );
			arr.setValue( arr.size() - i - 1, tempY );
		}
	}

	private static final double sqDistance( final RealPoint p1, final RealPoint p2 )
	{
		final double dx = p2.getDoublePosition( 0 ) - p1.getDoublePosition( 0 );
		final double dy = p2.getDoublePosition( 1 ) - p1.getDoublePosition( 1 );
		return dx * dx + dy * dy;
	}

	private static void getEnd( final DoubleArray arr, final RealPoint p )
	{
		p.setPosition( arr.getValue( arr.size() - 2 ), 0 );
		p.setPosition( arr.getValue( arr.size() - 1 ), 1 );
	}

	private static void getBegin( final DoubleArray arr, final RealPoint p )
	{
		p.setPosition( arr.getValue( 0 ), 0 );
		p.setPosition( arr.getValue( 1 ), 1 );
	}

	private static final void copyPixelsTo( final MembranePart mb, final DoubleArray tmp )
	{
		tmp.clear();
		final double[] pixels = mb.getPixels();
		if ( pixels == null )
		{
			final Junction source = mb.getSource();
			tmp.addValue( source.getDoublePosition( 0 ) );
			tmp.addValue( source.getDoublePosition( 1 ) );
			final Junction target = mb.getTarget();
			tmp.addValue( target.getDoublePosition( 0 ) );
			tmp.addValue( target.getDoublePosition( 1 ) );
			return;
		}

		for ( final double d : pixels )
			tmp.addValue( d );
	}
}
