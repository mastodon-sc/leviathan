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

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayEdge;

public class JunctionOverlayEdgeWrapper< V extends Vertex< E >, E extends Edge< V > >
		implements JunctionOverlayEdge< JunctionOverlayEdgeWrapper< V, E >, JunctionOverlayVertexWrapper< V, E > >
{
	private final JunctionOverlayGraphWrapper< V, E > wrapper;

	final E ref;

	E we;

	private final JunctionOverlayProperties< V, E > overlayProperties;

	JunctionOverlayEdgeWrapper( final JunctionOverlayGraphWrapper< V, E > wrapper )
	{
		this.wrapper = wrapper;
		ref = wrapper.wrappedGraph.edgeRef();
		overlayProperties = wrapper.overlayProperties;
	}

	@Override
	public int getInternalPoolIndex()
	{
		return wrapper.idmap.getEdgeId( we );
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > refTo( final JunctionOverlayEdgeWrapper< V, E > obj )
	{
		we = wrapper.idmap.getEdge( obj.getInternalPoolIndex(), ref );
		return this;
	}

	@Override
	public JunctionOverlayEdgeWrapper< V, E > init()
	{
		overlayProperties.initEdge( we );
		return this;
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > getSource()
	{
		return getSource( wrapper.vertexRef() );
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > getSource( final JunctionOverlayVertexWrapper< V, E > vertex )
	{
		vertex.wv = we.getSource( vertex.ref );
		return vertex;
	}

	@Override
	public int getSourceOutIndex()
	{
		return we.getSourceOutIndex();
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > getTarget()
	{
		return getTarget( wrapper.vertexRef() );
	}

	@Override
	public JunctionOverlayVertexWrapper< V, E > getTarget( final JunctionOverlayVertexWrapper< V, E > vertex )
	{
		vertex.wv = we.getTarget( vertex.ref );
		return vertex;
	}

	@Override
	public int getTargetInIndex()
	{
		return we.getTargetInIndex();
	}

	@Override
	public int hashCode()
	{
		return we.hashCode();
	}

	@Override
	public boolean equals( final Object obj )
	{
		return obj instanceof JunctionOverlayEdgeWrapper< ?, ? > &&
				we.equals( ( (org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayEdgeWrapper< ?, ? > ) obj ).we );
	}

	/**
	 * Returns {@code this} if this {@link JunctionOverlayEdgeWrapper} currently wraps
	 * an {@code E}, or null otherwise.
	 *
	 * @return {@code this} if this {@link JunctionOverlayEdgeWrapper} currently wraps
	 *         an {@code E}, or null otherwise.
	 */
	JunctionOverlayEdgeWrapper< V, E > orNull()
	{
		return we == null ? null : this;
	}

	/**
	 * If called with a non-null {@link JunctionOverlayEdgeWrapper} returns the
	 * currently wrapped {@code E}, otherwise null.
	 *
	 * @return {@code null} if {@code wrapper == null}, otherwise the {@code E}
	 *         wrapped by {@code wrapper}.
	 */
	static < E extends Edge< ? > > E wrappedOrNull( final JunctionOverlayEdgeWrapper< ?, E > wrapper )
	{
		return wrapper == null ? null : wrapper.we;
	}

	@Override
	public void setPixels( final double[] pixels )
	{
		overlayProperties.setPixels( we, pixels );
	}

	@Override
	public double[] getPixels()
	{
		return overlayProperties.getPixels( we );
	}

	@Override
	public int getCellIdCCW()
	{
		return overlayProperties.getCellIdCCW( we );
	}

	@Override
	public int getCellIdCW()
	{
		return overlayProperties.getCellIdCW( we );
	}
}
