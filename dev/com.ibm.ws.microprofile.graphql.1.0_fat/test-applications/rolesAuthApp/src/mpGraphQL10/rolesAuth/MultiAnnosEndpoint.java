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
public class MultiAnnosEndpoint {
    private static Logger LOG = Logger.getLogger(MultiAnnosEndpoint.class.getName());
    
    private final List<Widget> allWidgets = new ArrayList<>();

    public MultiAnnosEndpoint() {
        allWidgets.add(new Widget("Notebook", 20, 2.0));
        allWidgets.add(new Widget("Pencil", 200, 0.5));
    }

    @Query("denyAllAndPermitAll")
    @DenyAll // should win
    @PermitAll
    public List<Widget> denyAllAndPermitAll() {
        LOG.info("denyAllAndPermitAll invoked");
        return allWidgets;
    }

    @Query("denyAllAndRolesAllowed")
    @DenyAll // should win
    @RolesAllowed("Role1")
    public List<Widget> rolesAllowedClassDenyAllMethod() {
        LOG.info("denyAllAndRolesAllowed invoked");
        return allWidgets;
    }

    @Query("rolesAllowedAndPermitAll")
    @PermitAll
    @RolesAllowed("Role1") // should win
    public List<Widget> rolesAllowedClassPermitAllMethod() {
        LOG.info("rolesAllowedAndPermitAll invoked");
        return allWidgets;
    }

    @Query("allThree")
    @DenyAll // should win
    @PermitAll
    @RolesAllowed("Role1")
    public List<Widget> allThree() {
        LOG.info("allThree invoked");
        return allWidgets;
    }
}
