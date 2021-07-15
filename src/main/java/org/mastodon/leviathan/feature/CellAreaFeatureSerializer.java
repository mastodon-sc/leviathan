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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.io.FeatureSerializer;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;
import org.mastodon.io.properties.DoublePropertyMapSerializer;
import org.mastodon.leviathan.feature.CellAreaFeature.Spec;
import org.mastodon.leviathan.model.cell.Cell;
import org.mastodon.properties.DoublePropertyMap;
import org.scijava.plugin.Plugin;

@Plugin( type = FeatureSerializer.class )
public class CellAreaFeatureSerializer implements FeatureSerializer< CellAreaFeature, Cell >
{

	@Override
	public Spec getFeatureSpec()
	{
		return CellAreaFeature.SPEC;
	}

	@Override
	public void serialize( final CellAreaFeature feature, final ObjectToFileIdMap< Cell > idmap, final ObjectOutputStream oos ) throws IOException
	{
		// UNITS.
		final FeatureProjection< Cell > proj = feature.projections().iterator().next();
		oos.writeUTF( proj.units() );
		// DATA.
		final DoublePropertyMapSerializer< Cell > propertyMapSerializer = new DoublePropertyMapSerializer<>( feature.map );
		propertyMapSerializer.writePropertyMap( idmap, oos );
	}

	@Override
	public CellAreaFeature deserialize( final FileIdToObjectMap< Cell > idmap, final RefCollection< Cell > pool, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
	{
		// UNITS.
		final String units = ois.readUTF();
		// DATA.
		final DoublePropertyMap< Cell > map = new DoublePropertyMap<>( pool, Double.NaN );
		final DoublePropertyMapSerializer< Cell > propertyMapSerializer = new DoublePropertyMapSerializer<>( map );
		propertyMapSerializer.readPropertyMap( idmap, ois );
		return new CellAreaFeature( map, units );
	}
}
