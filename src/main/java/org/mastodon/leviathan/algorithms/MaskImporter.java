package org.mastodon.leviathan.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.scijava.util.DoubleArray;

import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class MaskImporter< T extends RealType< T > >
{

	public static final < T extends RealType< T > > JunctionGraph importMask( final RandomAccessibleInterval< T > mask, final int timepoint )
	{
		final JunctionGraph graph = new JunctionGraph();
		importMask( mask, graph, timepoint );
		return graph;
	}

	public static < T extends RealType< T > > void importMask( final RandomAccessibleInterval< T > mask, final JunctionGraph graph, final int timepoint )
	{
		final MaskImporter< T > importer = new MaskImporter<>( mask, graph, timepoint );
		importer.process();
	}

	private final RandomAccessibleInterval< T > mask;

	private final int timepoint;

	private final JunctionGraph graph;

	private final ImgLabeling< Integer, UnsignedIntType > junctionLabelImg;

	private final RandomAccess< T > raMask;

	private final RandomAccess< UnsignedIntType > raLbl;

	private MaskImporter( final RandomAccessibleInterval< T > mask, final JunctionGraph graph, final int timepoint )
	{
		this.mask = mask;
		this.graph = graph;
		this.timepoint = timepoint;
		this.junctionLabelImg = junctionLabelImg();
		this.raMask = Views.extendZero( mask ).randomAccess();
		this.raLbl = junctionLabelImg.getIndexImg().randomAccess();
	}

	private void process()
	{
		final RectangleShape shape = new RectangleShape( 1, true );
		final RandomAccess< Neighborhood< UnsignedIntType > > raRegion = shape
				.neighborhoodsRandomAccessible( junctionLabelImg.getIndexImg() )
				.randomAccess( junctionLabelImg.getIndexImg() );

		final LabelRegions< Integer > regions = new LabelRegions<>( junctionLabelImg );
		final Junction ref1 = graph.vertexRef();
		for ( final LabelRegion< Integer > region : regions )
		{
			// Find all the branch stems for this junction.
			final Set< Point > branchStems = new HashSet<>();
			final LabelRegionCursor cursor = region.localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				raRegion.setPosition( cursor );
				final Cursor< UnsignedIntType > nCursor = raRegion.get().localizingCursor();
				while ( nCursor.hasNext() )
				{
					nCursor.fwd();
					if ( isVisitable( nCursor ) && wasVisited( nCursor ) == 0 )
						branchStems.add( Point.wrap( nCursor.positionAsLongArray() ) );
				}
			}

			raLbl.setPosition( cursor );
			final Junction source = lblToJunction( raLbl.get().getInt(), ref1 );
			// Walk from each branch stem.
			for ( final Point stem : branchStems )
				walkBranch( stem, source );
		}
		graph.releaseRef( ref1 );

		/*
		 * Prune solitary junctions.
		 */

		final RefList< Junction > toRemove = RefCollections.createRefList( graph.vertices() );
		RefCollection< Junction > toInspect = graph.vertices();
		final Junction ref = graph.vertexRef();
		do
		{
			for ( final Junction v : toRemove )
			{
				// Before we remove them, add their neighbor to the collection
				// of vertices we should inspect in the next iteration.
				for ( final MembranePart e : v.edges() )
				{
					final Junction other = GraphUtils.vertexAcross( e, v, ref );
					toInspect.add( other );
				}
				// Remove them. Now their neighbors might be lonely as well, but
				// we will inspect them later.
				graph.remove( v );
			}
			toRemove.clear();

			// Inspect vertices to check if they are lonely.
			for ( final Junction junction : toInspect )
			{
				if ( junction.edges().size() == 1 )
					toRemove.add( junction );
			}
			toInspect = RefCollections.createRefList( graph.vertices() );
		}
		while ( !toRemove.isEmpty() );
	}

	private void walkBranch( final Point stem, final Junction source )
	{
		final int sourceLbl = junctionToLbl( source );
		final RectangleShape shape = new RectangleShape( 1, true );
		final RandomAccess< Neighborhood< T > > raNMask = shape.neighborhoodsRandomAccessible( mask ).randomAccess();
		raNMask.setPosition( stem );
		final RandomAccess< UnsignedIntType > raLblImg = junctionLabelImg.getIndexImg().randomAccess();

		final DoubleArray arr = new DoubleArray();
		// Store position of the source.
		storePosition( arr, Point.wrap( new long[] {
				( long ) source.getDoublePosition( 0 ),
				( long ) source.getDoublePosition( 1 ) } ) );

		WALK: while ( true )
		{
			// Mark current position as visited.
			raLblImg.setPosition( raNMask );
			raLblImg.get().set( sourceLbl );

			// Store current position and length.
			storePosition( arr, raLblImg );

			// Look for the next position.
			final Cursor< T > nCursor = raNMask.get().localizingCursor();
			while ( nCursor.hasNext() )
			{
				nCursor.fwd();
				if ( !isVisitable( nCursor ) )
					continue;
				// Not a pixel belonging to the mask.

				final int lbl = wasVisited( nCursor );
				if ( lbl == sourceLbl )
					continue;
				// We have been there already.

				if ( lbl == 0 )
				{
					// Never been there, let's walk.
					raNMask.setPosition( nCursor );
					continue WALK;
				}

				// Only remaining possibility is that we have found another
				// junction.
				storePosition( arr, nCursor );
				final Junction ref = graph.vertexRef();
				final MembranePart eref = graph.edgeRef();
				final Junction target = lblToJunction( lbl, ref );

				// Store position of the target.
				storePosition( arr, Point.wrap( new long[] {
						( long ) target.getDoublePosition( 0 ),
						( long ) target.getDoublePosition( 1 ) } ) );
				final double[] pos = arr.copyArray();

				final MembranePart edge = graph.addEdge( source, target, eref ).init();
				edge.setPixels( pos );
				graph.releaseRef( ref );
				graph.releaseRef( eref );
				return;
			}
			// Did not found a next pixel to iterate to. Finished for this stem.
			return;
		}
	}

	private static final void storePosition( final DoubleArray storage, final Localizable pos )
	{
		final double x2 = pos.getDoublePosition( 0 );
		final double y2 = pos.getDoublePosition( 1 );
		final int size = storage.size();
		if ( size == 0 )
		{
			storage.addValue( x2 );
			storage.addValue( y2 );
			return;
		}

		final double y1 = storage.getValue( size - 1 );
		final double x1 = storage.getValue( size - 2 );
		// Don't store position is identical to previous one.
		if ( x1 == x2 && y1 == y2 )
			return;

		if ( size < 4 )
		{
			storage.addValue( x2 );
			storage.addValue( y2 );
			return;
		}

		// Check whether the new position is aligned with the previous ones.
		final double y0 = storage.getValue( size - 3 );
		final double x0 = storage.getValue( size - 4 );
		final double slopeDiff = ( x2 - x1 ) * ( y0 - y1 ) - ( y2 - y1 ) * ( x0 - x1 );
		if ( slopeDiff == 0. )
		{
			// Collinear, we replace the middle point instead of adding
			storage.setValue( size - 1, y2 );
			storage.setValue( size - 2, x2 );
		}
		else
		{
			storage.addValue( x2 );
			storage.addValue( y2 );
		}
	}

	private int wasVisited( final Localizable pos )
	{
		raLbl.setPosition( pos );
		return raLbl.get().getInt();
	}

	private boolean isVisitable( final Localizable pos )
	{
		raMask.setPosition( pos );
		return raMask.get().getRealDouble() > 0;
	}

	private final ImgLabeling< Integer, UnsignedIntType > junctionLabelImg()
	{
		final Img< BitType > junctionMaskImg = ArrayImgs.bits( mask.dimensionsAsLongArray() );
		final RandomAccess< BitType > raJunctionMask = junctionMaskImg.randomAccess( junctionMaskImg );

		final RectangleShape shape = new RectangleShape( 1, true );
		final Cursor< T > cursor = Views.iterable( mask ).localizingCursor();
		final RandomAccess< Neighborhood< T > > ra = shape.neighborhoodsRandomAccessible( Views.extendZero( mask ) ).randomAccess( mask );
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			if ( cursor.get().getRealDouble() <= 0. )
				continue;

			ra.setPosition( cursor );
			int nWhite = 0;
			for ( final T t : ra.get() )
			{
				if ( t.getRealDouble() > 0. )
					nWhite++;
			}
			if ( nWhite >= 3 )
			{
				raJunctionMask.setPosition( cursor );
				raJunctionMask.get().set( true );
			}
		}

		// Connected components.
		final Img< UnsignedIntType > junctionLbl = ArrayImgs.unsignedInts( mask.dimensionsAsLongArray() );
