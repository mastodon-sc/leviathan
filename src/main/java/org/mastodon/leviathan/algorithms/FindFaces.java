package org.mastodon.leviathan.algorithms;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.leviathan.model.cell.Cell;
import org.mastodon.leviathan.model.cell.CellGraph;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;

import gnu.trove.set.hash.TIntHashSet;

public class FindFaces
{

	private final JunctionGraph junctionGraph;

	private final CellGraph cellGraph;

	private final MembraneConcatenator mbcat;

	private final FaceIteratorGen< Junction, MembranePart > itgen;

	/**
	 * Creates a {@link CellModel} from the junction graph in the specified
	 * junction model. Cells are created from the faces of the junction graph.
	 * The cell corresponding to the perimeter of the tissue is cleared.
	 * 
	 * @param junctionModel
	 *            the junction model.
	 * @return a new cell model.
	 */
	public static final CellModel findFaces( final JunctionModel junctionModel )
	{
		final CellModel cellModel = new CellModel( junctionModel.getSpaceUnits(), junctionModel.getSpaceUnits() );
		final CellGraph cellGraph = cellModel.getGraph();
		findFaces( junctionModel.getGraph(), cellGraph );
		return cellModel;
	}

	/**
	 * Add cell vertices to the specified cell graph from the specified junction
	 * graph. Cells are created from the faces of the junction graph. The cell
	 * corresponding to the perimeter of the tissue is cleared. The specified
	 * cell graph should be cleared before calling this method.
	 * 
	 * @param junctionGraph
	 *            the junction graph.
	 * @param cellGraph
	 *            the cell graph/
	 */
	public static void findFaces( final JunctionGraph junctionGraph, final CellGraph cellGraph )
	{
		final FindFaces faceFinder = create( junctionGraph, cellGraph );
		faceFinder.process();
	}

	public static FindFaces create( final JunctionGraph junctionGraph, final CellGraph cellGraph )
	{
		return new FindFaces( junctionGraph, cellGraph );
	}

	private FindFaces( final JunctionGraph junctionGraph, final CellGraph cellGraph )
	{
		this.junctionGraph = junctionGraph;
		this.cellGraph = cellGraph;
		this.itgen = new FaceIteratorGen<>( junctionGraph );
		this.mbcat = new MembraneConcatenator();
	}

	private void process()
	{
		resetJunctionGraph();
		discoverCells();
		removePerimeterCell();
	}

	private void resetJunctionGraph()
	{
		// Reset junction graph.
		for ( final MembranePart edge : junctionGraph.edges() )
		{
			edge.setCellIdCCW( MembranePart.UNINITIALIZED );
			edge.setCellIdCW( MembranePart.UNINITIALIZED );
		}
	}

	private void discoverCells()
	{

		final Junction vref1 = junctionGraph.vertexRef();
		final Junction vref2 = junctionGraph.vertexRef();
		final Cell cref = cellGraph.vertexRef();
		final RefList< MembranePart > face = RefCollections.createRefList( junctionGraph.edges() );
		for ( final MembranePart edge : junctionGraph.edges() )
		{
			if ( edge.getCellIdCW() == MembranePart.UNINITIALIZED )
				processEdge( edge, face, true, vref1, vref2, cref );
			if ( edge.getCellIdCCW() == MembranePart.UNINITIALIZED )
				processEdge( edge, face, false, vref1, vref2, cref );
		}

		junctionGraph.releaseRef( vref1 );
		junctionGraph.releaseRef( vref2 );
		cellGraph.releaseRef( cref );
	}

	/**
	 * Remove the perimeter cell. We detect it as the largest (in area) cell
	 * that is next to a junction on the convex hull of the tissue.
	 */
	private void removePerimeterCell()
	{
		// Get a junction on the convex hull. One with the minimal Y value will
		// do.
		double minY = Double.POSITIVE_INFINITY;
		final Junction lowestJunction = junctionGraph.vertexRef();
		for ( final Junction j : junctionGraph.vertices() )
		{
			final double y = j.getDoublePosition( 1 );
			if ( y < minY )
			{
				minY = y;
				lowestJunction.refTo( j );
			}
		}

		// Get all the cells around this junction.
		final TIntHashSet idset = new TIntHashSet();
		for ( final MembranePart mb : lowestJunction.edges() )
		{
			idset.add( mb.getCellIdCW() );
			idset.add( mb.getCellIdCCW() );
		}
		junctionGraph.releaseRef( lowestJunction );

		final RefList< Cell > neighborCells = RefCollections.createRefList( cellGraph.vertices() );
		final Cell cref = cellGraph.vertexRef();
		for ( final int id : idset.toArray() )
		{
			final Cell cell = cellGraph.getGraphIdBimap().getVertex( id, cref );
			neighborCells.add( cell );
		}
		
		// Find the one with the largest area.
		double maxArea = Double.NEGATIVE_INFINITY;
		for ( final Cell cell : neighborCells )
		{
			final double area = Math.abs( GraphUtils.signedArea( cell ) );
			if ( area > maxArea )
			{
				maxArea = area;
				cref.refTo( cell );
			}
		}

		// Delete it.
		cellGraph.remove( cref );
		cellGraph.releaseRef( cref );
	}

