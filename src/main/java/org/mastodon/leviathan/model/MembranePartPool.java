package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableEdgePool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.pool.attributes.IntAttribute;
import org.mastodon.properties.ObjPropertyMap;

public class MembranePartPool extends AbstractListenableEdgePool< MembranePart, Junction, ByteMappedElement >
{

	public static class MembranePartLayout extends AbstractEdgeLayout
	{

		final IntField cellIdCW = intField();

		final IntField cellIdCCW = intField();
	}

	public static final MembranePartLayout layout = new MembranePartLayout();

	final IntAttribute< MembranePart > cellIdCW = new IntAttribute<>( layout.cellIdCW, this );

	final IntAttribute< MembranePart > cellIdCCW = new IntAttribute<>( layout.cellIdCCW, this );

	final ObjPropertyMap< MembranePart, double[] > pixels;

	MembranePartPool( final int initialCapacity, final JunctionPool vertexPool )
	{
		super( initialCapacity, layout, MembranePart.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ), vertexPool );
		pixels = new ObjPropertyMap< >( this );
		registerPropertyMap( pixels );
	}

	@Override
	protected MembranePart createEmptyRef()
	{
		return new MembranePart( this );
	}

	public IntAttribute< MembranePart > cellIdCW()
	{
		return cellIdCW;
	}

	public IntAttribute< MembranePart > cellIdCCW()
	{
		return cellIdCCW;
	}
}
