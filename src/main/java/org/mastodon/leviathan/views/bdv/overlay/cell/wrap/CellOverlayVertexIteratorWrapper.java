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
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.Vertex;

public class CellOverlayVertexIteratorWrapper< V extends Vertex< E >, E extends Edge< V > >
		implements Iterator< CellOverlayVertexWrapper< V, E > >
{
	private final CellOverlayVertexWrapper< V, E > vertex;

	private Iterator< V > wrappedIterator;

	private final GraphIdBimap< V, E > idmap;

	public CellOverlayVertexIteratorWrapper(
			final CellOverlayGraphWrapper< V, E > graph,
			final CellOverlayVertexWrapper< V, E > vertex,
			final Iterator< V > wrappedIterator )
	{
		this.idmap = graph.idmap;
		this.vertex = vertex;
		this.wrappedIterator = wrappedIterator;
	}

	void wrap( final Iterator< V > iterator )
	{
		wrappedIterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return wrappedIterator.hasNext();
	}

	@Override
	public CellOverlayVertexWrapper< V, E > next()
	{
		vertex.wv = idmap.getVertex( idmap.getVertexId( wrappedIterator.next() ), vertex.ref );
		return vertex;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
