package org.mastodon.leviathan.model;

import org.mastodon.graph.ref.AbstractListenableVertexPool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.pool.attributes.RealPointAttribute;
import org.mastodon.properties.Property;

import net.imglib2.EuclideanSpace;

public class JunctionPool extends AbstractListenableVertexPool< 
		Junction, 
		MembranePart, 
		ByteMappedElement > 
	implements EuclideanSpace
{

	public static class JunctionLayout extends AbstractVertexLayout
	{
		final DoubleArrayField position;

		public JunctionLayout()
		{
			position = doubleArrayField( 2 );
		}
	}

	public static final JunctionLayout layout = new JunctionLayout();

	final RealPointAttribute< Junction > position;

	final int timepoint;

	JunctionPool( final int timepoint, final int initialCapacity )
	{
		super( initialCapacity, layout, Junction.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ) );
		this.timepoint = timepoint;
		this.position = new RealPointAttribute<>( layout.position, this );
	}

	@Override
	public int numDimensions()
	{
		return layout.position.numElements();
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
