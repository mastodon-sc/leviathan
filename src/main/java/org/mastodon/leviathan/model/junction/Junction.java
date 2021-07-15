package org.mastodon.leviathan.model.junction;

import org.mastodon.model.AbstractSpot;
import org.mastodon.pool.ByteMappedElement;

public class Junction extends AbstractSpot< Junction, MembranePart, JunctionPool, ByteMappedElement, JunctionGraph >
{

	Junction( final JunctionPool pool )
	{
		super( pool );
	}

	void notifyVertexAdded()
	{
		super.initDone();
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
	 * @param timepoint
	 *            the time-point of the junction.
	 * @param pos
	 *            position of the junction as a 2 element <code>double</code>
	 *            array.
	 * @return this {@link Junction}.
	 */
	public Junction init( final int timepoint, final double[] pos )
	{
		super.partialInit( timepoint, pos );
		super.initDone();
		return this;
	}

	@Override
	public String toString()
	{
		return String.format( "Junction( %d, X=%.2f, Y=%.2f, tp=%d )",
				getInternalPoolIndex(),
				getDoublePosition( 0 ),
				getDoublePosition( 1 ),
				getTimepoint() );
	}

}
