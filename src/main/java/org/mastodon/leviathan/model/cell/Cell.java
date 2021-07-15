package org.mastodon.leviathan.model.cell;

import org.mastodon.model.AbstractSpot;
import org.mastodon.model.HasLabel;
import org.mastodon.pool.ByteMappedElement;

public class Cell extends AbstractSpot< Cell, Link, CellPool, ByteMappedElement, CellGraph > implements HasLabel
{

	Cell( final CellPool pool )
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
	 * Initialize a new {@link Cell}.
	 * <p>
	 * <em>Note that this is equivalent to a constructor. It should be only
	 * called on newly created {@link Cell}s, and only once.</em>
	 *
	 * @param timepoint
	 *            the time-point of the junction.
	 * @param pos
	 *            position of the junction as a 2 element <code>double</code>
	 *            array.
	 * @return this {@link Cell}.
	 */
	public Cell init( final int timepoint, final double[] pos )
	{
		super.partialInit( timepoint, pos );
		super.initDone();
		return this;
	}

	@Override
	public String getLabel()
	{
		if ( pool.label.isSet( this ) )
			return pool.label.get( this );
		else
			return Integer.toString( getInternalPoolIndex() );
	}

	@Override
	public void setLabel( final String label )
	{
		pool.label.set( this, label );
	}

	public void setMembranes( final int[] ids )
	{
		pool.membranes.set( this, ids );
	}

	public int[] getMembranes()
	{
		return pool.membranes.get( this );
	}

	public void setBoundary( final double[] boundary )
	{
		pool.boundary.set( this, boundary );
	}

	public double[] getBoundary()
	{
		return pool.boundary.get( this );
	}

	@Override
	public String toString()
	{
		return String.format( "Cell( %d, X=%.2f, Y=%.2f, tp=%d )",
				getInternalPoolIndex(),
				getDoublePosition( 0 ),
				getDoublePosition( 1 ),
				getTimepoint() );
	}

}
