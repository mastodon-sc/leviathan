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
package org.mastodon.leviathan.views.bdv.overlay.cell.wrap;

import java.util.Iterator;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Edges;
import org.mastodon.graph.Vertex;
import org.mastodon.leviathan.views.bdv.overlay.cell.CellOverlayVertex;

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;

public class CellOverlayVertexWrapper< V extends Vertex< E >, E extends Edge< V > >
		implements CellOverlayVertex< CellOverlayVertexWrapper< V, E >, CellOverlayEdgeWrapper< V, E > >
{
	private final int n = 2;

	private final CellOverlayGraphWrapper< V, E > wrapper;

	final V ref;

	V wv;

	private final EdgesWrapper incomingEdges;

	private final EdgesWrapper outgoingEdges;

	final EdgesWrapper edges;

	private final CellOverlayProperties< V, E > overlayProperties;

	CellOverlayVertexWrapper( final CellOverlayGraphWrapper< V, E > wrapper )
	{
		this.wrapper = wrapper;
		ref = wrapper.wrappedGraph.vertexRef();
		incomingEdges = new EdgesWrapper();
		outgoingEdges = new EdgesWrapper();
		edges = new EdgesWrapper();
		overlayProperties = wrapper.overlayProperties;
	}

	@Override
	public int getInternalPoolIndex()
	{
		return wrapper.idmap.getVertexId( wv );
	}

	@Override
	public CellOverlayVertexWrapper< V, E > refTo( final CellOverlayVertexWrapper< V, E > obj )
	{
		wv = wrapper.idmap.getVertex( obj.getInternalPoolIndex(), ref );
		return this;
	}

	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = getFloatPosition( d );
	}

	@Override
	public int getTimepoint()
	{
		return overlayProperties.getTimepoint( wv );
	}

	@Override
	public CellOverlayVertexWrapper< V, E > init( final int timepoint, final double[] position )
	{
		overlayProperties.initVertex( wv, timepoint, position );
		return this;
	}

	@Override
	public Edges< CellOverlayEdgeWrapper< V, E > > incomingEdges()
	{
		incomingEdges.wrap( wv.incomingEdges() );
		return incomingEdges;
	}


	@Override
	public Edges< CellOverlayEdgeWrapper< V, E > > outgoingEdges()
	{
		outgoingEdges.wrap( wv.outgoingEdges() );
		return outgoingEdges;
	}


	@Override
	public Edges< CellOverlayEdgeWrapper< V, E > > edges()
	{
		edges.wrap( wv.edges() );
		return edges;
	}

	@Override
	public int hashCode()
	{
		return wv.hashCode();
	}

	@Override
	public boolean equals( final Object obj )
	{
		return obj instanceof CellOverlayVertexWrapper< ?, ? > &&
				wv.equals( ( ( org.mastodon.leviathan.views.bdv.overlay.cell.wrap.CellOverlayVertexWrapper< ?, ? > ) obj ).wv );
	}

	/**
	 * Returns {@code this} if this {@link CellOverlayVertexWrapper} currently wraps
	 * a {@code V}, or null otherwise.
	 *
	 * @return {@code this} if this {@link CellOverlayVertexWrapper} currently wraps
	 *         a {@code V}, or null otherwise.
	 */
	CellOverlayVertexWrapper< V, E > orNull()
	{
		return wv == null ? null : this;
	}

	/**
	 * If called with a non-null {@link CellOverlayVertexWrapper} returns the
	 * currently wrapped {@code V}, otherwise null.
	 *
	 * @return {@code null} if {@code wrapper == null}, otherwise the {@code V}
	 *         wrapped by {@code wrapper}.
	 */
	static < V extends Vertex< ? > > V wrappedOrNull( final CellOverlayVertexWrapper< V, ? > wrapper )
	{
		return wrapper == null ? null : wrapper.wv;
	}

	class EdgesWrapper implements Edges< CellOverlayEdgeWrapper< V, E > >
	{
		private Edges< E > wrappedEdges;

		private CellOverlayEdgeIteratorWrapper< V, E > iterator = null;

		void wrap( final Edges< E > edges )
		{
			wrappedEdges = edges;
		}

		@Override
		public Iterator< CellOverlayEdgeWrapper< V, E > > iterator()
		{
			if ( iterator == null )
				iterator = new CellOverlayEdgeIteratorWrapper<>( wrapper, wrapper.edgeRef(), wrappedEdges.iterator() );
			else
				iterator.wrap( wrappedEdges.iterator() );
			return iterator;
		}

		@Override
		public int size()
		{
			return wrappedEdges.size();
		}

		@Override
		public boolean isEmpty()
		{
			return wrappedEdges.isEmpty();
		}

		@Override
		public CellOverlayEdgeWrapper< V, E > get( final int i )
		{
			return get( i, wrapper.edgeRef() );
		}

		@Override
		public CellOverlayEdgeWrapper< V, E > get( final int i, final CellOverlayEdgeWrapper< V, E > edge )
		{
			edge.we = wrappedEdges.get( i, edge.ref );
			return edge;
		}

		@Override
		public Iterator< CellOverlayEdgeWrapper< V, E > > safe_iterator()
		{
			return new CellOverlayEdgeIteratorWrapper<>( wrapper, wrapper.edgeRef(), wrappedEdges.iterator() );
		}
	}

	// === RealLocalizable ===

	@Override
	public void localize( final double[] position )
	{
		overlayProperties.localize( wv, position );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return ( float ) overlayProperties.getDoublePosition( wv, d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return overlayProperties.getDoublePosition( wv, d );
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	// === RealPositionable ===

	@Override
	public void setPosition( final double[] position )
	{
		overlayProperties.setPosition( wv, position );
	}

	@Override
	public void setPosition( final double position, final int d )
	{
		overlayProperties.setPosition( wv, position, d );
	}

	@Override
	public void move( final float distance, final int d )
	{
		setPosition( getDoublePosition( d ) + distance, d );
	}

	@Override
	public void move( final double distance, final int d )
	{
		setPosition( getDoublePosition( d ) + distance, d );
	}

	@Override
	public void move( final RealLocalizable localizable )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( getDoublePosition( d ) + localizable.getDoublePosition( d ), d );
	}

	@Override
	public void move( final float[] distance )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( getDoublePosition( d ) + distance[ d ], d );
	}

	@Override
	public void move( final double[] distance )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( getDoublePosition( d ) + distance[ d ], d );
	}

	@Override
	public void setPosition( final RealLocalizable localizable )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( localizable.getDoublePosition( d ), d );
	}

	@Override
	public void setPosition( final float[] position )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( position[ d ], d );
	}

	@Override
	public void setPosition( final float position, final int d )
	{
		setPosition( ( double ) position, d );
	}

	@Override
	public void setPosition( final long position, final int d )
	{
		setPosition( ( double ) position, d );
	}

	@Override
	public void setPosition( final int position, final int d )
	{
		setPosition( ( double ) position, d );
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		move( ( RealLocalizable ) localizable );
	}

	@Override
	public void setPosition( final int[] position )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( ( double ) position[ d ], d );
	}

	@Override
	public void setPosition( final long[] position )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( ( double ) position[ d ], d );
	}

	@Override
	public void fwd( final int d )
	{
		move( 1, d );
	}

	@Override
	public void bck( final int d )
	{
		move( -1, d );
	}

	@Override
	public void move( final int distance, final int d )
	{
		setPosition( getDoublePosition( d ) + distance, d );
	}

	@Override
	public void move( final long distance, final int d )
	{
		setPosition( getDoublePosition( d ) + distance, d );
	}

	@Override
	public void move( final Localizable localizable )
	{
		move( ( RealLocalizable ) localizable );
	}

	@Override
	public void move( final int[] distance )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( getDoublePosition( d ) + distance[ d ], d );
	}

	@Override
	public void move( final long[] distance )
	{
		for ( int d = 0; d < n; ++d )
			setPosition( getDoublePosition( d ) + distance[ d ], d );
	}

	@Override
	public int[] getMembranes()
	{
		return overlayProperties.getMembranes( wv );
	}

	@Override
	public double[] getBoundary()
	{
		return overlayProperties.getBoundary( wv );
	}

	@Override
	public String getLabel()
	{
		return overlayProperties.getLabel( wv );
	}

	@Override
	public void setLabel( final String label )
	{
		overlayProperties.setLabel( wv, label );
	}
}
