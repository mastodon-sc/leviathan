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
package org.mastodon.leviathan.views.bdv.overlay.cell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.mastodon.app.ui.settings.style.Style;
import org.scijava.listeners.Listeners;

public class CellRenderSettings implements Style< CellRenderSettings >
{
	/*
	 * PUBLIC DISPLAY CONFIG DEFAULTS.
	 */

	public static final int DEFAULT_LIMIT_TIME_RANGE = 20;
	public static final boolean DEFAULT_USE_ANTI_ALIASING = true;
	public static final boolean DEFAULT_USE_GRADIENT = false;
	public static final boolean DEFAULT_DRAW_CELLS = true;
	public static final boolean DEFAULT_DRAW_LINKS = true;
	public static final boolean DEFAULT_DRAW_LINKS_AHEAD_IN_TIME = false;
	public static final boolean DEFAULT_DRAW_ARROW_HEADS = false;
	public static final boolean DEFAULT_DRAW_CELL_CONTOUR = true;
	public static final boolean DEFAULT_DRAW_CELL_FILLED = false;
	public static final boolean DEFAULT_DRAW_CELL_CENTER = false;
	public static final boolean DEFAULT_DRAW_CELL_LABELS = false;
	public static final int DEFAULT_COLOR_CELL_AND_PRESENT = Color.GREEN.getRGB();
	public static final int DEFAULT_COLOR_PAST = Color.RED.getRGB();
	public static final int DEFAULT_COLOR_FUTURE = Color.BLUE.getRGB();

	public interface UpdateListener
	{
		public void cellRenderSettingsChanged();
	}

	private final Listeners.List< UpdateListener > updateListeners;

	private CellRenderSettings()
	{
		updateListeners = new Listeners.SynchronizedList<>();
	}

	/**
	 * Returns a new render settings, copied from this instance.
	 *
	 * @param name
	 *            the name for the copied render settings.
	 * @return a new {@link CellRenderSettings} instance.
	 */
	@Override
	public CellRenderSettings copy( final String name )
	{
		final CellRenderSettings rs = new CellRenderSettings();
		rs.set( this );
		if ( name != null )
			rs.setName( name );
		return rs;
	}

	@Override
	public CellRenderSettings copy()
	{
		return copy( null );
	}

	public synchronized void set( final CellRenderSettings settings )
	{
		name = settings.name;
		useAntialiasing = settings.useAntialiasing;
		useGradient = settings.useGradient;
		timeLimit = settings.timeLimit;
		drawLinks = settings.drawLinks;
		drawLinksAheadInTime = settings.drawLinksAheadInTime;
		drawArrowHeads = settings.drawArrowHeads;
		drawCells = settings.drawCells;
		drawCellContour = settings.drawCellContour;
		drawCellFilled = settings.drawCellFilled;
		drawCellCenter = settings.drawCellCenter;
		drawCellLabel = settings.drawCellLabel;
		cellColor = settings.cellColor;
		colorPast = settings.colorPast;
		colorFuture = settings.colorFuture;
		notifyListeners();
	}

