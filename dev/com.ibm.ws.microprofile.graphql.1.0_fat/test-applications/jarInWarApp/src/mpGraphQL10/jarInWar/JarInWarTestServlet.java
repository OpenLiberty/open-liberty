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
package mpGraphQL10.jarInWar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
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

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;



@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/RolesAuthTestServlet")
public class JarInWarTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(JarInWarTestServlet.class.getName());

    RestClientBuilder builder;

    @Override
    public void init() throws ServletException {
        String contextPath = System.getProperty("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default", "8010") + "/jarInWarApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);

        URI baseUri = URI.create(baseUriStr);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUri(baseUri);
    }

    @Test
    public void testSchemaContainsWarAndJarComponents(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        String schema = client.schema();
        System.out.println("Schema: " + System.lineSeparator() + schema);
        assertNotNull("Could not find schema", schema);
        assertTrue("Missing root level GraphQLApi from WAR", schema.contains("allWars"));
        assertTrue("Missing referenced type from WAR", schema.contains("type EntityInWar"));
        assertTrue("Missing root level query in WAR referencing type in JAR", schema.contains("allJarsRefdFromWar"));
        assertTrue("Missing root level GraphQLApi from JAR", schema.contains("allJars"));
        assertTrue("Missing referenced type from JAR", schema.contains("type EntityInJar"));
        assertTrue("Missing referenced type from JAR", schema.contains("type EntityInJarRefdFromWar"));
        assertTrue("Missing enum from JAR", schema.contains("enum LidTightness"));
    }
}