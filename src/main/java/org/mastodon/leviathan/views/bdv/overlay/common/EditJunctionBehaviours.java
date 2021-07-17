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
package org.mastodon.leviathan.views.bdv.overlay.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.adapter.RefBimap;
import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayEdge;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayGraph;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayVertex;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.undo.UndoPointMarker;
import org.mastodon.views.bdv.overlay.OverlayGraphRenderer;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.OverlayRenderer;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;

public class EditJunctionBehaviours< V extends JunctionOverlayVertex< V, E >, E extends JunctionOverlayEdge< E, V > >
{

	private static final String CONNECT_JUNCTIONS = "connect junctions";

	private static final String[] CONNECT_JUNCTIONS_KEYS = new String[] { "L" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CONNECT_JUNCTIONS, CONNECT_JUNCTIONS_KEYS, "Connect two junctions, splitting a cell in two." );
		}
	}

	public static final Color EDIT_GRAPH_OVERLAY_COLOR = Color.WHITE;
	public static final BasicStroke EDIT_GRAPH_OVERLAY_STROKE = new BasicStroke( 2f );
	public static final BasicStroke EDIT_GRAPH_OVERLAY_GHOST_STROKE = new BasicStroke(
			1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
			1.0f, new float[] { 4f, 10f }, 0f );

	private static final double POINT_SELECT_DISTANCE_TOLERANCE = 8.;

	private final ConnectJunctionBehaviour linkJunctions;


	public static < V extends JunctionOverlayVertex< V, E >, E extends JunctionOverlayEdge< E, V > > void install(
			final Behaviours behaviours,
			final ViewerPanel viewer,
			final JunctionOverlayGraph< V, E > overlayGraph,
			final FindFaces faceFinder,
			final RefBimap< Junction, V > vertexMap,
			final RefBimap< MembranePart, E > edgeMap,
			final OverlayGraphRenderer< V, E > renderer,
			final SelectionModel< V, E > selection,
			final FocusModel< V, E > focus,
			final UndoPointMarker undo )
	{
		final EditJunctionBehaviours< V, E > eb = new EditJunctionBehaviours<>(
				viewer,
				overlayGraph,
				faceFinder,
				vertexMap,
				edgeMap,
				renderer,
				selection,
				focus,
				undo );
		behaviours.namedBehaviour( eb.linkJunctions, CONNECT_JUNCTIONS_KEYS );
	}

	private final JunctionOverlayGraph< V, E > overlayGraph;

	private final ReentrantReadWriteLock lock;

	private final OverlayGraphRenderer< V, E > renderer;

	private final UndoPointMarker undo;

	private final EditJunctionBehaviours< V, E >.EditJunctionBehavioursOverlay overlay;

	private final FindFaces faceFinder;

	private final RefBimap< Junction, V > vertexMap;

	private final RefBimap< MembranePart, E > edgeMap;

	private EditJunctionBehaviours(
			final ViewerPanel viewer,
			final JunctionOverlayGraph< V, E > overlayGraph,
			final FindFaces faceFinder,
			final RefBimap< Junction, V > vertexMap,
			final RefBimap< MembranePart, E > edgeMap,
			final OverlayGraphRenderer< V, E > renderer,
			final SelectionModel< V, E > selection,
			final FocusModel< V, E > focus,
			final UndoPointMarker undo )
	{
		this.overlayGraph = overlayGraph;
		this.faceFinder = faceFinder;
		this.vertexMap = vertexMap;
		this.edgeMap = edgeMap;
		this.lock = overlayGraph.getLock();
		this.renderer = renderer;
		this.undo = undo;

		// Create and register overlay.
		overlay = new EditJunctionBehavioursOverlay();
		overlay.transformChanged( viewer.state().getViewerTransform() );
		viewer.getDisplay().overlays().add( overlay );
		viewer.renderTransformListeners().add( overlay );

		// Behaviours.
		linkJunctions = new ConnectJunctionBehaviour( CONNECT_JUNCTIONS );
	}

	private class EditJunctionBehavioursOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
	{

		/** The global coordinates to paint the link from. */
		private final double[] from;

		/** The global coordinates to paint the link to. */
		private final double[] to;

		/** The viewer coordinates to paint the link from. */
		private final double[] vFrom;

		/** The viewer coordinates to paint the link to. */
		private final double[] vTo;

		private final AffineTransform3D renderTransform;

		public boolean paintGhostLink;


		public EditJunctionBehavioursOverlay()
		{
			from = new double[ 3 ];
			vFrom = new double[ 3 ];
			to = new double[ 3 ];
			vTo = new double[ 3 ];

			renderTransform = new AffineTransform3D();
		}

		@Override
		public void drawOverlays( final Graphics g )
		{
			final Graphics2D graphics = ( Graphics2D ) g;
			g.setColor( EDIT_GRAPH_OVERLAY_COLOR );

			// The link.
			if ( paintGhostLink )
			{
				graphics.setStroke( EDIT_GRAPH_OVERLAY_STROKE );
				renderer.getViewerPosition( from, vFrom );
				renderer.getViewerPosition( to, vTo );
				g.drawLine( ( int ) vFrom[ 0 ], ( int ) vFrom[ 1 ],
						( int ) vTo[ 0 ], ( int ) vTo[ 1 ] );
			}
		}

		@Override
		public void setCanvasSize( final int width, final int height )
		{}

		@Override
		public void transformChanged( final AffineTransform3D transform )
		{
			synchronized ( renderTransform )
			{
				renderTransform.set( transform );
			}
		}
	}

	private class ConnectJunctionBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{

		private final V source;

		private final V target;

		private final E edgeRef;

		private boolean editing;

		public ConnectJunctionBehaviour( final String name )
		{
			super( name );
			source = overlayGraph.vertexRef();
			target = overlayGraph.vertexRef();
			edgeRef = overlayGraph.edgeRef();
			editing = false;
		}

		@Override
		public void init( final int x, final int y )
		{
			lock.readLock().lock();

			// Get vertex we clicked inside.
			if ( renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, source ) != null )
			{
				source.localize( overlay.from );
				source.localize( overlay.to );
				overlay.paintGhostLink = true;
				editing = true;
			}
			else
				lock.readLock().unlock();
		}

		@Override
		public void drag( final int x, final int y )
		{
			if ( editing )
			{
				if ( renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, target ) != null )
					target.localize( overlay.to );
				else
					renderer.getGlobalPosition( x, y, overlay.to );
			}
		}

		@Override
		public void end( final int x, final int y )
		{
			if ( editing )
			{
				lock.readLock().unlock();
				lock.writeLock().lock();
				try
				{
					source.getInternalPoolIndex();
					if ( renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, target ) != null )
					{
						target.localize( overlay.to );

						// Test forward.
						E edge = overlayGraph.getEdge( source, target, edgeRef );
						if ( null == edge )
						{
							// Test backward.
							edge = overlayGraph.getEdge( target, source, edgeRef );
							if ( edge == null )
							{
								// We are now sure there isn't an edge between source and target.
								addEdge( source, target );
							}
							else
							{
								removeEdge( edge );
							}
						}
						else
						{
							removeEdge( edge );
						}

						overlayGraph.notifyGraphChanged();
						undo.setUndoPoint();

					}
					overlay.paintGhostLink = false;
					editing = false;
				}
				finally
				{
					lock.writeLock().unlock();
				}
			}
		}
	}

	private void addEdge( final V source, final V target )
	{
		final Junction js = vertexMap.getLeft( source );
		final Junction jt = vertexMap.getLeft( target );
		faceFinder.split( js, jt, edgeMap.reusableLeftRef( overlayGraph.edgeRef() ) );
	}

	private void removeEdge( final E edge )
	{
		faceFinder.merge( edgeMap.getLeft( edge ) );
	}


}
