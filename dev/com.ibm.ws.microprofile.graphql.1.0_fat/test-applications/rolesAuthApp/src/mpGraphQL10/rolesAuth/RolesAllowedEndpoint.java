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
@RolesAllowed("Role1")
public class RolesAllowedEndpoint {
    private static Logger LOG = Logger.getLogger(RolesAllowedEndpoint.class.getName());
    
    private final List<Widget> allWidgets = new ArrayList<>();

    public RolesAllowedEndpoint() {
        allWidgets.add(new Widget("Notebook", 20, 2.0));
        allWidgets.add(new Widget("Pencil", 200, 0.5));
    }

    @Query("rolesAllowedClass")
    public List<Widget> rolesAllowedClass() {
        LOG.info("rolesAllowedClass invoked");
        return allWidgets;
    }

    @Query("rolesAllowedClassDenyAllMethod")
    @DenyAll
    public List<Widget> rolesAllowedClassDenyAllMethod() {
        LOG.info("rolesAllowedClassDenyAllMethod invoked");
        return allWidgets;
    }

    @Query("rolesAllowedClassPermitAllMethod")
    @PermitAll
    public List<Widget> rolesAllowedClassPermitAllMethod() {
        LOG.info("rolesAllowedClassPermitAllMethod invoked");
        return allWidgets;
    }

    @Query("rolesAllowedClassRolesAllowedMethod")
    @RolesAllowed("Role1")
    public List<Widget> noAnnoClassRolesAllowedMethod() {
        LOG.info("noAnnoClassRolesAllowedMethod invoked");
        return allWidgets;
    }
}