	private void processEdge(
			final MembranePart edge,
			final RefList< MembranePart > face,
			final boolean iscw,
			final Junction vref1,
			final Junction vref2,
			final Cell cref )
	{
		// Create cell with dummy position for now.
		final double[] pos = new double[ 2 ];
		final int timepoint = edge.getSource( vref1 ).getTimepoint();
		final Cell cell = cellGraph.addVertex( cref ).init( timepoint, pos );
		final int cellId = cell.getInternalPoolIndex();

		// Get membrane for face and set cell id for membranes.
		// And compute centroid.
		face.clear();
		final FaceIteratorGen< Junction, MembranePart >.FaceIterator it = iscw ? itgen.iterateCW( edge ) : itgen.iterateCCW( edge );
		while ( it.hasNext() )
		{
			final MembranePart mb = it.next();
			face.add( mb );
			if ( it.isCW() )
				mb.setCellIdCW( cellId );
			else
				mb.setCellIdCCW( cellId );
		}

		// Store membrane ids.
		cell.setMembranes( face
				.stream()
				.mapToInt( MembranePart::getInternalPoolIndex )
				.toArray() );

		// Correct centroid position.
		mbcat.getCentroid( face, pos, vref1, vref2 );
		cell.setPosition( pos );

		// Store cell boundary.
		final double[] boundary = mbcat.getBoundary( face, pos );
		cell.setBoundary( boundary );
	}

	/**
	 * Returns the new {@link MembranePart} added when splitting the cells.
	 * Returns <code>null</code> if the specified source and target junctions
	 * are not on a cell boundary.
	 * 
	 * @param source
	 *            the source junction.
	 * @param target
	 *            the target junction.
	 * @param eref
	 *            a ref to use to return the new {@link MembranePart}.
	 * @return the new {@link MembranePart}.
	 */
	public MembranePart split( final Junction source, final Junction target, final MembranePart eref )
	{
		// Sanity check. Are the source and target really connected by a cell?
		boolean iscw = true;
		boolean connected = false;
		final MembranePart connectingEdge = junctionGraph.edgeRef();
		final Junction jref = junctionGraph.vertexRef();
		for ( final MembranePart mb : source.edges() )
		{
			final FaceIteratorGen< Junction, MembranePart >.FaceIterator itcw = itgen.iterateCW( mb );
			while ( itcw.hasNext() )
			{
				final MembranePart e = itcw.next();
				if ( e.getSource( jref ).equals( target ) || e.getTarget( jref ).equals( target ) )
				{
					connectingEdge.refTo( mb );
					iscw = true;
					connected = true;
					break;
				}
			}
			if ( connected )
				break;
			final FaceIteratorGen< Junction, MembranePart >.FaceIterator itccw = itgen.iterateCCW( mb );
			while ( itccw.hasNext() )
			{
				final MembranePart e = itccw.next();
				if ( e.getSource( jref ).equals( target ) || e.getTarget( jref ).equals( target ) )
				{
					connectingEdge.refTo( mb );
					iscw = false;
					connected = true;
					break;
				}
			}
			if ( connected )
				break;
		}
		junctionGraph.releaseRef( jref );
		if ( !connected )
			return null;

		// Refs.
		final Junction vref1 = junctionGraph.vertexRef();
		final Junction vref2 = junctionGraph.vertexRef();
		final Cell cref1 = cellGraph.vertexRef();
		final Cell cref2 = cellGraph.vertexRef();

		// What cell shall we split?
		final int cellid = iscw ? connectingEdge.getCellIdCW() : connectingEdge.getCellIdCCW();
		final Cell cell = cellGraph.getGraphIdBimap().getVertex( cellid, cref1 );

		// Add the new membrane part.
		final MembranePart newEdge = junctionGraph.addEdge( source, target, eref ).init();
		// Default pixels: the junctions.
		final double[] pixels = new double[ 4 ];
		final double[] pos = new double[ source.numDimensions() ];
		source.localize( pos );
		pixels[ 0 ] = pos[ 0 ];
		pixels[ 1 ] = pos[ 1 ];
		target.localize( pos );
		pixels[ 2 ] = pos[ 0 ];
		pixels[ 3 ] = pos[ 1 ];
		newEdge.setPixels( pixels );

		// Create and add two new cells around the new edge.
		final RefList< MembranePart > face = RefCollections.createRefList( junctionGraph.edges() );

		processEdge( newEdge, face, true, vref1, vref2, cref2 );
		processEdge( newEdge, face, false, vref1, vref2, cref2 );

		// Remove the cell we just split.
		cellGraph.remove( cell );

		// Return.
		junctionGraph.releaseRef( vref1 );
		junctionGraph.releaseRef( vref2 );
		cellGraph.releaseRef( cref1 );
		cellGraph.releaseRef( cref2 );
		return newEdge;
	}

