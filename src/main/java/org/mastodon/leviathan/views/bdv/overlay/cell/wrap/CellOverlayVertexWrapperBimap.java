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

import org.mastodon.adapter.RefBimap;
import org.mastodon.collection.RefCollection;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;

public class CellOverlayVertexWrapperBimap< V extends Vertex< E >, E extends Edge< V > >
		implements RefBimap< V, CellOverlayVertexWrapper< V, E > >
{
	private final RefCollection< CellOverlayVertexWrapper< V, E > > vertices;

	public CellOverlayVertexWrapperBimap( final CellOverlayGraphWrapper< V, E > graph )
	{
		this.vertices = graph.vertices();
	}

	@Override
	public V getLeft( final CellOverlayVertexWrapper< V, E > right )
	{
		return right == null ? null : right.wv;
	}

	@Override
	public CellOverlayVertexWrapper< V, E > getRight( final V left, final CellOverlayVertexWrapper< V, E > ref )
	{
		ref.wv = left;
		return ref.orNull();
	}

	@Override
	public V reusableLeftRef( final CellOverlayVertexWrapper< V, E > right )
	{
		return right.ref;
	}

	@Override
	public CellOverlayVertexWrapper< V, E > reusableRightRef()
	{
		return vertices.createRef();
	}

	@Override
	public void releaseRef( final CellOverlayVertexWrapper< V, E > ref )
	{
		vertices.releaseRef( ref );
	}
}
