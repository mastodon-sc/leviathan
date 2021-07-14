package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.pool.ByteMappedElement;

public class MembranePart extends AbstractListenableEdge< MembranePart, Junction, MembranePartPool, ByteMappedElement >
{

	public static final int UNINITIALIZED = -2;

	public static final int PERIMETER = -1;

	/**
	 * Initialize a new {@MembranePart MembranePart}.
	 *
	 * @return this {@MembranePart MembranePart}.
	 */
	public MembranePart init()
	{
		pool.cellIdCCW.set( this, UNINITIALIZED );
		pool.cellIdCW.set( this, UNINITIALIZED );
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

	public void setCellIdCCW( final int cellId )
	{
		pool.cellIdCCW.set( this, cellId );
	}

	public void setCellIdCW( final int cellId )
	{
		pool.cellIdCW.set( this, cellId );
	}

	public int getCellIdCCW()
	{
		return pool.cellIdCCW.get( this );
	}

	public int getCellIdCW()
	{
		return pool.cellIdCW.get( this );
	}
}
