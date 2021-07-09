package org.mastodon.leviathan.model;

import org.mastodon.model.AbstractSpotPool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.properties.Property;

public class JunctionPool extends AbstractSpotPool< Junction, MembranePart, ByteMappedElement, JunctionGraph >
{

	public static class JunctionLayout extends AbstractSpotLayout
	{
		public JunctionLayout()
		{
			super( 2 );
		}
	}

	public static final JunctionLayout layout = new JunctionLayout();

	JunctionPool( final int initialCapacity )
	{
		super( initialCapacity, layout, Junction.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ) );
	}

	@Override
	protected Junction createEmptyRef()
	{
		return new Junction( this );
	}

	public final Property< Junction > positionProperty()
	{
		return position;
	}
}
