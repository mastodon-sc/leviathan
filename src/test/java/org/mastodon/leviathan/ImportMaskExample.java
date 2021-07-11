package org.mastodon.leviathan;

import java.util.ArrayList;
import java.util.List;

import org.mastodon.leviathan.app.LeviathanAppModel;
import org.mastodon.leviathan.app.LeviathanKeyConfigContexts;
import org.mastodon.leviathan.mask.MaskImporter;
import org.mastodon.leviathan.model.JunctionModel;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanViewBdv;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import ij.ImageJ;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.boundary.Boundary;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ImportMaskExample
{
	public static < T extends RealType< T > > void main( final String[] args ) throws SpimDataException

	{
		final KeymapManager keymapManager = new KeymapManager();
		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		final KeyPressedManager keyPressedManager = new KeyPressedManager();
		final ViewerOptions options = ViewerOptions.options().shareKeyPressedEvents( keyPressedManager );
		final FeatureColorModeManager featureColorModeManager = new FeatureColorModeManager();
		final LeviathanPlugins plugins = new LeviathanPlugins( keymap );
		final Actions globalAppActions = new Actions( keymap.getConfig(), LeviathanKeyConfigContexts.LEVIATHAN );

		final String maskPath = "samples/Segmentation-2.xml";
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( maskPath );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				maskPath,
				spimData,
				options,
				() -> System.out.println( "Repaint" ) );

		final JunctionModel model = new JunctionModel( "pixel", "frame" );

		final int boundSourceID = 0;
		final int timepoint = 0;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( boundSourceID ).getSpimSource().getSource( timepoint, 0 ) );

		MaskImporter.importMask( img, model.getGraph(), timepoint );

		final LeviathanAppModel appModel =
				new LeviathanAppModel(
						model,
						sharedBdvData,
						keyPressedManager,
						featureColorModeManager,
						keymapManager,
						plugins,
						globalAppActions );

		new LeviathanViewBdv( appModel );
	}

	private static < T extends IntegerType< T > & NativeType< T > > void process2( final SharedBigDataViewerData sharedBdvData, final JunctionModel model )
	{
		ImageJ.main( null );

		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = Views.dropSingletonDimensions(
				( RandomAccessibleInterval< T > ) sharedBdvData.getSources().get( 0 ).getSpimSource().getSource( 0, 0 ) );
		int m = Integer.MIN_VALUE;
		for ( final T t : Views.iterable( img ) )
			m = Math.max( ( int ) t.getRealDouble(), m );
		final int max = m;

		// Invert so that inside is white.
		final T type = Util.getTypeFromInterval( img ).createVariable();
		final RandomAccessibleInterval< T > input = Converters.convertRAI( img, ( i, o ) -> o.setReal( max - ( int ) i.getRealDouble() ), type );

		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( ( Interval ) input, type );
		final Img< T > output = factory.create( input );

		// Connected components.
		final StructuringElement se = StructuringElement.FOUR_CONNECTED;;
		ConnectedComponents.labelAllConnectedComponents( Views.extendZero( input ), output, se );

		// Label image.
		m = Integer.MIN_VALUE;
		for ( final T t : Views.iterable( output ) )
			m = Math.max( ( int ) t.getRealDouble(), m );
		final List< Integer > labels = new ArrayList< >( m );
		for ( int j = 0; j < m; j++ )
			labels.add( Integer.valueOf( j ) );
		final ImgLabeling< Integer, T > labeling = ImgLabeling.fromImageAndLabels( output, labels );

		// Boundaries.
		final Img< NativeBoolType > dilated = ArrayImgs.booleans( img.dimension( 0 ), img.dimension( 1 ) );
		final Img< T > boundaries = factory.create( img );
		final RandomAccess< T > ra = boundaries.randomAccess( boundaries );
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		for ( final LabelRegion< Integer > region : regions )
		{
			final NativeBoolType nbt = new NativeBoolType( false );
			final RandomAccessibleInterval< NativeBoolType > regionNBT = Converters.convertRAI( region, ( i, o ) -> o.set( i.get() ), nbt );
			Dilation.dilate( regionNBT, dilated, StructuringElements.square( 1, 2 ), 1 );
			ImageJFunctions.show( dilated, "" + region.getLabel() );

			final Boundary< NativeBoolType > boundary = new Boundary<>( regionNBT );
			final Cursor< Void > cursor = boundary.localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				ra.setPosition( cursor );
				ra.get().setInteger( region.getLabel().intValue() );
			}

			break;
		}

		ImageJFunctions.show( boundaries, "boundary" );
	}
}
