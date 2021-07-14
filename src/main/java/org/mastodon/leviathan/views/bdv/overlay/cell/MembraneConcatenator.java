package org.mastodon.leviathan.views.bdv.overlay.cell;

import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.MembranePart;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayEdgeWrapper;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayGraphWrapper;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayVertexWrapper;
import org.scijava.util.DoubleArray;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

public class MembraneConcatenator
{

	private final JunctionOverlayGraphWrapper< Junction, MembranePart > graph;

	private final double[] posSource = new double[ 3 ];

	private final double[] posTarget = new double[ 3 ];

	private final double[] vPos = new double[ 3 ];

	private final DoubleArray tmp;

	private DoubleArray out;

	private AffineTransform3D transform;

	private boolean initialized;

	private DoubleArray first;

	private final RealPoint A1 = new RealPoint( 2 );

	private final RealPoint A2 = new RealPoint( 2 );

	private final RealPoint B1 = new RealPoint( 2 );

	private final RealPoint B2 = new RealPoint( 2 );

	private final JunctionOverlayEdgeWrapper< Junction, MembranePart > eref;

	private final JunctionOverlayVertexWrapper< Junction, MembranePart > vref1;

	private final JunctionOverlayVertexWrapper< Junction, MembranePart > vref2;

	public MembraneConcatenator( final JunctionOverlayGraphWrapper< Junction, MembranePart > graph )
	{
		this.graph = graph;
		this.eref = graph.edgeRef();
		this.vref1 = graph.vertexRef();
		this.vref2 = graph.vertexRef();
		this.tmp = new DoubleArray();
	}

	public void init( final DoubleArray arr, final AffineTransform3D transform )
	{
		arr.clear();
		this.out = arr;
		this.transform = transform;
		this.initialized = false;
		this.first = null;
	}

	public void cat( final int mbid )
	{
		if ( !initialized )
		{
			if ( first == null )
			{
				this.first = new DoubleArray( getPos( mbid ).copyArray() );
				return;
			}

			// Concatenate first and second in proper order.
			getBegin( first, A1 );
			getEnd( first, A2 );
			final DoubleArray second = getPos( mbid );
			getBegin( second, B1 );
			getEnd( second, B2 );

			final double a1b1 = sqDistance( A1, B1 );
			final double a1b2 = sqDistance( A1, B2 );
			final double a2b1 = sqDistance( A2, B1 );
			final double a2b2 = sqDistance( A2, B2 );
			final double smallest = Math.min( a1b1, Math.min( a1b2, Math.min( a2b1, a2b2 ) ) );

			if ( smallest == a1b1 )
			{
				// Reverse first then cat.
				reverse( first );
			}
			else if ( smallest == a1b2 )
			{
				// Reverse both then cat.
				reverse( first );
				reverse( second );
			}
			else if ( smallest == a2b1 )
			{
				// They already are in proper order.
			}
			else
			{
				// Reverse second.
				reverse( second );
			}
			cat( out, first );
			cat( out, second );
			initialized = true;
			return;
		}

		getEnd( out, A2 );
		final DoubleArray toadd = getPos( mbid );
		getBegin( toadd, B1 );
		getEnd( toadd, B2 );

		final double a2b1 = sqDistance( A2, B1 );
		final double a2b2 = sqDistance( A2, B2 );

		if ( a2b2 < a2b1 ) // Reverse new one.
			reverse( toadd );
		cat( out, toadd );

	}

	private static final void cat( final DoubleArray arr1, final DoubleArray arr2 )
	{
		for ( int i = 0; i < arr2.size(); i++ )
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

	private static final void getEnd( final DoubleArray arr, final RealPoint p )
	{
		p.setPosition( arr.getValue( arr.size() - 2 ), 0 );
		p.setPosition( arr.getValue( arr.size() - 1 ), 1 );
	}

	private static final void getBegin( final DoubleArray arr, final RealPoint p )
	{
		p.setPosition( arr.getValue( 0 ), 0 );
		p.setPosition( arr.getValue( 1 ), 1 );
	}

	private final DoubleArray getPos( final int mbid )
	{
		tmp.clear();
		final JunctionOverlayEdgeWrapper< Junction, MembranePart > mb = graph.getEdgePool().getObject( mbid, eref );
		final JunctionOverlayVertexWrapper< Junction, MembranePart > source = mb.getSource( vref1 );
		final JunctionOverlayVertexWrapper< Junction, MembranePart > target = mb.getTarget( vref2 );

		source.localize( posSource );
		transform.apply( posSource, vPos );

		final double[] increments = mb.getPixels();
		if ( increments == null )
		{
			tmp.addValue( vPos[ 0 ] );
			tmp.addValue( vPos[ 1 ] );
			target.localize( posTarget );
			transform.apply( posTarget, vPos );
			tmp.addValue( vPos[ 0 ] );
			tmp.addValue( vPos[ 1 ] );
			return tmp;
		}

		for ( int i = 0; i < increments.length; i = i + 2 )
		{
			final double incx = increments[ i ];
			final double incy = increments[ i + 1 ];
			posTarget[ 0 ] = posSource[ 0 ] + incx;
			posTarget[ 1 ] = posSource[ 1 ] + incy;
			transform.apply( posTarget, vPos );

			tmp.addValue( vPos[ 0 ] );
			tmp.addValue( vPos[ 1 ] );

			posSource[ 0 ] = posTarget[ 0 ];
			posSource[ 1 ] = posTarget[ 1 ];
		}
		return tmp;
	}
}
