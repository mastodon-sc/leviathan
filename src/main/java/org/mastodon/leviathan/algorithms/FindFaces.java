package org.mastodon.leviathan.algorithms;

import java.util.Iterator;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.leviathan.algorithms.FaceIteratorGen.FaceIterator;
import org.mastodon.leviathan.model.Cell;
import org.mastodon.leviathan.model.CellGraph;
import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.MembranePart;

public class FindFaces
{

	private static final int DEFAULT_MAX_NPARTS = 20;

	private final JunctionGraph junctionGraph;

	private final CellGraph cellGraph;

	private final FaceIteratorGen itgen;

	private final int maxNParts;

	/**
	 * Creates a {@link CellGraph} from the specified junction graph. Cells are
	 * created from the faces of the junction graph. There is also a big cell
	 * created for the periphery of the tissue.
	 * 
	 * TODO deal with time later.
	 * 
	 * @param junctionGraph
	 *            the junction graph.
	 * @return a new cell graph.
	 */
	public static final CellGraph findFaces( final JunctionGraph junctionGraph )
	{
		final CellGraph cellGraph = new CellGraph();
		findFaces( junctionGraph, cellGraph );
		return cellGraph;
	}

	public static void findFaces( final JunctionGraph junctionGraph, final CellGraph cellGraph )
	{
		final FindFaces faceFinder = new FindFaces( junctionGraph, cellGraph, DEFAULT_MAX_NPARTS );
		faceFinder.process();
	}

	private FindFaces( final JunctionGraph junctionGraph, final CellGraph cellGraph, final int maxNParts )
	{
		this.junctionGraph = junctionGraph;
		this.cellGraph = cellGraph;
		this.maxNParts = maxNParts;
		this.itgen = new FaceIteratorGen( junctionGraph );
	}

	private void process()
	{

		// Reset junction graph.
		for ( final MembranePart edge : junctionGraph.edges() )
		{
			edge.setCellIdCCW( MembranePart.UNINITIALIZED );
			edge.setCellIdCW( MembranePart.UNINITIALIZED );
		}

		final Junction vref1 = junctionGraph.vertexRef();
		final Junction vref2 = junctionGraph.vertexRef();
		final Cell cref = cellGraph.vertexRef();
		final RefList< MembranePart > face = RefCollections.createRefList( junctionGraph.edges() );
		final double[] pos = new double[ 2 ];

		for ( final MembranePart edge : junctionGraph.edges() )
		{
			if ( edge.getCellIdCW() == MembranePart.UNINITIALIZED )
				processEdge( edge, face, true, pos, vref1, vref2, cref );
			if ( edge.getCellIdCCW() == MembranePart.UNINITIALIZED )
				processEdge( edge, face, false, pos, vref1, vref2, cref );
		}

		junctionGraph.releaseRef( vref1 );
		junctionGraph.releaseRef( vref2 );
		cellGraph.releaseRef( cref );

		// Prune large cells.
		final MembranePart eref = junctionGraph.edgeRef();
		final RefList< Cell > toRemove = RefCollections.createRefList( cellGraph.vertices() );
		for ( final Cell cell : cellGraph.vertices() )
		{
			final int[] mbids = cell.getMembranes();
			if ( mbids.length > maxNParts )
			{
				// Remove it from the cell graph, and edit the cell if stored in
				// the membrane parts.
				toRemove.add( cell );
				final int cellid = cell.getInternalPoolIndex();
				for ( final int mbid : mbids )
				{
					final MembranePart mb = junctionGraph.getGraphIdBimap().getEdge( mbid, eref );
					if (mb.getCellIdCW() == cellid)
						mb.setCellIdCW( MembranePart.PERIMETER );
					if ( mb.getCellIdCCW() == cellid )
						mb.setCellIdCCW( MembranePart.PERIMETER );
				}
			}
		}
		junctionGraph.releaseRef( eref );

		for ( final Cell cell : toRemove )
			cellGraph.remove( cell );
	}

	private void processEdge(
			final MembranePart edge,
			final RefList< MembranePart > face,
			final boolean iscw,
			final double[] pos,
			final Junction vref1,
			final Junction vref2,
			final Cell cref )
	{
		// Create cell with dummy position for now.
		pos[ 0 ] = 0.;
		pos[ 1 ] = 0.;
		final int timepoint = edge.getSource( vref1 ).getTimepoint();
		final Cell cell = cellGraph.addVertex( cref ).init( timepoint, pos );
		final int cellId = cell.getInternalPoolIndex();

		// Get membrane for face and set cell id for membranes.
		face.clear();
		final FaceIterator it = iscw ? itgen.iterateCW( edge ) : itgen.iterateCCW( edge );
		while ( it.hasNext() )
		{
			final MembranePart mb = it.next();
			face.add( mb );
			if ( it.isCW() )
				mb.setCellIdCW( cellId );
			else
				mb.setCellIdCCW( cellId );
		}

		// Compute cell position from membranes.
		getCentroid( face, pos, vref1, vref2 );
		cell.setPosition( pos );

		// Store membrane ids.
		cell.setMembranes( face
				.stream()
				.mapToInt( MembranePart::getInternalPoolIndex )
				.toArray() );
	}

	private void getCentroid(
			final RefList< MembranePart > face,
			final double[] pos,
			final Junction vref1,
			final Junction vref2 )
	{
		assert !face.isEmpty();

		pos[ 0 ] = 0.;
		pos[ 1 ] = 0.;
		final Iterator< MembranePart > it = face.iterator();
		final MembranePart start = it.next();
		if ( face.size() == 1 )
		{
			// Cell made of one edge: a solitary cell.
			final double[] pixels = start.getPixels();
			for ( int i = 0; i < pixels.length; i = i + 2 )
			{
				final double x = pixels[ i ];
				final double y = pixels[ i + 1 ];
				pos[ 0 ] += x;
				pos[ 1 ] += y;
			}
			pos[ 0 ] /= pixels.length / 2;
			pos[ 1 ] /= pixels.length / 2;
			return;
		}

		final Junction source = start.getSource( vref1 );
		pos[ 0 ] += source.getDoublePosition( 0 );
		pos[ 1 ] += source.getDoublePosition( 1 );
		final Junction other = JunctionGraphUtils.junctionAcross( start, source, vref2 );
		while ( it.hasNext() )
		{
			pos[ 0 ] += other.getDoublePosition( 0 );
			pos[ 1 ] += other.getDoublePosition( 1 );
			final MembranePart next = it.next();
			JunctionGraphUtils.junctionAcross( next, other, vref1 );
			other.refTo( vref1 );
		}
		pos[ 0 ] /= face.size();
		pos[ 1 ] /= face.size();
	}
}
