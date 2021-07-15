/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.leviathan.views.bdv.overlay.cell.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mastodon.app.ui.settings.style.AbstractStyleManager;
import org.mastodon.leviathan.views.bdv.overlay.cell.CellRenderSettings;
import org.yaml.snakeyaml.Yaml;

/**
 * Manages a list of {@link CellRenderSettings} for multiple BDV windows.
 * Provides models based on a common list of settings than can be used in swing
 * items.
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 */
public class CellRenderSettingsManager extends AbstractStyleManager< CellRenderSettingsManager, CellRenderSettings >
{
	private static final String STYLE_FILE = System.getProperty( "user.home" ) + "/.mastodon/cellrendersettings.yaml";

	/**
	 * A {@code CellRenderSettings} that has the same properties as the default
	 * CellRenderSettings. In contrast to defaultStyle this will always refer to
	 * the same object, so a consumers can just use this one CellRenderSettings
	 * to listen for changes and for painting.
	 */
	private final CellRenderSettings forwardDefaultStyle;

	private final CellRenderSettings.UpdateListener updateForwardDefaultListeners;

	public CellRenderSettingsManager()
	{
		this( true );
	}

	public CellRenderSettingsManager( final boolean loadStyles )
	{
		forwardDefaultStyle = CellRenderSettings.defaultStyle().copy();
		updateForwardDefaultListeners = () -> forwardDefaultStyle.set( defaultStyle );
		defaultStyle.updateListeners().add( updateForwardDefaultListeners );
		if ( loadStyles )
			loadStyles();
	}

	@Override
	protected List< CellRenderSettings > loadBuiltinStyles()
	{
		return Collections.unmodifiableList( new ArrayList<>( CellRenderSettings.defaults ) );
	}

	@Override
	public synchronized void setDefaultStyle( final CellRenderSettings CellRenderSettings )
	{
		defaultStyle.updateListeners().remove( updateForwardDefaultListeners );
		defaultStyle = CellRenderSettings;
		forwardDefaultStyle.set( defaultStyle );
		defaultStyle.updateListeners().add( updateForwardDefaultListeners );
	}

	/**
	 * Returns a final {@link CellRenderSettings} instance that always has the
	 * same properties as the default style.
	 *
	 * @return the {@link CellRenderSettings} instance.
	 */
	public CellRenderSettings getForwardDefaultStyle()
	{
		return forwardDefaultStyle;
	}

	public void loadStyles()
	{
		loadStyles( STYLE_FILE );
	}

	public void loadStyles( final String filename )
	{
		userStyles.clear();
		final Set< String > names = builtinStyles.stream().map( CellRenderSettings::getName ).collect( Collectors.toSet() );
		try
		{
			final FileReader input = new FileReader( filename );
			final Yaml yaml = CellRenderSettingsIO.createYaml();
			final Iterable< Object > objs = yaml.loadAll( input );
			String defaultStyleName = null;
			for ( final Object obj : objs )
			{
				if ( obj instanceof String )
				{
					defaultStyleName = ( String ) obj;
				}
				else if ( obj instanceof CellRenderSettings )
				{
					final CellRenderSettings ts = ( CellRenderSettings ) obj;
					if ( null != ts )
					{
						// sanity check: style names must be unique
						if ( names.add( ts.getName() ) )
							userStyles.add( ts );
					}
				}
			}
			setDefaultStyle( styleForName( defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
		}
		catch ( final FileNotFoundException e )
		{}
	}

	@Override
	public void saveStyles()
	{
		saveStyles( STYLE_FILE );
	}

	public void saveStyles( final String filename )
	{
		try
		{
			mkdirs( filename );
			final FileWriter output = new FileWriter( filename );
			final Yaml yaml = CellRenderSettingsIO.createYaml();
			final ArrayList< Object > objects = new ArrayList<>();
			objects.add( defaultStyle.getName() );
			objects.addAll( userStyles );
			yaml.dumpAll( objects.iterator(), output );
			output.close();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	/*
	 * STATIC UTILITIES
	 */

	private static boolean mkdirs( final String fileName )
	{
		final File dir = new File( fileName ).getParentFile();
		return dir == null ? false : dir.mkdirs();
	}
}
