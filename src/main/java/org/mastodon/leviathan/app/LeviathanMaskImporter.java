package org.mastodon.leviathan.app;

import java.io.IOException;

import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.algorithms.MaskImporter;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class LeviathanMaskImporter
{

	public static final < T extends RealType< T > > LeviathanWM importMask( final String maskPath, final Context context ) throws IOException, SpimDataException
	{
		/*
		 * Prepare window manager.
		 */


		final LeviathanWM wm = new LeviathanWM( context );
		wm.setImagePath( maskPath );

		/*
		 * Physical units.
		 */

		final SharedBigDataViewerData sharedBdvData = wm.getSharedBdvData();
		final String spaceUnits = sharedBdvData.getSpimData().getSequenceDescription().getViewSetupsOrdered()
				.stream()
				.filter( BasicViewSetup::hasVoxelSize )
				.map( setup -> setup.getVoxelSize().unit() )
				.findFirst()
				.orElse( "pixel" );
		final String timeUnits = "frame";

		/*
		 * Create junction graph from image data.
		 */

		final JunctionModel junctionModel = new JunctionModel( spaceUnits, timeUnits );

		final int boundSourceID = 0;
		final int timepoint = 0; // TODO
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( boundSourceID ).getSpimSource().getSource( timepoint, 0 ) );

		MaskImporter.importMask( img, junctionModel.getGraph(), timepoint );

		/*
		 * Create cell graph from junction graph.
		 */

		final CellModel cellModel = new CellModel( spaceUnits, timeUnits );
		FindFaces.findFaces( junctionModel.getGraph(), cellModel.getGraph() );
		/*
		 * Pass results to window manager and return it.
		 */

		wm.setJunctionModel( junctionModel );
		wm.setCellModel( cellModel );
		return wm;
	}
}
