/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.leviathan.views.bdv.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.util.GeometryUtil;
import org.mastodon.views.bdv.overlay.OverlayContext;
import org.mastodon.views.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.views.bdv.overlay.util.BdvRendererUtil;
import org.scijava.util.IntArray;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class JunctionOverlayGraphRenderer< V extends JunctionOverlayVertex< V, E >, E extends JunctionOverlayEdge< E, V > >
		implements OverlayGraphRenderer< V, E >
{

	private int width;

	private int height;

	private final AffineTransform3D renderTransform;

	private int renderTimepoint;

	private final JunctionOverlayGraph< V, E > graph;

	private final SpatioTemporalIndex< V > index;

	private final HighlightModel< V, E > highlight;

	private final FocusModel< V, E > focus;

	private final SelectionModel< V, E > selection;

	private final GraphColorGenerator< V, E > coloring;

	private JunctionRenderSettings settings;

	public JunctionOverlayGraphRenderer(
			final JunctionOverlayGraph< V, E > graph,
			final HighlightModel< V, E > highlight,
			final FocusModel< V, E > focus,
			final SelectionModel< V, E > selection,
			final GraphColorGenerator< V, E > coloring )
	{
		this.graph = graph;
		this.highlight = highlight;
		this.focus = focus;
		this.selection = selection;
		this.coloring = coloring;
		index = graph.getIndex();
		renderTransform = new AffineTransform3D();
		setRenderSettings( JunctionRenderSettings.defaultStyle() );
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized ( renderTransform )
		{
			renderTransform.set( transform );
		}
	}

	@Override
	public void timePointChanged( final int timepoint )
	{
		renderTimepoint = timepoint;
	}

	public void setRenderSettings( final JunctionRenderSettings settings )
	{
		this.settings = settings;
	}

	public static final double pointRadius = 2.5;

	private static int trunc255( final int i )
	{
		return Math.min( 255, Math.max( 0, i ) );
	}

	private static int truncRGBA( final int r, final int g, final int b, final int a )
	{
		return ARGBType.rgba(
				trunc255( r ),
				trunc255( g ),
				trunc255( b ),
				trunc255( a ) );
	}

	private static int truncRGBA( final double r, final double g, final double b, final double a )
	{
		return truncRGBA(
				( int ) ( 255 * r ),
				( int ) ( 255 * g ),
				( int ) ( 255 * b ),
				( int ) ( 255 * a ) );
	}

	private static final int complementaryColor( final int color )
	{
		return 0xff000000 | ~color;
	}

	/**
	 * Generates a color suitable to paint an abject that might be away from the
	 * focus plane, or away in time.
	 *
	 * @param sd
	 *            sliceDistande, between -1 and 1. see
	 *            {@link #sliceDistance(double, double)}.
	 * @param td
	 *            timeDistande, between -1 and 1. see
	 *            {@link #timeDistance(double, double, double)}.
	 * @param sdFade
	 *            between 0 and 1, from which |sd| value color starts to fade
	 *            (alpha value decreases).
	 * @param tdFade
	 *            between 0 and 1, from which |td| value color starts to fade
	 *            (alpha value decreases).
	 * @param isSelected
	 *            whether to use selected or un-selected color scheme.
	 * @param color
	 *            the color assigned to the object when using a coloring scheme.
	 * @return vertex/edge color suitable for display in a BDV.
	 */
	private static Color getColor(
			final boolean isSelected,
			final boolean isHighlighted,
			final int colorSpot,
			final int color )
	{

		if ( color == 0 )
		{
			// No coloring. Color are set by the RenderSettings.
			final int colorFrom = isSelected
					? complementaryColor( colorSpot )
					: colorSpot;
			final int r = ( colorFrom >> 16 ) & 0xff;
			final int g = ( colorFrom >> 8 ) & 0xff;
			final int b = ( colorFrom ) & 0xff;
			final double a = Math.max(
					isHighlighted
							? 0.8
							: ( isSelected ? 0.6 : 0.4 ),
					1. );
			return new Color( truncRGBA( r / 255., g / 255., b / 255., a ), true );
		}
		else
		{
			/*
			 * Use some default coloring when selected. The same than when we
			 * have no coloring. There is a chance that this color is confused
			 * with a similar color in the ColorMap then.
			 */
			final int r = isSelected ? 255 : ( ( color >> 16 ) & 0xff );
			final int g = isSelected ? 0 : ( ( color >> 8 ) & 0xff );
			final int b = isSelected ? 25 : ( ( color ) & 0xff );
			final double a = Math
					.max(
							isHighlighted
									? 0.8
									: ( isSelected ? 0.6 : 0.4 ),
							1. );
			return new Color( truncRGBA( r / 255., g / 255., b / 255., a ), true );
		}
	}

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * range that is large enough border to ensure that it contains center of
	 * every ellipsoid touching the specified coordinate range.
	 *
	 * @param xMin
	 *            minimum X position on the z=0 plane in viewer coordinates.
	 * @param xMax
	 *            maximum X position on the z=0 plane in viewer coordinates.
	 * @param yMin
	 *            minimum Y position on the z=0 plane in viewer coordinates.
	 * @param yMax
	 *            maximum Y position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getOverlappingPolytopeGlobal(
			final double xMin,
			final double xMax,
			final double yMin,
			final double yMax,
			final AffineTransform3D transform,
			final int timepoint )
	{
//		final double globalToViewerScale = Affine3DHelpers.extractScale( transform, 0 );
		final double border = 0.; // TODO
		return BdvRendererUtil.getPolytopeGlobal( transform,
				xMin - border, xMax + border,
				yMin - border, yMax + border,
				-border, border );
	}

	/**
	 * Get a copy of the {@code renderTransform} (avoids synchronizing on it for
	 * a longer time period).
	 *
	 * @return a copy of the {@code renderTransform}.
	 */
	private AffineTransform3D getRenderTransformCopy()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		synchronized ( renderTransform )
		{
			transform.set( renderTransform );
		}
		return transform;
	}

	/**
	 * Get the {@link ConvexPolytope} bounding the visible region of global
	 * space, extended by a large enough border to ensure that it contains the
	 * center of every ellipsoid that intersects the visible volume.
	 *
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getVisiblePolytopeGlobal(
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( 0, width, 0, height, transform, timepoint );
	}

	private interface EdgeOperation< E, V >
	{
		void apply( final E edge, V source, V target );
	}

	private void forEachVisibleEdge(
			final AffineTransform3D transform,
			final int currentTimepoint,
			final EdgeOperation< E, V > edgeOperation )
	{
		if ( !settings.getDrawLinks() )
			return;

		final V refSource = graph.vertexRef();
		final V refTarget = graph.vertexRef();
		final SpatialIndex< V > si = index.getSpatialIndex( currentTimepoint );
		final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
		ccp.clip( getVisiblePolytopeGlobal( transform, currentTimepoint ) );
		for ( final V vertex : ccp.getInsideValues() )
		{
			for ( final E edge : vertex.incomingEdges() )
			{
				final V source = edge.getSource( refSource );
				edgeOperation.apply( edge, source, vertex );
			}
		}
		graph.releaseRef( refSource );
		graph.releaseRef( refTarget );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final BasicStroke defaultVertexStroke = new BasicStroke();
		final BasicStroke defaultEdgeStroke = new BasicStroke();
		final BasicStroke highlightedEdgeStroke = new BasicStroke( 3f );

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final Object antialiasing = settings.getUseAntialiasing()
				? RenderingHints.VALUE_ANTIALIAS_ON
				: RenderingHints.VALUE_ANTIALIAS_OFF;
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasing );

		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		final E ref3 = graph.edgeRef();

		final int colorSpot = settings.getColorSpot();

		graph.getLock().readLock().lock();
		index.readLock().lock();
		try
		{
			if ( settings.getDrawLinks() )
			{
				final E highlighted = highlight.getHighlightedEdge( ref3 );
				graphics.setStroke( defaultEdgeStroke );

				final IntArray arr = new IntArray();
				final IntArray tmp = new IntArray();
				forEachVisibleEdge( transform, currentTimepoint, ( edge, source, target ) -> {

					final boolean isHighlighted = edge.equals( highlighted );

					final int edgeColor = coloring.color( edge, source, target );
					graphics.setPaint( getColor(
							selection.isSelected( edge ),
							isHighlighted,
							colorSpot,
							edgeColor ) );

					edgeLines( edge, source, target, transform, arr, tmp );
					int xf = arr.getValue( 0 );
					int yf = arr.getValue( 1 );

					if ( isHighlighted )
						graphics.setStroke( highlightedEdgeStroke );
					for ( int i = 2; i < arr.size(); i = i + 2 )
					{
						final int xt = arr.getValue( i );
						final int yt = arr.getValue( i + 1 );
						graphics.drawLine( xf, yf, xt, yt );
//						graphics.drawRect( xt - 1, yt - 1, 3, 3 );
						xf = xt;
						yf = yt;
					}
					if ( isHighlighted )
						graphics.setStroke( defaultEdgeStroke );
				} );
			}

			if ( settings.getDrawSpots() )
			{
				final V highlighted = highlight.getHighlightedVertex( ref1 );
				final V focused = focus.getFocusedVertex( ref2 );

				graphics.setStroke( defaultVertexStroke );

				final ConvexPolytope cropPolytopeGlobal = getVisiblePolytopeGlobal( transform, currentTimepoint );
				final ClipConvexPolytope< V > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
				ccp.clip( cropPolytopeGlobal );

				final double[] pos = new double[ 3 ];
				final double[] vPos = new double[ 3 ];

				for ( final V vertex : ccp.getInsideValues() )
				{
					final int color = coloring.color( vertex );
					final boolean isHighlighted = vertex.equals( highlighted );
					final boolean isFocused = vertex.equals( focused );

					vertex.localize( pos );
					transform.apply( pos, vPos );
					final double x = vPos[ 0 ];
					final double y = vPos[ 1 ];

					graphics.setColor( getColor(
							selection.isSelected( vertex ),
							isHighlighted,
							colorSpot,
							color ) );
					double radius = pointRadius;
					if ( isHighlighted || isFocused )
						radius *= 3;
					final int ox = ( int ) ( x - radius );
					final int oy = ( int ) ( y - radius );
					final int ow = ( int ) ( 2 * radius );
					if ( isFocused )
						graphics.fillRect( ox, oy, ow, ow );
					else
						graphics.fillOval( ox, oy, ow, ow );
				}
			}
		}
		finally
		{
			graph.getLock().readLock().unlock();
			index.readLock().unlock();
		}
		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
		graph.releaseRef( ref3 );
	}

	private final static < V extends JunctionOverlayVertex< V, E >, E extends JunctionOverlayEdge< E, V > > void edgeLines(
			final E edge,
			final V source,
			final V target,
			final AffineTransform3D transform,
			final IntArray arr,
			final IntArray tmp )
	{
		tmp.clear();
		arr.clear();
		final double[] posSource = new double[ 3 ];
		final double[] posTarget = new double[ 3 ];
		final double[] vPosSource = new double[ 3 ];
		final double[] vPosTarget = new double[ 3 ];

		source.localize( posSource );
		transform.apply( posSource, vPosSource );

		final double[] increments = edge.getPixels();
		for ( int i = 0; i < increments.length; i = i + 2 )
		{
			final double incx = increments[ i ];
			final double incy = increments[ i + 1 ];
			posTarget[ 0 ] = posSource[ 0 ] + incx;
			posTarget[ 1 ] = posSource[ 1 ] + incy;
			transform.apply( posTarget, vPosTarget );

			tmp.addValue( ( int ) vPosTarget[ 0 ] );
			tmp.addValue( ( int ) vPosTarget[ 1 ] );

			posSource[ 0 ] = posTarget[ 0 ];
			posSource[ 1 ] = posTarget[ 1 ];
			vPosSource[ 0 ] = vPosTarget[ 0 ];
			vPosSource[ 1 ] = vPosTarget[ 1 ];
		}

		DouglasPeucker.douglasPeucker( tmp, arr, 1. );
	}

	/**
	 * Returns the edge currently painted close to the specified location.
	 * <p>
	 * It is the responsibility of the caller to lock the graph it inspects for
	 * reading operations, prior to calling this method. A typical call from
	 * another method would happen like this:
	 *
	 * <pre>
	 * ReentrantReadWriteLock lock = graph.getLock();
	 * lock.readLock().lock();
	 * boolean found = false;
	 * try
	 * {
	 * 	E edge = renderer.getEdgeAt( x, y, EDGE_SELECT_DISTANCE_TOLERANCE, ref )
	 * 	... // do something with the edge
	 * 	... // edge is guaranteed to stay valid while the lock is held
	 * }
	 * finally
	 * {
	 * 	lock.readLock().unlock();
	 * }
	 * </pre>
	 *
	 * @param x
	 *            the x location to search, in viewer coordinates (screen).
	 * @param y
	 *            the y location to search, in viewer coordinates (screen).
	 * @param tolerance
	 *            the distance tolerance to accept close edges.
	 * @param ref
	 *            an edge reference, that might be used to return the vertex
	 *            found.
	 * @return the closest edge within tolerance, or <code>null</code> if it
	 *         could not be found.
	 */
	@Override
	public E getEdgeAt( final int x, final int y, final double tolerance, final E ref )
	{
		if ( !settings.getDrawLinks() )
			return null;

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;
		final IntArray arr = new IntArray();
		final IntArray tmp = new IntArray();

		class Op implements EdgeOperation< E, V >
		{
			final double squTolerance = tolerance * tolerance;

			double bestSquDist = Double.POSITIVE_INFINITY;

			boolean found = false;

			@Override
			public void apply( final E edge, final V source, final V target )
			{
				edgeLines( edge, source, target, transform, arr, tmp );
				int xf = arr.getValue( 0 );
				int yf = arr.getValue( 1 );

				for ( int i = 2; i < arr.size(); i = i + 2 )
				{
					final int xt = arr.getValue( i );
					final int yt = arr.getValue( i + 1 );
					final double squDist = GeometryUtil.squSegmentDist( x, y, xf, yf, xt, yt );
					if ( squDist <= squTolerance && squDist < bestSquDist )
					{
						found = true;
						bestSquDist = squDist;
						ref.refTo( edge );
						break;
					}
					xf = xt;
					yf = yt;
				}
			}
		};
		final Op op = new Op();

		index.readLock().lock();
		try
		{
			forEachVisibleEdge( transform, currentTimepoint, op );
		}
		finally
		{
			index.readLock().unlock();
		}
		return op.found ? ref : null;
	}

	/**
	 * Transform viewer coordinates to global (world) coordinates.
	 *
	 * @param x
	 *            viewer X coordinate
	 * @param y
	 *            viewer Y coordinate
	 * @param gPos
	 *            receives global coordinates corresponding to viewer
	 *            coordinates <em>(x, y, 0)</em>.
	 */
	@Override
	public void getGlobalPosition( final int x, final int y, final double[] gPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.applyInverse( gPos, new double[] { x, y, 0 } );
		}
	}

	/**
	 * Transform global (world) coordinates to viewer coordinates.
	 *
	 * @param gPos
	 *            global coordinates to transform.
	 * @param vPos
	 *            receives the viewer coordinates.
	 */
	@Override
	public void getViewerPosition( final double[] gPos, final double[] vPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.apply( gPos, vPos );
		}
	}

	@Override
	public int getCurrentTimepoint()
	{
		return renderTimepoint;
	}

	/**
	 * Returns the vertex currently painted close to the specified location.
	 * <p>
	 * It is the responsibility of the caller to lock the graph it inspects for
	 * reading operations, prior to calling this method. A typical call from
	 * another method would happen like this:
	 *
	 * <pre>
	 * ReentrantReadWriteLock lock = graph.getLock();
	 * lock.readLock().lock();
	 * try
	 * {
	 * 	V vertex = renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, ref );
	 * 	... // do something with the vertex
	 * 	... // vertex is guaranteed to stay valid while the lock is held
	 * }
	 * finally
	 * {
	 * 	lock.readLock().unlock();
	 * }
	 * </pre>
	 *
	 * @param x
	 *            the x location to search, in viewer coordinates (screen).
	 * @param y
	 *            the y location to search, in viewer coordinates (screen).
	 * @param tolerance
	 *            the distance tolerance to accept close vertices.
	 * @param ref
	 *            a vertex reference, that might be used to return the vertex
	 *            found.
	 * @return the closest vertex within tolerance, or <code>null</code> if it
	 *         could not be found.
	 */
	@Override
	public V getVertexAt( final int x, final int y, final double tolerance, final V ref )
	{
		if ( !settings.getDrawSpots() )
			return null;

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final double[] lPos = new double[] { x, y, 0 };
		final double[] gPos = new double[ 3 ];
		transform.applyInverse( gPos, lPos );
		// Copy to 2-element array not to break the search.
		final double[] gPos2 = new double[ 2 ];
		for ( int d = 0; d < gPos2.length; d++ )
			gPos2[ d ] = gPos[ d ];

		boolean found = false;
		index.readLock().lock();
		final NearestNeighborSearch< V > nns = index.getSpatialIndex( currentTimepoint ).getNearestNeighborSearch();
		nns.search( RealPoint.wrap( gPos2 ) );
		final V vertex = nns.getSampler().get();
		if ( vertex != null )
		{
			final double[] pos = new double[ 3 ];
			final double[] p = new double[ 3 ];
			vertex.localize( pos );
			transform.apply( pos, p );
			final double dx = p[ 0 ] - x;
			final double dy = p[ 1 ] - y;
			final double dr = pointRadius + tolerance;
			if ( dx * dx + dy * dy <= dr * dr )
			{
				found = true;
				ref.refTo( vertex );
			}
		}

		index.readLock().unlock();
		return found ? ref : null;
	}

	/**
	 * Get all vertices that would be visible with the current display settings
	 * and the specified {@code transform} and {@code timepoint}. This is used
	 * to compute {@link OverlayContext}.
	 * <p>
	 * Note, that it doesn't lock the {@link SpatioTemporalIndex}: we assumed,
	 * that this is already done by the caller.
	 *
	 * @param transform
	 *            the transform.
	 * @param timepoint
	 *            the time-point.
	 * @return vertices that would be visible with the current display settings
	 *         and the specified {@code transform} and {@code timepoint}.
	 */
	@Override
	public RefCollection< V > getVisibleVertices( final AffineTransform3D transform, final int timepoint )
	{
		final RefList< V > contextList = RefCollections.createRefList( graph.vertices() );
		final ConvexPolytope cropPolytopeGlobal = getVisiblePolytopeGlobal( transform, timepoint );
		final ClipConvexPolytope< V > ccp = index.getSpatialIndex( timepoint ).getClipConvexPolytope();
		ccp.clip( cropPolytopeGlobal );
		final double[] pos = new double[ 3 ];
		final double[] vPos = new double[ 3 ];
		for ( final V vertex : ccp.getInsideValues() )
		{
			vertex.localize( pos );
			transform.apply( pos, vPos );
			final double x = vPos[ 0 ];
			final double y = vPos[ 1 ];
			if ( 0 <= x && x <= width && 0 <= y && y <= height )
				contextList.add( vertex );
		}
		return contextList;
	}
}
