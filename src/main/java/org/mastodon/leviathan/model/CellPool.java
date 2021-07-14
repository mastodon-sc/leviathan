package org.mastodon.leviathan.model;

import org.mastodon.model.AbstractSpotPool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.pool.attributes.DoubleAttribute;
import org.mastodon.properties.ObjPropertyMap;
import org.mastodon.properties.Property;

public class CellPool extends AbstractSpotPool< Cell, Link, ByteMappedElement, CellGraph >
{

	public static class CellLayout extends AbstractSpotLayout
	{
		public CellLayout()
		{
			super( 2 );
		}

		final DoubleField boundingSphereRadiusSqu = doubleField();
	}

	public static final CellLayout layout = new CellLayout();

	final DoubleAttribute< Cell > boundingSphereRadiusSqu = new DoubleAttribute<>( layout.boundingSphereRadiusSqu, this );

	final ObjPropertyMap< Cell, int[] > membranes;

	final ObjPropertyMap< Cell, String > label;

	CellPool( final int initialCapacity )
	{
		super( initialCapacity, layout, Cell.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ) );
		membranes = new ObjPropertyMap<>( this );
		registerPropertyMap( membranes );
		label = new ObjPropertyMap<>( this );
		registerPropertyMap( label );
	}

	@Override
	protected Cell createEmptyRef()
	{
		return new Cell( this );
	}

	public final Property< Cell > positionProperty()
	{
		return position;
	}

	public final Property< Cell > boundingSphereRadiusSquProperty()
	{
		return boundingSphereRadiusSqu;
	}

	public final Property< Cell > membraneProperty()
	{
		return membranes;
	}

	public final Property< Cell > labelProperty()
	{
		return label;
	}
}
