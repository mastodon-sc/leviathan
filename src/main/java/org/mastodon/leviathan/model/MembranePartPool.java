package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractEdgePool;
import org.mastodon.graph.ref.AbstractListenableEdgePool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.properties.ObjPropertyMap;

public class MembranePartPool extends AbstractListenableEdgePool< MembranePart, Junction, ByteMappedElement >
{

	final ObjPropertyMap< MembranePart, double[] > pixels;

	MembranePartPool( final int initialCapacity, final JunctionPool vertexPool )
	{
		super( initialCapacity, AbstractEdgePool.layout, MembranePart.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ), vertexPool );
		pixels = new ObjPropertyMap< >( this );
		registerPropertyMap( pixels );
	}

	@Override
	protected MembranePart createEmptyRef()
	{
		return new MembranePart( this );
	}
}
