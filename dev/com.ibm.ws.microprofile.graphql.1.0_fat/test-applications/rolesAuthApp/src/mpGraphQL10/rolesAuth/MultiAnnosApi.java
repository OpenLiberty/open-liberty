/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.rolesAuth;

import static mpGraphQL10.rolesAuth.RolesAuthTestServlet.ACCESSED;

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
public class MultiAnnosApi {

    @Query("denyAllAndPermitAll")
    @DenyAll // should win
    @PermitAll
    public String denyAllAndPermitAll() { return ACCESSED; }

    @Query("denyAllAndRolesAllowed1")
    @DenyAll // should win
    @RolesAllowed("Role1")
    public String rolesAllowedAndDenyAllMethod() { return ACCESSED; }

    @Query("rolesAllowed1AndPermitAll")
    @PermitAll
    @RolesAllowed("Role1") // should win
    public String rolesAllowedAndPermitAllMethod() { return ACCESSED; }

    @Query("allThree")
    @DenyAll // should win
    @PermitAll
    @RolesAllowed("Role1")
    public String allThree() { return ACCESSED; }
}