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
package mpGraphQL10.voidQuery;

import static org.junit.Assert.fail;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/VoidQueryTestServlet")
public class VoidQueryTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(VoidQueryTestServlet.class.getName());

    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/voidQueryApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);
        URI baseUri = URI.create(baseUriStr);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .register(new My404ResponseExceptionMapper())
                        .baseUri(baseUri);
    }

    @Test
    public void testSchemaIsNotGenerated(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        try {
            String schema = client.schema();
            fail("did not throw expected 404 exception - instead, got schema data: " + schema);
        } catch (Expected404Exception expected) {
            return; //PASS
        } catch (Throwable t) {
            t.printStackTrace();
            fail("caught an unexected exception " + t);
        }
    }

    @Test
    public void testSimpleQueryFails(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allWidgets");
        query.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        try {
            WidgetQueryResponse response = client.allWidgets(query);
            fail("did not throw expected 404 exception - instead got data from query: " +response);
        } catch (Expected404Exception expected) {
            return; //PASS
        } catch (Throwable t) {
            t.printStackTrace();
            fail("caught an unexected exception " + t);
        }
    }
}
