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
package org.mastodon.leviathan.model;

import org.mastodon.graph.io.GraphSerializer;
import org.mastodon.graph.ref.AbstractEdgePool;
import org.mastodon.graph.ref.AbstractVertexPool;
import org.mastodon.pool.PoolObjectAttributeSerializer;

class CellModelSerializer implements GraphSerializer< Cell, Link >
{
	private CellModelSerializer()
	{}

	private static CellModelSerializer instance = new CellModelSerializer();

	public static CellModelSerializer getInstance()
	{
		return instance;
	}

	private final CellSerializer vertexSerializer = new CellSerializer();

	private final LinkSerializer edgeSerializer = new LinkSerializer();

	@Override
	public CellSerializer getVertexSerializer()
	{
		return vertexSerializer;
	}

	@Override
	public LinkSerializer getEdgeSerializer()
	{
		return edgeSerializer;
	}


	static class CellSerializer extends PoolObjectAttributeSerializer< Cell >
	{
		public CellSerializer()
		{
			super(
					AbstractVertexPool.layout.getSizeInBytes(),
					CellPool.layout.getSizeInBytes() - AbstractVertexPool.layout.getSizeInBytes() );
		}

		@Override
		public void notifySet( final Cell vertex )
		{
			vertex.notifyVertexAdded();
		}
	}

	static class LinkSerializer extends PoolObjectAttributeSerializer< Link >
	{
		public LinkSerializer()
		{
			super(
					AbstractEdgePool.layout.getSizeInBytes(),
					LinkPool.layout.getSizeInBytes() - AbstractEdgePool.layout.getSizeInBytes() );
		}

		@Override
		public void notifySet( final Link edge )
		{
			edge.notifyEdgeAdded();
		}
	}
}
