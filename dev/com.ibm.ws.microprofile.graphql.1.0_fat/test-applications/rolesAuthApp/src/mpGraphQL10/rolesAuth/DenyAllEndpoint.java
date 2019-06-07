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
@DenyAll
public class DenyAllEndpoint {
    private static Logger LOG = Logger.getLogger(DenyAllEndpoint.class.getName());

    private final List<Widget> allWidgets = new ArrayList<>();

    public DenyAllEndpoint() {
        allWidgets.add(new Widget("Notebook", 20, 2.0));
        allWidgets.add(new Widget("Pencil", 200, 0.5));
    }

    @Query("denyAllClass")
    public List<Widget> denyAllClass() {
        LOG.info("denyAllClass invoked");
        return allWidgets;
    }
    
    @Query("denyAllClassDenyAllMethod")
    @DenyAll
    public List<Widget> denyAllClassDenyAllMethod() {
        LOG.info("denyAllClassDenyAllMethod invoked");
        return allWidgets;
    }

    @Query("denyAllClassPermitAllMethod")
    @PermitAll
    public List<Widget> denyAllClassPermitAllMethod() {
        LOG.info("denyAllClassPermitAllMethod invoked");
        return allWidgets;
    }

    @Query("denyAllClassRolesAllowedMethod")
    @RolesAllowed("Role1")
    public List<Widget> denyAllClassRolesAllowedMethod() {
        LOG.info("denyAllClassRolesAllowedMethod invoked");
        return allWidgets;
    }
}
