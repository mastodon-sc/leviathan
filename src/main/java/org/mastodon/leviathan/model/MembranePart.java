package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.pool.ByteMappedElement;

public class MembranePart extends AbstractListenableEdge< MembranePart, Junction, MembranePartPool, ByteMappedElement >
{

	/**
	 * Initialize a new {@MembranePart MembranePart}.
	 *
	 * @return this {@MembranePart MembranePart}.
	 */
	public MembranePart init()
	{
		super.initDone();
		return this;
	}

	@Override
	public String toString()
	{
		return String.format( "MembranePart( %d -> %d )", getSource().getInternalPoolIndex(), getTarget().getInternalPoolIndex() );
	}

	MembranePart( final MembranePartPool pool )
	{
		super( pool );
	}

	protected void notifyEdgeAdded()
	{
		super.initDone();
	}

	public void setPixels( final double[] pixels )
	{
		pool.pixels.set( this, pixels );
	}

	public double[] getPixels()
	{
		return pool.pixels.get( this );
	}
}
