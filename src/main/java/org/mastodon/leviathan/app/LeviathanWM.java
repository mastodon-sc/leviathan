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

import static org.mastodon.app.MastodonIcons.BDV_VIEW_ICON;
import static org.mastodon.app.MastodonIcons.FEATURES_ICON;
import static org.mastodon.app.MastodonIcons.TABLE_VIEW_ICON;
import static org.mastodon.app.MastodonIcons.TAGS_ICON;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JDialog;

import org.mastodon.app.ui.MastodonFrameView;
import org.mastodon.feature.FeatureSpecsService;
import org.mastodon.feature.ui.FeatureColorModeConfigPage;
import org.mastodon.leviathan.algorithms.FindFaces;
import org.mastodon.leviathan.feature.LeviathanCellFeatureComputation;
import org.mastodon.leviathan.feature.LeviathanCellFeatureProjectionsManager;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.plugin.LeviathanPlugin;
import org.mastodon.leviathan.plugin.LeviathanPluginAppModel;
import org.mastodon.leviathan.plugin.LeviathanPlugins;
import org.mastodon.leviathan.views.bdv.LeviathanCellViewBdv;
import org.mastodon.leviathan.views.bdv.LeviathanJunctionViewBdv;
import org.mastodon.leviathan.views.bdv.overlay.cell.ui.CellRenderSettingsConfigPage;
import org.mastodon.leviathan.views.bdv.overlay.cell.ui.CellRenderSettingsManager;
import org.mastodon.leviathan.views.table.LeviathanCellViewTable;
import org.mastodon.mamut.PreferencesDialog;
import org.mastodon.model.tag.ui.TagSetDialog;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.CommandDescriptionsBuilder;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.ui.keymap.KeymapSettingsPage;
import org.mastodon.util.DummySpimData;
import org.mastodon.util.ToggleDialogAction;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.InvokeOnEDT;
import bdv.viewer.ViewerOptions;
import bdv.viewer.animate.MessageOverlayAnimator;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;

public class LeviathanWM
{
	public static final String NEW_CELL_BDV_VIEW = "new cell bdv view";

	public static final String NEW_JUNCTION_BDV_VIEW = "new junction bdv view";

	public static final String NEW_CELL_TABLE_VIEW = "new full cell table view";

	public static final String NEW_CELL_SELECTION_TABLE_VIEW = "new selection cell table view";

	public static final String PREFERENCES_DIALOG = "Preferences";

	public static final String TAGSETS_DIALOG = "edit tag sets";

	public static final String COMPUTE_FEATURE_DIALOG = "compute features";

	static final String[] NEW_CELL_BDV_VIEW_KEYS = new String[] { "not mapped" };

	static final String[] NEW_JUNCTION_BDV_VIEW_KEYS = new String[] { "not mapped" };

	static final String[] NEW_CELL_TABLE_VIEW_KEYS = new String[] { "not mapped" };

	static final String[] NEW_CELL_SELECTION_TABLE_VIEW_KEYS = new String[] { "not mapped" };

	static final String[] PREFERENCES_DIALOG_KEYS = new String[] { "meta COMMA", "ctrl COMMA" };

	static final String[] TAGSETS_DIALOG_KEYS = new String[] { "not mapped" };

