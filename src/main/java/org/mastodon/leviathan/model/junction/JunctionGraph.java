package org.mastodon.leviathan.model.junction;

import org.mastodon.graph.GraphIdBimap;
import org.mastodon.io.properties.ObjPropertyMapSerializer;
import org.mastodon.io.properties.PropertyMapSerializers;
import org.mastodon.model.AbstractModelGraph;
import org.mastodon.pool.ByteMappedElement;

public class JunctionGraph extends AbstractModelGraph< JunctionGraph, JunctionPool, MembranePartPool, Junction, MembranePart, ByteMappedElement >
{

	protected final PropertyMapSerializers< MembranePart > edgePropertySerializers;

	public JunctionGraph()
	{
		this( 1024 );
	}

	public JunctionGraph( final int initialCapacity )
	{
		super( new MembranePartPool( initialCapacity, new JunctionPool( initialCapacity ) ) );

		edgePropertySerializers = new PropertyMapSerializers<>();
		edgePropertySerializers.put( "pixels", new ObjPropertyMapSerializer<>( edgePool.pixels ) );
	}

	JunctionPool getVertexPool()
	{
		return vertexPool;
	}

	MembranePartPool getEdgePool()
	{
		return edgePool;
	}

	GraphIdBimap< Junction, MembranePart > idmap()
	{
		return idmap;
	}
}