//		this.junctionLbl = ArrayImgs.unsignedInts( mask.dimensionsAsLongArray() );
		final StructuringElement se = StructuringElement.FOUR_CONNECTED;;
		ConnectedComponents.labelAllConnectedComponents( Views.extendZero( junctionMaskImg ), junctionLbl, se );
		int m = Integer.MIN_VALUE;
		for ( final UnsignedIntType t : Views.iterable( junctionLbl ) )
			m = Math.max( t.getInt(), m );
		final List< Integer > labels = new ArrayList<>( m );
		for ( int j = 0; j < m; j++ )
			labels.add( Integer.valueOf( j ) );
		final ImgLabeling< Integer, UnsignedIntType > labeling = ImgLabeling.fromImageAndLabels( junctionLbl, labels );
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		final Junction ref = graph.vertexRef();
		graph.getLock().writeLock().lock();
		try
		{
			final RandomAccess< UnsignedIntType > raJunctionLbl = junctionLbl.randomAccess( junctionLbl );
			for ( final LabelRegion< Integer > region : regions )
			{
				/*
				 * Find the pixel in the junction the closest to the center of
				 * mass. So that junctions are created with integer positions
				 * and within the junction pixels.
				 */
				final RealLocalizable com = region.getCenterOfMass();
				final Point junction = new Point( 2 );
				double bestDistance = Double.POSITIVE_INFINITY;
				final LabelRegionCursor regionCursor = region.localizingCursor();
				while ( regionCursor.hasNext() )
				{
					regionCursor.fwd();
					final double d = Util.distance( regionCursor, com );
					if ( d < bestDistance )
					{
						bestDistance = d;
						junction.setPosition( regionCursor );
					}
				}

				// Create a junction at this position.
				final Junction vertex = graph.addVertex( ref ).init( timepoint, junction.positionAsDoubleArray() );

				// Write the vertex id into the junction img.
				regionCursor.reset();
				while ( regionCursor.hasNext() )
				{
					regionCursor.fwd();
					raJunctionLbl.setPosition( regionCursor );
					raJunctionLbl.get().set( junctionToLbl( vertex ) );
					// Avoid writing 0...
				}
			}
			return labeling;
		}
		finally
		{
			graph.getLock().writeLock().unlock();
		}
	}

	private Junction lblToJunction( final int label, final Junction ref )
	{
		final int poolIndex = label - 1;
		return graph.getGraphIdBimap().getVertex( poolIndex, ref );
	}

	private int junctionToLbl( final Junction vertex )
	{
		return 1 + graph.getGraphIdBimap().getVertexId( vertex );
	}
}
