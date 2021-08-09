/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.apacheds.EmbeddedApacheDS;

/** Unit tests to validate the {@link TimedDirContext} class. */
@SuppressWarnings("restriction")
public class TimedDirContextTest {

    private static EmbeddedApacheDS ldapServer;
    private static final String BASE_ENTRY = "o=ibm,c=us";
    private static final String OU_BASE_ENTRY = "ou=branch," + BASE_ENTRY;
    private static final String USER_1_DN = "uid=user1," + BASE_ENTRY;
    private static final String USER_2_DN = "uid=user2," + BASE_ENTRY;
    private static final String NON_EXISTENT_DN = "uid=newuser,o=invalid,c=invalid";

    @BeforeClass
    public static void setUp() throws Exception {
        ldapServer = new EmbeddedApacheDS("primaryLdap");
        ldapServer.addPartition("testing", BASE_ENTRY);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(BASE_ENTRY);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        /*
         * Add the partition entries.
         */
        entry = ldapServer.newEntry(OU_BASE_ENTRY);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "branch");
        ldapServer.add(entry);

        /*
         * Create the users.
         */
        entry = ldapServer.newEntry(USER_1_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "user1");
        entry.add("sn", "user1");
        entry.add("cn", "user1");
        entry.add("description", "description text");
        entry.add("userPassword", "password");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(USER_2_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "user2");
        entry.add("sn", "user2");
        entry.add("cn", "user2");
        entry.add("userPassword", "password");
        ldapServer.add(entry);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (ldapServer != null) {
            ldapServer.stopService();
        }
    }

    private Hashtable<Object, Object> getEnvironment() {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.PROVIDER_URL, "ldap://localhost:" + ldapServer.getLdapServer().getPort());
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_PRINCIPAL, EmbeddedApacheDS.getBindDN());
        env.put(Context.SECURITY_CREDENTIALS, EmbeddedApacheDS.getBindPassword().toCharArray());
        return env;
    }

    @Test
    public void constructor_NamingException() throws Exception {
        try {
            Hashtable<Object, Object> env = getEnvironment();
            env.put(Context.PROVIDER_URL, "ldap://badhost:389");
            new TimedDirContext(env, null, 0);
            fail("Expected NamingException.");
        } catch (NamingException e) {
            // Passed
        }
    }

    @Test
    public void createSubContext() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            /*
             * Create a new branch.
             */
            String subcontextDN = "ou=subcontext," + BASE_ENTRY;
            Attributes attrs = new BasicAttributes();
            attrs.put(new BasicAttribute("objectclass", "organizationalunit"));
            attrs.put(new BasicAttribute("ou", "subcontext"));
            ctx.createSubcontext(new LdapName(subcontextDN), attrs);

            /*
             * Verify the entry exists.
             */
            assertNotNull(ldapServer.lookup(subcontextDN));

            /*
             * Now delete it.
             */
            ctx.destroySubcontext(new LdapName(subcontextDN));

            /*
             * Verify the entry no longer exists.
             */
            try {
                ldapServer.lookup(subcontextDN);
                fail("Expected LdapNoSuchObjectException.");
            } catch (LdapNoSuchObjectException e) {
                // passed
            }

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void createSubContext_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                /*
                 * Try to create on an invalid DN.
                 */
                Attributes attrs = new BasicAttributes();
                attrs.put(new BasicAttribute("objectclass", "organizationalunit"));
                attrs.put(new BasicAttribute("ou", "subcontext"));
                ctx.createSubcontext(new LdapName(NON_EXISTENT_DN), attrs);

                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed
            }

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void destroySubContext_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                /*
                 * Try to destroy on an invalid DN.
                 */
                ctx.destroySubcontext(new LdapName(NON_EXISTENT_DN));

                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed
            }

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void getAttributes() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            Attributes attrs = ctx.getAttributes(new LdapName(USER_1_DN), new String[] { "uid", "description" });
            assertEquals("user1", attrs.get("uid").get());
            assertEquals("description text", attrs.get("description").get());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void getAttributes_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                ctx.getAttributes(new LdapName(NON_EXISTENT_DN), new String[] { "uid", "description" });
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void getNameParser() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            NameParser parser = ctx.getNameParser("");
            assertEquals(USER_1_DN, parser.parse(USER_1_DN).toString());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void modifyAttributes_1() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("displayName", "new display name"));

            /*
             * Modify the attributes.
             */
            ctx.modifyAttributes(new LdapName(USER_1_DN), mods);

            /*
             * Verify the entry exists.
             */
            Entry user = ldapServer.lookup(USER_1_DN);
            assertEquals("new display name", user.get("displayName").get().toString());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void modifyAttributes_1_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("displayName", "new display name"));

                /*
                 * Modify the attributes.
                 */
                ctx.modifyAttributes(new LdapName(NON_EXISTENT_DN), mods);
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void modifyAttributes_2() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            Attributes attrs = new BasicAttributes();
            attrs.put(new BasicAttribute("employeeType", "new employee type"));

            /*
             * Modify the attributes.
             */
            ctx.modifyAttributes(new LdapName(USER_1_DN), DirContext.ADD_ATTRIBUTE, attrs);

            /*
             * Verify the entry exists.
             */
            Entry user = ldapServer.lookup(USER_1_DN);
            assertEquals("new employee type", user.get("employeeType").get().toString());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void modifyAttributes_2_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                Attributes attrs = new BasicAttributes();
                attrs.put(new BasicAttribute("employeeType", "new employee type"));

                /*
                 * Modify the attributes.
                 */
                ctx.modifyAttributes(new LdapName(NON_EXISTENT_DN), DirContext.ADD_ATTRIBUTE, attrs);
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void rename() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            /*
             * Rename user2.
             */
            String newDN = "uid=user2," + OU_BASE_ENTRY;
            ctx.rename(USER_2_DN, newDN);

            /*
             * Verify the new name exists.
             */
            assertNotNull(ldapServer.lookup(newDN));

            /*
             * Verify the old name no longer exists.
             */
            try {
                ldapServer.lookup(USER_2_DN);
                fail("Expected LdapNoSuchObjectException.");
            } catch (LdapNoSuchObjectException e) {
                // passed
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_1() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            NamingEnumeration<SearchResult> results = ctx.search(new LdapName(BASE_ENTRY), "uid=user1", new SearchControls());

            assertTrue(results.hasMoreElements());
            assertEquals(USER_1_DN, results.nextElement().getNameInNamespace());
            assertFalse(results.hasMoreElements());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_1_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                ctx.search(new LdapName(NON_EXISTENT_DN), "uid=user1", new SearchControls());
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_2() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            NamingEnumeration<SearchResult> results = ctx.search(BASE_ENTRY, "uid=user1", null);

            assertTrue(results.hasMoreElements());
            assertEquals(USER_1_DN, results.nextElement().getNameInNamespace());
            assertFalse(results.hasMoreElements());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_2_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                ctx.search(NON_EXISTENT_DN, "uid=user1", null);
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_3() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            NamingEnumeration<SearchResult> results = ctx.search(new LdapName(BASE_ENTRY), "uid={0}", new String[] { "user1" }, new SearchControls());

            assertTrue(results.hasMoreElements());
            assertEquals(USER_1_DN, results.nextElement().getNameInNamespace());
            assertFalse(results.hasMoreElements());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void search_3_NamingException() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            try {
                ctx.search(new LdapName(NON_EXISTENT_DN), "uid={0}", new String[] { "user1" }, new SearchControls());
                fail("Expected NamingException.");
            } catch (NamingException e) {
                // Passed.
            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void setCreateTimestamp() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            ctx.setCreateTimestamp(12345l);
            assertEquals(12345l, ctx.getCreateTimestamp());

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void setPoolTimestamp() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            ctx.setPoolTimeStamp(12345l);
            assertEquals(12345l, ctx.getPoolTimestamp());
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void setRequestControls() throws Exception {
        TimedDirContext ctx = null;
        try {
            ctx = new TimedDirContext(getEnvironment(), null, 0);

            /*
             * Page results 1 entry at a time.
             */
            ctx.setRequestControls(new Control[] { new PagedResultsControl(1, false) });

            /*
             * Get page 1 which should only return 1 result.
             */
            NamingEnumeration<SearchResult> results = ctx.search(new LdapName(BASE_ENTRY), "uid=*", new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 60000, null, false, false));
            int count = 0;
            while (results.hasMoreElements()) {
                results.nextElement();
                count++;
            }
            assertEquals("Paging is set to 1, only 1 result should have been returned.", 1, count);

            /*
             * Check for the cookie indicating our request controls worked.
             */
            assertNotNull("Expected paging cookie in response control.", ((PagedResultsResponseControl) ctx.getResponseControls()[0]).getCookie());
            ctx.setRequestControls(new Control[] { new PagedResultsControl(1, false) });

        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
}
