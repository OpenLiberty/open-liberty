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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.junit.Test;



@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/RolesAuthTestServlet")
public class RolesAuthTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(RolesAuthTestServlet.class.getName());

    static final String ACCESSED = "Accessed";
    
    ClientInterface1 role1Client;
    ClientInterface2 role2Client;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @FunctionalInterface
    private static interface ThrowingSupplier<T> {
        T getStringThatCouldThrowGraphQlClientException() throws GraphQlClientException;
    }

    private static void assertAuthorized(ThrowingSupplier<String> supplier) {
        try {
            assertEquals(ACCESSED, supplier.getStringThatCouldThrowGraphQlClientException());
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Caught unexpected exception when expected an authorized response, " + t);
        }
    }
    private static void assertUnauthorized(ThrowingSupplier<String> supplier) {
        try {
            String s = supplier.getStringThatCouldThrowGraphQlClientException();
            fail("Expected to be unauthorized, but was able to execute query and receive: " + s);
        } catch (GraphQlClientException ex) {
            assertNotNull(ex.getMessage());
            assertTrue("Unexpected error message (expected \"Unauthorized\"): " + ex.getMessage(),
                       ex.getMessage().contains("Unauthorized"));
        }
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/rolesAuthApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);
        role1Client = GraphQlClientBuilder.newBuilder()
                                          .endpoint(baseUriStr)
                                          .build(ClientInterface1.class);
        role2Client = GraphQlClientBuilder.newBuilder()
                        .endpoint(baseUriStr)
                        .build(ClientInterface2.class);
    }

    @Test
    public void permitAll_unannotated() {
        assertAuthorized(() -> role1Client.permitAll_unannotated());
        assertAuthorized(() -> role2Client.permitAll_unannotated());
    }

    @Test
    public void permitAll_permitAll() {
        assertAuthorized(() -> role1Client.permitAll_permitAll());
        assertAuthorized(() -> role2Client.permitAll_permitAll());
    }

    @Test
    public void permitAll_denyAll() {
        assertUnauthorized(() -> role1Client.permitAll_denyAll());
        assertUnauthorized(() -> role2Client.permitAll_denyAll());
    }

    @Test
    public void permitAll_rolesAllowed1() {
        assertAuthorized(() -> role1Client.permitAll_rolesAllowed1());
        assertUnauthorized(() -> role2Client.permitAll_rolesAllowed1());
    }

    @Test
    public void permitAll_rolesAllowed2() {
        assertUnauthorized(() -> role1Client.permitAll_rolesAllowed2());
        assertAuthorized(() -> role2Client.permitAll_rolesAllowed2());
    }

    @Test
    public void denyAll_unannotated() {
        assertUnauthorized(() -> role1Client.denyAll_unannotated());
        assertUnauthorized(() -> role2Client.denyAll_unannotated());
    }

    @Test
    public void denyAll_permitAll() {
        assertAuthorized(() -> role1Client.denyAll_permitAll());
        assertAuthorized(() -> role2Client.denyAll_permitAll());
    }

    @Test
    public void denyAll_denyAll() {
        assertUnauthorized(() -> role1Client.denyAll_denyAll());
        assertUnauthorized(() -> role2Client.denyAll_denyAll());
    }

    @Test
    public void denyAll_rolesAllowed1() {
        assertAuthorized(() -> role1Client.denyAll_rolesAllowed1());
        assertUnauthorized(() -> role2Client.denyAll_rolesAllowed1());
    }

    @Test
    public void denyAll_rolesAllowed2() {
        assertUnauthorized(() -> role1Client.denyAll_rolesAllowed2());
        assertAuthorized(() -> role2Client.denyAll_rolesAllowed2());
    }

    @Test
    public void rolesAllowed1_unannotated() {
        assertAuthorized(() -> role1Client.rolesAllowed1_unannotated());
        assertUnauthorized(() -> role2Client.rolesAllowed1_unannotated());
    }

    @Test
    public void rolesAllowed1_permitAll() {
        assertAuthorized(() -> role1Client.rolesAllowed1_permitAll());
        assertAuthorized(() -> role2Client.rolesAllowed1_permitAll());
    }

    @Test
    public void rolesAllowed1_denyAll() {
        assertUnauthorized(() -> role1Client.rolesAllowed1_denyAll());
        assertUnauthorized(() -> role2Client.rolesAllowed1_denyAll());
    }

    @Test
    public void rolesAllowed1_rolesAllowed1() {
        assertAuthorized(() -> role1Client.rolesAllowed1_rolesAllowed1());
        assertUnauthorized(() -> role2Client.rolesAllowed1_rolesAllowed1());
    }

    @Test
    public void rolesAllowed1_rolesAllowed2() {
        assertUnauthorized(() -> role1Client.rolesAllowed1_rolesAllowed2());
        assertAuthorized(() -> role2Client.rolesAllowed1_rolesAllowed2());
    }

    @Test
    public void rolesAllowed2_unannotated() {
        assertUnauthorized(() -> role1Client.rolesAllowed2_unannotated());
        assertAuthorized(() -> role2Client.rolesAllowed2_unannotated());
    }

    @Test
    public void rolesAllowed2_permitAll() {
        assertAuthorized(() -> role1Client.rolesAllowed2_permitAll());
        assertAuthorized(() -> role2Client.rolesAllowed2_permitAll());
    }

    @Test
    public void rolesAllowed2_denyAll() {
        assertUnauthorized(() -> role1Client.rolesAllowed2_denyAll());
        assertUnauthorized(() -> role2Client.rolesAllowed2_denyAll());
    }

    @Test
    public void rolesAllowed2_rolesAllowed1() {
        assertAuthorized(() -> role1Client.rolesAllowed2_rolesAllowed1());
        assertUnauthorized(() -> role2Client.rolesAllowed2_rolesAllowed1());
    }

    @Test
    public void rolesAllowed2_rolesAllowed2() {
        assertUnauthorized(() -> role1Client.rolesAllowed2_rolesAllowed2());
        assertAuthorized(() -> role2Client.rolesAllowed2_rolesAllowed2());
    }

    @Test
    public void unannotated_unannotated() {
        assertAuthorized(() -> role1Client.unannotated_unannotated());
        assertAuthorized(() -> role2Client.unannotated_unannotated());
    }

    @Test
    public void unannotated_permitAll() {
        assertAuthorized(() -> role1Client.unannotated_permitAll());
        assertAuthorized(() -> role2Client.unannotated_permitAll());
    }

    @Test
    public void unannotated_denyAll() {
        assertUnauthorized(() -> role1Client.unannotated_denyAll());
        assertUnauthorized(() -> role2Client.unannotated_denyAll());
    }

    @Test
    public void unannotated_rolesAllowed1() {
        assertAuthorized(() -> role1Client.unannotated_rolesAllowed1());
        assertUnauthorized(() -> role2Client.unannotated_rolesAllowed1());
    }

    @Test
    public void unannotated_rolesAllowed2() {
        assertUnauthorized(() -> role1Client.unannotated_rolesAllowed2());
        assertAuthorized(() -> role2Client.unannotated_rolesAllowed2());
    }

    @Test
    public void denyAllAndPermitAll() {
        assertUnauthorized(() -> role1Client.denyAllAndPermitAll());
        assertUnauthorized(() -> role2Client.denyAllAndPermitAll());
    }

    @Test
    public void denyAllAndRolesAllowed() {
        assertUnauthorized(() -> role1Client.denyAllAndRolesAllowed1());
        assertUnauthorized(() -> role2Client.denyAllAndRolesAllowed1());
    }

    @Test
    public void rolesAllowed1AndPermitAll() {
        assertAuthorized(() -> role1Client.rolesAllowed1AndPermitAll());
        assertUnauthorized(() -> role2Client.rolesAllowed1AndPermitAll());
    }

    @Test
    public void allThree() {
        assertUnauthorized(() -> role1Client.allThree());
        assertUnauthorized(() -> role2Client.allThree());
    }
}