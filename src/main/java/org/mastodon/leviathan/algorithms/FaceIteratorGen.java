package org.mastodon.leviathan.algorithms;

import java.util.Iterator;

import org.mastodon.Ref;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Edges;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;

import net.imglib2.RealLocalizable;

public class FaceIteratorGen< V extends Vertex< E > & Ref< V > & RealLocalizable, E extends Edge< V > & Ref< E > >
{

	private final ReadOnlyGraph< V, E > graph;

	public FaceIteratorGen( final ReadOnlyGraph< V, E > graph )
	{
		this.graph = graph;
	}

	public FaceIterator iterateCW( final E from )
	{
		return new FaceIterator( from, true );
	}

	public FaceIterator iterateCCW( final E from )
	{
		return new FaceIterator( from, false );
	}

	public class FaceIterator implements Iterator< E >
	{

		private final E start;

		private final E next;

		private final V pivot;

		private final V vref1;

		private final V vref2;

		private final E eref;

		private final E out;

		private final V oldpivot;

		private boolean started;

		private final boolean iscw;

		private FaceIterator( final E start, final boolean iscw )
		{
			this.start = start;
			this.iscw = iscw;
			this.next = graph.edgeRef();
			next.refTo( start );
			this.pivot = graph.vertexRef();
			start.getTarget( pivot );
			this.oldpivot = graph.vertexRef();
			oldpivot.refTo( pivot );
			this.vref1 = graph.vertexRef();
			this.vref2 = graph.vertexRef();
			this.out = graph.edgeRef();
			this.eref = graph.edgeRef();
			this.started = false;
		}

		private void prefetch()
		{
			started = true;
			final Edges< E > edges = pivot.edges();
			if ( edges.size() == 1 )
			{
				// This vertex has only 1 edge. So we walk back.
				GraphUtils.vertexAcross( next, pivot, vref1 );
				pivot.refTo( vref1 );
				return;
			}

			double thetaBound = iscw ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
			for ( final E candidate : edges )
			{
				if ( candidate.equals( next ) )
					continue;

				final double theta = GraphUtils.angle( pivot, next, candidate, vref1, vref2 );
				if ( iscw ? theta < thetaBound : theta > thetaBound )
				{
					thetaBound = theta;
					eref.refTo( candidate );
				}
			}
			next.refTo( eref );
			GraphUtils.vertexAcross( next, pivot, vref1 );
			oldpivot.refTo( pivot );
			pivot.refTo( vref1 );
		}

		@Override
		public boolean hasNext()
		{
			return !started || !next.equals( start );
		}

		@Override
		public E next()
		{
			out.refTo( next );
			prefetch();
			return out;
		}

		public boolean isCW()
		{
			final boolean cw = out.getTarget( vref1 ).equals( oldpivot );
			return iscw ? cw : !cw;
		}

		public boolean isCCW()
		{
			return !isCW();
		}
	}
}
