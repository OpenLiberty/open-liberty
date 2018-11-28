/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.war.jaxrs.securitycontext;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.Password;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

//http://localhost:8010/securityContextHamApp/rest/scham/
@BasicAuthenticationMechanismDefinition(
                                        realmName = "testRealm")

@Path("scham")
@Produces(TEXT_PLAIN)
public class SecurityContextHamApp extends CommonJaxRSApp {

    // get the Security Context
    @Inject
    private SecurityContext securityContext;

    @GET
    @Path("hello")
    public String hello() {
        return "Hello!";
    }

    @GET
    @Path("callerName")

    //non-secure method
    public String getCallerName(@Context HttpServletRequest request, @Context HttpServletResponse response) {

        if (securityContext != null) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            Credential credential = new UsernamePasswordCredential(username, new Password(password));

            AuthenticationStatus status = securityContext.authenticate(request, response, AuthenticationParameters.withParams().credential(credential));
            String retString = "authenticated callerPrincipal: " + securityContext.getCallerPrincipal().getName();
            return retString;
        }

        return "Null SecurityContext!";
    }

    @GET
    @Path("authCallerName")

    @RolesAllowed("Employee")
    public String getAuthCallerName(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        if (securityContext != null) {
            String retString = "callerPrincipal: " + securityContext.getCallerPrincipal().getName();
            return retString;
        }

        return "Null SecurityContext!";
    }

    @GET
    @Path("hasRole")
    @RolesAllowed({ "Employee", "Manager" })
    public String hasRole(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        String role = request.getParameter("role");
        if (securityContext != null) {
            if (securityContext.isCallerInRole(role)) {
                return "callerPrincipal " + securityContext.getCallerPrincipal().getName() + " is in role " + role + ".";
            } else {
                return "callerPrincipal " + securityContext.getCallerPrincipal().getName() + " is not in role " + role + ".";
            }
        }
        return "Null SecurityContext!";
    }

}
