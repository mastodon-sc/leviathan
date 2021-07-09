package org.mastodon.leviathan.model;

import java.util.ArrayList;
import java.util.List;

import org.mastodon.properties.Property;
import org.mastodon.undo.GraphUndoRecorder;
import org.mastodon.undo.UndoPointMarker;

import gnu.trove.map.hash.TIntObjectHashMap;

public class LeviathanModel implements UndoPointMarker
{

	private final static int NO_ENTRY_KEY = -1;

	private static final int initialCapacity = 1024;

	private final TIntObjectHashMap< JunctionGraph > graphs;

	private final TIntObjectHashMap< GraphUndoRecorder< Junction, MembranePart > > undos;

	private final String spaceUnits;

	private final String timeUnits;

	private final JunctionGraphsSpatioTemporalIndex index;

	public LeviathanModel( final String spaceUnits, final String timeUnits )
	{
		this.spaceUnits = spaceUnits;
		this.timeUnits = timeUnits;
		this.graphs = new TIntObjectHashMap<>( 10, 0.5f, NO_ENTRY_KEY );
		this.undos = new TIntObjectHashMap<>( 10, 0.5f, NO_ENTRY_KEY );
		this.index = new JunctionGraphsSpatioTemporalIndex( this );

	}

	public JunctionGraph junctionGraph( final int timepoint )
	{
		if ( !graphs.containsKey( timepoint ) )
			registerNewGraph( timepoint );
		return graphs.get( timepoint );
	}

	public GraphUndoRecorder< Junction, MembranePart > undo( final int timepoint )
	{
		if ( !undos.containsKey( timepoint ) )
			registerNewGraph( timepoint );
		return undos.get( timepoint );
	}

	public JunctionGraphsSpatioTemporalIndex index()
	{
		return index;
	}

	private void registerNewGraph( final int timepoint )
	{
		final JunctionGraph graph = new JunctionGraph( timepoint, initialCapacity );
		final List< Property< Junction > > vertexUndoableProperties = new ArrayList<>();
		vertexUndoableProperties.add( graph.getVertexPool().positionProperty() );
		final List< Property< MembranePart > > edgeUndoableProperties = new ArrayList<>();
		final GraphUndoRecorder< Junction, MembranePart > undoRecorder =
				new GraphUndoRecorder<>(
						initialCapacity,
						graph,
						graph.idmap(),
						JunctionGraphSerializer.getInstance().getVertexSerializer(),
						JunctionGraphSerializer.getInstance().getEdgeSerializer(),
						vertexUndoableProperties,
						edgeUndoableProperties );

		graphs.put( timepoint, graph );
		undos.put( timepoint, undoRecorder );
	}

	public void undo()
	{
		graphs.keySet().forEach( t -> {
			final JunctionGraph graph = graphs.get( t );
			final GraphUndoRecorder< Junction, MembranePart > undo = undos.get( t );
			graph.lock.writeLock().lock();
			try
			{
				undo.undo();
				graph.notifyGraphChanged();
			}
			finally
			{
				graph.lock.writeLock().unlock();
			}
			return true;
		} );
	}

	public void redo()
	{
		graphs.keySet().forEach( t -> {
			final JunctionGraph graph = graphs.get( t );
			final GraphUndoRecorder< Junction, MembranePart > undo = undos.get( t );
			graph.lock.writeLock().lock();
			try
			{
				undo.redo();
				graph.notifyGraphChanged();
			}
			finally
			{
				graph.lock.writeLock().unlock();
			}
			return true;
		} );
	}

	@Override
	public void setUndoPoint()
	{
		undos.forEachValue( u -> {
			u.setUndoPoint();
			return true;
		} );
	}

	public String getSpaceUnits()
	{
		return spaceUnits;
	}

	public String getTimeUnits()
	{
		return timeUnits;
	}

}