	private void notifyListeners()
	{
		for ( final UpdateListener l : updateListeners.list )
			l.cellRenderSettingsChanged();
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	/*
	 * DISPLAY SETTINGS FIELDS.
	 */

	/**
	 * The name of this render settings object.
	 */
	private String name;

	/**
	 * Whether to use antialiasing (for drawing everything).
	 */
	private boolean useAntialiasing;

	/**
	 * If {@code true}, draw links using a gradient from source color to target
	 * color. If {@code false}, draw links using the target color.
	 */
	private boolean useGradient;

	/**
	 * Maximum number of timepoints into the past for which outgoing edges
	 * should be drawn.
	 */
	private int timeLimit;

	/**
	 * Whether to draw links (at all).
	 */
	private boolean drawLinks;

	/**
	 * Whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 */
	private boolean drawLinksAheadInTime;

	/**
	 * Whether to draw links with an arrow head, in time direction.
	 */
	private boolean drawArrowHeads;

	/**
	 * Whether to draw cells (at all).
	 */
	private boolean drawCells;

	/**
	 * Whether to draw cell contours.
	 */
	private boolean drawCellContour;

	/**
	 * Whether to draw cell filled.
	 */
	private boolean drawCellFilled;

	/**
	 * Whether to draw cell centers.
	 */
	private boolean drawCellCenter;

	/**
	 * Whether to draw cell labels next to ellipses.
	 */
	private boolean drawCellLabel;

	/**
	 * The color used to paint cells and links in the current time-point.
	 */
	private int cellColor;

	/**
	 * The color used to paint links in the past time-points.
	 */
	private int colorPast;

	/**
	 * The color used to paint links in the future time-points.
	 */
	private int colorFuture;

	/**
	 * Returns the name of this {@link CellRenderSettings}.
	 *
	 * @return the name.
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of this {@link CellRenderSettings}.
	 *
	 * @param name
	 *            the name to set.
	 */
	@Override
	public synchronized void setName( final String name )
	{
		if ( !Objects.equals( this.name, name ) )
		{
			this.name = name;
			notifyListeners();
		}
	}

	/**
	 * Get the antialiasing setting.
	 *
	 * @return {@code true} if antialiasing is used.
	 */
	public boolean getUseAntialiasing()
	{
		return useAntialiasing;
	}

	/**
	 * Sets whether to use anti-aliasing for drawing.
	 *
	 * @param useAntialiasing
	 *            whether to use use anti-aliasing.
	 */
	public synchronized void setUseAntialiasing( final boolean useAntialiasing )
	{
		if ( this.useAntialiasing != useAntialiasing )
		{
			this.useAntialiasing = useAntialiasing;
			notifyListeners();
		}
	}

	/**
	 * Returns whether a gradient is used for drawing links.
	 *
	 * @return {@code true} if links are drawn using a gradient from source
	 *         color to target color, or {@code false}, if links are drawn using
	 *         the target color.
	 */
	public boolean getUseGradient()
	{
		return useGradient;
	}

	/**
	 * Sets whether to use a gradient for drawing links. If
	 * {@code useGradient=true}, draw links using a gradient from source color
	 * to target color. If {@code useGradient=false}, draw links using the
	 * target color.
	 *
	 * @param useGradient
	 *            whether to use a gradient for drawing links.
	 */
	public synchronized void setUseGradient( final boolean useGradient )
	{
		if ( this.useGradient != useGradient )
		{
			this.useGradient = useGradient;
			notifyListeners();
		}
	}

	/**
	 * Gets the maximum number of time-points into the past for which outgoing
	 * edges should be drawn.
	 *
	 * @return maximum number of time-points into the past to draw links.
	 */
	public int getTimeLimit()
	{
		return timeLimit;
	}

	/**
	 * Sets the maximum number of time-points into the past for which outgoing
	 * edges should be drawn.
	 *
	 * @param timeLimit
	 *            maximum number of time-points into the past to draw links.
	 */
	public synchronized void setTimeLimit( final int timeLimit )
	{
		if ( this.timeLimit != timeLimit )
		{
			this.timeLimit = timeLimit;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw links (at all). For specific settings, see
	 * {@link #getTimeLimit()}, {@link #getUseGradient()}.
	 *
	 * @return {@code true} if links are drawn.
	 */
	public boolean getDrawLinks()
	{
		return drawLinks;
	}

	/**
	 * Gets whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 * 
	 * @return {@code true} if links are drawn ahead in time.
	 */
	public boolean getDrawLinksAheadInTime()
	{
		return drawLinksAheadInTime;
	}

	/**
	 * Gets whether to draw links with arrow heads.
	 *
	 * @return {@code true} if links are drawn with arrow heads.
	 */
	public boolean getDrawArrowHeads()
	{
		return drawArrowHeads;
	}

	/**
	 * Sets whether to draw links (at all). For specific settings, see
	 * {@link #setTimeLimit(int)}, {@link #setUseGradient(boolean)}.
	 *
	 * @param drawLinks
	 *            whether to draw links.
	 */
	public synchronized void setDrawLinks( final boolean drawLinks )
	{
		if ( this.drawLinks != drawLinks )
		{
			this.drawLinks = drawLinks;
			notifyListeners();
		}
	}

	/**
	 * Sets whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 *
	 * @param drawLinksAheadInTime
	 *            whether to draw links ahead in time.
	 */
	public synchronized void setDrawLinksAheadInTime( final boolean drawLinksAheadInTime )
	{
		if ( this.drawLinksAheadInTime != drawLinksAheadInTime )
		{
			this.drawLinksAheadInTime = drawLinksAheadInTime;
			notifyListeners();
		}
	}

	/**
	 * Sets whether to draw links with arrow heads.
	 *
	 * @param drawArrowHeads
	 *            whether to draw links with arrow heads.
	 */
	public synchronized void setDrawArrowHeads( final boolean drawArrowHeads )
	{
		if ( this.drawArrowHeads != drawArrowHeads )
		{
			this.drawArrowHeads = drawArrowHeads;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw cells (at all).
	 *
	 * @return {@code true} if cells are to be drawn.
	 */
	public boolean getDrawCells()
	{
		return drawCells;
	}

	/**
	 * Sets whether to draw cells (at all).
	 *
	 * @param drawCells
	 *            whether to draw cells.
	 */
	public synchronized void setDrawCells( final boolean drawCells )
	{
		if ( this.drawCells != drawCells )
		{
			this.drawCells = drawCells;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw cell contours.
	 *
	 * @return {@code true} if contours are to be drawn.
	 */
	public boolean getDrawCellContour()
	{
		return drawCellContour;
	}

	/**
	 * Sets whether to draw cell contours.
	 *
	 * @param drawCellContour
	 *            whether to draw cell contours.
	 */
	public synchronized void setDrawCellContour( final boolean drawCellContour )
	{
		if ( this.drawCellContour != drawCellContour )
		{
			this.drawCellContour = drawCellContour;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw cell filled.
	 *
	 * @return {@code true} if cells are to be drawn filled.
	 */
	public boolean getDrawCellFilled()
	{
		return drawCellFilled;
	}

	/**
	 * Sets whether to draw cell filled.
	 *
	 * @param drawCellFilled
	 *            whether to draw cell filled.
	 */
	public synchronized void setDrawCellFilled( final boolean drawCellFilled )
	{
		if ( this.drawCellFilled != drawCellFilled )
		{
			this.drawCellFilled = drawCellFilled;
			notifyListeners();
		}
	}

	/**
	 * Gets whether cell centers are drawn.
	 *
	 * @return whether cell centers are drawn.
	 */
	public boolean getDrawCellCenter()
	{
		return drawCellCenter;
	}

	/**
	 * Sets whether cell centers are drawn.
	 *
	 * @param drawCellCenter
	 *            whether cell centers are drawn.
	 */
	public synchronized void setDrawCellCenter( final boolean drawCellCenter )
	{
		if ( this.drawCellCenter != drawCellCenter )
		{
			this.drawCellCenter = drawCellCenter;
			notifyListeners();
		}
	}

	/**
	 * Gets whether cell labels are drawn.
	 *
	 * @return whether cell labels are drawn.
	 */
	public boolean getDrawCellLabel()
	{
		return drawCellLabel;
	}

	/**
	 * Sets whether cell labels are drawn.
	 *
	 * @param drawCellLabel
	 *            whether cell labels.
	 */
	public synchronized void setDrawCellLabel( final boolean drawCellLabel )
	{
		if ( this.drawCellLabel != drawCellLabel )
		{
			this.drawCellLabel = drawCellLabel;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint cells and links in the current
	 * time-point.
	 * 
	 * @return the color used to paint cells and links in the current
	 *         time-point.
	 */
	public int getCellColor()
	{
		return cellColor;
	}

	/**
	 * Sets the color used to paint cells and links in the current time-point.
	 * 
	 * @param cellColor
	 *            the color used to paint cells and links in the current
	 *            time-point.
	 */
	public synchronized void setCellColor( final int cellColor )
	{
		if ( this.cellColor != cellColor )
		{
			this.cellColor = cellColor;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint links in the past time-points.
	 * 
	 * @return the color used to paint links in the past time-points.
	 */
	public int getColorPast()
	{
		return colorPast;
	}

	/**
	 * Sets the color used to paint links in the past time-points.
	 * 
	 * @param colorPast
	 *            the color used to paint links in the past time-points.
	 */
	public synchronized void setColorPast( final int colorPast )
	{
		if ( this.colorPast != colorPast )
		{
			this.colorPast = colorPast;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint links in the future time-points.
	 * 
	 * @return the color used to paint links in the future time-points.
	 */
	public int getColorFuture()
	{
		return colorFuture;
	}

	/**
	 * Sets the color used to paint links in the future time-points.
	 * 
	 * @param colorFuture
	 *            the color used to paint links in the future time-points.
	 */
	public synchronized void setColorFuture( final int colorFuture )
	{
		if ( this.colorFuture != colorFuture )
		{
			this.colorFuture = colorFuture;
			notifyListeners();
		}
	}

	/*
	 * DEFAULTS RENDER SETTINGS LIBRARY.
	 */

	private static final CellRenderSettings df;
	static
	{
		df = new CellRenderSettings();
		df.useAntialiasing = DEFAULT_USE_ANTI_ALIASING;
		df.useGradient = DEFAULT_USE_GRADIENT;
		df.timeLimit = DEFAULT_LIMIT_TIME_RANGE;
		df.drawLinks = DEFAULT_DRAW_LINKS;
		df.drawLinksAheadInTime = DEFAULT_DRAW_LINKS_AHEAD_IN_TIME;
		df.drawArrowHeads = DEFAULT_DRAW_ARROW_HEADS;
		df.drawCells = DEFAULT_DRAW_CELLS;
		df.drawCellContour = DEFAULT_DRAW_CELL_CONTOUR;
		df.drawCellFilled = DEFAULT_DRAW_CELL_FILLED;
		df.drawCellCenter = DEFAULT_DRAW_CELL_CENTER;
		df.drawCellLabel = DEFAULT_DRAW_CELL_LABELS;
		df.cellColor = DEFAULT_COLOR_CELL_AND_PRESENT;
		df.colorPast = DEFAULT_COLOR_PAST;
		df.colorFuture = DEFAULT_COLOR_FUTURE;
		df.name = "Default";
	}

	private static final CellRenderSettings CELL_CENTER;
	static
	{
		CELL_CENTER = df.copy( "Cell center" );
		CELL_CENTER.drawLinks = false;
		CELL_CENTER.drawCellContour = false;
		CELL_CENTER.drawCellCenter = true;
		CELL_CENTER.drawCellLabel = true;
	}

	private static final CellRenderSettings NONE;
	static
	{
		NONE = df.copy( "No overlay" );
		NONE.drawLinks = false;
		NONE.drawCells = false;
	}

	public static final Collection< CellRenderSettings > defaults;
	static
	{
		defaults = new ArrayList<>( 3 );
		defaults.add( df );
		defaults.add( CELL_CENTER );
		defaults.add( NONE );
	}

	public static CellRenderSettings defaultStyle()
	{
		return df;
	}
}
