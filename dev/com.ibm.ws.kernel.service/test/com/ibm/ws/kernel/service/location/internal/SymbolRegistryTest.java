/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Matcher;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsResource.Type;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class SymbolRegistryTest {
    /**
     * Test data directory: note the space! always test paths with spaces. Dratted
     * windows.
     */
    public static final String TEST_DATA_DIR = "../com.ibm.ws.kernel.service.location/test/test data";

    static SharedOutputManager outputMgr;
    static InternalWsResource fileRes;
    static InternalWsResource dirRes;

    static SymbolicRootResource symRoot;
    static SymbolRegistry registry = SymbolRegistry.getRegistry();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownClass() {
        registry.clear();
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        registry.clear();
        outputMgr.resetStreams();
    }

    public void addSymbols() {
        // symbolic root adds itself to the registry
        symRoot = new SymbolicRootResource(TEST_DATA_DIR, "root", null);

        fileRes = symRoot.createDescendantResource("file");
        registry.addResourceSymbol("file", fileRes);

        dirRes = symRoot.createDescendantResource("dir/");
        registry.addResourceSymbol("dir", dirRes);

        registry.addStringSymbol("sub", "${root}");
        registry.addStringSymbol("sub1", "${file}");
        registry.addStringSymbol("sub2", "plainString");
        registry.addStringSymbol("sub3", "${sub2}");
        registry.addStringSymbol("sub4", "${sub}/${sub3}");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#SYMBOL_DEF} .
     */
    @Test
    public void testSymbolPattern() {
        String path;
        Matcher m;
        boolean result;

        try {
            path = "A";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertFalse("Regular expression should find symbolic", result);

            path = "${A}B";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${A} in " + path, "${A}", m.group());

            path = "${A}B${C}";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${A} in " + path, "${A}", m.group());

            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${C} in " + path, "${C}", m.group());

            path = "${A${B}}C";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${B} in " + path, "${B}", m.group());

            path = "A${B${C}D}E";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${C} in " + path, "${C}", m.group());

            // ------------ {}

            path = "${A}B";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${A} in " + path, "${A}", m.group());

            path = "${A}B${C}";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${A} in " + path, "${A}", m.group());

            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${C} in " + path, "${C}", m.group());

            path = "${A${B}}C";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${B} in " + path, "${B}", m.group());

            path = "A${B${C}D}E";
            m = SymbolRegistry.SYMBOL_DEF.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Regular expression should find symbolic", result);
            assertEquals("Should find ${C} in " + path, "${C}", m.group());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testResolve", t);
        }
    }

    /**
     * This method checks if you do ${env.<environment variable>} it resolves the string
     */
    @Test
    public void testResolveEnvStrings() {
        Map<String, String> env = System.getenv();
        Map.Entry<String, String> entry = env.entrySet().iterator().next();

        SymbolRegistry sr = SymbolRegistry.getRegistry();

        String value = sr.resolveSymbolicString("${env." + entry.getKey() + "}");
        String expectedValue = PathUtils.normalize(entry.getValue());

        assertEquals("Expected to resolve relative to the env", expectedValue, value);

    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testResolveSymbolicResourceNull() {
        registry.resolveSymbolicResource(null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test
    public void testResolveSymbolicResourceNullValue() {
        final String m = "testResolveSymbolicResourceNullValue";
        try {
            registry.addResourceSymbol("nullValue", null);
            assertNull(registry.resolveSymbolicResource("${nullValue}"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test
    public void testResolveSymbolicRootRelative() {
        final String m = "testResolveSymbolicRootRelative";
        try {
            WsResource r;

            addSymbols();
            r = registry.resolveSymbolicResource("${root}");
            assertNotNull("WsResource should be resolved for ${root}", r);
            assertEquals("WsResource should match symbolic root", symRoot, r);

            r = registry.resolveSymbolicResource("${root}/");
            assertNotNull("WsResource should be resolved for ${root}/", r);
            assertEquals("WsResource should match symbolic root", symRoot, r);
            assertEquals("Repository path should have trailing slash (as a directory)", "${root}/", r.toRepositoryPath());

            WsResource res = symRoot.createDescendantResource("a/b");

            r = registry.resolveSymbolicResource("${root}/a/b");
            assertNotNull("WsResource should be resolved for ${root}/a/b", r);
            assertEquals("WsResource should match symbolic root", res, r);
            assertEquals("Repository path should match ${root}/a/b", "${root}/a/b", r.toRepositoryPath());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test
    public void testResolveResource() {
        WsResource r;
        addSymbols();

        r = registry.resolveSymbolicResource("${file}");
        assertNotNull("WsResource should be resolved for ${file}", r);
        assertEquals("WsResource should match symbolic root", fileRes, r);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     * 
     * @throws URISyntaxException
     */
    @Test(expected = MalformedLocationException.class)
    public void testResolveResourceRelativeFile() throws URISyntaxException {
        registry.resolveSymbolicResource("${file}/");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     * 
     * @throws URISyntaxException
     */
    @Test(expected = MalformedLocationException.class)
    public void testResolveResourceRelativeUnknown() throws URISyntaxException {
        registry.resolveSymbolicResource("${garbage}");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test
    public void testResolveResourceRelative() {
        final String m = "testResolveResourceRelative";
        try {
            WsResource r;
            addSymbols();

            r = registry.resolveSymbolicResource("${dir}/");
            assertNotNull("WsResource should be resolved for ${dir}/", r);
            assertEquals("WsResource should match symbolic root", dirRes, r);
            assertEquals("Repository path should have trailing slash", "${root}/dir/", r.toRepositoryPath());

            r = registry.resolveSymbolicResource("${dir}/./");
            assertNotNull("WsResource should be resolved for ${dir}/./", r);
            assertEquals("WsResource should match file resource", dirRes, r);
            assertEquals("Repository path should have trailing /./", "${root}/dir/", r.toRepositoryPath());

            WsResource res = dirRes.resolveRelative("./a/b");

            r = registry.resolveSymbolicResource("${dir}/a/b");
            assertNotNull("WsResource should be resolved for ${dir}/a/b", r);
            assertEquals("WsResource should match symbolic root", res, r);
            assertEquals("Repository path should match ${dir}/a/b", "${root}/dir/a/b", r.toRepositoryPath());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)

     */
    @Test
    public void testResolveString() {
        String m = "testResolveString";
        try {
            registry.addStringSymbol("windows.root", "x:\\abc\\def");
            registry.addStringSymbol("windows.file.root", "file:///x:/abc/def");
            registry.addStringSymbol("windows.file2.root", "file:/x:/abc/def");

            // UNC URL: \\machine\shared
            registry.addStringSymbol("windows.unc.root", "file:////127.0.0.1/C$");

            registry.addStringSymbol("nix.root", "/abc/def");
            registry.addStringSymbol("nix.file.root", "file:///abc/def");
            registry.addStringSymbol("nix.file2.root", "file:/abc/def");

            String s;

            // Resolve resource containing a symbolic(does not exist)
            s = registry.resolveSymbolicString("${garbage}");
            assertEquals("Unknown symbol should be unchanged", "${garbage}", s);

            // Resolve an env var that doesn't exist
            s = registry.resolveSymbolicString("${env.garbage}");
            assertEquals("Unknown env symbol should be unchanged", "${env.garbage}", s);

            s = registry.resolveSymbolicString("${windows.root}");
            assertEquals("Windows root should be normalized", "x:/abc/def", s);

            s = registry.resolveSymbolicString("${windows.file.root}");
            assertEquals("Windows file:// root should be normalized (three slashes preserved)", "file:///x:/abc/def", s);

            s = registry.resolveSymbolicString("${windows.file2.root}");
            assertEquals("Windows file:// root should be normalized (only one slash)", "file:/x:/abc/def", s);

            s = registry.resolveSymbolicString("${nix.root}");
            assertEquals("*nix root should be identical", "/abc/def", s);

            s = registry.resolveSymbolicString("${nix.file.root}");
            assertEquals("*nix file:/// root should be normalized (three slashes preserved)", "file:///abc/def", s);

            s = registry.resolveSymbolicString("${nix.file2.root}");
            assertEquals("*nix file:// root should be normalized (only one slash)", "file:/abc/def", s);

            System.setProperty("system.property", "abc");
            s = registry.resolveSymbolicString("${system.property}");
            assertEquals("system property should be substituted", "abc", s);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = MalformedLocationException.class)
    public void testStringMatchRelativePathAfterFile() {
        registry.resolveSymbolicResource("${file}/plainString");
    }

    @Test(expected = MalformedLocationException.class)
    public void testStringMatchNotPath() {
        registry.resolveSymbolicResource("${sub2}");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.SymbolRegistry#resolveSymbolicResource(String)} .
     */
    @Test
    public void testStringMatch() {
        final String m = "testStringMatch";
        try {
            WsResource r;
            addSymbols();

            r = registry.resolveSymbolicResource("${sub}");
            assertNotNull("WsResource should be resolved for ${sub} --> ${root}", r);
            assertEquals("${root}/", r.toRepositoryPath());
            assertEquals("WsResource should match symbolic root", symRoot, r);

            r = registry.resolveSymbolicResource("${sub1}");
            assertNotNull("WsResource should be resolved for ${sub1} --> ${file}", r);
            assertEquals("WsResource should match symbolic root", fileRes, r);
            assertEquals("${root}/file", r.toRepositoryPath());

            r = registry.resolveSymbolicResource("${sub4}/otherdir/");
            assertNotNull("WsResource should be resolved for ${sub4}/otherdir --> ${root}/plainString/otherdir/", r);
            assertEquals("${root}/plainString/otherdir/", r.toRepositoryPath());
            assertTrue("${root}/plainString/otherdir/.isType(NODE)", r.isType(Type.DIRECTORY));

            String s = registry.resolveSymbolicString("${sub4}/otherdir/");
            assertNotNull("String should be resolved for ${sub4}/otherdir/", s);
            assertTrue("String should end with plainString/otherdir/", s.endsWith("plainString/otherdir/"));

            File f = new File(TEST_DATA_DIR);
            String root = PathUtils.normalize(f.getAbsolutePath());
            assertEquals("resolved path should equal derived path", root + "/plainString/otherdir/", s);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    boolean doesItMatch(String path, Matcher m) {
        boolean found = m.find();
        if (found)
            System.out.format("From %d to %d, found %s in %s%n", m.start(), m.end(), m.group(), path);
        else
            System.out.println("No match found for " + path);

        return found;
    }
}
