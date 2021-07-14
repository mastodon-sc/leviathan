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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.RefPool;
import org.mastodon.adapter.RefBimap;
import org.mastodon.app.ViewGraph;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.util.AbstractRefPoolCollectionWrapper;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Edges;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.leviathan.views.bdv.overlay.cell.CellOverlayGraph;
import org.mastodon.spatial.SpatioTemporalIndex;

public class CellOverlayGraphWrapper< V extends Vertex< E >, E extends Edge< V > > implements
		CellOverlayGraph< CellOverlayVertexWrapper< V, E >, CellOverlayEdgeWrapper< V, E > >,
		ViewGraph< V, E, CellOverlayVertexWrapper< V, E >, CellOverlayEdgeWrapper< V, E > >
{
	final ReadOnlyGraph< V, E > wrappedGraph;

	final GraphIdBimap< V, E > idmap;

	final CellOverlayProperties< V, E > overlayProperties;

	private final ReentrantReadWriteLock lock;

	private final ConcurrentLinkedQueue< CellOverlayVertexWrapper< V, E > > tmpVertexRefs;

	private final ConcurrentLinkedQueue< CellOverlayEdgeWrapper< V, E > > tmpEdgeRefs;

	private final SpatioTemporalIndex< CellOverlayVertexWrapper< V, E > > wrappedIndex;

	private final RefBimap< V, CellOverlayVertexWrapper< V, E > > vertexMap;

	private final RefBimap< E, CellOverlayEdgeWrapper< V, E > > edgeMap;

	public CellOverlayGraphWrapper(
			final ReadOnlyGraph< V, E > graph,
			final GraphIdBimap< V, E > idmap,
			final SpatioTemporalIndex< V > graphIndex,
			final ReentrantReadWriteLock lock,
			final CellOverlayProperties< V, E > overlayProperties )
	{
		this.wrappedGraph = graph;
		this.idmap = idmap;
		this.lock = lock;
		this.overlayProperties = overlayProperties;
		tmpVertexRefs =	new ConcurrentLinkedQueue<>();
		tmpEdgeRefs = new ConcurrentLinkedQueue<>();
		wrappedIndex = new SpatioTemporalIndexWrapper<>( this, graphIndex );
		vertexMap = new CellOverlayVertexWrapperBimap<>( this );
		edgeMap = new CellOverlayEdgeWrapperBimap<>( this );
	}

	@Override
	public CellOverlayVertexWrapper< V, E > vertexRef()
	{
		final CellOverlayVertexWrapper< V, E > ref = tmpVertexRefs.poll();
		return ref == null ? new CellOverlayVertexWrapper<>( this ) : ref;
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > edgeRef()
	{
		final CellOverlayEdgeWrapper< V, E > ref = tmpEdgeRefs.poll();
		return ref == null ? new CellOverlayEdgeWrapper<>( this ) : ref;
	}

	@Override
	public void releaseRef( final CellOverlayVertexWrapper< V, E > ref )
	{
		tmpVertexRefs.add( ref );
	}

	@Override
	public void releaseRef( final CellOverlayEdgeWrapper< V, E > ref )
	{
		tmpEdgeRefs.add( ref );
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > getEdge( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target )
	{
		return getEdge( source, target, edgeRef() );
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > getEdge( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target, final CellOverlayEdgeWrapper< V, E > edge )
	{
		edge.we = wrappedGraph.getEdge( source.wv, target.wv, edge.ref );
		return edge.orNull();
	}

	@Override
	public Edges< CellOverlayEdgeWrapper< V, E > > getEdges( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target )
	{
		return getEdges( source, target, vertexRef() );
	}

	@Override
	public Edges< CellOverlayEdgeWrapper< V, E > > getEdges( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target, final CellOverlayVertexWrapper< V, E > ref )
	{
		final Edges< E > wes = wrappedGraph.getEdges( source.wv, target.wv, ref.wv );
		ref.edges.wrap( wes );
		return ref.edges;
	}

	@Override
	public RefCollection< CellOverlayVertexWrapper< V, E > > vertices()
	{
		return vertices;
	}

	@Override
	public RefCollection< CellOverlayEdgeWrapper< V, E > > edges()
	{
		return edges;
	}

	@Override
	public SpatioTemporalIndex< CellOverlayVertexWrapper< V, E > > getIndex()
	{
		return wrappedIndex;
	}

	@Override
	public CellOverlayVertexWrapper< V, E > addVertex()
	{
		return addVertex( vertexRef() );
	}

	@Override
	public CellOverlayVertexWrapper< V, E > addVertex( final CellOverlayVertexWrapper< V, E > ref )
	{
		ref.wv = overlayProperties.addVertex( ref.ref );
		return ref;
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > addEdge( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target )
	{
		return addEdge( source, target, edgeRef() );
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > addEdge( final CellOverlayVertexWrapper< V, E > source, final CellOverlayVertexWrapper< V, E > target, final CellOverlayEdgeWrapper< V, E > ref )
	{
		ref.we = overlayProperties.addEdge( source.wv, target.wv, ref.ref );
		return ref;
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > insertEdge( final CellOverlayVertexWrapper< V, E > source, final int sourceOutIndex, final CellOverlayVertexWrapper< V, E > target, final int targetInIndex )
	{
		return insertEdge( source, sourceOutIndex, target, targetInIndex, edgeRef() );
	}

	@Override
	public CellOverlayEdgeWrapper< V, E > insertEdge( final CellOverlayVertexWrapper< V, E > source, final int sourceOutIndex, final CellOverlayVertexWrapper< V, E > target, final int targetInIndex, final CellOverlayEdgeWrapper< V, E > ref )
	{
		ref.we = overlayProperties.insertEdge( source.wv, sourceOutIndex, target.wv, targetInIndex, ref.ref );
		return ref;
	}

	@Override
	public void remove( final CellOverlayEdgeWrapper< V, E > edge )
	{
		overlayProperties.removeEdge( edge.we );
	}

	@Override
	public void remove( final CellOverlayVertexWrapper< V, E > vertex )
	{
		overlayProperties.removeVertex( vertex.wv );
	}

	@Override
	public ReentrantReadWriteLock getLock()
	{
		return lock;
	}

	@Override
	public void notifyGraphChanged()
	{
		overlayProperties.notifyGraphChanged();
	}

	/**
	 * Get bidirectional mapping between model vertices and view vertices.
	 *
	 * @return bidirectional mapping between model vertices and view vertices.
	 */
	@Override
	public RefBimap< V, CellOverlayVertexWrapper< V, E > > getVertexMap()
	{
		return vertexMap;
	}

	/**
	 * Get bidirectional mapping between model edges and view edges.
	 *
	 * @return bidirectional mapping between model edges and view edges.
	 */
	@Override
	public RefBimap< E, CellOverlayEdgeWrapper< V, E > > getEdgeMap()
	{
		return edgeMap;
	}

	private final RefPool< CellOverlayVertexWrapper< V, E > > vertexPool = new RefPool< CellOverlayVertexWrapper< V, E > >()
	{
		@Override
		public CellOverlayVertexWrapper< V, E > createRef()
		{
			return vertexRef();
		}

		@Override
		public void releaseRef( final CellOverlayVertexWrapper< V, E > v )
		{
			CellOverlayGraphWrapper.this.releaseRef( v );
		}

		@Override
		public CellOverlayVertexWrapper< V, E > getObject( final int index, final CellOverlayVertexWrapper< V, E > v )
		{
			v.wv = idmap.getVertex( index, v.ref );
			return v;
		}

		@Override
		public CellOverlayVertexWrapper< V, E > getObjectIfExists( final int index, final CellOverlayVertexWrapper< V, E > v )
		{
			v.wv = idmap.getVertexIfExists( index, v.ref );
			return v.wv == null ? null : v;
		}

		@Override
		public int getId( final CellOverlayVertexWrapper< V, E > v )
		{
			return idmap.getVertexId( v.wv );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< CellOverlayVertexWrapper< V, E > > getRefClass()
		{
			return ( Class ) CellOverlayVertexWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< CellOverlayVertexWrapper< V, E >, RefPool< CellOverlayVertexWrapper< V, E > > > vertices = new AbstractRefPoolCollectionWrapper< CellOverlayVertexWrapper< V, E >, RefPool< CellOverlayVertexWrapper< V, E > > >( vertexPool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.vertices().size();
		}

		@Override
		public Iterator< CellOverlayVertexWrapper< V, E > > iterator()
		{
			return new CellOverlayVertexIteratorWrapper<>( CellOverlayGraphWrapper.this, CellOverlayGraphWrapper.this.vertexRef(), wrappedGraph.vertices().iterator() );
		}
	};

	private final RefPool< CellOverlayEdgeWrapper< V, E > > edgePool = new RefPool< CellOverlayEdgeWrapper< V, E > >()
	{
		@Override
		public CellOverlayEdgeWrapper< V, E > createRef()
		{
			return edgeRef();
		}

		@Override
		public void releaseRef( final CellOverlayEdgeWrapper< V, E > e )
		{
			CellOverlayGraphWrapper.this.releaseRef( e );
		}

		@Override
		public CellOverlayEdgeWrapper< V, E > getObject( final int index, final CellOverlayEdgeWrapper< V, E > e )
		{
			e.we = idmap.getEdge( index, e.ref );
			return e;
		}

		@Override
		public CellOverlayEdgeWrapper< V, E > getObjectIfExists( final int index, final CellOverlayEdgeWrapper< V, E > e )
		{
			e.we = idmap.getEdgeIfExists( index, e.ref );
			return e.we == null ? null : e;
		}

		@Override
		public int getId( final CellOverlayEdgeWrapper< V, E > e )
		{
			return idmap.getEdgeId( e.we );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< CellOverlayEdgeWrapper< V, E > > getRefClass()
		{
			return ( Class ) CellOverlayEdgeWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< CellOverlayEdgeWrapper< V, E >, RefPool< CellOverlayEdgeWrapper< V, E > > > edges = new AbstractRefPoolCollectionWrapper< CellOverlayEdgeWrapper< V, E >, RefPool< CellOverlayEdgeWrapper< V, E > > >( edgePool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.edges().size();
		}

		@Override
		public Iterator< CellOverlayEdgeWrapper< V, E > > iterator()
		{
			return new CellOverlayEdgeIteratorWrapper<>( CellOverlayGraphWrapper.this, CellOverlayGraphWrapper.this.edgeRef(), wrappedGraph.edges().iterator() );
		}
	};
}
