/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.rolesAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.GraphQLApi;

@GraphQLApi
public class NoSecAnnotationEndpoint {
    private static Logger LOG = Logger.getLogger(NoSecAnnotationEndpoint.class.getName());
    
    private final List<Widget> allWidgets = new ArrayList<>();

    public NoSecAnnotationEndpoint() {
        allWidgets.add(new Widget("Notebook", 20, 2.0));
        allWidgets.add(new Widget("Pencil", 200, 0.5));
    }

    @Query("noAnnoClass")
    public List<Widget> noAnnoClass() {
        LOG.info("noAnnoClass invoked");
        return allWidgets;
    }

    @Query("noAnnoClassDenyAllMethod")
    @DenyAll
    public List<Widget> noAnnoClassDenyAllMethod() {
        LOG.info("noAnnoClassDenyAllMethod invoked");
        return allWidgets;
    }

    @Query("noAnnoClassPermitAllMethod")
    @PermitAll
    public List<Widget> noAnnoClassPermitAllMethod() {
        LOG.info("noAnnoClassPermitAllMethod invoked");
        return allWidgets;
    }

    @Query("noAnnoClassRolesAllowedMethod")
    @RolesAllowed("Role1")
    public List<Widget> noAnnoClassRolesAllowedMethod() {
        LOG.info("noAnnoClassRolesAllowedMethod invoked");
        return allWidgets;
    }
}
