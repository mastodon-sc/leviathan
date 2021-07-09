package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableVertex;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.attributes.RealPointAttributeValue;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.util.DelegateRealLocalizable;
import org.mastodon.util.DelegateRealPositionable;

public class Junction extends AbstractListenableVertex< 
			Junction, 
			MembranePart, 
			JunctionPool, 
			ByteMappedElement >
		implements DelegateRealLocalizable, DelegateRealPositionable, HasTimepoint
{

	private final RealPointAttributeValue position;

	Junction( final JunctionPool pool )
	{
		super( pool );
		position = pool.position.createAttributeValue( this );
	}

	@Override
	public RealPointAttributeValue delegate()
	{
		return position;
	}

	/*
	 * Public API
	 */

	/**
	 * Initialize a new {@link Junction}.
	 * <p>
	 * <em>Note that this is equivalent to a constructor. It should be only
	 * called on newly created {@link Junction}s, and only once.</em>
	 * 
	 * @param x
	 *            the junction x position.
	 * @param y
	 *            the junction y position.
	 *
	 * @return this {@link Junction}.
	 */
	public Junction init( final double x, final double y )
	{
		pool.position.setPositionQuiet( this, x, 0 );
		pool.position.setPositionQuiet( this, y, 1 );
		super.initDone();
		return this;
	}

	void notifyVertexAdded()
	{
		super.initDone();
	}

	@Override
	public int numDimensions()
	{
		return pool.numDimensions();
	}

	@Override
	public int getTimepoint()
	{
		return pool.timepoint;
	}
}
