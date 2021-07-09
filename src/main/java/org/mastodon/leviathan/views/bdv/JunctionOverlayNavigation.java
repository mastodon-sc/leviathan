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

import org.mastodon.leviathan.model.Junction;
import org.mastodon.leviathan.model.JunctionGraph;
import org.mastodon.leviathan.model.LeviathanModel;
import org.mastodon.leviathan.model.MembranePart;
import org.mastodon.model.NavigationListener;
import org.mastodon.ui.NavigationEtiquette;

import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.TranslationAnimator;
import net.imglib2.realtransform.AffineTransform3D;

public class JunctionOverlayNavigation
		implements NavigationListener< Junction, MembranePart >
{
	private final ViewerPanel panel;

	private NavigationEtiquette navigationEtiquette;

	private NavigationBehaviour navigationBehaviour;

	private final LeviathanModel model;

	public JunctionOverlayNavigation(
			final ViewerPanel panel,
			final LeviathanModel model )
	{
		this.panel = panel;
		this.model = model;
		setNavigationEtiquette( NavigationEtiquette.MINIMAL );
	}

	public NavigationEtiquette getNavigationEtiquette()
	{
		return navigationEtiquette;
	}

	public void setNavigationEtiquette( final NavigationEtiquette navigationEtiquette )
	{
		this.navigationEtiquette = navigationEtiquette;

		switch ( navigationEtiquette )
		{
		case MINIMAL:
			navigationBehaviour = new MinimalNavigationBehaviour( 100, 100 );
			break;
		case CENTER_IF_INVISIBLE:
			navigationBehaviour = new CenterIfInvisibleNavigationBehaviour();
			break;
		case CENTERING:
		default:
			navigationBehaviour = new CenteringNavigationBehaviour();
			break;
		}
	}

	@Override
	public void navigateToVertex( final Junction vertex )
	{
//		// Always move in T.
//		final int tp = vertex.getTimepoint();
//		panel.setTimepoint( tp );

		final AffineTransform3D currentTransform = panel.state().getViewerTransform();
		final double[] target = navigationBehaviour.navigateToVertex( vertex, currentTransform );
		if ( target != null )
		{
			final TranslationAnimator animator = new TranslationAnimator( currentTransform, target, 300 );
			animator.setTime( System.currentTimeMillis() );
			panel.setTransformAnimator( animator );
		}

		panel.requestRepaint();
	}

	@Override
	public void navigateToEdge( final MembranePart edge )
	{
		// Always move in T.
//		final Junction ref = graph.vertexRef();
//		final int tp = edge.getTarget( ref ).getTimepoint();
//		graph.releaseRef( ref );
//		panel.setTimepoint( tp );

		final AffineTransform3D currentTransform = panel.state().getViewerTransform();
		final double[] target = navigationBehaviour.navigateToEdge( edge, currentTransform );
		if ( target != null )
		{
			final TranslationAnimator animator = new TranslationAnimator( currentTransform, target, 300 );
			animator.setTime( System.currentTimeMillis() );
			panel.setTransformAnimator( animator );
		}

		panel.requestRepaint();
	}

	/*
	 * Navigation behaviours
	 */

	interface NavigationBehaviour
	{
		public double[] navigateToVertex( final Junction vertex, final AffineTransform3D currentTransform );

		public double[] navigateToEdge( final MembranePart edge, final AffineTransform3D currentTransform );
	}

	private class CenteringNavigationBehaviour implements NavigationBehaviour
	{

		/** Junction position in global coordinate system. */
		private final double[] pos = new double[ 3 ];

		/** Junction position in viewer coordinate system. */
		private final double[] vPos = new double[ 3 ];

		@Override
		public double[] navigateToVertex( final Junction vertex, final AffineTransform3D t )
		{
			final int width = panel.getWidth();
			final int height = panel.getHeight();
			vertex.localize( pos );
			t.apply( pos, vPos );

			final double dx = width / 2 - vPos[ 0 ] + t.get( 0, 3 );
			final double dy = height / 2 - vPos[ 1 ] + t.get( 1, 3 );
			final double dz = -vPos[ 2 ] + t.get( 2, 3 );

			return new double[] { dx, dy, dz };
		}

		@Override
		public double[] navigateToEdge( final MembranePart edge, final AffineTransform3D currentTransform )
		{
			System.err.println( "not implemented: CenteringNavigationBehaviour.navigateToEdge()" );
			new Throwable().printStackTrace( System.out );
			return null;
		}
	}

	private class CenterIfInvisibleNavigationBehaviour implements NavigationBehaviour
	{

		/** Junction position in global coordinate system. */
		private final double[] pos = new double[ 3 ];

		/** Junction position in viewer coordinate system. */
		private final double[] vPos = new double[ 3 ];

		@Override
		public double[] navigateToVertex( final Junction vertex, final AffineTransform3D t )
		{
			final int width = panel.getWidth();
			final int height = panel.getHeight();
			vertex.localize( pos );
			t.apply( pos, vPos );

			double dx = t.get( 0, 3 );
			if ( vPos[ 0 ] < 0 || vPos[ 0 ] > width )
				dx += width / 2 - vPos[ 0 ];
			double dy = t.get( 1, 3 );
			if ( vPos[ 1 ] < 0 || vPos[ 1 ] > height )
				dy += height / 2 - vPos[ 1 ];
			final double dz = -vPos[ 2 ] + t.get( 2, 3 );

			return new double[] { dx, dy, dz };
		}

		@Override
		public double[] navigateToEdge( final MembranePart edge, final AffineTransform3D currentTransform )
		{
			System.err.println( "not implemented: CenterIfInvisibleNavigationBehaviour.navigateToEdge()" );
			new Throwable().printStackTrace( System.out );
			return null;
		}
	}

	private class MinimalNavigationBehaviour implements NavigationBehaviour
	{
		/** Junction position in global coordinate system. */
		private final double[] pos1 = new double[ 3 ];

		private final double[] pos2 = new double[ 3 ];

		/** Junction position in viewer coordinate system. */
		private final double[] vPos1 = new double[ 3 ];

		private final double[] vPos2 = new double[ 3 ];

		final double[] min = new double[ 3 ];

		final double[] max = new double[ 3 ];

		final double[] c = new double[ 3 ];

		private final int screenBorderX;

		private final int screenBorderY;

		public MinimalNavigationBehaviour( final int screenBorderX, final int screenBorderY )
		{
			this.screenBorderX = screenBorderX;
			this.screenBorderY = screenBorderY;
		}

		@Override
		public double[] navigateToVertex( final Junction vertex, final AffineTransform3D t )
		{
			final int width = panel.getWidth();
			final int height = panel.getHeight();
			vertex.localize( pos1 );
			t.apply( pos1, vPos1 );

			double dx = t.get( 0, 3 );
			if ( vPos1[ 0 ] < screenBorderX )
				dx += screenBorderX - vPos1[ 0 ];
			else if ( vPos1[ 0 ] > width - screenBorderX )
				dx += width - screenBorderX - vPos1[ 0 ];

			double dy = t.get( 1, 3 );
			if ( vPos1[ 1 ] < screenBorderY )
				dy += screenBorderY - vPos1[ 1 ];
			else if ( vPos1[ 1 ] > height - screenBorderY )
				dy += height - screenBorderY - vPos1[ 1 ];

			final double dz = -vPos1[ 2 ] + t.get( 2, 3 );

			return new double[] { dx, dy, dz };
		}

		@Override
		public double[] navigateToEdge( final MembranePart edge, final AffineTransform3D t )
		{
			final int timepoint = edge.getTimepoint();
			final JunctionGraph graph = model.junctionGraph( timepoint );
			final Junction ref1 = graph.vertexRef();
			final Junction ref2 = graph.vertexRef();

			final int width = panel.getWidth();
			final int height = panel.getHeight();
			final int edgeMaxWidth = width - 2 * screenBorderX;
			final int edgeMaxHeight = height - 2 * screenBorderY;


			edge.getSource( ref1 ).localize( pos1 );
			t.apply( pos1, vPos1 );
			edge.getTarget( ref2 ).localize( pos2 );
			t.apply( pos2, vPos2 );
			for ( int d = 0; d < 3; ++d )
			{
				if ( vPos1[ d ] < vPos2[ d ] )
				{
					min[ d ] = vPos1[ d ];
					max[ d ] = vPos2[ d ];
				}
				else
				{
					max[ d ] = vPos1[ d ];
					min[ d ] = vPos2[ d ];
				}
				c[ d ] = 0.5 * ( min[ d ] + max[ d ] );
			}

			double dx = t.get( 0, 3 );
			if ( max[ 0 ] - min[ 0 ] > edgeMaxWidth )
				dx += ( width / 2 ) - c[ 0 ];
			else if ( min[ 0 ] < screenBorderX )
				dx += screenBorderX - min[ 0 ];
			else if ( max[ 0 ] > width - screenBorderX )
				dx += width - screenBorderX - max[ 0 ];

			double dy = t.get( 1, 3 );
			if ( max[ 1 ] - min[ 1 ] > edgeMaxHeight )
				dy += ( height / 2 ) - c[ 1 ];
			else if ( min[ 1 ] < screenBorderY )
				dy += screenBorderY - min[ 1 ];
			else if ( max[ 1 ] > height - screenBorderY )
				dy += height - screenBorderY - max[ 1 ];

			final double dz = -c[ 2 ] + t.get( 2, 3 );

			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );

			return new double[] { dx, dy, dz };
		}
	}
}
