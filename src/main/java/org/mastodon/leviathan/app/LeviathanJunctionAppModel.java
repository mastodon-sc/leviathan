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
package org.mastodon.leviathan.app;

import org.mastodon.app.MastodonAppModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

/**
 * Data class that stores the data model and the application model of the
 * Leviathan application.
 *
 * @author Jean-Yves Tinevez
 */
public class LeviathanJunctionAppModel extends MastodonAppModel< JunctionModel, Junction, MembranePart >
{
	private static final int NUM_GROUPS = 3;

	private final SharedBigDataViewerData sharedBdvData;

	private final FeatureColorModeManager featureColorModeManager;

	private final int minTimepoint;

	private final int maxTimepoint;

	public LeviathanJunctionAppModel(
			final JunctionModel model,
			final SharedBigDataViewerData sharedBdvData,
			final KeyPressedManager keyPressedManager,
			final FeatureColorModeManager featureColorModeManager,
			final KeymapManager keymapManager,
			final LeviathanPlugins plugins,
			final Actions globalActions )
	{
		super(
				NUM_GROUPS,
				model,
				keyPressedManager,
				keymapManager,
				plugins,
				globalActions,
				new String[] { LeviathanKeyConfigContexts.LEVIATHAN } );

		this.sharedBdvData = sharedBdvData;
		this.featureColorModeManager = featureColorModeManager;
		this.minTimepoint = 0;
		final int maxt;
		if ( sharedBdvData != null )
			maxt = sharedBdvData.getNumTimepoints() - 1;
		else
			maxt = model.getGraph().vertices().stream().mapToInt( Junction::getTimepoint ).max().getAsInt();

		this.maxTimepoint = maxt;
	}

	public FeatureColorModeManager getFeatureColorModeManager()
	{
		return featureColorModeManager;
	}

	public SharedBigDataViewerData getSharedBdvData()
	{
		return sharedBdvData;
	}

	public int getMinTimepoint()
	{
		return minTimepoint;
	}

	public int getMaxTimepoint()
	{
		return maxTimepoint;
	}
}
