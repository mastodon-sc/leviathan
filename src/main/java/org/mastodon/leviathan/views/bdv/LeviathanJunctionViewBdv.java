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
package org.mastodon.leviathan.views.bdv;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.mamut.MamutMenuBuilder.colorMenu;
import static org.mastodon.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.mamut.MamutMenuBuilder.fileMenu;
import static org.mastodon.mamut.MamutMenuBuilder.tagSetMenu;
import static org.mastodon.mamut.MamutMenuBuilder.viewMenu;

import javax.swing.ActionMap;

import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.leviathan.app.LeviathanJunctionAppModel;
import org.mastodon.leviathan.model.junction.Junction;
import org.mastodon.leviathan.model.junction.JunctionGraph;
import org.mastodon.leviathan.model.junction.JunctionModel;
import org.mastodon.leviathan.model.junction.MembranePart;
import org.mastodon.leviathan.views.LeviathanJunctionView;
import org.mastodon.leviathan.views.bdv.overlay.common.OverlayNavigation;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionModelOverlayProperties;
import org.mastodon.leviathan.views.bdv.overlay.junction.JunctionOverlayGraphRenderer;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayEdgeWrapper;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayGraphWrapper;
import org.mastodon.leviathan.views.bdv.overlay.junction.wrap.JunctionOverlayVertexWrapper;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.mamut.UndoActions;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.ui.FocusActions;
import org.mastodon.ui.HighlightBehaviours;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.bdv.BigDataViewerActionsMamut;
import org.mastodon.views.bdv.BigDataViewerMamut;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.ViewerFrameMamut;
import org.mastodon.views.bdv.overlay.BdvHighlightHandler;
import org.mastodon.views.bdv.overlay.BdvSelectionBehaviours;

import bdv.BigDataViewerActions;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerPanel;

