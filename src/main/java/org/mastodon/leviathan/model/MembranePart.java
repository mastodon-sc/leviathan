package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.spatial.HasTimepoint;

public class MembranePart extends AbstractListenableEdge< MembranePart, Junction, MembranePartPool, ByteMappedElement > implements HasTimepoint
{

	MembranePart( final MembranePartPool pool )
	{
		super( pool );
	}

	protected void notifyEdgeAdded()
	{
		super.initDone();
	}

	@Override
	public int getTimepoint()
	{
		return pool.timepoint;
	}
}
