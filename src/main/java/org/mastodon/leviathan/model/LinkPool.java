package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractEdgePool;
import org.mastodon.graph.ref.AbstractListenableEdgePool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;

public class LinkPool extends AbstractListenableEdgePool< Link, Cell, ByteMappedElement >
{

	LinkPool( final int initialCapacity, final CellPool vertexPool )
	{
		super( initialCapacity, AbstractEdgePool.layout, Link.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ), vertexPool );
	}

	@Override
	protected Link createEmptyRef()
	{
		return new Link( this );
	}
}
