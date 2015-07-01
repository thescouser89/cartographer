/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.discover;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( "default-carto-source-mgr" )
public class SourceManagerImpl
    implements DiscoverySourceManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public URI createSourceURI( final String source )
    {
        try
        {
            return new URL( source ).toURI();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( String.format( "Invalid source URI: %s. Reason: %s", source, e.getMessage() ), e );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( String.format( "Invalid source URL: %s. Reason: %s", source, e.getMessage() ), e );
        }

        return null;
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final String... sources )
        throws CartoDataException
    {
        boolean result = false;
        for ( final String source : sources )
        {
            final URI src = createSourceURI( source );
            if ( src != null )
            {
                if ( params.getActiveSources()
                           .contains( src ) )
                {
                    continue;
                }

                params.addActiveSource( src );
                result = result || params.getActiveSources()
                                         .contains( src );
            }
        }

        return result;
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final Collection<? extends Location> sources )
        throws CartoDataException
    {
        boolean result = false;
        for ( final Location source : sources )
        {
            final URI src = createSourceURI( source.getUri() );
            if ( src != null )
            {
                if ( params.getActiveSources()
                           .contains( src ) )
                {
                    continue;
                }

                params.addActiveSource( src );
                result = result || params.getActiveSources()
                                         .contains( src );
            }
        }

        return result;
    }

    @Override
    public String getFormatHint()
    {
        return "Any valid URL supported by a configured galley transport";
    }

    @Override
    public Location createLocation( final Object source )
    {
        return new SimpleLocation( source.toString() );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
    {
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>( sources.length );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<SimpleLocation> locations = new ArrayList<SimpleLocation>( sources.size() );
        for ( final Object source : sources )
        {
            locations.add( new SimpleLocation( source.toString() ) );
        }

        return locations;
    }

}