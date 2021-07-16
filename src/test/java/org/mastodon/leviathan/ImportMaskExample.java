package org.mastodon.leviathan;

import java.io.IOException;

import org.mastodon.leviathan.app.LeviathanMainWindow;
import org.mastodon.leviathan.app.LeviathanMaskImporter;
import org.mastodon.leviathan.app.LeviathanWM;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;
import net.imglib2.type.numeric.RealType;

public class ImportMaskExample
{
	public static < T extends RealType< T > > void main( final String[] args ) throws SpimDataException, IOException
	{
		final String maskPath = "samples/Segmentation-2.xml";
		final LeviathanWM wm = LeviathanMaskImporter.importMask( maskPath, new Context() );
		new LeviathanMainWindow( wm ).setVisible( true );
	}
}
