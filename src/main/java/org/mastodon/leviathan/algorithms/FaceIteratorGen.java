package org.mastodon.leviathan.algorithms;

import java.util.Iterator;

import org.mastodon.graph.ref.AllEdges;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.MembranePart;

public class FaceIteratorGen
{

	private final JunctionGraph graph;

	public FaceIteratorGen( final JunctionGraph graph )
	{
		this.graph = graph;
	}

	public FaceIterator iterateCW( final MembranePart from )
	{
		return new FaceIterator( from, true );
	}

	public FaceIterator iterateCCW( final MembranePart from )
	{
		return new FaceIterator( from, false );
	}

	public class FaceIterator implements Iterator< MembranePart >
	{

		private final MembranePart start;

		private final MembranePart next;

		private final Junction pivot;

		private final Junction vref1;

		private final Junction vref2;

		private final MembranePart eref;

		private final MembranePart out;

		private final Junction oldpivot;

		private boolean started;

		private final boolean iscw;

		private FaceIterator( final MembranePart start, final boolean iscw )
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
			final AllEdges< MembranePart > edges = pivot.edges();
			if ( edges.size() == 1 )
			{
				// This vertex has only 1 edge. So we walk back.
				JunctionGraphUtils.junctionAcross( next, pivot, vref1 );
				pivot.refTo( vref1 );
				return;
			}

			double thetaBound = iscw ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
			for ( final MembranePart candidate : edges )
			{
				if ( candidate.equals( next ) )
					continue;

				final double theta = angle( pivot, next, candidate, vref1, vref2 );
				if ( iscw ? theta < thetaBound : theta > thetaBound )
				{
					thetaBound = theta;
					eref.refTo( candidate );
				}
			}
			next.refTo( eref );
			JunctionGraphUtils.junctionAcross( next, pivot, vref1 );
			oldpivot.refTo( pivot );
			pivot.refTo( vref1 );
		}

		@Override
		public boolean hasNext()
		{
			return !started || !next.equals( start );
		}

		@Override
		public MembranePart next()
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

	public static final double angle(
					final Junction pivot,
					final MembranePart v1,
					final MembranePart v2,
					final Junction vref1,
					final Junction vref2 )
	{
		final Junction s1 = JunctionGraphUtils.junctionAcross( v1, pivot, vref1 );
		final double dx1 = pivot.getDoublePosition( 0 ) - s1.getDoublePosition( 0 );
		final double dy1 = pivot.getDoublePosition( 1 ) - s1.getDoublePosition( 1 );
		final double alpha1 = Math.atan2( dy1, dx1 );

		final Junction t2 = JunctionGraphUtils.junctionAcross( v2, pivot, vref2 );
		final double dx2 = t2.getDoublePosition( 0 ) - pivot.getDoublePosition( 0 );
		final double dy2 = t2.getDoublePosition( 1 ) - pivot.getDoublePosition( 1 );
		final double alpha2 = Math.atan2( dy2, dx2 );

		// Normalize from -pi to pi;
		double theta = alpha2 - alpha1;
		theta = theta - 2. * Math.PI * Math.floor( ( theta + Math.PI ) / ( 2. * Math.PI ) );
		return theta;
	}
}
