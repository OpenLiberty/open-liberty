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

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
public class UnannotatedApi {

    @PermitAll
    @Query("unannotated_permitAll")
    public String permitAll() { return ACCESSED; }
    
    @Query("unannotated_unannotated")
    public String unannotated() { return ACCESSED; }
    
    @DenyAll
    @Query("unannotated_denyAll")
    public String denyAll() { return ACCESSED; }
    
    @RolesAllowed("Role1")
    @Query("unannotated_rolesAllowed1")
    public String rolesAllowed1() { return ACCESSED; }
    
    @RolesAllowed("Role2")
    @Query("unannotated_rolesAllowed2")
    public String rolesAllowed2() { return ACCESSED; }
}
