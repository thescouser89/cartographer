/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.discover.post.meta;

import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.TRANSFER_CTX_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataScannerSupport
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<MetadataScanner> scannerInstances;

    private Iterable<MetadataScanner> scanners;

    protected MetadataScannerSupport()
    {
    }

    public MetadataScannerSupport( final MetadataScanner... scanners )
    {
        this.scanners = new HashSet<MetadataScanner>( Arrays.asList( scanners ) );
    }

    public MetadataScannerSupport( final Collection<MetadataScanner> scanners )
    {
        this.scanners = new HashSet<MetadataScanner>( scanners );
    }

    @PostConstruct
    public void setScanners()
    {
        this.scanners = this.scannerInstances;
    }

    public Map<String, String> scan( final ProjectVersionRef ref, final List<? extends Location> locations, final MavenPomView pomView,
                                     final Transfer transfer )
    {
        if ( scanners == null || !scanners.iterator()
                                          .hasNext() )
        {
            return null;
        }

        final Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put( POM_VIEW_CTX_KEY, pomView );
        ctx.put( TRANSFER_CTX_KEY, transfer );

        final Map<String, String> result = new HashMap<String, String>();
        for ( final MetadataScanner scanner : scanners )
        {
            if ( scanner == null )
            {
                continue;
            }

            logger.debug( "Running metadata scanner: {} for: {}", scanner.getClass()
                                                                         .getSimpleName(), ref );
            try
            {
                final Map<String, String> scanResult = scanner.scan( ref, locations, ctx );
                if ( scanResult != null && !scanResult.isEmpty() )
                {
                    result.putAll( scanResult );
                }
            }
            catch ( final Exception e )
            {
                logger.error( "Failed to execute metadata scanner: {} against: {}. Reason: {}", e, scanner.getClass()
                                                                                                          .getSimpleName(), ref, e.getMessage() );
            }
        }

        return result;
    }

}
