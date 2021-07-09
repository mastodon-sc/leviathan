package org.mastodon.leviathan.model;

import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

public class LeviathanAppModel
{

	private static final int NUM_GROUPS = 3;

	private final SharedBigDataViewerData sharedBdvData;

	private final FeatureColorModeManager featureColorModeManager;

	private final int minTimepoint;

	private final int maxTimepoint;

	private final LeviathanModel model;

	public LeviathanAppModel(
			final LeviathanModel model,
			final SharedBigDataViewerData sharedBdvData,
			final KeyPressedManager keyPressedManager,
			final FeatureColorModeManager featureColorModeManager,
			final KeymapManager keymapManager,
			final Actions globalActions )
	{
		this.model = model;
		this.sharedBdvData = sharedBdvData;
		this.featureColorModeManager = featureColorModeManager;
		this.minTimepoint = 0;
		this.maxTimepoint = sharedBdvData.getNumTimepoints() - 1;
	}

	public SharedBigDataViewerData getSharedBdvData()
	{
		return sharedBdvData;
	}

	public LeviathanModel getModel()
	{
		return model;
	}
}