public class LeviathanJunctionViewBdv extends LeviathanJunctionView< 
	JunctionOverlayGraphWrapper< Junction, MembranePart >, 
	JunctionOverlayVertexWrapper< Junction, MembranePart >, 
	JunctionOverlayEdgeWrapper< Junction, MembranePart > >
{
	private static int bdvName = 1;

	private final SharedBigDataViewerData sharedBdvData;

//	private final BdvContextProvider< Junction, MembranePart > contextProvider;

	private final ViewerPanel viewer;

	/**
	 * A reference on a supervising instance of the {@code ColoringModel} that
	 * is bound to this instance/window.
	 */
	private final ColoringModel coloringModel;

	public LeviathanJunctionViewBdv( final LeviathanJunctionAppModel appModel )
	{
		super( appModel,
				new JunctionOverlayGraphWrapper<>(
						appModel.getModel().getGraph(),
						appModel.getModel().getGraphIdBimap(),
						appModel.getModel().getSpatioTemporalIndex(),
						appModel.getModel().getGraph().getLock(),
						new JunctionModelOverlayProperties( appModel.getModel().getGraph() ) ),
				new String[] { KeyConfigContexts.BIGDATAVIEWER } );

		sharedBdvData = appModel.getSharedBdvData();

		final String windowTitle = "BigDataViewer " + ( bdvName++ );
		final BigDataViewerMamut bdv = new BigDataViewerMamut( sharedBdvData, windowTitle, groupHandle );
		final ViewerFrameMamut frame = bdv.getViewerFrame();
		setFrame( frame );

		MastodonFrameViewActions.install( viewActions, this );
		BigDataViewerActionsMamut.install( viewActions, bdv );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle menuHandle = new JMenuHandle();
		final JMenuHandle tagSetMenuHandle = new JMenuHandle();
		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu,
				actionMap,
				fileMenu(
						separator(),
						item( BigDataViewerActions.LOAD_SETTINGS ),
						item( BigDataViewerActions.SAVE_SETTINGS ) ),
				viewMenu(
						colorMenu( menuHandle ),
						separator(),
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL ) ),
				editMenu(
						item( UndoActions.UNDO ),
						item( UndoActions.REDO ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ),
						separator(),
						tagSetMenu( tagSetMenuHandle ) ),
				ViewMenuBuilder.menu( "Settings",
						item( BigDataViewerActions.BRIGHTNESS_SETTINGS ),
						item( BigDataViewerActions.VISIBILITY_AND_GROUPING ) ) );
		appModel.getPlugins().addMenus( menu );

		viewer = bdv.getViewer();

		final GraphColorGeneratorAdapter< Junction, MembranePart, JunctionOverlayVertexWrapper< Junction, MembranePart >, JunctionOverlayEdgeWrapper< Junction, MembranePart > > coloring =
				new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );

		final JunctionOverlayGraphRenderer< JunctionOverlayVertexWrapper< Junction, MembranePart >, JunctionOverlayEdgeWrapper< Junction, MembranePart > > junctionOverlay =
				new JunctionOverlayGraphRenderer<>(
				viewGraph,
				highlightModel,
				focusModel,
				selectionModel,
				coloring );
		viewer.getDisplay().overlays().add( junctionOverlay );
		viewer.renderTransformListeners().add( junctionOverlay );
		viewer.addTimePointListener( junctionOverlay );

		final JunctionModel model = appModel.getModel();
		final JunctionGraph modelGraph = model.getGraph();

		coloringModel = registerColoring( coloring, menuHandle,
				() -> viewer.getDisplay().repaint() );

		registerTagSetMenu( tagSetMenuHandle,
				() -> viewer.getDisplay().repaint() );

		highlightModel.listeners().add( () -> viewer.getDisplay().repaint() );
		focusModel.listeners().add( () -> viewer.getDisplay().repaint() );
		modelGraph.addGraphChangeListener( () -> viewer.getDisplay().repaint() );
		modelGraph.addVertexPositionListener( v -> viewer.getDisplay().repaint() );
		selectionModel.listeners().add( () -> viewer.getDisplay().repaint() );

		final OverlayNavigation< JunctionOverlayVertexWrapper< Junction, MembranePart >, JunctionOverlayEdgeWrapper< Junction, MembranePart > > overlayNavigation = new OverlayNavigation<>( viewer, viewGraph );
		navigationHandler.listeners().add( overlayNavigation );

		final BdvHighlightHandler< ?, ? > highlightHandler = new BdvHighlightHandler<>( viewGraph, junctionOverlay, highlightModel );
		viewer.getDisplay().addHandler( highlightHandler );
		viewer.renderTransformListeners().add( highlightHandler );

		// TODO
//		contextProvider = new BdvContextProvider<>( windowTitle, viewGraph, junctionOverlay );
//		viewer.renderTransformListeners().add( contextProvider );

		final AutoNavigateFocusModel< JunctionOverlayVertexWrapper< Junction, MembranePart >, JunctionOverlayEdgeWrapper< Junction, MembranePart > > navigateFocusModel = new AutoNavigateFocusModel<>( focusModel, navigationHandler );

		HighlightBehaviours.install( viewBehaviours, viewGraph, viewGraph.getLock(), viewGraph, highlightModel, model );
		FocusActions.install( viewActions, viewGraph, viewGraph.getLock(), navigateFocusModel, selectionModel );

		BdvSelectionBehaviours.install( viewBehaviours, viewGraph, junctionOverlay, selectionModel, focusModel, navigationHandler );

		NavigationActions.install( viewActions, viewer, sharedBdvData.is2D() );
		viewer.getTransformEventHandler().install( viewBehaviours );

		viewer.addTimePointListener( timePointIndex -> timepointModel.setTimepoint( timePointIndex ) );
		timepointModel.listeners().add( () -> viewer.setTimepoint( timepointModel.getTimepoint() ) );

		// Give focus to display so that it can receive key-presses immediately.
		viewer.getDisplay().requestFocusInWindow();

		frame.setVisible( true );
	}

	// TODO
//	public ContextProvider< Junction > getContextProvider()
//	{
//		return contextProvider;
//	}

	public ViewerPanel getViewerPanelLeviathan()
	{
		return viewer;
	}

	public void requestRepaint()
	{
		viewer.requestRepaint();
	}

	public ColoringModel getColoringModel()
	{
		return coloringModel;
	}
}
