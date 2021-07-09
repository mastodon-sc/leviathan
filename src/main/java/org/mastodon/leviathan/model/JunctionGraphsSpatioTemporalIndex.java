package org.mastodon.leviathan.model;

import org.mastodon.collection.RefCollections;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatialIndexImp;

import gnu.trove.map.hash.TIntObjectHashMap;

public class JunctionGraphsSpatioTemporalIndex
{

	private final static int NO_ENTRY_KEY = -1;

	final TIntObjectHashMap< SpatialIndexImp< Junction > > timepointToSpatialIndex;


	private final LeviathanModel model;

	JunctionGraphsSpatioTemporalIndex( final LeviathanModel model )
	{
		this.model = model;
		this.timepointToSpatialIndex = new TIntObjectHashMap<>( 10, 0.5f, NO_ENTRY_KEY );
	}

	public SpatialIndex< Junction > getSpatialIndex( final int timepoint )
	{
		return getSpatialIndexImp( timepoint );
	}

	private SpatialIndexImp< Junction > getSpatialIndexImp( final int timepoint )
	{
		SpatialIndexImp< Junction > index = timepointToSpatialIndex.get( timepoint );
		if ( index == null )
		{
			final JunctionGraph graph = model.junctionGraph( timepoint );
			index = new SpatialIndexImp<>( RefCollections.createRefSet( graph.vertices() ), graph.idmap.vertexIdBimap() );
			timepointToSpatialIndex.put( timepoint, index );
		}
		return index;
	}
}
