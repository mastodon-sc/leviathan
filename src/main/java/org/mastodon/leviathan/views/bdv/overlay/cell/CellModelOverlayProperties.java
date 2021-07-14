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

import org.mastodon.leviathan.model.Cell;
import org.mastodon.leviathan.model.CellGraph;
import org.mastodon.leviathan.model.Link;
import org.mastodon.leviathan.views.bdv.overlay.cell.wrap.CellOverlayProperties;

public class CellModelOverlayProperties implements CellOverlayProperties< Cell, Link >
{
	private final CellGraph modelGraph;

	public CellModelOverlayProperties( final CellGraph modelGraph )
	{
		this.modelGraph = modelGraph;
	}

	@Override
	public void localize( final Cell v, final double[] position )
	{
		v.localize( position );
	}

	@Override
	public double getDoublePosition( final Cell v, final int d )
	{
		return v.getDoublePosition( d );
	}

	@Override
	public void setPosition( final Cell v, final double position, final int d )
	{
		v.setPosition( position, d );
	}

	@Override
	public void setPosition( final Cell v, final double[] position )
	{
		v.setPosition( position );
	}

	@Override
	public int getTimepoint( final Cell v )
	{
		return v.getTimepoint();
	}

	@Override
	public Cell addVertex( final Cell ref )
	{
		return modelGraph.addVertex( ref );
	}

	@Override
	public Link addEdge( final Cell source, final Cell target, final Link ref )
	{
		return modelGraph.addEdge( source, target, ref );
	}

	@Override
	public Link insertEdge( final Cell source, final int sourceOutIndex, final Cell target, final int targetInIndex, final Link ref )
	{
		return modelGraph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref );
	}

	@Override
	public Link initEdge( final Link e )
	{
		return e.init();
	}

	@Override
	public void removeEdge( final Link e )
	{
		modelGraph.remove( e );
	}

	@Override
	public void removeVertex( final Cell v )
	{
		modelGraph.remove( v );
	}

	@Override
	public void notifyGraphChanged()
	{
		modelGraph.notifyGraphChanged();
	}

	@Override
	public Cell initVertex( final Cell v, final int timepoint, final double[] position )
	{
		return v.init( timepoint, position );
	}

	@Override
	public int[] getMembranes( final Cell v )
	{
		return v.getMembranes();
	}

	@Override
	public double[] getBoundary( final Cell v )
	{
		return v.getBoundary();
	}
}
