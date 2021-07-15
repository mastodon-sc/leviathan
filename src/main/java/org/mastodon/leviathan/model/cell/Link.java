package org.mastodon.leviathan.model.cell;

import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.pool.ByteMappedElement;

public class Link extends AbstractListenableEdge< Link, Cell, LinkPool, ByteMappedElement >
{

	/**
	 * Initialize a new {@MembranePart MembranePart}.
	 *
	 * @return this {@MembranePart MembranePart}.
	 */
	public Link init()
	{
		super.initDone();
		return this;
	}

	@Override
	public String toString()
	{
		return String.format( "Link( %d -> %d )", getSource().getInternalPoolIndex(), getTarget().getInternalPoolIndex() );
	}

	Link( final LinkPool pool )
	{
		super( pool );
	}

	protected void notifyEdgeAdded()
	{
		super.initDone();
	}
}
