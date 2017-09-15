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
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.OperationNotSupportedException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.container.service.naming.NamingConstants;

public class JavaURLContextTest extends MockServicesHelper {
    // test jndi name string and name
    private static String testString = "java:comp/env/test";
    private static Name testName = null;

    // comp env 
    private static String compEnvString = "java:comp/env";
    private static Name compEnv = null;

    // java:comp space
    private static String compString = "java:comp";
    private static Name compName = null;

    // test suffixes
    private static String testStringSuffix = "suffix";
    private static Name testNameSuffix = null;
    // the complete names
    private static String testStringSuffixed = testString + "/" + testStringSuffix;
    private static Name testNameSuffixed = null;
    // test env entry
    private static String envEntryKey = "test1";
    private static Object envEntryValue = new Object();

    private static final Hashtable<Object, Object> envmt = new Hashtable<Object, Object>(0);
    private JavaURLContext javaUrlContext;

    /**
     * This method initializes the javax.naming.Name objects for the test based
     * on the String names. It also puts something into the env Hashtable
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setupTestName() throws Exception {
        // names
        testName = new CompositeName(testString);
        testNameSuffix = new CompositeName(testStringSuffix);
        testNameSuffixed = new CompositeName(testStringSuffixed);
        compEnv = new CompositeName(compEnvString);
        compName = new CompositeName(compString);

        // hashtable env
        envmt.clear();
        envmt.put(envEntryKey, envEntryValue);

    }

    @Override
    @Before
    public void setupTest() throws Exception {
        super.setupTest();
        javaUrlContext = (JavaURLContext) factory.getObjectInstance(null, null, null, envmt);
    }

    @Test
    //
    public void testJavaURLContext() {
        assertNotNull(javaUrlContext);
    }

    @Test
    //
    public void testAddToEnvironment() throws Exception {
        Object addedObject = new Object();
        javaUrlContext.addToEnvironment("addedEntry", addedObject);
        Object envObject = javaUrlContext.getEnvironment().get("addedEntry");
        assertSame("The retrieved environment object was not the same as the one added", addedObject, envObject);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testBindNameObject() throws Exception {
        javaUrlContext.bind(testName, null);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testBindStringObject() throws Exception {
        javaUrlContext.bind(testString, null);
    }

    @Test
    //
    public void testComposeNameNameName() throws Exception {
        Name composed = javaUrlContext.composeName(testNameSuffix, testName);
        assertEquals("The composed name was not correct", testNameSuffixed, composed);
    }

    @Test
    //
    public void testComposeNameStringString() throws Exception {
        String composed = javaUrlContext.composeName(testStringSuffix, testString);
        assertEquals("The composed name was not correct", testStringSuffixed, composed);
    }

    @Test
    //
    public void testClose() {
        // Nothing to test for this method at present
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testCreateSubcontextName() throws Exception {
        javaUrlContext.createSubcontext(testName);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testCreateSubcontextString() throws Exception {
        javaUrlContext.createSubcontext(testString);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testDestroySubcontextName() throws Exception {
        javaUrlContext.destroySubcontext(testName);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testDestroySubcontextString() throws Exception {
        javaUrlContext.destroySubcontext(testString);
    }

    @Test
    //
    public void testGetEnvironment() throws Exception {
        // check that the envmt we get back is not the original one we added
        // this is part of the contract, it should be copied not reused
        assertNotSame(
                      "The returned environment was the same as the one used during construction, it should be copied not reused",
                      envmt, javaUrlContext.getEnvironment());
        // check that they have equivalent entries
        assertEquals("The returned environment had different entries to those expected", envmt, javaUrlContext
                        .getEnvironment());
    }

    @Test
    //
    public void testGetNameInNamespace() throws Exception {
        assertEquals("The result of getNameInNamespace should have been \"" + NamingConstants.JAVA_NS + "\"",
                     NamingConstants.JAVA_NS, javaUrlContext.getNameInNamespace());
    }

    @Test
    //
    public void testGetNameInNamespaceFromLookedUpContext() throws Exception {
        final String expectedNameInNamespace = "java:comp/env";
        Context ctx = (Context) javaUrlContext.lookup(expectedNameInNamespace);
        assertEquals("The result of ctx.getNameInNamespace() should have been \"" + expectedNameInNamespace + "\"",
                     expectedNameInNamespace, ctx.getNameInNamespace());
    }

    @Test
    //
    public void testGetNameParserName() throws Exception {
        NameParser parser = javaUrlContext.getNameParser(testName);
        assertNotNull("The returned NameParser was null", parser);
        assertTrue("The parser was not an instance of" + JavaURLNameParser.class.getName(),
                   parser instanceof JavaURLNameParser);
    }

    @Test
    //
    public void testGetNameParserString() throws Exception {
        NameParser parser = javaUrlContext.getNameParser(testString);
        assertNotNull("The returned NameParser was null", parser);
        assertTrue("The parser was not an instance of" + JavaURLNameParser.class.getName(),
                   parser instanceof JavaURLNameParser);
    }

    @Test
    public void testListName() throws Exception {
        NamingEnumeration<NameClassPair> pairs = javaUrlContext.list(compEnv);

        int size = 0;
        while (pairs.hasMore()) {
            NameClassPair pair = pairs.next();
            assertEquals("test", pair.getName());
            assertEquals(Object.class.getName(), pair.getClassName());
            size++;
        }
        assertEquals(size, 1);
    }

    @Test
    public void testListNameSubContext() throws Exception {
        NamingEnumeration<NameClassPair> pairs = javaUrlContext.list(compName);

        int size = 0;
        while (pairs.hasMore()) {
            NameClassPair pair = pairs.next();
            assertEquals(Context.class.getName(), pair.getClassName());
            boolean okay = ("env".equals(pair.getName()) || "websphere".equals(pair.getName()));
            assertTrue("Entry should be named env or websphere", okay);
            size++;
        }
        assertEquals(size, 2);
    }

    @Test
    public void testListString() throws Exception {
        NamingEnumeration<NameClassPair> pairs = javaUrlContext.list(compEnvString);

        int size = 0;
        while (pairs.hasMore()) {
            NameClassPair pair = pairs.next();
            assertEquals("test", pair.getName());
            assertEquals(Object.class.getName(), pair.getClassName());
            size++;
        }
        assertEquals(1, size);
    }

    @Test
    public void testListStringSubContext() throws Exception {
        NamingEnumeration<NameClassPair> pairs = javaUrlContext.list(compString);

        int size = 0;
        while (pairs.hasMore()) {
            NameClassPair pair = pairs.next();
            assertEquals(Context.class.getName(), pair.getClassName());
            boolean okay = ("env".equals(pair.getName()) || "websphere".equals(pair.getName()));
            assertTrue("Entry should be named env or websphere", okay);
            size++;
        }
        assertEquals(size, 2);

    }

    @Test
    public void testListBindingsName() throws Exception {
        NamingEnumeration<Binding> bindings = javaUrlContext.listBindings(compEnv);

        int size = 0;
        while (bindings.hasMore()) {
            Binding binding = bindings.next();
            assertEquals("test", binding.getName());
            assertEquals(Object.class.getName(), binding.getClassName());
            assertEquals(testObject, binding.getObject());
            size++;
        }
        assertEquals(1, size);
    }

    @Test
    public void testListBindingsNameSubcontext() throws Exception {
        NamingEnumeration<Binding> bindings = javaUrlContext.listBindings(compName);

        int size = 0;
        while (bindings.hasMore()) {
            Binding binding = bindings.next();
            boolean okay = ("env".equals(binding.getName()) || "websphere".equals(binding.getName()));
            assertTrue("Entry should be named env or websphere", okay);
            assertEquals(Context.class.getName(), binding.getClassName());
            assertEquals(JavaURLContext.class, binding.getObject().getClass());
            size++;
        }
        assertEquals(2, size);
    }

    @Test
    public void testListBindingsString() throws Exception {
        NamingEnumeration<Binding> bindings = javaUrlContext.listBindings(compEnvString);

        int size = 0;
        while (bindings.hasMore()) {
            Binding binding = bindings.next();
            assertEquals("test", binding.getName());
            assertEquals(Object.class.getName(), binding.getClassName());
            assertEquals(testObject, binding.getObject());
            size++;
        }
        assertEquals(1, size);
    }

    @Test
    public void testLookupName() throws Exception {
        Object lookedUp = javaUrlContext.lookup(testName);
        assertSame("The object looked up was not the same as the one registered", testObject, lookedUp);
    }

    @Test(expected = NameNotFoundException.class)
    public void testLookupNameNotFound() throws Exception {
        //try to lookup an object that isn't bound and check for the NameNotFoundException
        javaUrlContext.lookup(testNameSuffixed);
    }

    @Test
    public void testLookupNameContext() throws Exception {
        //lookup java:comp/env and check that we get a context back
        Object ctx = javaUrlContext.lookup(new CompositeName(NamingConstants.JavaColonNamespace.COMP_ENV.toString()));
        assertTrue("A Context was not returned for the 'java:comp/env' lookup", ctx instanceof Context);
        //now try to lookup the name test on that object and verify that we get the same object as if we'd looked up "java:comp/env/test"
        Object fullLookup = javaUrlContext.lookup(testName);
        Object ctxLookup = ((Context) ctx).lookup(new CompositeName("test"));
        assertSame("The lookup of 'test' on the java:comp/env context did not return the same object as looking up 'java:comp/env/test'", fullLookup, ctxLookup);
    }

    @Test
    public void testLookupString() throws Exception {
        Object lookedUp = javaUrlContext.lookup(testString);
        assertSame("The object looked up was not the same as the one registered", testObject, lookedUp);
    }

    @Test(expected = NameNotFoundException.class)
    public void testLookupStringNotFound() throws Exception {
        //try to lookup an object that isn't bound and check for the NameNotFoundException
        javaUrlContext.lookup(testStringSuffixed);
    }

    @Test
    public void testLookupStringContext() throws Exception {
        //lookup java:comp/env and check that we get a context back
        Object ctx = javaUrlContext.lookup(NamingConstants.JavaColonNamespace.COMP_ENV.toString());
        assertTrue("A Context was not returned for the 'java:comp/env' lookup", ctx instanceof Context);
        //now try to lookup the name test on that object and verify that we get the same object as if we'd looked up "java:comp/env/test"
        Object fullLookup = javaUrlContext.lookup(testString);
        Object ctxLookup = ((Context) ctx).lookup("test");
        assertSame("The lookup of 'test' on the java:comp/env context did not return the same object as looking up 'java:comp/env/test'", fullLookup, ctxLookup);
    }

    @Test(expected = InvalidNameException.class)
    public void testLookupNameNull() throws Exception {
        //should get an InvalidNameException if looking up "null"
        javaUrlContext.lookup((Name) null);
    }

    @Test(expected = InvalidNameException.class)
    public void testLookupStringNull() throws Exception {
        //should get an InvalidNameException if looking up "null"
        javaUrlContext.lookup((String) null);
    }

    @Test
    public void testLookupLinkName() throws Exception {
        Object lookedUpViaLink = javaUrlContext.lookupLink(testName);
        Object lookedUpViaLookup = javaUrlContext.lookup(testName);
        // we expect the same thing to be returned from
        // lookup and lookuplink, so verify that was the case
        assertSame(
                   "The lookupLink method did not return the same object as the lookup method: expected object from lookup(...); actual object from lookupLink()",
                   lookedUpViaLookup, lookedUpViaLink);
    }

    @Test
    public void testLookupLinkString() throws Exception {
        Object lookedUpViaLink = javaUrlContext.lookupLink(testString);
        Object lookedUpViaLookup = javaUrlContext.lookup(testString);
        // we expect the same thing to be returned from
        // lookup and lookuplink, so verify that was the case
        assertSame(
                   "The lookupLink method did not return the same object as the lookup method: expected object from lookup(...); actual object from lookupLink()",
                   lookedUpViaLookup, lookedUpViaLink);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testRebindNameObject() throws Exception {
        javaUrlContext.rebind(testName, null);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testRebindStringObject() throws Exception {
        javaUrlContext.rebind(testString, null);
    }

    @Test
    //
    public void testRemoveFromEnvironment() throws Exception {
        Object removed = javaUrlContext.removeFromEnvironment(envEntryKey);
        assertEquals("The removed object was not the one we were expecting to remove", envEntryValue, removed);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testRenameNameName() throws Exception {
        javaUrlContext.rename(testName, testNameSuffixed);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testRenameStringString() throws Exception {
        javaUrlContext.rename(testString, testStringSuffixed);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testUnbindName() throws Exception {
        javaUrlContext.unbind(testName);
    }

    @Test(expected = OperationNotSupportedException.class)
    public void testUnbindString() throws Exception {
        javaUrlContext.unbind(testString);
    }

}
