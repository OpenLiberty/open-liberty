/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configmod.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CloudantConfigModTestServlet")
public class CloudantConfigModServlet extends FATServlet {
    /**
     * Test case should invoke this method before and after running to ensure the database is deleted.
     */
    public void deleteDatabase(HttpServletRequest request, HttpServletResponse response) throws NamingException {
        String databaseName = request.getParameter("databaseName");
        ClientBuilder builder = InitialContext.doLookup("java:module/env/cloudant/builderRef");
        CloudantClient client = builder.build();
        try {
            client.deleteDB(databaseName);
            System.out.println("Deleted database " + databaseName);
        } catch (NoDocumentException nde) {
            System.out.println("Exception received while attempting to delete database, possibly didn't exist, continuing:" + nde);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    public void testContainsId(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");

        CloudantClient client = null;
        try {
            Database db;

            if (jndiName.contains("db"))
                db = InitialContext.doLookup(jndiName);
            else {
                ClientBuilder builder = InitialContext.doLookup(jndiName);
                client = builder.build();
                db = client.database(request.getParameter("databaseName"), true);
            }

            assertTrue(db.contains(request.getParameter("id")));
        } finally {
            if (client != null)
                client.shutdown();
        }
    }

    public void testDatabaseNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        Database db = InitialContext.doLookup(jndiName);
        try {
            DbInfo info = db.info();
            fail("First operation should fail with database name does not exist. Instead " + info);
        } catch (NoDocumentException x) {
            // expected
        }
    }

    public void testFindById(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");

        CloudantClient client = null;
        try {
            Database db;

            if (jndiName.contains("db"))
                db = InitialContext.doLookup(jndiName);
            else {
                ClientBuilder builder = InitialContext.doLookup(jndiName);
                client = builder.build();
                db = client.database(request.getParameter("databaseName"), true);
            }

            String id = request.getParameter("id");
            LinkedHashMap<?, ?> animal = db.find(LinkedHashMap.class, id);
            for (String attr : Arrays.asList("kingdom", "phylum", "order", "class", "family", "genus", "species"))
                assertEquals(animal.get(attr), request.getParameter(attr));
        } finally {
            if (client != null)
                client.shutdown();
        }
    }

    public void testInsert(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");

        CloudantClient client = null;
        try {
            Database db;

            if (jndiName.contains("db"))
                db = InitialContext.doLookup(jndiName);
            else {
                ClientBuilder builder = InitialContext.doLookup(jndiName);
                client = builder.build();
                db = client.database(request.getParameter("databaseName"), true);
            }

            Map<String, String> animal = new LinkedHashMap<String, String>();
            for (String attr : Arrays.asList("kingdom", "phylum", "order", "class", "family", "genus", "species"))
                animal.put(attr, request.getParameter(attr));
            animal.put("_id", animal.get("genus") + ' ' + animal.get("species"));

            db.save(animal);
        } finally {
            if (client != null)
                client.shutdown();
        }
    }

    public void testIncorrectPassword(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CloudantClient client = null;
        try {
            ClientBuilder builder = InitialContext.doLookup(request.getParameter("jndiName"));
            client = builder.build();
            Database db = client.database(request.getParameter("databaseName"), true);
            fail("Should not be able to access database " + db + " with incorrect password");
        } catch (CouchDbException x) {
            if (x.getMessage() == null || !(x.getStatusCode() == 401)) // Unauthorized
                throw x;
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    public void testInvalidUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ClientBuilder builder = InitialContext.doLookup(request.getParameter("jndiName"));
        CloudantClient client = builder.build();
        try {
            Database db = client.database(request.getParameter("databaseName"), true);
            fail("Should not be able to access database " + db);
        } catch (CouchDbException x) {
            if (x.getMessage() == null || !(x.getStatusCode() == 401)) // Unauthorized
                throw x;
        } finally {
            client.shutdown();
        }
    }

    // A special test for a case where a different Cloudant library (one that lacks the gson jar)
    // is used by the app vs the server. The classloader will different and so the application
    // will not be able to cast. We can get around this by invoking reflectively.
    public void testMissingLibraryJAR(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Object builder = InitialContext.doLookup(request.getParameter("jndiName"));
            Object client = builder.getClass().getMethod("build").invoke(builder);
            fail("Should not be able to build " + client + " after gson jar is removed from library.");
        } catch (Throwable x) {
            Throwable t = x.getCause();

            boolean found = false;
            while (!found && t != null) {
                if (t instanceof NoClassDefFoundError) {
                    found = true;
                }
                t = t.getCause();
            }

            if (!found) {
                fail("Didn't receive expected NoClassDefFoundError exception.  Rather, we received: " + x);
            }
        }
    }

    public void testNameNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            String jndiName = request.getParameter("jndiName");
            Object object = InitialContext.doLookup(jndiName);
            fail("Should not be able to look up as " + jndiName + ". Result: " + object);
        } catch (NamingException x) {
        } // pass
    }

    public void testNoAuthentication(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ClientBuilder builder = InitialContext.doLookup(request.getParameter("jndiName"));
        CloudantClient client = builder.build();
        try {
            Database db = client.database(request.getParameter("databaseName"), true);
            fail("Should not be able to access database " + db);
        } catch (CouchDbException x) {
            if (x.getMessage() == null || !(x.getStatusCode() == 401)) // Unauthorized
                throw x;
        } finally {
            client.shutdown();
        }
    }

    // A special test for a case where a different Cloudant library is used by the app vs the server.
    // The classloader will different and so the application will not be able to cast.
    // We can get around this by invoking reflectively.
    public void testReflectiveContainsId(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object db = InitialContext.doLookup(request.getParameter("jndiName"));

        assertTrue((Boolean) db.getClass().getMethod("contains", String.class).invoke(db, request.getParameter("id")));
    }
}
