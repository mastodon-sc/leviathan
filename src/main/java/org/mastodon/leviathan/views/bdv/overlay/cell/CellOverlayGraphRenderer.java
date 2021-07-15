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
package org.mastodon.leviathan.views.bdv.overlay.cell;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.views.bdv.overlay.common.DouglasPeucker;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayGraphWrapper;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.util.GeometryUtil;
import org.mastodon.views.bdv.overlay.OverlayContext;
import org.mastodon.views.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.views.bdv.overlay.Visibilities;
import org.mastodon.views.bdv.overlay.Visibilities.Visibility;
import org.mastodon.views.bdv.overlay.Visibilities.VisibilityMode;
import org.mastodon.views.bdv.overlay.util.BdvRendererUtil;
import org.scijava.util.DoubleArray;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class CellOverlayGraphRenderer< V extends CellOverlayVertex< V, E >, E extends CellOverlayEdge< E, V > >
		implements OverlayGraphRenderer< V, E >
{

	public static final double pointRadius = 2.5;

	private int width;

	private int height;

	private final AffineTransform3D renderTransform;

	private int renderTimepoint;

	private final CellOverlayGraph< V, E > graph;

	private final SpatioTemporalIndex< V > index;

	private final HighlightModel< V, E > highlight;

	private final FocusModel< V, E > focus;

	private final SelectionModel< V, E > selection;

	private final GraphColorGenerator< V, E > coloring;

	private CellRenderSettings settings;

	private final Visibilities< V, E > visibilities;

	public CellOverlayGraphRenderer(
			final CellOverlayGraph< V, E > graph,
			final JunctionOverlayGraphWrapper< Junction, MembranePart > junctionGraphWrapper,
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
		this.visibilities = new Visibilities<>( graph, selection, focus, graph.getLock() );
		index = graph.getIndex();
		renderTransform = new AffineTransform3D();
		setCellRenderSettings( CellRenderSettings.defaultStyle() );
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

	public void setCellRenderSettings( final CellRenderSettings settings )
	{
		this.settings = settings;
	}

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
	 * Return signed distance of timepoint t to t0, truncated at cutoff and
	 * scaled by 1/cutoff. t=t0 has d=0. t&le;t0-cutoff has d=-1. t&ge;t0+cutoff
	 * has d=1.
	 */
	private static double timeDistance( final double t, final double t0, final double cutoff )
	{
		final double d = t - t0;
		if ( d > 0 )
			return Math.min( d, cutoff ) / cutoff;
		else
			return Math.max( d, -cutoff ) / cutoff;
	}

	/**
	 * Generates a color suitable to paint an abject that might be away from the
	 * focus plane, or away in time.
	 *
	 * @param td
	 *            timeDistande, between -1 and 1. see
	 *            {@link #timeDistance(double, double, double)}.
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
			final double td,
			final double tdFade,
			final boolean isSelected,
			final boolean isHighlighted,
			final int colorSpot,
			final int colorPast,
			final int colorFuture,
			final int color )
	{
		final double tf;
		final int colorTo;
		if ( td > 0 )
		{
			tf = Math.max( 0, ( td - tdFade ) / ( 1 - tdFade ) );
			colorTo = isSelected
					? complementaryColor( colorFuture )
					: colorFuture;
		}
		else
		{
			tf = -Math.max( 0, ( -td - tdFade ) / ( 1 - tdFade ) );
			colorTo = isSelected
					? complementaryColor( colorPast )
					: colorPast;
		}
		final int colorFrom = isSelected
				? complementaryColor( colorSpot )
				: colorSpot;

		if ( color == 0 )
		{
			// No coloring. Color are set by the RenderSettings.
			final int r0 = ( colorFrom >> 16 ) & 0xff;
			final int g0 = ( colorFrom >> 8 ) & 0xff;
			final int b0 = ( colorFrom ) & 0xff;
			final int r1 = ( colorTo >> 16 ) & 0xff;
			final int g1 = ( colorTo >> 8 ) & 0xff;
			final int b1 = ( colorTo ) & 0xff;
			final double r = ( Math.abs( td ) * ( r1 - r0 ) + r0 ) / 255.;
			final double g = ( Math.abs( td ) * ( g1 - g0 ) + g0 ) / 255.;
			final double b = ( Math.abs( td ) * ( b1 - b0 ) + b0 ) / 255.;
			final double a = Math.max(
					isHighlighted
							? 0.8
							: ( isSelected ? 0.6 : 0.4 ),
					( 1 + tf ) );
			return new Color( truncRGBA( r, g, b, a ), true );
		}
		else
		{
			/*
			 * Use some default coloring when selected. The same than when we
			 * have no coloring. There is a chance that this color is confused
			 * with a similar color in the ColorMap then.
			 */
			final int a0 = isSelected ? 255 : ( ( color >> 24 ) & 0xff );
			final int r0 = isSelected ? 255 : ( ( color >> 16 ) & 0xff );
			final int g0 = isSelected ? 0 : ( ( color >> 8 ) & 0xff );
			final int b0 = isSelected ? 25 : ( ( color ) & 0xff );
			final double r = ( 1 + Math.abs( td ) ) * r0 / 255;
			final double g = ( 1 + Math.abs( td ) ) * g0 / 255;
			final double b = ( 1 + Math.abs( td ) ) * b0 / 255;
			final double a = Math.max(
					isHighlighted
							? 0.8
							: ( isSelected ? 0.6 : 0.4 ),
					a0 / 255f * ( 1 + tf ) );
			return new Color( truncRGBA( r, g, b, a ), true );
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

	private interface EdgeOperation< E >
	{
		void apply( final E edge, final double td0, final double td1, final int x0, final int y0, final int x1, final int y1 );
	}

	private void forEachVisibleEdge(
			final AffineTransform3D transform,
			final int currentTimepoint,
			final EdgeOperation< E > edgeOperation )
	{
		if ( !settings.getDrawLinks() || visibilities.getMode() == VisibilityMode.NONE )
			return;

		final Visibility< V, E > visibility = visibilities.getVisibility();
		final boolean drawLinksAheadInTime = settings.getDrawLinksAheadInTime();

		final V ref = graph.vertexRef();
		final double[] gPos = new double[ 3 ];
		final double[] lPos = new double[ 3 ];

		final int timeLimit = settings.getTimeLimit();
		final int minT = Math.max( 0, currentTimepoint - timeLimit + 1 );
		final int maxT = drawLinksAheadInTime
				? currentTimepoint + timeLimit - 1
				: currentTimepoint;

		for ( int t = minT; t <= maxT; ++t )
		{
			final double td0 = timeDistance( t - 1, currentTimepoint, timeLimit );
			final double td1 = timeDistance( t, currentTimepoint, timeLimit );

			final SpatialIndex< V > si = index.getSpatialIndex( t );
			final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
			ccp.clip( getVisiblePolytopeGlobal( transform, t ) );
			for ( final V vertex : ccp.getInsideValues() )
			{
				vertex.localize( gPos );
				transform.apply( gPos, lPos );
				final int x1 = ( int ) lPos[ 0 ];
				final int y1 = ( int ) lPos[ 1 ];

				for ( final E edge : vertex.incomingEdges() )
				{
					if ( !visibility.isVisible( edge ) )
						continue;

					final V source = edge.getSource( ref );
					source.localize( gPos );
					transform.apply( gPos, lPos );
					final int x0 = ( int ) lPos[ 0 ];
					final int y0 = ( int ) lPos[ 1 ];
					edgeOperation.apply( edge, td0, td1, x0, y0, x1, y1 );
				}
			}
		}

		graph.releaseRef( ref );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final BasicStroke defaultVertexStroke = new BasicStroke();
		final BasicStroke defaultEdgeStroke = new BasicStroke();
		final BasicStroke highlightedEdgeStroke = new BasicStroke( 3f );
		final BasicStroke focusedEdgeStroke = new BasicStroke( 3f,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[] { 6f }, 0f );

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;
		final double timepointDistanceFade = 0.5;

		final Object antialiasing = settings.getUseAntialiasing()
				? RenderingHints.VALUE_ANTIALIAS_ON
				: RenderingHints.VALUE_ANTIALIAS_OFF;
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasing );

		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		final E ref3 = graph.edgeRef();
		final V source = graph.vertexRef();
		final V target = graph.vertexRef();

		final int colorSpot = settings.getCellColor();
		final int colorPast = settings.getColorPast();
		final int colorFuture = settings.getColorFuture();
		final boolean useGradient = settings.getUseGradient();
		final boolean drawArrowHeads = settings.getDrawArrowHeads();

		graph.getLock().readLock().lock();
		index.readLock().lock();
		try
		{
			if ( settings.getDrawLinks() )
			{
				final E highlighted = highlight.getHighlightedEdge( ref3 );
				graphics.setStroke( defaultEdgeStroke );
				forEachVisibleEdge( transform, currentTimepoint, ( edge, td0, td1, x0, y0, x1, y1 ) -> {
					final boolean isHighlighted = edge.equals( highlighted );

					edge.getSource( source );
					edge.getTarget( target );
					final int edgeColor = coloring.color( edge, source, target );
					final Color c1 = getColor(
							td1,
							timepointDistanceFade,
							selection.isSelected( edge ),
							isHighlighted,
							colorSpot,
							colorPast,
							colorFuture,
							edgeColor );
					if ( useGradient )
					{
						final Color c0 = getColor(
								td0,
								timepointDistanceFade,
								selection.isSelected( edge ),
								isHighlighted,
								colorSpot,
								colorPast,
								colorFuture,
								edgeColor );
						graphics.setPaint( new GradientPaint( x0, y0, c0, x1, y1, c1 ) );
					}
					else
					{
						graphics.setPaint( c1 );
					}
					if ( isHighlighted )
						graphics.setStroke( highlightedEdgeStroke );
					graphics.drawLine( x0, y0, x1, y1 );

					// Draw arrows for edge direction.
					if ( drawArrowHeads )
					{
						final double dx = x1 - x0;
						final double dy = y1 - y0;
						final double alpha = Math.atan2( dy, dx );
						final double l = 5;
						final double theta = Math.PI / 6.;
						final int x1a = ( int ) Math.round( x1 - l * Math.cos( alpha - theta ) );
						final int x1b = ( int ) Math.round( x1 - l * Math.cos( alpha + theta ) );
						final int y1a = ( int ) Math.round( y1 - l * Math.sin( alpha - theta ) );
						final int y1b = ( int ) Math.round( y1 - l * Math.sin( alpha + theta ) );
						graphics.drawLine( x1, y1, x1a, y1a );
						graphics.drawLine( x1, y1, x1b, y1b );
					}

					if ( isHighlighted )
						graphics.setStroke( defaultEdgeStroke );
				} );
			}

			final boolean drawCellCenter = settings.getDrawCellCenter();
			final boolean drawCellContour = settings.getDrawCellContour();
			final boolean drawCells = settings.getDrawCells();
			if ( drawCells && ( drawCellCenter || drawCellContour ) )
			{
				final boolean drawCellFilled = settings.getDrawCellFilled();
				final boolean drawCellLabel = settings.getDrawCellLabel();

				final V highlighted = highlight.getHighlightedVertex( ref1 );
				final V focused = focus.getFocusedVertex( ref2 );

				graphics.setStroke( defaultVertexStroke );

				final ConvexPolytope cropPolytopeGlobal = getVisiblePolytopeGlobal( transform, currentTimepoint );
				final ClipConvexPolytope< V > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
				ccp.clip( cropPolytopeGlobal );

				final Path2D.Double path = new Path2D.Double();
				final DoubleArray arr = new DoubleArray();
				final DoubleArray tmp = new DoubleArray();
				final double[] pos = new double[ 3 ];
				final double[] vPos = new double[ 3 ];

				for ( final V vertex : ccp.getInsideValues() )
				{
					final int color = coloring.color( vertex );
					final boolean isHighlighted = vertex.equals( highlighted );
					final boolean isFocused = vertex.equals( focused );

					graphics.setColor( getColor(
							0,
							timepointDistanceFade,
							selection.isSelected( vertex ),
							isHighlighted,
							colorSpot,
							colorPast,
							colorFuture,
							color ) );

					if ( drawCellContour )
					{
						toPath( vertex,
								transform,
								path,
								arr,
								tmp,
								pos,
								vPos );

						if ( isFocused )
							graphics.setStroke( focusedEdgeStroke );
						else if ( isHighlighted )
							graphics.setStroke( highlightedEdgeStroke );

						if ( drawCellFilled )
						{
							graphics.fill( path );
							graphics.setColor( Color.BLACK );
						}
						graphics.draw( path );
						if ( drawCellLabel )
						{
							final String str = vertex.getLabel();
							final Rectangle2D bounds = graphics.getFontMetrics().getStringBounds( str, graphics );
							graphics.drawString( str,
									( float ) ( vPos[ 0 ] - bounds.getWidth() / 2. ),
									( float ) ( vPos[ 1 ] - bounds.getHeight() / 2. ) );
						}

						if ( isHighlighted || isFocused )
							graphics.setStroke( defaultVertexStroke );
					}
					else
					{
						// Draw cell center.
						vertex.localize( pos );
						transform.apply( pos, vPos );

						final double x = vPos[ 0 ];
						final double y = vPos[ 1 ];
						double radius = pointRadius;
						if ( isHighlighted || isFocused )
							radius *= 2;
						final int ox = ( int ) ( x - radius );
						final int oy = ( int ) ( y - radius );
						final int ow = ( int ) ( 2 * radius );
						if ( isFocused )
							graphics.fillRect( ox, oy, ow, ow );
						else
							graphics.fillOval( ox, oy, ow, ow );

						if ( drawCellLabel )
						{
							final String str = vertex.getLabel();
							final Rectangle2D bounds = graphics.getFontMetrics().getStringBounds( str, graphics );
							graphics.drawString( str,
									( float ) ( x + radius + 1. ),
									( float ) ( y - bounds.getHeight() / 2. ) );
						}

					}
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

	private void toPath(
			final V vertex,
			final AffineTransform3D transform,
			final java.awt.geom.Path2D.Double path,
			final DoubleArray arr,
			final DoubleArray tmp,
			final double[] pos,
			final double[] vPos )
	{
		arr.clear();
		tmp.clear();
		final double[] boundary = vertex.getBoundary();
		final double xc = vertex.getDoublePosition( 0 );
		final double yc = vertex.getDoublePosition( 1 );

		for ( int i = 0; i < boundary.length; i = i + 2 )
		{
			pos[ 0 ] = xc + boundary[ i ];
			pos[ 1 ] = yc + boundary[ i + 1 ];
			transform.apply( pos, vPos );
			tmp.addValue( vPos[ 0 ] );
			tmp.addValue( vPos[ 1 ] );
		}
		DouglasPeucker.douglasPeucker( tmp, arr, 1. );

		path.reset();
		path.moveTo( arr.getValue( 0 ), arr.getValue( 1 ) );
		for ( int i = 2; i < arr.size(); i = i + 2 )
		{
			final double x = arr.getValue( i );
			final double y = arr.getValue( i + 1 );
			path.lineTo( x, y );
		}
		path.closePath();
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

		class Op implements EdgeOperation< E >
		{
			final double squTolerance = tolerance * tolerance;

			double bestSquDist = Double.POSITIVE_INFINITY;

			boolean found = false;

			@Override
			public void apply( final E edge, final double td0, final double td1, final int x0, final int y0, final int x1, final int y1 )
			{
				final double squDist = GeometryUtil.squSegmentDist( x, y, x0, y0, x1, y1 );
				if ( squDist <= squTolerance && squDist < bestSquDist )
				{
					found = true;
					bestSquDist = squDist;
					ref.refTo( edge );
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
		if ( !settings.getDrawCells() )
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
			final Path2D.Double path = new Path2D.Double();
			final DoubleArray arr = new DoubleArray();
			final DoubleArray tmp = new DoubleArray();
			toPath( vertex, transform, path, arr, tmp, lPos, gPos );
			if ( path.contains( x, y ) )
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
