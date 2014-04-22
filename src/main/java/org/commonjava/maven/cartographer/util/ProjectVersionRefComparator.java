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

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;

public class ProjectVersionRefComparator
    implements Comparator<ProjectVersionRef>
{

    @Override
    public int compare( final ProjectVersionRef first, final ProjectVersionRef second )
    {
        int comp = first.getGroupId()
                        .compareTo( second.getGroupId() );

        if ( comp == 0 )
        {
            comp = first.getArtifactId()
                        .compareTo( second.getArtifactId() );
        }

        if ( comp == 0 )
        {
            try
            {
                comp = first.getVersionSpec()
                            .compareTo( second.getVersionSpec() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                comp = first.getVersionString()
                            .compareTo( second.getVersionString() );
            }
        }

        return comp;
    }

}