	public Cell merge( final MembranePart mb, final Cell cellref )
	{
		final Cell cref = cellGraph.vertexRef();

		// Get timepoint.
		final Junction jref = junctionGraph.vertexRef();
		final int timepoint = mb.getSource( jref ).getTimepoint();
		junctionGraph.releaseRef( jref );

		// Collect membrane ids for the CW and remove corresponding cell.
		final int cellIdCW = mb.getCellIdCW();
		final Cell cellCW = cellGraph.getGraphIdBimap().getVertex( cellIdCW, cref );
		final int[] mbsCW = cellCW.getMembranes();
		cellGraph.remove( cellCW );

		// Collect membrane ids for the CCW and remove corresponding cell.
		final int cellIdCCW = mb.getCellIdCCW();
		final Cell cellCCW = cellGraph.getGraphIdBimap().getVertex( cellIdCCW, cref );
		final int[] mbsCCW = cellCCW.getMembranes();
		cellGraph.remove( cellCCW );

		// Create new membrane array.
		final int[] membranes = new int[ mbsCW.length + mbsCCW.length - 2 ];
		int id = 0;
		for ( int i = 0; i < mbsCW.length; i++ )
		{
			final int mbid = mbsCW[ i ];
			if ( mbid == mb.getInternalPoolIndex() )
				continue;

			membranes[ id++ ] = mbid;
		}
		for ( int i = 0; i < mbsCCW.length; i++ )
		{
			final int mbid = mbsCCW[ i ];
			if ( mbid == mb.getInternalPoolIndex() )
				continue;

			membranes[ id++ ] = mbid;
		}

		// Create cell with dummay position.
		final double[] pos = new double[ 2 ];
		final Cell newCell = cellGraph.addVertex( cellref ).init( timepoint, pos );

		// Set membrane ids.
		newCell.setMembranes( membranes );

		/*
		 * Now we need to find an edge that we won't be deleting and that will
		 * be used to iterate around the cell.
		 */
		final MembranePart tostartwith = junctionGraph.edgeRef();
		boolean found = false;
		boolean iscw = false;
		final FaceIteratorGen< Junction, MembranePart >.FaceIterator it1 = itgen.iterateCW( mb );
		it1.next(); // Skip first (the one we will remove).
		while ( it1.hasNext() )
		{
			final MembranePart e = it1.next();
			if ( e.getCellIdCW() == cellIdCW )
			{
				found = true;
				iscw = true;
				tostartwith.refTo( e );
				break;
			}
			if ( found )
				break;
		}
		if ( !found )
		{
			// Try walking in the other direction.
			final FaceIteratorGen< Junction, MembranePart >.FaceIterator it2 = itgen.iterateCCW( mb );
			it2.next(); // Skip first (the one we will remove).
			while ( it2.hasNext() )
			{
				final MembranePart e = it2.next();
				if ( e.getCellIdCCW() == cellIdCCW )
				{
					found = true;
					iscw = false;
					tostartwith.refTo( e );
					break;
				}
				if ( found )
					break;
			}
		}
		if ( !found )
			throw new IllegalStateException( "Could not find an edge next to the one to remove." );

		// Remove edge that set the merge.
		junctionGraph.remove( mb );

		// Iterate around the edge to start with and set the cell properly.
		final RefList< MembranePart > face = RefCollections.createRefList( junctionGraph.edges() );
		final FaceIteratorGen< Junction, MembranePart >.FaceIterator it3 = iscw
				? itgen.iterateCW( tostartwith )
				: itgen.iterateCCW( tostartwith );
		while ( it3.hasNext() )
		{
			// Build new face array.
			final MembranePart e = it3.next();
			face.add( e );
			// Replace cell id.
			if ( e.getCellIdCW() == cellIdCW )
				e.setCellIdCW( newCell.getInternalPoolIndex() );
			else if ( e.getCellIdCCW() == cellIdCCW )
				e.setCellIdCCW( newCell.getInternalPoolIndex() );
		}

		final Junction vref1 = junctionGraph.vertexRef();
		final Junction vref2 = junctionGraph.vertexRef();
		mbcat.getCentroid( face, pos, vref1, vref2 );
		final double[] boundary = mbcat.getBoundary( face, pos );

		newCell.setPosition( pos );
		newCell.setBoundary( boundary );

		// Return.
		cellGraph.releaseRef( cref );
		junctionGraph.releaseRef( vref1 );
		junctionGraph.releaseRef( vref2 );
		return newCell;
	}

	public void merge( final MembranePart mb )
	{
		final Cell ref = cellGraph.vertexRef();
		merge( mb, ref );
		cellGraph.releaseRef( ref );
	}
}
