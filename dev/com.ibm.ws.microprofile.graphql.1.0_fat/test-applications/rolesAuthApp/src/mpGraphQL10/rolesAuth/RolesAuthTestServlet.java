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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/RolesAuthTestServlet")
public class RolesAuthTestServlet extends FATServlet {
    private static final Logger LOG = Logger.getLogger(RolesAuthTestServlet.class.getName());
    
    private static final String USER1 = "Basic " + Base64.getEncoder().encodeToString(("user1:user1pwd").getBytes());
    private static final String USER2 = "Basic " + Base64.getEncoder().encodeToString(("user2:user2pwd").getBytes());

    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/rolesAuthApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);
        URI baseUri = URI.create(baseUriStr);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .register(ClientFilter.class)
                        .baseUri(baseUri);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // No security annotations at class level
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithNoSecAnnotation(HttpServletRequest req, 
                                                                     HttpServletResponse res) throws Exception {
        runQuery("noAnnoClass", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithNoSecAnnotation(HttpServletRequest req, 
                                                                     HttpServletResponse res) throws Exception {
        runQuery("noAnnoClass", USER1, false);
        runQuery("noAnnoClass", USER2, false);
    }
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithNoSecAnnoClassPermitAllMethod(HttpServletRequest req, 
                                                                                   HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassPermitAllMethod", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithNoSecAnnoClassPermitAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassPermitAllMethod", USER1, false);
        runQuery("noAnnoClassPermitAllMethod", USER2, false);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithNoSecAnnoClassDenyAllMethod(HttpServletRequest req, 
                                                                                   HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassDenyAllMethod", null, true);
    }
    @Test
    public void testAuthenticatedUserCannotAccessQueryWithNoSecAnnoClassDenyAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassDenyAllMethod", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithNoSecAnnoClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                        HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassRolesAllowedMethod", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithNoSecAnnoClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                 HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassRolesAllowedMethod", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithNoSecAnnoClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                       HttpServletResponse res) throws Exception {
        runQuery("noAnnoClassRolesAllowedMethod", USER2, true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // @DenyAll annotation at class level
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithDenyAllAnnotation(HttpServletRequest req, 
                                                                          HttpServletResponse res) throws Exception {
        runQuery("denyAllClass", null, true);
    }
    @Test
    public void testAuthenticatedUsersCannotAccessQueryWithDenyAlllAnnotation(HttpServletRequest req, 
                                                                              HttpServletResponse res) throws Exception {
        runQuery("denyAllClass", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithDenyAllClassPermitAllMethod(HttpServletRequest req, 
                                                                                 HttpServletResponse res) throws Exception {
        runQuery("denyAllClassPermitAllMethod", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithDenyAllClassPermitAllMethod(HttpServletRequest req, 
                                                                                    HttpServletResponse res) throws Exception {
        runQuery("denyAllClassPermitAllMethod", USER1, false);
        runQuery("denyAllClassPermitAllMethod", USER2, false);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithDenyAllClassDenyAllMethod(HttpServletRequest req, 
                                                                                 HttpServletResponse res) throws Exception {
        runQuery("denyAllClassDenyAllMethod", null, true);
    }
    @Test
    public void testAuthenticatedUserCannotAccessQueryWithDenyAllClassDenyAllMethod(HttpServletRequest req, 
                                                                                    HttpServletResponse res) throws Exception {
        runQuery("denyAllClassDenyAllMethod", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithDenyAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("denyAllClassRolesAllowedMethod", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithDenyAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                               HttpServletResponse res) throws Exception {
        runQuery("denyAllClassRolesAllowedMethod", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithDenyAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                     HttpServletResponse res) throws Exception {
        runQuery("denyAllClassRolesAllowedMethod", USER2, true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // @PermitAll annotation at class level
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithPermitAllAnnotation(HttpServletRequest req, 
                                                                         HttpServletResponse res) throws Exception {
        runQuery("permitAllClass", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithPermitAlllAnnotation(HttpServletRequest req, 
                                                                             HttpServletResponse res) throws Exception {
        runQuery("permitAllClass", USER1, false);
    }
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithPermitAllClassPermitAllMethod(HttpServletRequest req, 
                                                                                   HttpServletResponse res) throws Exception {
        runQuery("permitAllClassPermitAllMethod", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithPermitAllClassPermitAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("permitAllClassPermitAllMethod", USER1, false);
        runQuery("permitAllClassPermitAllMethod", USER2, false);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithPermitAllClassDenyAllMethod(HttpServletRequest req, 
                                                                                   HttpServletResponse res) throws Exception {
        runQuery("permitAllClassDenyAllMethod", null, true);
    }
    @Test
    public void testAuthenticatedUserCannotAccessQueryWithPermitAllClassDenyAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("permitAllClassDenyAllMethod", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithPermitAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                        HttpServletResponse res) throws Exception {
        runQuery("permitAllClassRolesAllowedMethod", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithPermitAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                 HttpServletResponse res) throws Exception {
        runQuery("permitAllClassRolesAllowedMethod", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithPermitAllClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                       HttpServletResponse res) throws Exception {
        runQuery("permitAllClassRolesAllowedMethod", USER2, true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // @RolesAllowed annotation at class level
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithRolesAllowedAnnotation(HttpServletRequest req, 
                                                                               HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClass", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithRolesAllowedAnnotation(HttpServletRequest req, 
                                                                       HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClass", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithRolesAllowedAnnotation(HttpServletRequest req, 
                                                                             HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClass", USER2, true);
    }
    @Test
    public void testUNAUTHENTICATEDCanAccessQueryWithRolesAllowedClassPermitAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassPermitAllMethod", null, false);
    }
    @Test
    public void testAuthenticatedUsersCanAccessQueryWithRolesAllowedClassPermitAllMethod(HttpServletRequest req, 
                                                                                         HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassPermitAllMethod", USER1, false);
        runQuery("rolesAllowedClassPermitAllMethod", USER2, false);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithRolesAllowedClassDenyAllMethod(HttpServletRequest req, 
                                                                                      HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassDenyAllMethod", null, true);
    }
    @Test
    public void testAuthenticatedUserCannotAccessQueryWithRolesAllowedClassDenyAllMethod(HttpServletRequest req, 
                                                                                         HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassDenyAllMethod", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithRolesAllowedClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                           HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassRolesAllowedMethod", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithRolesAllowedClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                    HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassRolesAllowedMethod", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithRolesAllowedClassRolesAllowedMethod(HttpServletRequest req, 
                                                                                          HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedClassRolesAllowedMethod", USER2, true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Multiple annotations at same level (method) - per docs, @DenyAll trumps @RolesAllowed(...) which trumps @PermitAll
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithDenyAllAndPermitAll(HttpServletRequest req, 
                                                                            HttpServletResponse res) throws Exception {
        runQuery("denyAllAndPermitAll", null, true);
    }
    @Test
    public void testUserInRoleCannotAccessQueryWithDenyAllAndPermitAll(HttpServletRequest req, 
                                                                       HttpServletResponse res) throws Exception {
        runQuery("denyAllAndPermitAll", USER1, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithDenyAllAndRolesAllowed(HttpServletRequest req, 
                                                                               HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", null, true);
    }
    @Test
    public void testUserInRoleCannotAccessQueryWithDenyAllAndRolesAllowed(HttpServletRequest req, 
                                                                          HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", USER1, true);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithDenyAllAndRolesAllowed(HttpServletRequest req, 
                                                                             HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", USER2, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithPermitAllAndRolesAllowed(HttpServletRequest req, 
                                                                                 HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedAndPermitAll", null, true);
    }
    @Test
    public void testUserInRoleCanAccessQueryWithPermitAllAndRolesAllowed(HttpServletRequest req, 
                                                                         HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedAndPermitAll", USER1, false);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithPermitAllAndRolesAllowed(HttpServletRequest req, 
                                                                               HttpServletResponse res) throws Exception {
        runQuery("rolesAllowedAndPermitAll", USER2, true);
    }
    @Test
    public void testUNAUTHENTICATEDCannotAccessQueryWithAllAnnotations(HttpServletRequest req, 
                                                                       HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", null, true);
    }
    @Test
    public void testUserInRoleCannotAccessQueryWithAllAnnotations(HttpServletRequest req, 
                                                                  HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", USER1, true);
    }
    @Test
    public void testUserNotInRoleCannotAccessQueryWithAllAnnotations(HttpServletRequest req, 
                                                                     HttpServletResponse res) throws Exception {
        runQuery("denyAllAndRolesAllowed", USER2, true);
    }

    public void runQuery(String queryName, String auth, boolean expectAuthFailure) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("getAllWidgets");
        query.setQuery("query getAllWidgets {" + System.lineSeparator() +
                       "  " + queryName + " {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response;
        try {
            if (auth == null) {
                response = client.allWidgetsNoAuth(query);
            } else {
                response = client.allWidgets(auth, query);
            }
        } catch (Throwable t) {
            LOG.log(Level.INFO, "Caught (possibly expected) exception during HTTP request", t);
            if (!expectAuthFailure) {
                fail("Caught unexpected exception: " + t);
            }
            return;
        }
        List<Error> errors = response.getErrors();
        if (errors == null) {
            errors = Collections.emptyList();
        }
        if (expectAuthFailure) {
            assertFalse("No exception was thrown when user should have been denied access", errors.isEmpty());
        } else {
            assertTrue("Authorization failure occurred when user should have been granted access", errors.isEmpty());
        }
        System.out.println("Response: " + response);
    }
}
