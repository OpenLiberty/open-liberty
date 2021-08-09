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
package cloudant.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.IOException;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CloudantTestServlet")
public class CloudantTestServlet extends FATServlet {
    @Resource(lookup = "cloudant/dbDoesNotExist")
    private Database dbDoesNotExist;

    @Resource(lookup = "cloudant/defCtrAuth")
    private ClientBuilder cloudant_defCtrAuth;

    @Resource(lookup = "cloudant/defCtrAuth", authenticationType = AuthenticationType.APPLICATION)
    private ClientBuilder cloudant_AppAuthNone;

    @Resource(name = "java:app/env/cloudant/resRefCtrAuth")
    // see web.xml and bindings for attributes
    private ClientBuilder cloudant_resRefCtrAuth;

    @Resource(lookup = "cloudant/loadFromApp")
    private ClientBuilder cloudant_loadFromApp;

    @Resource(lookup = "cloudant/withSSL")
    private ClientBuilder cloudant_withSSL;

    @Resource(lookup = "cloudant/nestedSSL")
    private ClientBuilder cloudant_nestedSSL;

    @Resource(lookup = "cloudant/invalidSSL")
    private ClientBuilder cloudant_invalidSSL;

    @Resource(lookup = "cloudant/noSSLRef")
    private ClientBuilder cloudant_noSSLRef;

    @Resource(lookup = "cloudant/sslAuthDisabled")
    private ClientBuilder cloudant_sslAuthDisabled;

