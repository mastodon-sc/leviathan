package org.mastodon.leviathan.model;

import org.mastodon.graph.GraphIdBimap;
import org.mastodon.model.AbstractModelGraph;
import org.mastodon.pool.ByteMappedElement;

public class JunctionGraph extends AbstractModelGraph< JunctionGraph, JunctionPool, MembranePartPool, Junction, MembranePart, ByteMappedElement >
{

	public JunctionGraph()
	{
		this( 1024 );
	}

	public JunctionGraph( final int initialCapacity )
	{
		super( new MembranePartPool( initialCapacity, new JunctionPool( initialCapacity ) ) );
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
