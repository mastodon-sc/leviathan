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
package org.mastodon.leviathan.views.bdv.overlay.junction.wrap;

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
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayGraph;
import org.mastodon.spatial.SpatioTemporalIndex;

public class JunctionOverlayGraphWrapper< V extends Vertex< E >, E extends Edge< V > > implements
		JunctionOverlayGraph< JunctionOverlayVertexWrapper< V, E >, JunctionOverlayEdgeWrapper< V, E > >,
		ViewGraph< V, E, JunctionOverlayVertexWrapper< V, E >, JunctionOverlayEdgeWrapper< V, E > >
{
	final ReadOnlyGraph< V, E > wrappedGraph;

	final GraphIdBimap< V, E > idmap;

	final JunctionOverlayProperties< V, E > overlayProperties;

	private final ReentrantReadWriteLock lock;

	private final ConcurrentLinkedQueue< JunctionOverlayVertexWrapper< V, E > > tmpVertexRefs;

	private final ConcurrentLinkedQueue< JunctionOverlayEdgeWrapper< V, E > > tmpEdgeRefs;

	private final SpatioTemporalIndex< JunctionOverlayVertexWrapper< V, E > > wrappedIndex;

	private final RefBimap< V, JunctionOverlayVertexWrapper< V, E > > vertexMap;

	private final RefBimap< E, JunctionOverlayEdgeWrapper< V, E > > edgeMap;

	public JunctionOverlayGraphWrapper(
			final ReadOnlyGraph< V, E > graph,
			final GraphIdBimap< V, E > idmap,
			final SpatioTemporalIndex< V > graphIndex,
			final ReentrantReadWriteLock lock,
			final JunctionOverlayProperties< V, E > overlayProperties )
	{
		this.wrappedGraph = graph;
		this.idmap = idmap;
		this.lock = lock;
		this.overlayProperties = overlayProperties;
		tmpVertexRefs =	new ConcurrentLinkedQueue<>();
		tmpEdgeRefs = new ConcurrentLinkedQueue<>();
		wrappedIndex = new SpatioTemporalIndexWrapper<>( this, graphIndex );
		vertexMap = new JunctionOverlayVertexWrapperBimap<>( this );
		edgeMap = new JunctionOverlayEdgeWrapperBimap<>( this );
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > vertexRef()
	{
		final JunctionOverlayVertexWrapper< V, E > ref = tmpVertexRefs.poll();
		return ref == null ? new JunctionOverlayVertexWrapper<>( this ) : ref;
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > edgeRef()
	{
		final JunctionOverlayEdgeWrapper< V, E > ref = tmpEdgeRefs.poll();
		return ref == null ? new JunctionOverlayEdgeWrapper<>( this ) : ref;
	}

	@Override
	public void releaseRef( final JunctionOverlayVertexWrapper< V, E > ref )
	{
		tmpVertexRefs.add( ref );
	}

	@Override
	public void releaseRef( final JunctionOverlayEdgeWrapper< V, E > ref )
	{
		tmpEdgeRefs.add( ref );
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > getEdge( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target )
	{
		return getEdge( source, target, edgeRef() );
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > getEdge( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target, final JunctionOverlayEdgeWrapper< V, E > edge )
	{
		edge.we = wrappedGraph.getEdge( source.wv, target.wv, edge.ref );
		return edge.orNull();
	}

	@Override public Edges< JunctionOverlayEdgeWrapper< V, E > > getEdges( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target )
	{
		return getEdges( source, target, vertexRef() );
	}

	@Override public Edges< JunctionOverlayEdgeWrapper< V, E > > getEdges( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target, final JunctionOverlayVertexWrapper< V, E > ref )
	{
		final Edges< E > wes = wrappedGraph.getEdges( source.wv, target.wv, ref.wv );
		ref.edges.wrap( wes );
		return ref.edges;
	}

	@Override
	public RefCollection< JunctionOverlayVertexWrapper< V, E > > vertices()
	{
		return vertices;
	}

	@Override
	public RefCollection< JunctionOverlayEdgeWrapper< V, E > > edges()
	{
		return edges;
	}

	@Override
	public SpatioTemporalIndex< JunctionOverlayVertexWrapper< V, E > > getIndex()
	{
		return wrappedIndex;
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > addVertex()
	{
		return addVertex( vertexRef() );
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > addVertex( final JunctionOverlayVertexWrapper< V, E > ref )
	{
		ref.wv = overlayProperties.addVertex( ref.ref );
		return ref;
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > addEdge( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target )
	{
		return addEdge( source, target, edgeRef() );
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > addEdge( final JunctionOverlayVertexWrapper< V, E > source, final JunctionOverlayVertexWrapper< V, E > target, final JunctionOverlayEdgeWrapper< V, E > ref )
	{
		ref.we = overlayProperties.addEdge( source.wv, target.wv, ref.ref );
		return ref;
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > insertEdge( final JunctionOverlayVertexWrapper< V, E > source, final int sourceOutIndex, final JunctionOverlayVertexWrapper< V, E > target, final int targetInIndex )
	{
		return insertEdge( source, sourceOutIndex, target, targetInIndex, edgeRef() );
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > insertEdge( final JunctionOverlayVertexWrapper< V, E > source, final int sourceOutIndex, final JunctionOverlayVertexWrapper< V, E > target, final int targetInIndex, final JunctionOverlayEdgeWrapper< V, E > ref )
	{
		ref.we = overlayProperties.insertEdge( source.wv, sourceOutIndex, target.wv, targetInIndex, ref.ref );
		return ref;
	}

	@Override
	public void remove( final JunctionOverlayEdgeWrapper< V, E > edge )
	{
		overlayProperties.removeEdge( edge.we );
	}

	@Override
	public void remove( final JunctionOverlayVertexWrapper< V, E > vertex )
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
	public RefBimap< V, JunctionOverlayVertexWrapper< V, E > > getVertexMap()
	{
		return vertexMap;
	}

	/**
	 * Get bidirectional mapping between model edges and view edges.
	 *
	 * @return bidirectional mapping between model edges and view edges.
	 */
	@Override
	public RefBimap< E, JunctionOverlayEdgeWrapper< V, E > > getEdgeMap()
	{
		return edgeMap;
	}

	private final RefPool< JunctionOverlayVertexWrapper< V, E > > vertexPool = new RefPool< JunctionOverlayVertexWrapper< V, E > >()
	{
		@Override
		public JunctionOverlayVertexWrapper< V, E > createRef()
		{
			return vertexRef();
		}

		@Override
		public void releaseRef( final JunctionOverlayVertexWrapper< V, E > v )
		{
			JunctionOverlayGraphWrapper.this.releaseRef( v );
		}

		@Override
		public JunctionOverlayVertexWrapper< V, E > getObject( final int index, final JunctionOverlayVertexWrapper< V, E > v )
		{
			v.wv = idmap.getVertex( index, v.ref );
			return v;
		}

		@Override
		public JunctionOverlayVertexWrapper< V, E > getObjectIfExists( final int index, final JunctionOverlayVertexWrapper< V, E > v )
		{
			v.wv = idmap.getVertexIfExists( index, v.ref );
			return v.wv == null ? null : v;
		}

		@Override
		public int getId( final JunctionOverlayVertexWrapper< V, E > v )
		{
			return idmap.getVertexId( v.wv );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< JunctionOverlayVertexWrapper< V, E > > getRefClass()
		{
			return ( Class ) JunctionOverlayVertexWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< JunctionOverlayVertexWrapper< V, E >, RefPool< JunctionOverlayVertexWrapper< V, E > > > vertices = new AbstractRefPoolCollectionWrapper< JunctionOverlayVertexWrapper< V, E >, RefPool< JunctionOverlayVertexWrapper< V, E > > >( vertexPool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.vertices().size();
		}

		@Override
		public Iterator< JunctionOverlayVertexWrapper< V, E > > iterator()
		{
			return new JunctionOverlayVertexIteratorWrapper<>( JunctionOverlayGraphWrapper.this, JunctionOverlayGraphWrapper.this.vertexRef(), wrappedGraph.vertices().iterator() );
		}
	};

	private final RefPool< JunctionOverlayEdgeWrapper< V, E > > edgePool = new RefPool< JunctionOverlayEdgeWrapper< V, E > >()
	{
		@Override
		public JunctionOverlayEdgeWrapper< V, E > createRef()
		{
			return edgeRef();
		}

		@Override
		public void releaseRef( final JunctionOverlayEdgeWrapper< V, E > e )
		{
			JunctionOverlayGraphWrapper.this.releaseRef( e );
		}

		@Override
		public JunctionOverlayEdgeWrapper< V, E > getObject( final int index, final JunctionOverlayEdgeWrapper< V, E > e )
		{
			e.we = idmap.getEdge( index, e.ref );
			return e;
		}

		@Override
		public JunctionOverlayEdgeWrapper< V, E > getObjectIfExists( final int index, final JunctionOverlayEdgeWrapper< V, E > e )
		{
			e.we = idmap.getEdgeIfExists( index, e.ref );
			return e.we == null ? null : e;
		}

		@Override
		public int getId( final JunctionOverlayEdgeWrapper< V, E > e )
		{
			return idmap.getEdgeId( e.we );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< JunctionOverlayEdgeWrapper< V, E > > getRefClass()
		{
			return ( Class ) JunctionOverlayEdgeWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< JunctionOverlayEdgeWrapper< V, E >, RefPool< JunctionOverlayEdgeWrapper< V, E > > > edges = new AbstractRefPoolCollectionWrapper< JunctionOverlayEdgeWrapper< V, E >, RefPool< JunctionOverlayEdgeWrapper< V, E > > >( edgePool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.edges().size();
		}

		@Override
		public Iterator< JunctionOverlayEdgeWrapper< V, E > > iterator()
		{
			return new JunctionOverlayEdgeIteratorWrapper<>( JunctionOverlayGraphWrapper.this, JunctionOverlayGraphWrapper.this.edgeRef(), wrappedGraph.edges().iterator() );
		}
	};

	public RefPool< JunctionOverlayVertexWrapper< V, E > > getVertexPool()
	{
		return vertexPool;
	}

	public RefPool< JunctionOverlayEdgeWrapper< V, E > > getEdgePool()
	{
		return edgePool;
	}
}
