package org.mastodon.leviathan.model;

public class CreateModelExample
{

	public static void main( final String[] args )
	{
		final JunctionGraph graph = new JunctionGraph( 100 );

		final double length = 20.;
		final int size = 5;
		final double ang30 = Math.toRadians( 30 );

		final Junction vref1 = graph.vertexRef();
		final Junction vref2 = graph.vertexRef();
		final Junction vref3 = graph.vertexRef();
		final Junction vref4 = graph.vertexRef();
		final MembranePart eref = graph.edgeRef();
		Junction v1 = null;
		Junction v2 = null;
		Junction v3 = null;
		Junction v4 = null;

		for ( int row = 0; row < size; row++ )
		{
			final double y1 = 2 * row * length * Math.cos( ang30 );
			for ( int col = 0; col < size; col++ )
			{
				// Top line.
				final double x1 = ( 3 * col ) * length;
				v1 = graph.addVertex( vref1 ).init( x1, y1 );
				final double x2 = ( 3 * col + 1 ) * length;
				v2 = graph.addVertex( vref2 ).init( x2, y1 );
				graph.addEdge( v1, v2, eref );

				// To above.
				if ( v3 != null )
					graph.addEdge( v1, v3, eref );
				if ( v4 != null )
					graph.addEdge( v2, v4, eref );

				// Mid line.
				final double y2 = y1 + length * Math.cos( ang30 );
				final double x3 = x1 - length * Math.sin( ang30 );
				v3 = graph.addVertex( vref3 ).init( x3, y2 );
				graph.addEdge( v1, v3, eref );
				if ( v4 != null )
					graph.addEdge( v4, v3, eref );

				final double x4 = x2 + length * Math.sin( ang30 );
				v4 = graph.addVertex( vref4 ).init( x4, y2 );
				graph.addEdge( v2, v4, eref );
			}
		}
	}
}
