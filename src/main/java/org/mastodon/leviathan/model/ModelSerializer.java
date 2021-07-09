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

class JunctionModelSerializer implements GraphSerializer< Junction, MembranePart >
{
	private JunctionModelSerializer()
	{}

	private static JunctionModelSerializer instance = new JunctionModelSerializer();

	public static JunctionModelSerializer getInstance()
	{
		return instance;
	}

	private final JunctionSerializer vertexSerializer = new JunctionSerializer();

	private final MembranePartSerializer edgeSerializer = new MembranePartSerializer();

	@Override
	public JunctionSerializer getVertexSerializer()
	{
		return vertexSerializer;
	}

	@Override
	public MembranePartSerializer getEdgeSerializer()
	{
		return edgeSerializer;
	}


	static class JunctionSerializer extends PoolObjectAttributeSerializer< Junction >
	{
		public JunctionSerializer()
		{
			super(
					AbstractVertexPool.layout.getSizeInBytes(),
					JunctionPool.layout.getSizeInBytes() - AbstractVertexPool.layout.getSizeInBytes() );
		}

		@Override
		public void notifySet( final Junction vertex )
		{
			vertex.notifyVertexAdded();
		}
	}

	static class MembranePartSerializer extends PoolObjectAttributeSerializer< MembranePart >
	{
		public MembranePartSerializer()
		{
			super(
					AbstractEdgePool.layout.getSizeInBytes(),
					MembranePartPool.layout.getSizeInBytes() - AbstractEdgePool.layout.getSizeInBytes() );
		}

		@Override
		public void notifySet( final MembranePart edge )
		{
			edge.notifyEdgeAdded();
		}
	}
}
