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
package org.mastodon.leviathan.feature;

import static org.mastodon.leviathan.algorithms.JunctionGraphUtils.signedArea;

import java.util.concurrent.atomic.AtomicBoolean;

import org.mastodon.feature.Dimension;
import org.mastodon.leviathan.model.cell.Cell;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = CellAreaFeatureComputer.class )
public class CellAreaFeatureComputer implements CellFeatureComputer
{

	@Parameter
	private CellModel model;

	@Parameter
	private AtomicBoolean forceComputeAll;

	@Parameter( type = ItemIO.OUTPUT )
	private CellAreaFeature output;

	@Override
	public void run()
	{
		final boolean recomputeAll = forceComputeAll.get();
		if ( recomputeAll )
			output.map.beforeClearPool();

		for ( final Cell cell : model.getGraph().vertices() )
		{

			/*
			 * Skip if we are not force to recompute all and if a value is
			 * already computed.
			 */
			if ( !recomputeAll && output.map.isSet( cell ) )
				continue;

			final double[] boundary = cell.getBoundary();
			if ( boundary == null )
			{
				output.map.set( cell, Double.NaN );
				continue;
			}

			final double area = Math.abs( signedArea( boundary ) );
			output.map.set( cell, area );
		}
	}

	@Override
	public void createOutput()
	{
		if ( null == output )
			output = new CellAreaFeature(
					new DoublePropertyMap<>( model.getGraph().vertices().getRefPool(), Double.NaN ),
					Dimension.AREA.getUnits( model.getSpaceUnits(), model.getTimeUnits() ) );
	}
}