	static final String[] COMPUTE_FEATURE_DIALOG_KEYS = new String[] { "not mapped" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( LeviathanKeyConfigContexts.LEVIATHAN );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( NEW_CELL_BDV_VIEW, NEW_CELL_BDV_VIEW_KEYS, "Open a new BigDataViewer view for the cells." );
			descriptions.add( NEW_JUNCTION_BDV_VIEW, NEW_JUNCTION_BDV_VIEW_KEYS, "Open a new BigDataViewer view for the junctions." );
			descriptions.add( NEW_CELL_TABLE_VIEW, NEW_CELL_TABLE_VIEW_KEYS, "Open a new table view. "
					+ "The table displays the full data." );
			descriptions.add( NEW_CELL_SELECTION_TABLE_VIEW, NEW_CELL_SELECTION_TABLE_VIEW_KEYS,
					"Open a new selection table view. "
							+ "The table only displays the current selection and "
							+ "is updated as the selection changes." );
			descriptions.add( PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS, "Edit Mastodon preferences." );
			descriptions.add( TAGSETS_DIALOG, TAGSETS_DIALOG_KEYS, "Edit tag definitions." );
			descriptions.add( COMPUTE_FEATURE_DIALOG, COMPUTE_FEATURE_DIALOG_KEYS, "Show the feature computation dialog." );
		}
	}

	private final Context context;

	private final LeviathanPlugins plugins;

	private final List< LeviathanCellViewBdv > cellBdvWindows = new ArrayList<>();

	private final List< LeviathanJunctionViewBdv > junctionBdvWindows = new ArrayList<>();

	private final List< LeviathanCellViewTable > cellTableWindows = new ArrayList<>();

	private final KeyPressedManager keyPressedManager;

	private final CellRenderSettingsManager cellRenderSettingsManager;

	private final FeatureColorModeManager featureColorModeManager;

	private final LeviathanCellFeatureProjectionsManager featureProjectionsManager;

	private final KeymapManager keymapManager;

	private final Actions globalAppActions;

	private final AbstractNamedAction newCellBdvViewAction;

	private final AbstractNamedAction newJunctionBdvViewAction;

	private final AbstractNamedAction newCellTableViewAction;

	private final AbstractNamedAction newCellSelectionTableViewAction;

	private final AbstractNamedAction editTagSetsAction;

	private final AbstractNamedAction featureComputationAction;

	private LeviathanCellAppModel cellAppModel;

	private LeviathanJunctionAppModel junctionAppModel;

	private TagSetDialog tagSetDialog;

	private JDialog featureComputationDialog;

	private FindFaces faceFinder;

	private SharedBigDataViewerData sharedBdvData;

	public LeviathanWM( final Context context )
	{
		this.context = context;

		keyPressedManager = new KeyPressedManager();
		cellRenderSettingsManager = new CellRenderSettingsManager();
		featureColorModeManager = new FeatureColorModeManager();
		featureProjectionsManager = new LeviathanCellFeatureProjectionsManager(
				context.getService( FeatureSpecsService.class ),
				featureColorModeManager );
		keymapManager = new KeymapManager();

		final Keymap keymap = keymapManager.getForwardDefaultKeymap();

		plugins = new LeviathanPlugins( keymap );
		discoverPlugins();

		final CommandDescriptions descriptions = buildCommandDescriptions();
		final Consumer< Keymap > augmentInputTriggerConfig = k -> descriptions.augmentInputTriggerConfig( k.getConfig() );
		keymapManager.getUserStyles().forEach( augmentInputTriggerConfig );
		keymapManager.getBuiltinStyles().forEach( augmentInputTriggerConfig );

		globalAppActions = new Actions( keymap.getConfig(), LeviathanKeyConfigContexts.LEVIATHAN );
		keymap.updateListeners().add( () -> {
			globalAppActions.updateKeyConfig( keymap.getConfig() );
			if ( cellAppModel != null )
				cellAppModel.getAppActions().updateKeyConfig( keymap.getConfig() );
		} );

		newCellBdvViewAction = new RunnableAction( NEW_CELL_BDV_VIEW, this::createCellBigDataViewer );
		newJunctionBdvViewAction = new RunnableAction( NEW_JUNCTION_BDV_VIEW, this::createJunctionBigDataViewer );
		newCellTableViewAction = new RunnableAction( NEW_CELL_TABLE_VIEW, () -> createCellTable( false ) );
		newCellSelectionTableViewAction = new RunnableAction( NEW_CELL_SELECTION_TABLE_VIEW, () -> createCellTable( true ) );
		editTagSetsAction = new RunnableAction( TAGSETS_DIALOG, this::editTagSets );
		featureComputationAction = new RunnableAction( COMPUTE_FEATURE_DIALOG, this::computeFeatures );

		globalAppActions.namedAction( newCellBdvViewAction, NEW_CELL_BDV_VIEW_KEYS );
		globalAppActions.namedAction( newJunctionBdvViewAction, NEW_JUNCTION_BDV_VIEW_KEYS );
		globalAppActions.namedAction( newCellTableViewAction, NEW_CELL_TABLE_VIEW_KEYS );
		globalAppActions.namedAction( newCellSelectionTableViewAction, NEW_CELL_SELECTION_TABLE_VIEW_KEYS );
		globalAppActions.namedAction( editTagSetsAction, TAGSETS_DIALOG_KEYS );
		globalAppActions.namedAction( featureComputationAction, COMPUTE_FEATURE_DIALOG_KEYS );

		final PreferencesDialog settings = new PreferencesDialog( null, keymap, new String[] { KeyConfigContexts.MASTODON } );
		settings.addPage( new CellRenderSettingsConfigPage( "Cell BDV Render Settings", cellRenderSettingsManager ) );
		settings.addPage( new KeymapSettingsPage( "Keymap", keymapManager, descriptions ) );
		settings.addPage( new FeatureColorModeConfigPage( "Feature Color Modes", featureColorModeManager, featureProjectionsManager ) );

		final ToggleDialogAction tooglePreferencesDialogAction = new ToggleDialogAction( PREFERENCES_DIALOG, settings );
		globalAppActions.namedAction( tooglePreferencesDialogAction, PREFERENCES_DIALOG_KEYS );

		updateEnabledActions();
	}

	private void discoverPlugins()
	{
		if ( context == null )
			return;

		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< LeviathanPlugin > > infos = pluginService.getPluginsOfType( LeviathanPlugin.class );
		for ( final PluginInfo< LeviathanPlugin > info : infos )
		{
			try
			{
				final LeviathanPlugin plugin = info.createInstance();
				context.inject( plugin );
				plugins.register( plugin );
			}
			catch ( final InstantiableException e )
			{
				e.printStackTrace();
			}
		}
	}

	private void updateEnabledActions()
	{
		newCellBdvViewAction.setEnabled( cellAppModel != null && sharedBdvData != null );
		newJunctionBdvViewAction.setEnabled( junctionAppModel != null && sharedBdvData != null );
		newCellTableViewAction.setEnabled( cellAppModel != null );
		newCellSelectionTableViewAction.setEnabled( cellAppModel != null );
		editTagSetsAction.setEnabled( cellAppModel != null );
		featureComputationAction.setEnabled( cellAppModel != null );
	}

	public void setImagePath( final String path ) throws SpimDataException
	{
		closeAllWindows();
		this.sharedBdvData = toSharedBdvData( path, this );

		if ( cellAppModel != null )
			this.cellAppModel = toAppModel( cellAppModel.getModel(), sharedBdvData, this );

		if ( junctionAppModel != null )
			this.junctionAppModel = toAppModel( junctionAppModel.getModel(), sharedBdvData, this );

		updateEnabledActions();
	}

	public void setCellModel( final CellModel model )
	{
		closeAllWindows();
		this.cellAppModel = toAppModel( model, sharedBdvData, this );
		if ( model == null )
		{
			if ( tagSetDialog != null )
				tagSetDialog.dispose();
			tagSetDialog = null;
			if ( featureComputationDialog != null )
				featureComputationDialog.dispose();
			featureComputationDialog = null;
			if ( featureProjectionsManager != null )
				featureProjectionsManager.setModel( null, 1 );
			updateEnabledActions();
			return;
		}

		if ( junctionAppModel != null )
			faceFinder = FindFaces.create( junctionAppModel.getModel().getGraph(), model.getGraph() );
		SelectionActions.install( cellAppModel.getAppActions(), model.getGraph(), model.getGraph().getLock(), model.getGraph(), cellAppModel.getSelectionModel(), model );
		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		tagSetDialog = new TagSetDialog( null, model.getTagSetModel(), model, keymap, new String[] { LeviathanKeyConfigContexts.LEVIATHAN } );
		tagSetDialog.setIconImages( TAGS_ICON );
		featureComputationDialog = LeviathanCellFeatureComputation.getDialog( cellAppModel, context );
		featureComputationDialog.setIconImages( FEATURES_ICON );
		updateEnabledActions();
		plugins.setAppPluginModel( new LeviathanPluginAppModel( junctionAppModel, this ) );
	}

	public void setJunctionModel( final JunctionModel junctionModel )
	{
		closeAllWindows();
		this.junctionAppModel = toAppModel( junctionModel, sharedBdvData, this );
		if ( junctionAppModel != null && cellAppModel != null )
			faceFinder = FindFaces.create( 
					junctionAppModel.getModel().getGraph(), 
					cellAppModel.getModel().getGraph() );
		updateEnabledActions();
	}

	private synchronized void addCellBdvWindow( final LeviathanCellViewBdv w )
	{
		cellBdvWindows.add( w );
		w.onClose( () -> cellBdvWindows.remove( w ) );
	}

	public void forEachCellBdvView( final Consumer< ? super LeviathanCellViewBdv > action )
	{
		cellBdvWindows.forEach( action );
	}

	private synchronized void addJunctionBdvWindow( final LeviathanJunctionViewBdv w )
	{
		junctionBdvWindows.add( w );
		w.onClose( () -> junctionBdvWindows.remove( w ) );
	}

	public void forEachJunctionBdvView( final Consumer< ? super LeviathanJunctionViewBdv > action )
	{
		junctionBdvWindows.forEach( action );
	}

	private synchronized void addCellTableWindow( final LeviathanCellViewTable table )
	{
		cellTableWindows.add( table );
		table.onClose( () -> {
			cellTableWindows.remove( table );
			table.getContextChooser().updateContextProviders( new ArrayList<>() );
		} );
	}

	public void forEachCellTableView( final Consumer< ? super LeviathanCellViewTable > action )
	{
		cellTableWindows.forEach( action );
	}

	public void forEachView( final Consumer< ? super MastodonFrameView< ?, ?, ?, ?, ?, ? > > action )
	{
		forEachCellBdvView( action );
		forEachJunctionBdvView( action );
		forEachCellTableView( action );
	}

	public LeviathanCellViewBdv createCellBigDataViewer()
	{
		if ( cellAppModel != null )
		{
			final LeviathanCellViewBdv view = new LeviathanCellViewBdv( cellAppModel );
			view.getFrame().setIconImages( BDV_VIEW_ICON );
			addCellBdvWindow( view );
			return view;
		}
		return null;
	}

	public LeviathanJunctionViewBdv createJunctionBigDataViewer()
	{
		if ( junctionAppModel != null )
		{
			final LeviathanJunctionViewBdv view = new LeviathanJunctionViewBdv( junctionAppModel, faceFinder );
			view.getFrame().setIconImages( BDV_VIEW_ICON );
			addJunctionBdvWindow( view );
			return view;
		}
		return null;
	}

	public LeviathanCellViewTable createCellTable( final boolean selectionOnly )
	{
		if ( cellAppModel != null )
		{
			final LeviathanCellViewTable view = new LeviathanCellViewTable( cellAppModel, selectionOnly );
			view.getFrame().setIconImages( TABLE_VIEW_ICON );
			addCellTableWindow( view );
			return view;
		}
		return null;
	}

	public void editTagSets()
	{
		if ( cellAppModel != null )
		{
			tagSetDialog.setVisible( true );
		}
	}

	public void computeFeatures()
	{
		if ( cellAppModel != null )
		{
			featureComputationDialog.setVisible( true );
		}
	}

	public void closeAllWindows()
	{
		final ArrayList< Window > windows = new ArrayList<>();
		for ( final LeviathanCellViewBdv w : cellBdvWindows )
			windows.add( w.getFrame() );
		for ( final LeviathanJunctionViewBdv w : junctionBdvWindows )
			windows.add( w.getFrame() );
		windows.add( tagSetDialog );
		windows.add( featureComputationDialog );

		try
		{
			InvokeOnEDT.invokeAndWait(
					() -> windows.stream()
							.filter( Objects::nonNull )
							.forEach( window -> window.dispatchEvent( new WindowEvent( window, WindowEvent.WINDOW_CLOSING ) ) ) );
		}
		catch ( final InvocationTargetException e )
		{
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	KeyPressedManager getKeyPressedManager()
	{
		return keyPressedManager;
	}

	CellRenderSettingsManager getCellRenderSettingsManager()
	{
		return cellRenderSettingsManager;
	}

	FeatureColorModeManager getFeatureColorModeManager()
	{
		return featureColorModeManager;
	}

	KeymapManager getKeymapManager()
	{
		return keymapManager;
	}

	LeviathanCellAppModel getCellAppModel()
	{
		return cellAppModel;
	}

	LeviathanJunctionAppModel getJunctionAppModel()
	{
		return junctionAppModel;
	}

	Actions getGlobalAppActions()
	{
		return globalAppActions;
	}

	LeviathanPlugins getPlugins()
	{
		return plugins;
	}

	public Context getContext()
	{
		return context;
	}

	public SharedBigDataViewerData getSharedBdvData()
	{
		return sharedBdvData;
	}

	public FeatureSpecsService getFeatureSpecsService()
	{
		return context.getService( FeatureSpecsService.class );
	}

	private CommandDescriptions buildCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		context.inject( builder );
		builder.discoverProviders();
		return builder.build();
	}

	private static final LeviathanCellAppModel toAppModel( final CellModel cellModel, final SharedBigDataViewerData sharedBdvData, final LeviathanWM wm )
	{
		if ( cellModel == null )
			return null;
		return new LeviathanCellAppModel(
				cellModel,
				sharedBdvData,
				wm.keyPressedManager,
				wm.cellRenderSettingsManager,
				wm.featureColorModeManager,
				wm.keymapManager,
				wm.plugins,
				wm.globalAppActions );
	}

	private static final LeviathanJunctionAppModel toAppModel( final JunctionModel junctionModel, final SharedBigDataViewerData sharedBdvData, final LeviathanWM wm )
	{
		if ( junctionModel == null )
			return null;
		return new LeviathanJunctionAppModel(
				junctionModel,
				sharedBdvData,
				wm.keyPressedManager,
				wm.featureColorModeManager,
				wm.keymapManager, wm.plugins,
				wm.globalAppActions );
	}

	public static final String dummyBdvData( final JunctionGraph graph )
	{
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		int maxT = 0;
		for ( final Junction v : graph.vertices() )
		{
			final double x = v.getDoublePosition( 0 );
			if ( x > maxX )
				maxX = x;
			final double y = v.getDoublePosition( 1 );
			if ( y > maxY )
				maxY = y;
			final double z = v.getDoublePosition( 2 );
			if ( z > maxZ )
				maxZ = z;
			v.getDoublePosition( 2 );
			final int t = v.getTimepoint();
			if ( t > maxT )
				maxT = t;
		}

		final int lx = ( int ) Math.ceil( maxX ) + 1;
		final int ly = ( int ) Math.ceil( maxY ) + 1;
		final int lz = ( int ) Math.ceil( maxZ );
		return String.format( "x=%d y=%d z=%d sx=1 sy=1 sz=1 t=%d.dummy", lx, ly, lz, maxT + 1 );
	}

	private static final SharedBigDataViewerData toSharedBdvData( final String path, final LeviathanWM wm ) throws SpimDataException
	{
		final ViewerOptions options = ViewerOptions.options()
				.shareKeyPressedEvents( wm.keyPressedManager )
				.msgOverlay( new MessageOverlayAnimator( 1600 ) )
				.width( 600 )
				.height( 400 );

		SpimDataMinimal spimData = DummySpimData.tryCreate( new File( path ).getName() );
		if ( spimData == null )
		{
			try
			{
				spimData = new XmlIoSpimDataMinimal().load( path );
			}
			catch ( final SpimDataIOException e )
			{
				e.printStackTrace();
				System.err.println( "Could not open image data file. Opening with dummy dataset. Please fix dataset path!" );
				spimData = DummySpimData.tryCreate( "x=100 y=100 z=1 sx=1 sy=1 sz=1 t=10.dummy" );
			}
		}
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				path,
				spimData,
				options,
				() -> {} );
		return sharedBdvData;
	}
}
