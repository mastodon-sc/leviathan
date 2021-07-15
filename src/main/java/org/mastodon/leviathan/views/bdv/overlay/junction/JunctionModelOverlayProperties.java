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
package org.mastodon.leviathan.views.bdv.overlay.junction;

import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayProperties;

public class JunctionModelOverlayProperties implements JunctionOverlayProperties< Junction, MembranePart >
{
	private final JunctionGraph modelGraph;

	public JunctionModelOverlayProperties( final JunctionGraph modelGraph )
	{
		this.modelGraph = modelGraph;
	}

	@Override
	public void localize( final Junction v, final double[] position )
	{
		v.localize( position );
	}

	@Override
	public double getDoublePosition( final Junction v, final int d )
	{
		return v.getDoublePosition( d );
	}

	@Override
	public void setPosition( final Junction v, final double position, final int d )
	{
		v.setPosition( position, d );
	}

	@Override
	public void setPosition( final Junction v, final double[] position )
	{
		v.setPosition( position );
	}

	@Override
	public int getTimepoint( final Junction v )
	{
		return v.getTimepoint();
	}

	@Override
	public Junction addVertex( final Junction ref )
	{
		return modelGraph.addVertex( ref );
	}

	@Override
	public MembranePart addEdge( final Junction source, final Junction target, final MembranePart ref )
	{
		return modelGraph.addEdge( source, target, ref );
	}

	@Override
	public MembranePart insertEdge( final Junction source, final int sourceOutIndex, final Junction target, final int targetInIndex, final MembranePart ref )
	{
		return modelGraph.insertEdge( source, sourceOutIndex, target, targetInIndex, ref );
	}

	@Override
	public MembranePart initEdge( final MembranePart e )
	{
		return e.init();
	}

	@Override
	public void removeEdge( final MembranePart e )
	{
		modelGraph.remove( e );
	}

	@Override
	public void removeVertex( final Junction v )
	{
		modelGraph.remove( v );
	}

	@Override
	public void notifyGraphChanged()
	{
		modelGraph.notifyGraphChanged();
	}

	@Override
	public double[] getPixels( final MembranePart e )
	{
		return e.getPixels();
	}

	@Override
	public void setPixels( final MembranePart e, final double[] pixels )
	{
		e.setPixels( pixels );
	}

	@Override
	public Junction initVertex( final Junction v, final int timepoint, final double[] position )
	{
		return v.init( timepoint, position );
	}
}