    private String databaseName;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        databaseName = request.getParameter("databaseName");
        super.doGet(request, response);
    }

    // When using application authentication without a user/password, the connection should fail.
    public void testAuthenticationTypeApplicationUnauthenticated() throws Exception {
        CloudantClient client = cloudant_AppAuthNone.build();
        try {
            Database db = client.database(databaseName, true);
            fail("Should not be able to access database " + db + " with application authentication and no user/password.");
        } catch (CouchDbException x) {
            if (x.getMessage() == null || !(x.getStatusCode() == 401)) // Unauthorized
                throw x;
        } finally {
            client.shutdown();
        }
    }

    // Use Cloudant to insert 2 documents into CouchDB. Find both of them and verify their contents.
    public void testBasicSaveAndFind() throws Exception {
        System.out.println("testBasicSaveAndFind() BEGIN");
        Database testdb = InitialContext.doLookup("java:comp/env/cloudant/defCtrAuthDB");
        System.out.println("testBasicSaveAndFind() LOOKUP DONE");
        doInsertFind(testdb, 1, "one");
        doInsertFind(testdb, 2, "two");
        System.out.println("testBasicSaveAndFind() END");
    }

    // Verify that create=false is honored on cloudantDatabase by ensuring that the first operation to the
    // the database fails.
    public void testCreateFalse() throws Exception {
        try {
            dbDoesNotExist.info();
            fail("First operation must raise NoDocumentException when database does not exist");
        } catch (NoDocumentException x) {
        } // expected, database does not exist
    }

    // Attempt lookup of Cloudant ClientBuilder without a resource reference. Confirm that it is rejected.
    public void testDirectLookupOfClientBuilder() throws Exception {
        try {
            ClientBuilder builder = InitialContext.doLookup("cloudant/noAuth");
            fail("Should not be able to look up ClientBuilder without a resource reference. " + builder);
        } catch (NamingException x) {
        }
    }

    // Use Cloudant resources where libraries are loaded from the application
    public void testLoadFromApp() throws Exception {
        CloudantClient client = cloudant_loadFromApp.build();
        try {
            client.database(databaseName, true).info();
        } finally {
            client.shutdown();
        }

        Database dbLoadFromApp = InitialContext.doLookup("cloudant/dbLoadFromApp");
        dbLoadFromApp.info();

        // Temporarily remove the thread context class loader with access to Cloudant classes
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(null);
        try {
            Database db = InitialContext.doLookup("cloudant/dbLoadFromApp");
            fail("Should not be able to load Cloudant Database from thread without application class loader. " + db);
        } catch (NamingException x) {
            // expected
        } finally {
            Thread.currentThread().setContextClassLoader(tccl); // restore previous thread context class laoder
        }
    }

    // Use a Cloudant resource environment reference that specifies a container managed authentication alias.
    public void testResourceRefContainerAuthAlias() throws Exception {
        CloudantClient client = cloudant_resRefCtrAuth.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 3, "three");
        } finally {
            client.shutdown();
        }
    }

    // Use a Cloudant resource environment reference that specifies a container managed authentication alias
    // to access a cloudantDatabase.
    public void testResourceRefContainerAuthAliasDB() throws Exception {
        Database db = InitialContext.doLookup("java:module/env/cloudant/resRefCtrAuthDB");
        doInsertFind(db, 8, "eight");
    }

    // Use a Cloudant resource environment reference that specifies a container managed authentication alias that
    // does not exist in the server configuration. This also tests override of default container auth data.
    public void testResourceRefContainerAuthAliasInvalid() throws Exception {
        try {
            ClientBuilder builder = InitialContext.doLookup("java:module/env/cloudant/resRefCtrAuthInvalid");
            fail("Should not be able look up ClientBuilder with resource ref that points to missing authData. " + builder);
        } catch (NamingException x) {
            Throwable cause = x;
            while (cause != null && !(cause instanceof LoginException))
                cause = cause.getCause();
            if (cause == null || !cause.getMessage().matches("CWWKS1300E.*missingAuthData.*"))
                throw x;
        }
    }

    // Use a Cloudant resource environment reference that specifies a container managed authentication alias with
    // a user that isn't valid for the database. This also tests override of default container auth data.
    public void testResourceRefContainerAuthAliasInvalidUser() throws Exception {
        try {
            Database db = InitialContext.doLookup("java:comp/env/cloudant/resRefCtrAuthInvalidUserDB");
            fail("When user isn't valid, should not be able to connect to database " + db);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause())
                if (cause instanceof CouchDbException)
                    return;
            throw x;
        }
    }

    public void testResourceRefContainerAuthAliasNotAppliedtoApplicationAuth() throws Exception {
        try {
            Database db = InitialContext.doLookup("java:global/env/cloudant/resRefAppAuthWithCtrAuthAliasDB");
            fail("Container authentication alias should not be used for application authentication to " + db);
        } catch (NamingException x) {
            for (Throwable cause = x; cause != null; cause = cause.getCause())
                if (cause instanceof CouchDbException && cause.getMessage().contains("401")) // Unauthorized
                    return;
            throw x;
        }
    }

    /**
     * Most basic usage pattern of Cloudant with SSL.
     * Use a <cloudant> that has sslRef="defaultSSLConfig" and sslEnabled="true".
     */
    public void testSSLBasic() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_withSSL);

        CloudantClient client = cloudant_withSSL.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 4, "four");
        } finally {
            client.shutdown();
        }
    }

    public void testSSLRefWithFilter() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_withSSL);

        CloudantClient client = cloudant_withSSL.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 12, "twelve");
        } finally {
            client.shutdown();
        }
    }

    /**
     * Use a <cloudant> that has no sslRef specified, but instead has a nested <ssl> element.
     */
    public void testSSLNestedConfig() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_nestedSSL);

        CloudantClient client = cloudant_nestedSSL.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 5, "five");
        } finally {
            client.shutdown();
        }
    }

    /**
     * Use a <cloudant> that has no sslRef specified. Since there is a <ssl id="defaultSSLConfig">
     * configured, the <cloudant> will pick up the JVM default and use it for SSL.
     */
    public void testNoSSLRef() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_noSSLRef);

        CloudantClient client = cloudant_noSSLRef.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 6, "six");
        } finally {
            client.shutdown();
        }
    }

    public void testSSLOutboundDefault() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_noSSLRef);

        CloudantClient client = cloudant_noSSLRef.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 10, "ten");
        } finally {
            client.shutdown();
        }
    }

    public void testSSLOutboundFilter() {
        System.out.println("Using cloudant client builder: " + cloudant_noSSLRef);

        CloudantClient client = cloudant_noSSLRef.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 11, "eleven");
        } finally {
            client.shutdown();
        }
    }

    /**
     * Use a <cloudant> that points to an <ssl> with an invalid keystore.
     * Expect that we get a SSLHandshakeException when we try to create a database.
     */
    public void testInvalidSSL() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_invalidSSL);

        CloudantClient client = cloudant_invalidSSL.build();
        try {
            client.database(databaseName, true);
            fail("Expected to get a javax.net.ssl.SSLHandshakeException");
        } catch (CouchDbException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof javax.net.ssl.SSLHandshakeException)
                System.out.println("Got expected SSLHandshakeException");
            else
                throw ex;
        } finally {
            try {
                client.shutdown();
            } catch (CouchDbException x) {
            }
        }
    }

    /**
     * Use a <cloudant> with disableSSLAuthentication="true".
     */
    public void testSSLAuthDisabled() throws Exception {
        System.out.println("Using cloudant client builder: " + cloudant_sslAuthDisabled);

        CloudantClient client = cloudant_sslAuthDisabled.build();
        try {
            Database db = client.database(databaseName, true);
            doInsertFind(db, 7, "seven");
        } finally {
            client.shutdown();
        }
    }

    private void doInsertFind(Database db, int i, String str) {
        System.out.println("doInsertFind() BEGIN INSERTING " + str);
        MyDocument doc = new MyDocument();
        doc.setId("MyDocument-" + i);
        doc.setBValue(true);
        doc.setIValue(i);
        doc.setSValue(str);
        System.out.println("doInsertFind() ABOUT TO SAVE DOC");
        db.save(doc);
        System.out.println("doInsertFind() DOC SAVED");

        System.out.println("doInsertFind() PRE DB.FIND");
        doc = db.find(MyDocument.class, "MyDocument-" + i);
        System.out.println("doInsertFind() POST DB.FIND");
        assertTrue(doc.getBValue());
        assertEquals(doc.getIValue(), i);
        assertEquals(doc.getSValue(), str);
    }
}
