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
package org.mastodon.leviathan.views.table;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;

import java.util.function.Function;

import javax.swing.ActionMap;
import javax.swing.JPanel;

import org.mastodon.app.IdentityViewGraph;
import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.SearchVertexLabel;
import org.mastodon.app.ui.ViewFrame;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.feature.FeatureModel;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.leviathan.app.LeviathanCellAppModel;
import org.mastodon.leviathan.model.cell.Cell;
import org.mastodon.leviathan.model.cell.CellGraph;
import org.mastodon.leviathan.model.cell.CellModel;
import org.mastodon.leviathan.model.cell.CellPool;
import org.mastodon.leviathan.model.cell.Link;
import org.mastodon.leviathan.views.LeviathanCellView;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.MamutMenuBuilder;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.table.FeatureTagTablePanel;
import org.mastodon.views.table.TableViewActions;
import org.mastodon.views.table.TableViewFrame;

public class LeviathanCellViewTable extends LeviathanCellView< ViewGraph< Cell, Link, Cell, Link >, Cell, Link >
{

	private static final String[] CONTEXTS = new String[] { KeyConfigContexts.TABLE };

	private final ColoringModel coloringModel;

	private final boolean selectionOnly;

	public LeviathanCellViewTable( final LeviathanCellAppModel appModel, final boolean selectionOnly )
	{
		super( appModel, IdentityViewGraph.wrap( appModel.getModel().getGraph(), appModel.getModel().getGraphIdBimap() ), CONTEXTS );
		this.selectionOnly = selectionOnly;

		final GraphColorGeneratorAdapter< Cell, Link, Cell, Link > coloring = new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );

		final TableViewFrame< LeviathanCellAppModel, ViewGraph< Cell, Link, Cell, Link >, Cell, Link > frame =
				new TableViewFrame<>(
						appModel,
						viewGraph,
						appModel.getModel().getFeatureModel(),
						appModel.getModel().getTagSetModel(),
						( v ) -> v.getLabel(),
						new Function< Link, String >()
						{

							private final Cell ref = appModel.getModel().getGraph().vertexRef();

							@Override
							public String apply( final Link t )
							{
								return t.getSource( ref ).getLabel() + " \u2192 " + t.getTarget( ref ).getLabel();
							}
						},
						( v, lbl ) -> v.setLabel( lbl ),
						null,
						groupHandle,
						navigationHandler,
						appModel.getModel(),
						coloring );

		setFrame( frame );

		// Restore position.
		frame.setSize( 400, 400 );
		frame.setLocationRelativeTo( null );

		/*
		 * Deal with actions.
		 */

		final CellModel model = appModel.getModel();
		final FeatureModel featureModel = model.getFeatureModel();
		final TagSetModel< Cell, Link > tagSetModel = model.getTagSetModel();

		focusModel.listeners().add( frame );
		highlightModel.listeners().add( frame );
		featureModel.listeners().add( frame );
		tagSetModel.listeners().add( frame );

		MastodonFrameViewActions.install( viewActions, this );
		TableViewActions.install( viewActions, frame );

		final JPanel searchPanel = SearchVertexLabel.install( viewActions, viewGraph, navigationHandler, selectionModel, focusModel, frame.getCurrentlyDisplayedTable() );
		frame.getSettingsPanel().add( searchPanel );

		onClose( () -> {
			focusModel.listeners().remove( frame );
			highlightModel.listeners().remove( frame );
			featureModel.listeners().remove( frame );
			tagSetModel.listeners().remove( frame );
		} );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle menuHandle = new JMenuHandle();

		MamutMenuBuilder.build( menu, actionMap,
				MamutMenuBuilder.fileMenu(
						item( TableViewActions.EXPORT_TO_CSV ),
						separator() ) );
		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				MamutMenuBuilder.viewMenu(
						MamutMenuBuilder.colorMenu( menuHandle ),
						separator(),
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL ) ),
				MamutMenuBuilder.editMenu(
						item( TableViewActions.EDIT_LABEL ),
						item( TableViewActions.TOGGLE_TAG ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ) ) );
		appModel.getPlugins().addMenus( menu );

		coloringModel = registerColoring( coloring, menuHandle, () -> {
			frame.getEdgeTable().repaint();
			frame.getVertexTable().repaint();
		} );

		/*
		 * Deal with content.
		 */

		final FeatureTagTablePanel< Cell > vertexTable = frame.getVertexTable();
		final FeatureTagTablePanel< Link > edgeTable = frame.getEdgeTable();
		final SelectionModel< Cell, Link > selectionModel = appModel.getSelectionModel();

		if ( selectionOnly )
		{
			// Pass only the selection.
			frame.setTitle( "Selection table" );
			frame.setMirrorSelection( false );
			final SelectionListener selectionListener = () -> {
				vertexTable.setRows( selectionModel.getSelectedVertices() );
				edgeTable.setRows( selectionModel.getSelectedEdges() );
			};
			selectionModel.listeners().add( selectionListener );
			selectionListener.selectionChanged();
			onClose( () -> selectionModel.listeners().remove( selectionListener ) );
		}
		else
		{
			// Pass and listen to the full graph.
			final CellGraph graph = appModel.getModel().getGraph();
			final GraphChangeListener graphChangeListener = () -> {
				vertexTable.setRows( graph.vertices() );
				edgeTable.setRows( graph.edges() );
			};
			graph.addGraphChangeListener( graphChangeListener );
			graphChangeListener.graphChanged();
			onClose( () -> graph.removeGraphChangeListener( graphChangeListener ) );

			// Listen to selection changes.
			frame.setMirrorSelection( true );
			selectionModel.listeners().add( frame );
			frame.selectionChanged();
			onClose( () -> selectionModel.listeners().remove( frame ) );
		}
		/*
		 * Register a listener to vertex label property changes, will update the
		 * table-view when the label change.
		 */
		final CellPool cellPool = ( CellPool ) appModel.getModel().getGraph().vertices().getRefPool();
		cellPool.labelProperty().propertyChangeListeners().add( v -> frame.repaint() );

		/*
		 * Show table.
		 */

		frame.setVisible( true );
	}

	@Override
	public TableViewFrame< LeviathanCellAppModel, ViewGraph< Cell, Link, Cell, Link >, Cell, Link > getFrame()
	{
		final ViewFrame f = super.getFrame();
		@SuppressWarnings( "unchecked" )
		final TableViewFrame< LeviathanCellAppModel, ViewGraph< Cell, Link, Cell, Link >, Cell, Link > vf = ( TableViewFrame< LeviathanCellAppModel, ViewGraph< Cell, Link, Cell, Link >, Cell, Link > ) f;
		return vf;
	}

	public ContextChooser< Cell > getContextChooser()
	{
		return getFrame().getContextChooser();
	}

	public ColoringModel getColoringModel()
	{
		return coloringModel;
	}

	public boolean isSelectionTable()
	{
		return selectionOnly;
	}
}
