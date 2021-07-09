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
package org.mastodon.leviathan.views.bdv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.JunctionGraphsSpatioTemporalIndex;
import org.mastodon.leviathan.model.LeviathanModel;
import org.mastodon.leviathan.model.MembranePart;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.util.GeometryUtil;
import org.mastodon.views.bdv.overlay.ScreenVertexMath.Ellipse;
import org.mastodon.views.bdv.overlay.util.BdvRendererUtil;

import bdv.util.Affine3DHelpers;
import bdv.viewer.OverlayRenderer;
import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class JunctionOverlayGraphRenderer
		implements OverlayRenderer, TransformListener< AffineTransform3D >, TimePointListener
{
	private int width;

	private int height;

	private final AffineTransform3D renderTransform;

	private int renderTimepoint;

	private JunctionRenderSettings settings;

	private final LeviathanModel model;

	public JunctionOverlayGraphRenderer( final LeviathanModel model )
	{
		this.model = model;
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

	private static Color getColor(
			final boolean isSelected,
			final boolean isHighlighted,
			final int colorSpot,
			final int color )
	{
		if ( color == 0 )
		{
			// No coloring. Color are set by the RenderSettings.
			final int r = ( colorSpot >> 16 ) & 0xff;
			final int g = ( colorSpot >> 8 ) & 0xff;
			final int b = ( colorSpot ) & 0xff;
			final double a = Math.max(
					isHighlighted
							? 0.8
							: ( isSelected ? 0.6 : 0.4 ),
					1 );
			return new Color( truncRGBA( r / 255., g / 255., b / 255., a ), true );
		}
		else
		{
			final int r = isSelected ? 255 : ( ( color >> 16 ) & 0xff );
			final int g = isSelected ? 0 : ( ( color >> 8 ) & 0xff );
			final int b = isSelected ? 25 : ( ( color ) & 0xff );
			final double a = Math.max(
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
		final double globalToViewerScale = Affine3DHelpers.extractScale( transform, 0 );
		final double border = globalToViewerScale * pointRadius;
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

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * that is large enough to ensure that it contains the center of every
	 * ellipsoid containing the specified coordinate.
	 *
	 * @param x
	 *            position on the z=0 plane in viewer coordinates.
	 * @param y
	 *            position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getSurroundingPolytopeGlobal(
			final double x,
			final double y,
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( x, x, y, y, transform, timepoint );
	}

	private interface EdgeOperation
	{
		void apply( final MembranePart edge, final int x0, final int y0, final int x1, final int y1 );
	}

	private void forEachVisibleEdge(
			final AffineTransform3D transform,
			final int currentTimepoint,
			final EdgeOperation edgeOperation )
	{
		if ( !settings.getDrawLinks() )
			return;

		final JunctionGraph graph = model.junctionGraph( currentTimepoint );
		final Junction ref = graph.vertexRef();
		final double[] gPos = new double[ 3 ];
		final double[] lPos = new double[ 3 ];

		final JunctionGraphsSpatioTemporalIndex index = model.index();
		final SpatialIndex< Junction > si = index.getSpatialIndex( currentTimepoint );
		final ClipConvexPolytope< Junction > ccp = si.getClipConvexPolytope();
		ccp.clip( getVisiblePolytopeGlobal( transform, currentTimepoint ) );
		for ( final Junction vertex : ccp.getInsideValues() )
		{
			vertex.localize( gPos );
			transform.apply( gPos, lPos );
			final int x1 = ( int ) lPos[ 0 ];
			final int y1 = ( int ) lPos[ 1 ];

			for ( final MembranePart edge : vertex.incomingEdges() )
			{
				final Junction source = edge.getSource( ref );
				source.localize( gPos );
				transform.apply( gPos, lPos );
				final int x0 = ( int ) lPos[ 0 ];
				final int y0 = ( int ) lPos[ 1 ];
				edgeOperation.apply( edge, x0, y0, x1, y1 );
			}
		}
		graph.releaseRef( ref );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final BasicStroke defaultVertexStroke = new BasicStroke();
		final BasicStroke highlightedVertexStroke = new BasicStroke( 4f );
		final BasicStroke focusedVertexStroke = new BasicStroke( 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 8f, 3f }, 0 );
		final BasicStroke defaultEdgeStroke = new BasicStroke();
		final BasicStroke highlightedEdgeStroke = new BasicStroke( 3f );

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final Object antialiasing = settings.getUseAntialiasing()
				? RenderingHints.VALUE_ANTIALIAS_ON
				: RenderingHints.VALUE_ANTIALIAS_OFF;
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasing );

		final JunctionGraph graph = model.junctionGraph( currentTimepoint );
		final Junction ref1 = graph.vertexRef();
		final Junction ref2 = graph.vertexRef();
		final MembranePart ref3 = graph.edgeRef();
		final Junction source = graph.vertexRef();
		final Junction target = graph.vertexRef();

		final boolean drawArrowHeads = settings.getDrawArrowHeads();
		final int colorSpot = settings.getColorSpot();

		graph.getLock().readLock().lock();
		final JunctionGraphsSpatioTemporalIndex index = model.index();
		final SpatialIndex< Junction > spatialIndex = index.getSpatialIndex( currentTimepoint );
		try
		{
			if ( settings.getDrawLinks() )
			{
//				final MembranePart highlighted = highlight.getHighlightedEdge( ref3 );
				graphics.setStroke( defaultEdgeStroke );
				forEachVisibleEdge( transform, currentTimepoint, ( edge, x0, y0, x1, y1 ) -> {
					final boolean isHighlighted = false; // edge.equals(
															// highlighted );

					edge.getSource( source );
					edge.getTarget( target );
//					final int edgeColor = coloring.color( edge, source, target );
//					final Color c1 = getColor(
//							selection.isSelected( edge ),
//							isHighlighted,
//							colorSpot,
//							edgeColor );
//					if ( useGradient )
//					{
//						final Color c0 = getColor(
//								selection.isSelected( edge ),
//								isHighlighted,
//								colorSpot,
//								edgeColor );
//						graphics.setPaint( new GradientPaint( x0, y0, c0, x1, y1, c1 ) );
//					}
//					else
//					{
//						graphics.setPaint( c1 );
//					}
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

			if ( settings.getDrawSpots() )
			{
//				final Junction highlighted = highlight.getHighlightedVertex( ref1 );
//				final Junction focused = focus.getFocusedVertex( ref2 );

				graphics.setStroke( defaultVertexStroke );
				final ConvexPolytope cropPolytopeGlobal = getVisiblePolytopeGlobal( transform, currentTimepoint );
				final ClipConvexPolytope< Junction > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
				ccp.clip( cropPolytopeGlobal );
				final double[] pos = new double[ 3 ];
				final double[] vPos = new double[ 3 ];
				for ( final Junction vertex : ccp.getInsideValues() )
				{
//					final int color = coloring.color( vertex );
					final boolean isHighlighted = false; // vertex.equals(
															// highlighted );
					final boolean isFocused = false; // vertex.equals( focused
														// );

					vertex.localize( pos );
					transform.apply( pos, vPos );
					final double x = vPos[ 0 ];
					final double y = vPos[ 1 ];

//					graphics.setColor( getColor(
//							selection.isSelected( vertex ),
//							isHighlighted,
//							colorSpot,
//							color ) );
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
				}
			}
		}
		finally
		{
			graph.getLock().readLock().unlock();
		}
		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
		graph.releaseRef( ref3 );
		graph.releaseRef( source );
		graph.releaseRef( target );
	}

	static void drawEllipse( final Graphics2D graphics, final Ellipse ellipse, AffineTransform torig )
	{
		if ( torig == null )
			torig = graphics.getTransform();

		final double[] tr = ellipse.getCenter();
		final double theta = ellipse.getTheta();
		final double w = ellipse.getHalfWidth();
		final double h = ellipse.getHalfHeight();
		final Ellipse2D ellipse2D = new Ellipse2D.Double( -w, -h, 2. * w, 2. * h );

		graphics.translate( tr[ 0 ], tr[ 1 ] );
		graphics.rotate( theta );
		graphics.draw( ellipse2D );

		graphics.setTransform( torig );
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
	public MembranePart getEdgeAt( final int x, final int y, final double tolerance, final MembranePart ref )
	{
		if ( !settings.getDrawLinks() )
			return null;

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		class Op implements EdgeOperation
		{
			final double squTolerance = tolerance * tolerance;

			double bestSquDist = Double.POSITIVE_INFINITY;

			boolean found = false;

			@Override
			public void apply( final MembranePart edge, final int x0, final int y0, final int x1, final int y1 )
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

		try
		{
			forEachVisibleEdge( transform, currentTimepoint, op );
		}
		finally
		{}
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
	public void getViewerPosition( final double[] gPos, final double[] vPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.apply( gPos, vPos );
		}
	}

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
	public Junction getVertexAt( final int x, final int y, final double tolerance, final Junction ref )
	{
		if ( !settings.getDrawSpots() )
			return null;

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final double[] lPos = new double[] { x, y, 0 };
		final double[] gPos = new double[ 3 ];
		transform.applyInverse( gPos, lPos );

		boolean found = false;
		final double[] vPos = new double[ 3 ];

		final JunctionGraphsSpatioTemporalIndex index = model.index();
		final ConvexPolytope cropPolytopeGlobal = getSurroundingPolytopeGlobal( x, y, transform, currentTimepoint );
		final ClipConvexPolytope< Junction > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
		ccp.clip( cropPolytopeGlobal );

		final NearestNeighborSearch< Junction > nns = index.getSpatialIndex( currentTimepoint ).getNearestNeighborSearch();
		nns.search( RealPoint.wrap( gPos ) );
		final Junction vertex = nns.getSampler().get();
		if ( vertex != null )
		{
			final double[] pos = new double[ 3 ];
			vertex.localize( pos );
			transform.apply( pos, vPos );
			final double dx = vPos[ 0 ] - x;
			final double dy = vPos[ 1 ] - y;
			final double dr = pointRadius + tolerance;
			if ( dx * dx + dy * dy <= dr * dr )
			{
				found = true;
				ref.refTo( vertex );
			}
		}

		return found ? ref : null;
	}

	/**
	 * Get all vertices that would be visible with the current display settings
	 * and the specified {@code transform} and {@code timepoint}. This is used
	 * to compute {@link OverlayContext}.
	 * <p>
	 * Note, that it doesn't lock the {@link SpatioTemporalIndex}: we assumed,
	 * that this is already done by the caller.
	 * <p>
	 * TODO: The above means that the index is locked for longer than
	 * necessary.Revisit this and once it is clear how contexts are used in
	 * practice.
	 *
	 * @param transform
	 * @param timepoint
	 * @return vertices that would be visible with the current display settings
	 *         and the specified {@code transform} and {@code timepoint}.
	 */
	RefCollection< Junction > getVisibleVertices( final AffineTransform3D transform, final int timepoint )
	{
		final JunctionGraph graph = model.junctionGraph( timepoint );
		final JunctionGraphsSpatioTemporalIndex index = model.index();
		final RefList< Junction > contextList = RefCollections.createRefList( graph.vertices() );

		final ConvexPolytope cropPolytopeGlobal = getVisiblePolytopeGlobal( transform, timepoint );
		final ClipConvexPolytope< Junction > ccp = index.getSpatialIndex( timepoint ).getClipConvexPolytope();
		ccp.clip( cropPolytopeGlobal );
		final double[] pos = new double[ 3 ];
		final double[] vPos = new double[ 3 ];
		for ( final Junction vertex : ccp.getInsideValues() )
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
