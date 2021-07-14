package org.mastodon.leviathan.model;

import org.mastodon.graph.GraphIdBimap;
import org.mastodon.io.properties.ObjPropertyMapSerializer;
import org.mastodon.io.properties.StringPropertyMapSerializer;
import org.mastodon.model.AbstractModelGraph;
import org.mastodon.pool.ByteMappedElement;

public class CellGraph extends AbstractModelGraph< CellGraph, CellPool, LinkPool, Cell, Link, ByteMappedElement >
{

	public CellGraph()
	{
		this( 1024 );
	}

	public CellGraph( final int initialCapacity )
	{
		super( new LinkPool( initialCapacity, new CellPool( initialCapacity ) ) );

		vertexPropertySerializers.put( "membranes", new ObjPropertyMapSerializer<>( vertexPool.membranes ) );
		vertexPropertySerializers.put( "label", new StringPropertyMapSerializer<>( vertexPool.label ) );
	}

	CellPool getVertexPool()
	{
		return vertexPool;
	}

	LinkPool getEdgePool()
	{
		return edgePool;
	}

	GraphIdBimap< Cell, Link > idmap()
	{
		return idmap;
	}
}
