package org.mastodon.leviathan.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.graph.GraphChangeNotifier;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.io.GraphSerializer;
import org.mastodon.graph.io.RawGraphIO;
import org.mastodon.graph.io.RawGraphIO.FileIdToGraphMap;
import org.mastodon.graph.io.RawGraphIO.GraphToFileIdMap;
import org.mastodon.graph.ref.ListenableGraphImp;
import org.mastodon.io.properties.PropertyMapSerializers;
import org.mastodon.io.properties.RawPropertyIO;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.properties.PropertyChangeListener;
import org.mastodon.spatial.VertexPositionChangeProvider;
import org.mastodon.spatial.VertexPositionListener;

public class JunctionGraph extends
		ListenableGraphImp< 
			JunctionPool, 
			MembranePartPool, 
			Junction, 
			MembranePart, 
			ByteMappedElement >
		implements VertexPositionChangeProvider< Junction >, GraphChangeNotifier
{

	protected final GraphIdBimap< Junction, MembranePart > idmap;

	protected final PropertyMapSerializers< Junction > vertexPropertySerializers;

	protected final ReentrantReadWriteLock lock;

	private final int timepoint;

	public JunctionGraph( final int timepoint, final int initialCapacity )
	{
		super( new MembranePartPool( timepoint, initialCapacity, new JunctionPool( timepoint, initialCapacity ) ) );
		this.timepoint = timepoint;
		idmap = new GraphIdBimap<>( vertexPool, edgePool );
		vertexPropertySerializers = new PropertyMapSerializers<>();
		lock = new ReentrantReadWriteLock();
	}

	/**
	 * Exposes the bidirectional map between vertices and their id, and between
	 * edges and their id.
	 *
	 * @return the bidirectional id map.
	 */
	public GraphIdBimap< Junction, MembranePart > idmap()
	{
		return idmap;
	}

	public int timepoint()
	{
		return timepoint;
	}

	JunctionPool getVertexPool()
	{
		return vertexPool;
	}

	/**
	 * Clears this model and loads the model from the specified raw file using
	 * the specified serializer.
	 *
	 * @param is
	 *            the raw data to load. The stream will be closed when done!
	 * @param serializer
	 *            the serializer used for reading individual vertices.
	 * @return the map from IDs used in the raw file to vertices/edges.
	 * @throws IOException
	 *             if an I/O error occurs while reading the file.
	 */
	public FileIdToGraphMap< Junction, MembranePart >
			loadRaw(
					final InputStream is,
					final GraphSerializer< Junction, MembranePart > serializer )
					throws IOException
	{
		final ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( is, 1024 * 1024 ) );
		pauseListeners();
		clear();
		final FileIdToGraphMap< Junction, MembranePart > fileIdMap = RawGraphIO.read( this, idmap, serializer, ois );
		RawPropertyIO.readPropertyMaps( fileIdMap.vertices(), vertexPropertySerializers, ois );
		// TODO: edge properties
//		RawFeatureIO.readFeatureMaps( fileIdMap.vertices(), vertexFeatures, ois );
//		RawFeatureIO.readFeatureMaps( fileIdMap.edges(), edgeFeatures, ois );
		ois.close();
		resumeListeners();

		return fileIdMap;
	}

	/**
	 * Saves this model to the specified raw file using the specified
	 * serializer.
	 *
	 * @param os
	 *            the stream to which raw data will be written. The stream will
	 *            be closed when done!
	 * @param serializer
	 *            the serializer used for writing individual vertices. //
	 *            * @param vertexFeaturesToSerialize // * the vertex features to
	 *            serialize. // * @param edgeFeaturesToSerialize // * the edge
	 *            features to serialize.
	 * @return the map from vertices/edges to IDs used in the raw file.
	 * @throws IOException
	 *             if an I/O error occurs while writing the file.
	 */
	public GraphToFileIdMap< Junction, MembranePart >
			saveRaw(
					final OutputStream os,
					final GraphSerializer< Junction, MembranePart > serializer )
					throws IOException
	{
		final ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( os, 1024 * 1024 ) );
		final GraphToFileIdMap< Junction, MembranePart > fileIdMap = RawGraphIO.write( this, idmap, serializer, oos );
		RawPropertyIO.writePropertyMaps( fileIdMap.vertices(), vertexPropertySerializers, oos );
		// TODO: edge properties
//		RawFeatureIO.writeFeatureMaps( fileIdMap.vertices(), vertexFeatures, vertexFeaturesToSerialize, oos );
//		RawFeatureIO.writeFeatureMaps( fileIdMap.edges(), edgeFeatures, edgeFeaturesToSerialize, oos );
		oos.close();

		return fileIdMap;
	}

	public ReentrantReadWriteLock getLock()
	{
		return lock;
	}

	@Override
	protected void clear()
	{
		super.clear();
	}

	@Override
	protected void pauseListeners()
	{
		super.pauseListeners();
		vertexPool.getPropertyMaps().pauseListeners();
		edgePool.getPropertyMaps().pauseListeners();
	}

	@Override
	protected void resumeListeners()
	{
		edgePool.getPropertyMaps().resumeListeners();
		vertexPool.getPropertyMaps().resumeListeners();
		super.resumeListeners();
	}

	@Override
	public void notifyGraphChanged()
	{
		super.notifyGraphChanged();
	}

	/**
	 * Register a {@link VertexPositionListener} that will be notified when
	 * feature values are changed.
	 *
	 * @param listener
	 *            the listener to register.
	 * @return {@code true} if the listener was successfully registered.
	 *         {@code false} if it was already registered.
	 */
	@Override
	public boolean addVertexPositionListener( final VertexPositionListener< Junction > listener )
	{
		return vertexPool.position.propertyChangeListeners().add( wrap( listener ) );
	}

	/**
	 * Removes the specified {@link VertexPositionListener} from the set of
	 * listeners.
	 *
	 * @param listener
	 *            the listener to remove.
	 * @return {@code true} if the listener was present in the listeners of this
	 *         model and was successfully removed.
	 */
	@Override
	public boolean removeVertexPositionListener( final VertexPositionListener< Junction > listener )
	{
		return vertexPool.position.propertyChangeListeners().remove( wrap( listener ) );
	}

	private JunctionPositionListenerWrapper wrap( final VertexPositionListener< Junction > l )
	{
		return new JunctionPositionListenerWrapper( l );
	}

	private static class JunctionPositionListenerWrapper implements PropertyChangeListener< Junction >
	{
		private final VertexPositionListener< Junction > l;

		JunctionPositionListenerWrapper( final VertexPositionListener< Junction > l )
		{
			this.l = l;
		}

		@Override
		public void propertyChanged( final Junction vertex )
		{
			l.vertexPositionChanged( vertex );
		}

		@Override
		public int hashCode()
		{
			return l.hashCode();
		}

		@Override
		public boolean equals( final Object obj )
		{
			return ( obj instanceof JunctionPositionListenerWrapper )
					&& l.equals( ( ( JunctionPositionListenerWrapper ) obj ).l );
		}
	}
}
