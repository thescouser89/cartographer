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
package org.commonjava.maven.cartographer.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.testutil.CartoFixture;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.PluginDependencyView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.core.transport.job.TestDownload;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenModelProcessorTest
{

    private static final String PROJ_BASE = "pom-processor/";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public CartoFixture fixture = new CartoFixture( new CoreFixture() );

    @Before
    public void setup()
    {
        fixture.initMissingComponents();
    }

    @Test
    public void resolvePluginVersionFromManagementExpression()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final ProjectVersionRef childRef = new ProjectVersionRef( "org.test", "test-child", "1.0" );

        final LinkedHashMap<ProjectVersionRef, String> lineage = new LinkedHashMap<ProjectVersionRef, String>();
        lineage.put( childRef, "child.pom.xml" );
        lineage.put( new ProjectVersionRef( "org.test", "test-parent", "1.0" ), "parent.pom.xml" );

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true, 10 );

        final String base = PROJ_BASE + "version-expression-managed-parent-plugin/";

        for ( final Entry<ProjectVersionRef, String> entry : lineage.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final String filename = entry.getValue();

            final String path = ArtifactPathUtils.formatArtifactPath( ref.asPomArtifact(), fixture.getMapper() );

            fixture.getTransport()
                   .registerDownload( new ConcreteResource( location, path ), new TestDownload( base + filename ) );
        }

        final Transfer transfer = fixture.getArtifacts()
                                         .retrieve( location, childRef.asPomArtifact() );

        final MavenPomView pomView = fixture.getPomReader()
                                            .read( childRef, transfer, Collections.singletonList( location ) );

        final List<PluginView> buildPlugins = pomView.getAllBuildPlugins();

        assertThat( buildPlugins, notNullValue() );
        assertThat( buildPlugins.size(), equalTo( 1 ) );

        final PluginView pv = buildPlugins.get( 0 );
        assertThat( pv, notNullValue() );
        assertThat( pv.getVersion(), equalTo( "1.0" ) );

        final DefaultDiscoveryConfig discoveryConfig = new DefaultDiscoveryConfig( src );
        discoveryConfig.setIncludeManagedDependencies( true );
        discoveryConfig.setIncludeBuildSection( true );
        discoveryConfig.setIncludeManagedPlugins( false );

        final DiscoveryResult result = fixture.getModelProcessor()
                                              .readRelationships( pomView, src, discoveryConfig );

        final Set<ProjectRelationship<?>> rels = result.getAcceptedRelationships();

        logger.info( "Found {} relationships:\n\n  {}", rels.size(), new JoinString( "\n  ", rels ) );

        boolean seen = false;
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( rel.getType() == RelationshipType.PLUGIN && !rel.isManaged() )
            {
                if ( seen )
                {
                    fail( "Multiple plugins found!" );
                }

                seen = true;
                assertThat( rel.getTarget()
                               .getVersionString(), equalTo( "1.0" ) );
            }
        }

        if ( !seen )
        {
            fail( "Plugin relationship not found!" );
        }
    }

    @Test
    public void resolvePluginDependencyFromManagedInfo()
        throws Exception
    {
        final URI src = new URI( "http://nowhere.com/path/to/repo" );

        final ProjectVersionRef childRef = new ProjectVersionRef( "org.test", "test-child", "1.0" );

        final LinkedHashMap<ProjectVersionRef, String> lineage = new LinkedHashMap<ProjectVersionRef, String>();
        lineage.put( childRef, "child.pom.xml" );
        lineage.put( new ProjectVersionRef( "org.test", "test-parent", "1.0" ), "parent.pom.xml" );

        final Location location = new SimpleLocation( "test", src.toString(), false, true, true, false, true, 10 );

        final String base = PROJ_BASE + "dependency-in-managed-parent-plugin/";

        for ( final Entry<ProjectVersionRef, String> entry : lineage.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final String filename = entry.getValue();

            final String path = ArtifactPathUtils.formatArtifactPath( ref.asPomArtifact(), fixture.getMapper() );

            fixture.getTransport()
                   .registerDownload( new ConcreteResource( location, path ), new TestDownload( base + filename ) );
        }

        final Transfer transfer = fixture.getArtifacts()
                                         .retrieve( location, childRef.asPomArtifact() );

        final MavenPomView pomView = fixture.getPomReader()
                                            .read( childRef, transfer, Collections.singletonList( location ) );

        final List<PluginView> buildPlugins = pomView.getAllBuildPlugins();

        assertThat( buildPlugins, notNullValue() );
        assertThat( buildPlugins.size(), equalTo( 1 ) );

        final PluginView pv = buildPlugins.get( 0 );
        assertThat( pv, notNullValue() );

        final List<PluginDependencyView> deps = pv.getLocalPluginDependencies();
        assertThat( deps, notNullValue() );
        assertThat( deps.size(), equalTo( 1 ) );

        final PluginDependencyView pdv = deps.get( 0 );
        assertThat( pdv, notNullValue() );
        assertThat( pdv.asArtifactRef()
                       .getVersionString(), equalTo( "1.0" ) );

        final DefaultDiscoveryConfig discoveryConfig = new DefaultDiscoveryConfig( src );
        discoveryConfig.setIncludeManagedDependencies( true );
        discoveryConfig.setIncludeBuildSection( true );
        discoveryConfig.setIncludeManagedPlugins( false );

        final DiscoveryResult result = fixture.getModelProcessor()
                                              .readRelationships( pomView, src, discoveryConfig );
        final Set<ProjectRelationship<?>> rels = result.getAcceptedRelationships();

        logger.info( "Found {} relationships:\n\n  {}", rels.size(), new JoinString( "\n  ", rels ) );

        boolean seen = false;
        for ( final ProjectRelationship<?> rel : rels )
        {
            if ( rel.getType() == RelationshipType.PLUGIN_DEP && !rel.isManaged() )
            {
                if ( seen )
                {
                    fail( "Multiple plugin dependencies found!" );
                }

                seen = true;
                assertThat( rel.getTarget()
                               .getVersionString(), equalTo( "1.0" ) );
            }
        }

        if ( !seen )
        {
            fail( "Plugin-dependency relationship not found!" );
        }
    }

}
