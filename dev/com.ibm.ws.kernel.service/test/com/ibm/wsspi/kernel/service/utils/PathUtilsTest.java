/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.kernel.service.location.internal.ResourceUtils;
import com.ibm.ws.kernel.service.location.internal.SymbolRegistry;
import com.ibm.ws.kernel.service.location.internal.SymbolicRootResource;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.Utils;

public class PathUtilsTest {
    @Rule
    public final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    static File tempDirectory;
    static SymbolicRootResource commonRoot;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File f = Utils.createTempFile("ResourceUtilsTest", "tmp");
        f.delete();
        f.mkdir();

        tempDirectory = f;
        System.out.println("Using tmp directory: " + tempDirectory.getAbsolutePath());

        SharedLocationManager.createLocations(tempDirectory.getAbsolutePath());

        commonRoot = new SymbolicRootResource(tempDirectory.getAbsolutePath(), "A", null);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Utils.recursiveClean(tempDirectory);
        SymbolRegistry.getRegistry().clear();
    }

    @Test
    public void testNormalizeUnixStylePath() {
        //various .. path collapse tests
        assertEquals("/fish/flibble", PathUtils.normalizeUnixStylePath("/wibble/../fish/flibble"));
        assertEquals("fish/flibble", PathUtils.normalizeUnixStylePath("wibble/../fish/flibble"));
        assertEquals("fish/flibble", PathUtils.normalizeUnixStylePath("wibble/../fish/../fish/flibble"));
        assertEquals("flibble", PathUtils.normalizeUnixStylePath("wibble/../fish/../flibble"));
        assertEquals("../flibble", PathUtils.normalizeUnixStylePath("wibble/../../flibble"));
        assertEquals("/.", PathUtils.normalizeUnixStylePath("/./wibble/.."));
        //path collapses to root, with/without leading slash
        assertEquals("", PathUtils.normalizeUnixStylePath("wibble/.."));
        assertEquals("/", PathUtils.normalizeUnixStylePath("/wibble/.."));
        assertEquals("", PathUtils.normalizeUnixStylePath("m/.."));
        assertEquals("/", PathUtils.normalizeUnixStylePath("/./.."));
        assertEquals("/", PathUtils.normalizeUnixStylePath("/////////////"));
        //tests that .. as a part of a path element doesnt trigger normalising.
        assertEquals("/fred..", PathUtils.normalizeUnixStylePath("/fred.."));
        assertEquals("fred..", PathUtils.normalizeUnixStylePath("fred.."));
        assertEquals("zombies../cheerleaders", PathUtils.normalizeUnixStylePath("zombies../cheerleaders"));
        assertEquals("/zombies../cheerleaders", PathUtils.normalizeUnixStylePath("/zombies../cheerleaders"));
        //test that multiple adjacent .. directories are handled correctly
        assertEquals("wibble/bar", PathUtils.normalizeUnixStylePath("wibble/splat/splong/../../bar"));
        assertEquals("bar", PathUtils.normalizeUnixStylePath("wibble/splat/../splong/../../bar"));
        assertEquals("../bar", PathUtils.normalizeUnixStylePath("wibble/splat/../../splong/../../bar"));
        assertEquals("../bar", PathUtils.normalizeUnixStylePath("wibble/splat/../splong/../../../bar"));
        assertEquals("/wibble/bar", PathUtils.normalizeUnixStylePath("/wibble/splat/splong/../../bar"));
        assertEquals("/bar", PathUtils.normalizeUnixStylePath("/wibble/splat/../splong/../../bar"));
        assertEquals("/../bar", PathUtils.normalizeUnixStylePath("/wibble/splat/../../splong/../../bar"));
        assertEquals("/../bar", PathUtils.normalizeUnixStylePath("/wibble/splat/../splong/../../../bar"));
        assertEquals("/..", PathUtils.normalizeUnixStylePath("/a/ab/../../.."));
        assertEquals("/../..", PathUtils.normalizeUnixStylePath("/../.."));
        assertEquals("../..", PathUtils.normalizeUnixStylePath("../.."));
        assertEquals("/../..", PathUtils.normalizeUnixStylePath("//../.."));
        assertEquals("..", PathUtils.normalizeUnixStylePath("m/../.."));
        //no leading slash no embedded slash, paths
        assertEquals("fish", PathUtils.normalizeUnixStylePath("fish"));
        assertEquals(".", PathUtils.normalizeUnixStylePath("."));
        assertEquals("..", PathUtils.normalizeUnixStylePath(".."));
        //leading slash no embedded slash..
        assertEquals("/fish", PathUtils.normalizeUnixStylePath("/fish"));
        assertEquals("/.", PathUtils.normalizeUnixStylePath("/."));
        assertEquals("/..", PathUtils.normalizeUnixStylePath("/.."));
        //no special meaning for ... but it shouldn't affect processing..
        assertEquals("...", PathUtils.normalizeUnixStylePath("..."));
        assertEquals("/fish/...", PathUtils.normalizeUnixStylePath("/fish/..."));
        assertEquals("fish/...", PathUtils.normalizeUnixStylePath("fish/..."));
        assertEquals("/fish/.../flibble", PathUtils.normalizeUnixStylePath("/fish/.../flibble"));
        //no leading slash dot/dotdot terminated
        assertEquals("fish/.", PathUtils.normalizeUnixStylePath("fish/."));
        assertEquals("fish", PathUtils.normalizeUnixStylePath("fish/food/.."));
        //trailing / removed?
        assertEquals("fish", PathUtils.normalizeUnixStylePath("fish/"));
        assertEquals("/fish", PathUtils.normalizeUnixStylePath("/fish/"));
        assertEquals("fish/stiletto", PathUtils.normalizeUnixStylePath("fish/stiletto/"));
        assertEquals("/fish/stiletto", PathUtils.normalizeUnixStylePath("/fish/stiletto/"));
        //embedded // removed?
        assertEquals("fish/beer", PathUtils.normalizeUnixStylePath("fish//beer"));
        assertEquals("fish/beer", PathUtils.normalizeUnixStylePath("fish/beer//"));
        assertEquals("/fish/beer", PathUtils.normalizeUnixStylePath("//fish/beer//"));
        assertEquals("/fish/beer", PathUtils.normalizeUnixStylePath("//fish///beer//"));
        assertEquals("", PathUtils.normalizeUnixStylePath(""));
        assertEquals("/META-INF/resources/css", PathUtils.normalizeUnixStylePath("//META-INF/resources/css"));
    }

    @Test
    public void testIsUnixStylePathAbsolute() {
        assertTrue(PathUtils.isUnixStylePathAbsolute("/wibble/fish"));
        assertTrue(PathUtils.isUnixStylePathAbsolute("wibble/fish"));
        assertTrue(PathUtils.isUnixStylePathAbsolute("wibble/fish/../fred"));
        assertTrue(PathUtils.isUnixStylePathAbsolute("wibble"));

        // and the negative cases
        assertFalse("The path \"..\" should not be considered valid", PathUtils.isUnixStylePathAbsolute(".."));
        assertFalse("The path \"../foo\" should not be considered valid", PathUtils.isUnixStylePathAbsolute("../foo"));
        assertFalse("The path \"/../foo\" should not be considered valid", PathUtils.isUnixStylePathAbsolute("/../foo"));
    }

    /**
     * This tests that the {@link PathUtils#checkAndNormalizeRootPath(String)} method does what it's JavaDoc says it will, namely:
     *
     * <ol>
     * <li>Check that it is a valid <code>path</code> by calling {@link #isUnixStylePathAbsolute(String)}</li>
     * <li>Add a "/" to the front of the <code>path</code> if it is not present</li>
     * <li>Remove a "/" from the end of the <code>path</code> if it is present</li>
     * <li>Normalize the <code>path</code> via a call to {@link #normalizeUnixStylePath(String)}</li>
     * <li>Check that the <code>path</code> is not equals to "/" or ""</li>
     * </ol>
     */
    @Test
    public void testCheckAndNormalizeRootPath() {
        // Test non absolute path throws an exception (we don't use the expected value on the annotation so it will continue with the rest of the test
        try {
            PathUtils.checkAndNormalizeRootPath("/..");
            fail("checkAndNormalizeRootPath didn't throw an exception when passed \"/..\"");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Test adding "/" to the front, and make sure it doesn't add 2
        String normalizedPath = PathUtils.checkAndNormalizeRootPath("foo");
        assertTrue("The normalized path didn't start with \"/\"", normalizedPath.startsWith("/"));
        normalizedPath = PathUtils.checkAndNormalizeRootPath("/foo");
        assertFalse("The normalized path started with \"//\" so the method must of added an extra one", normalizedPath.startsWith("//"));

        // Test removing a "/" and it doesn't remove the last character when its not a /
        normalizedPath = PathUtils.checkAndNormalizeRootPath("/foo/");
        assertEquals("The normalized path was wrong with a trailing \"/\"", "/foo", normalizedPath);
        normalizedPath = PathUtils.checkAndNormalizeRootPath("/foo");
        assertEquals("The normalized path was wrong without a trailing \"/\"", "/foo", normalizedPath);

        // Make sure it is normalized
        normalizedPath = PathUtils.checkAndNormalizeRootPath("/foo/../bar");
        assertEquals("The normalized path was not normalized", "/bar", normalizedPath);

        // Test empty paths
        try {
            normalizedPath = PathUtils.checkAndNormalizeRootPath("/foo/..");
            fail("The normalized path should of been set to \"/\" but no illegal argument exception was thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            normalizedPath = PathUtils.checkAndNormalizeRootPath("");
            fail("The normalized path should of been set to \"\" but no illegal argument exception was thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testGetChildUnder() {
        assertEquals("pies", PathUtils.getChildUnder("/we/like/pies", "/we/like"));
        assertEquals("pies", PathUtils.getChildUnder("/we/like/pies/lots", "/we/like"));
        assertEquals("not", PathUtils.getChildUnder("/fish/are/friends/not/food", "/fish/are/friends"));
        assertEquals("pies", PathUtils.getChildUnder("/pies", "/"));
    }

    @Test
    public void testGetFirstPathComponent() {
        assertEquals("wibble", PathUtils.getFirstPathComponent("/wibble/fish"));
        assertEquals("we", PathUtils.getFirstPathComponent("/we/like/pie"));
        assertEquals("fish", PathUtils.getFirstPathComponent("/fish"));
        assertEquals("sheep", PathUtils.getFirstPathComponent("sheep"));
        assertEquals("sheep", PathUtils.getFirstPathComponent("sheep/like/pies"));
        assertEquals("sheep", PathUtils.getFirstPathComponent("sheep/shearing"));
        assertEquals("", PathUtils.getFirstPathComponent("/"));
        assertEquals("", PathUtils.getFirstPathComponent(""));
    }

    @Test
    public void testGetName() {
        assertEquals("fish", PathUtils.getName("/i/like/fish"));
        assertEquals("fish", PathUtils.getName("/fish"));
        assertEquals("fish", PathUtils.getName("fish"));
        assertEquals("monkeys", PathUtils.getName("happy/monkeys"));
    }

    @Test
    public void testGetParent() {
        assertEquals("/pie", PathUtils.getParent("/pie/good"));
        assertEquals("pie", PathUtils.getParent("pie/good"));
        assertEquals("pie/very", PathUtils.getParent("pie/very/good"));
        assertEquals("/pie/very", PathUtils.getParent("/pie/very/good"));
        assertEquals(null, PathUtils.getParent("pie"));
        assertEquals(null, PathUtils.getParent("/"));
        assertEquals("/", PathUtils.getParent("/pie"));
    }

    @Test
    public void testPathComparator() {
        //test data, deliberately not in order.
        List<String> data = Arrays.asList(new String[] {
                                                         "/bb/aa", "/aa/aa", "/cc/aa", "/ba/a", "/ca/c", "/aa/ab", "/bb/ab", "/ba/a/bb", "/ba/a/ab"
        });

        //create a sorted set, using the comparator.
        SortedSet<String> s = new TreeSet<String>(new PathUtils.PathComparator());
        s.addAll(data);

        //abuse toString on set to compare the required order.
        assertEquals("[/aa/aa, /aa/ab, /ba/a, /ba/a/ab, /ba/a/bb, /bb/aa, /bb/ab, /ca/c, /cc/aa]", s.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.ResourceUtils#normalize(java.lang.String)} .
     */
    @Test
    public void testNormalize() throws Exception {
        final String m = "testNormalize";
        String path, result;

        try {
            path = "a";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("No segments should be removed", path, result);

            path = "a//b";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("Extra slash should be removed", "a/b", result);

            path = "./a";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("Leading ./ should be removed", "a", result);

            path = "../a";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("No segments should be removed", path, result);

            path = "/a/..";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("a/.. should be removed", "/", result);

            path = "/a/../b";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("a/../ segment should be removed", "/b", result);

            path = "/a//../b";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("a//../ segment should be removed", "/b", result);

            path = "./..";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("./ segment should be removed", "..", result);

            path = "a/./../c";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("a/./.. should be removed", "c", result);

            path = "a/./../../c";
            result = PathUtils.normalize(path);
            System.out.println("Path: \t" + path);
            System.out.println("  URI:\t" + new URI(path).normalize().toString());
            System.out.println(" test:\t" + result);
            assertEquals("a/./.. should be removed", "../c", result);

            path = "a/./c";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t" + new URI(path).normalize().toString());
            System.out.println("test:\t" + result);
            assertEquals("./ should be removed", "a/c", result);

            path = "/a:/dir/b";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t" + new URI(path).normalize().toString() + " <--- correct windows path, remove leading /");
            System.out.println("test:\t" + result);
            assertEquals("leading / should be removed (windows path)", "a:/dir/b", result);

            path = "file:////a/b//c";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t" + new URI(path).normalize().toString() + " <-- UNC bug, leading // must be preserved");
            System.out.println("test:\t" + result);
            assertEquals("extra / should be removed between b & c", "file:////a/b/c", result);

            path = "//a/b//c";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t" + new URI(path).normalize().toString());
            System.out.println("test:\t" + result);
            assertEquals("extra / should be removed (between b & c), leading slashes preserved", "//a/b/c", result);

            path = "x:\\abc\\def";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t -- can't be converted to URI on all platforms backslashes");
            System.out.println("test:\t" + result);
            assertEquals("leading / should be removed (windows path)", "x:/abc/def", result);

            path = "//127.0.0.1/C$"; // Default UNC path
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            System.out.println(" URI:\t" + new URI(path).normalize().toString());
            System.out.println("test:\t" + result);
            assertEquals("leading UNC path & special character should be preserved", "//127.0.0.1/C$", result);

            // Don't collapse .. after a symbol @ beginning of string
            // Don't collapse .. after a symbol in the middle of the string
            // Collapse .. when a symbol is present, but not near the ..
            path = "${preserved}/../${preserved}/../collapsed/../end";
            result = PathUtils.normalize(path);
            System.out.println("Path:\t" + path);
            // {} unsupported in URI... System.out.println(" URI:\t" + new URI(path).normalize().toString());
            System.out.println("test:\t" + result);
            assertEquals("symbol should be preserved", "${preserved}/../${preserved}/../end", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.ResourceUtils#normalize(java.lang.String)} .
     */
    @Test
    public void testNormalize_unixRoot() throws Exception {
        final String m = "testNormalize";
        String path, result;

        try {
            path = "/";
            result = PathUtils.normalize(path);
            assertEquals("unix root should be preserved", "/", result);

            path = "//";
            result = PathUtils.normalize(path);
            assertEquals("unix root should be preserved, but duplicate slashes not removed", "//", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testContainsSymbol() throws Exception {
        final String m = "testContainsSymbol";
        String path;

        try {
            path = null;
            boolean containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "$";
            containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "${";
            containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "${a";
            containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "${}";
            containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "${x}";
            containsSymbol = PathUtils.containsSymbol(path);
            assertTrue("Should contain symbol: " + path, containsSymbol);

            path = "$x{x}";
            containsSymbol = PathUtils.containsSymbol(path);
            assertFalse("Should not contain symbol: " + path, containsSymbol);

            path = "a${x}";
            containsSymbol = PathUtils.containsSymbol(path);
            assertTrue("Should contain symbol: " + path, containsSymbol);

            path = "a${x}a";
            containsSymbol = PathUtils.containsSymbol(path);
            assertTrue("Should contain symbol: " + path, containsSymbol);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGetSymbol() throws Exception {
        final String m = "testGetSymbol";
        String path;

        try {
            path = null;
            String symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "$";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "${";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "${a";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "${}";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "${x}";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should get symbol: " + path, path, symbol);

            path = "$x{x}";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should not get symbol: " + path, null, symbol);

            path = "a${x}";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should get symbol: " + path, "${x}", symbol);

            path = "a${x}a";
            symbol = PathUtils.getSymbol(path);
            assertEquals("Should get symbol: " + path, "${x}", symbol);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.ResourceUtils#normalizeRelative(String)}
     */
    @Test(expected = com.ibm.wsspi.kernel.service.location.MalformedLocationException.class)
    public void testNormalizeRelativeSymbolic() {
        PathUtils.normalizeRelative(WsLocationConstants.SYMBOL_PREFIX + "a" + WsLocationConstants.SYMBOL_SUFFIX);
    }

    /**
     * Test method for {@link ResourceUtils#ABSOLUTE_URI }
     */
    @Test
    public void testAbsoluteURIPattern() {
        String path;
        Matcher m;
        boolean result;

        try {
            // Only test forward slashes: Pattern used after normalize
            path = "c:/temp";
            m = PathUtils.ABSOLUTE_URI.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("Windows path should be absolute: " + path, result);

            path = "file:/test/path";
            m = PathUtils.ABSOLUTE_URI.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("File URI should be absolute: " + path, result);

            path = "file:///test/path";
            m = PathUtils.ABSOLUTE_URI.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("File URI should be absolute: " + path, result);

            path = "arbitrary:/something";
            m = PathUtils.ABSOLUTE_URI.matcher(path);
            result = doesItMatch(path, m);
            assertTrue("URI with scheme should be absolute: " + path, result);

            path = "arbitrary:something"; // could be regular file
            m = PathUtils.ABSOLUTE_URI.matcher(path);
            result = doesItMatch(path, m);
            assertFalse("URI with scheme but no slash should be relative: " + path, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testResolve", t);
        }
    }

    boolean doesItMatch(String path, Matcher m) {
        boolean found = m.matches();
        if (found)
            System.out.printf("Matched %s%n", path);
        else
            System.out.println("No match found for " + path);

        return found;
    }

    private final static boolean isOsCaseSensitive = PathUtils.isOsCaseSensitive();

    private static boolean clearOutputDir() throws IOException {

        if (Utils.OUTPUT_DATA.exists())
            Utils.recursiveClean(Utils.OUTPUT_DATA);
        //b = Utils.OUTPUT_DATA.delete();
        if (Utils.OUTPUT_DATA.exists())
            fail("Could not clear Output data dir: " + Utils.OUTPUT_DATA_DIR);
        return Utils.OUTPUT_DATA.mkdirs();
    }

    /* On file systems that are case sensitive, like Linux, the checkCase method 
     * always returns true.  This is because it is assumed that prior to calling 
     * checkCase() you tested to see if the file exists using the "pathToTest". 
     * So the OS has already verified the case matches by verifying the file exists
     * using the "pathToTest".  Therefore, it is unnecessary to call checkCase() when 
     * the file system is case sensitive.
     * 
     * On a file system that is NOT case sensitive (eg. Windows & Mac), more testing 
     * is necessary to ensure the case actually matches the file as it exists on disk, 
     * because the artifact file system API IS case sensitive regardless of OS.
     * 
     * If the file does NOT exist on disk, then checkCase compares the "pathToTest"
     * to the canonical path of the java.io.File object.
     * 
     * The paths should match without regard to trailing slashes.
     */
    @Test
    public void testCheckCase() throws Exception {
        assertTrue("Unable to clear initial output dir", clearOutputDir());
        
        // Create file.  Note the case used when creating the file.
        String actualFileNameOnDisk = "WEB-INF/classes";
        File original = new File(Utils.OUTPUT_DATA, actualFileNameOnDisk); // No trailing slash
        File test = original;
        assertTrue("either couldn't create or didn't already exist",
                   test.isDirectory() || test.mkdirs());

        // Correct case, without and with the trailing slash
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, actualFileNameOnDisk));
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, actualFileNameOnDisk + "/"));

        // Repeat, but now java.io.file ends with  "/"
        test = new File(Utils.OUTPUT_DATA, "WEB-INF/classes/");
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, actualFileNameOnDisk));
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, actualFileNameOnDisk + "/"));

        // Case where passed in file doesn't match case on the disk.  
        // The checkCase() method cares about case on the file system when the file exists.
        String javaIoFileName = "WeB-INF/Classes";
        test = new File(Utils.OUTPUT_DATA, javaIoFileName);

        if (isOsCaseSensitive) { 
            assertTrue("checkCase should ALWAYS return true on case sensitive system",
                    PathUtils.checkCase(test, actualFileNameOnDisk));
            assertTrue("checkCase should ALWAYS return true on case sensitive system",
                    PathUtils.checkCase(test, actualFileNameOnDisk + "/"));
            assertTrue("checkCase should ALWAYS return true on case sensitive system",
                    PathUtils.checkCase(test, javaIoFileName));
            assertTrue("checkCase should ALWAYS return true on case sensitive system",
                    PathUtils.checkCase(test, javaIoFileName + "/"));
        } else {
            assertTrue("checkCase should have returned true on case insensitive system",
                    PathUtils.checkCase(test, actualFileNameOnDisk));
            assertTrue("checkCase should have returned true on case insensitive system",
                    PathUtils.checkCase(test, actualFileNameOnDisk + "/"));        	
            assertFalse("checkCase should have returned false on case insensitive system since pathToTest doesn't match actual file name on disk.",
                    PathUtils.checkCase(test, javaIoFileName));
            assertFalse("checkCase should have returned false on case insensitive system since pathToTest doesn't match actual file name on disk.",
                    PathUtils.checkCase(test, javaIoFileName + "/"));
        }

        // ** CLEAN UP 
        Utils.recursiveClean(original.getParentFile());

        // case where passed in file doesn't exist at all (matching or not in case)
        String nonExistingJavaFileName = "WeB-INF/Classes";
        test = new File(Utils.OUTPUT_DATA, nonExistingJavaFileName); // create a ref to the file, but don't actually create the file itself

        if ( !isOsCaseSensitive ) {
            assertFalse("checkCase should have returned false", PathUtils.checkCase(test, "WEB-INF/classes"));
            assertFalse("checkCase should have returned false", PathUtils.checkCase(test, "WEB-INF/classes/"));
            assertFalse("checkCase should have returned false", PathUtils.checkCase(test, "wEB-INF/claSses/"));
        }
        
        // note that checkCase will return true even if the file does not exist if the file ref matches the passed-in path
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, nonExistingJavaFileName));
        assertTrue("checkCase should have returned true", PathUtils.checkCase(test, nonExistingJavaFileName + "/"));

        // case where passed-in pathToTest does not match passed-in file at all
        test = new File(Utils.OUTPUT_DATA, "WEB-INF/classes");
        
        if ( isOsCaseSensitive ) {
            // On case sensitive file systems, checkCase(...) simply returns true.  It assumes that you already checked
            // for the file's existence using the pathToTest, and therefore the file name and case must be correct.
            assertTrue("checkCase ALWAYS returns true on case sensitive file system, even if no match", 
                       PathUtils.checkCase(test, "someOtherPath"));
            assertTrue("checkCase ALWAYS returns true on case sensitive file system, even if not match", 
                       PathUtils.checkCase(test, "someOtherPath/"));
        } else {
            // Note: Every "assertFalse", regarding case, must be inside an "if (!isOsCaseSensitive)", because 
            // for case sensitive systems, checkCase(...) always returns true without checking anything.
            assertFalse("checkCase should have return false - no match", PathUtils.checkCase(test, "someOtherPath"));
            assertFalse("checkCase should have return false - no match", PathUtils.checkCase(test, "someOtherPath/"));
        }
    }

    @Test
    public void testCheckCaseSymbolic() throws Exception {

        // CLEAN UP 
        assertTrue("could not clear initial output dir", clearOutputDir());
        String linkCommand = "/bin/ln";

        // only try this test if the unix ln command exists and can be executed
        File ln = new File(linkCommand);
        if (ln.exists() && ln.canExecute()) {

            // Create the paths on the file system if they do not already exist.
            // Note the actual case of the files created which will be used for the tests that follow.
            File canonicalFile = new File(Utils.OUTPUT_DATA, "checkCase/path/to/symlink");
            File webinfFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF");
            assertTrue("either couldn't create or didn't already exist: " + canonicalFile.getAbsolutePath(),
                       canonicalFile.exists() || canonicalFile.mkdirs());
            assertTrue("either couldn't create or didn't already exist: " + canonicalFile.getAbsolutePath(),
                       webinfFile.isDirectory() || webinfFile.mkdirs());

            // Create symbolic link .../checkCase/WEB-INF/classes ----> .../checkCase/path
            String hardPath = canonicalFile.getParentFile().getParentFile().getAbsolutePath(); // .../checkCase/path
            String symPath = webinfFile.getAbsolutePath() + "/classes";                        // .../checkCase/WEB-INF/classes
            String[] execParameters = new String[] { linkCommand, "-s", hardPath, symPath };
            Process process = Runtime.getRuntime().exec(execParameters);
            process.waitFor();

            // symlink at the end of pathToTest
            File symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/WEB-INF/classes"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/"));            
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do.
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/classes"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/clAsses/"));
            }

            // create file with an ending "/" and ensure same cases as above work
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes/");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/WEB-INF/classes"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/"));            
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do.
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/classes"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/clAsses/"));
            }

            // symlink in the middle of pathToTest
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes/to/symlink");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink/"));            
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do.
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/classes/to/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WEB-INF/Classes/to/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/tO/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/sYmlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WeB-INF/Classes/to/symlink"));
            }

            // symlink first directory in pathToTest
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/classes/to/symlink"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "classes/to/symlink"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "classes/to/symlink/"));
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do.
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "Classes/to/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "classes/tO/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "classes/to/sYmlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "ClAsses/to/syMlink"));
            }

            // symlink occurs after file path
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "checkCase/WEB-INF"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/WEB-INF/"));
            if ( !isOsCaseSensitive ) {
                // Every "assertFalse", regarding case, must be inside an "if (!isOsCaseSensitive)", because 
                // for case sensitive systems, checkCase(...) always returns true without checking anything.
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "/web-inf/"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "somethingelse/WEB-INF"));
            }

            // symlink occurs before pathToTest
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes/to/symlink");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "to/symlink"));
            if ( !isOsCaseSensitive ) {
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "TO/symlink"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "somethingelse/symlink"));
            }

            // test just one directory level in pathToTest
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/classes"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "/classes/"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "classes/"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "classes"));
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "Classes"));
            }
            
            // Pass in java.io.file whose case doesn't match case of file on file system.
            // ** An OS that is NOT case sensitive will allow you
            //    to create a java.io.File object using the wrong case, but checkCase uses the actual
            //    case of the file on the file system if the file exists.  It doesn't care
            //    about the case of the java.io.File unless the file does NOT exist.  The behavior
            //    comes from getCanonicalFile which returns the file using the actual case on disk
            //    unless the file does not exist, in which case it uses the case of the java.io.File object.
            //
            //symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WeB-INF/Classes/to/symlink");
            //boolean symlinkFileExists = symlinkFile.getCanonicalFile().exists();
            
            //if (isOsCaseSensitive) {
            //    assertFalse("file shouldn't exist: " + symlinkFile.getAbsolutePath(), symlinkFileExists);
            //    assertTrue("checkCase should always return true on case sensitive system",
            //               PathUtils.checkCase(symlinkFile, "WeB-INF/Classes/to/symlink"));
            //} else {
            //	// The OS doesn't care if the case matches, but we do
            //    assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFileExists);
            //    assertFalse("checkCase should have returned false even on case insensitive system",
            //               PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink"));  // case matches file system but not case of the java.io.file, this will return true!               
            //}

            // .../checkCase/WEB-INF/classes --> .../checkCase/path,  
            // .../checkCase/path/to/symlink/wait --> .../checkCase/really
            // logical path example: .../checkCase/WEB-INF/classes/to/symlink/wait/another/jump
            File canonicalFile2 = new File(Utils.OUTPUT_DATA, "checkCase/really/another/jump");
            assertTrue("either couldn't create or didn't already exist: " + canonicalFile2.getAbsolutePath(),
                       canonicalFile2.isDirectory() || canonicalFile2.mkdirs());

            hardPath = canonicalFile2.getParentFile().getParentFile().getAbsolutePath(); // .../checkCase/really
            symPath = canonicalFile.getAbsolutePath() + "/wait";                         // .../checkCase/path/to/symlink/wait
            execParameters = new String[] { linkCommand, "-s", hardPath, symPath };
            process = Runtime.getRuntime().exec(execParameters);
            process.waitFor();

            // pass in a file with two symbolic links in it's path
            symlinkFile = new File(Utils.OUTPUT_DATA, "checkCase/WEB-INF/classes/to/symlink/wait/another/jump");
            assertTrue("file should exist: " + symlinkFile.getAbsolutePath(), symlinkFile.exists());

            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "wait/another/jump"));
            assertTrue("checkCase should have returned true", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink/wait/another/jump"));
            if ( !isOsCaseSensitive ) {
                // The OS doesn't care if the case matches, but we do
                assertTrue("checkCase should have returned true",   PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink/wait/another/jump"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WEB-INF/CLASSES/to/symlink/wait/another/jump"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "WEB-INF/classes/to/symlink/WAIT/another/jump"));
                assertFalse("checkCase should have returned false", PathUtils.checkCase(symlinkFile, "web-inf/cLasses/to/symlink/wait/another/jump"));
            }

            // clean up all the files/links that were created
            Utils.recursiveClean(webinfFile.getParentFile());
        }
    }

    @Test
    public void testReplaceRestrictedCharactersInFileName() throws Exception {
        String testString = "Valid string";
        assertTrue("Strings should be equal",
                   testString.equals(PathUtils.replaceRestrictedCharactersInFileName(testString)));

        testString = "";
        assertTrue("Null should have been returned",
                   PathUtils.replaceRestrictedCharactersInFileName(testString) == null);

        testString = "<>:\"/\\|?*";
        assertTrue("Null should have been returned, all characters are restricted",
                   PathUtils.replaceRestrictedCharactersInFileName(testString) == null);

        testString = "Has[some/bad|chars]";
        String expectedString = "Has[some.bad.chars]";
        String resultString = PathUtils.replaceRestrictedCharactersInFileName(testString);
        assertTrue("String " + testString + " should have reduced to " + expectedString + " but instead reduced to " + resultString,
                   expectedString.equals(resultString));

        testString = "?FirstCharBad";
        expectedString = ".FirstCharBad";
        resultString = PathUtils.replaceRestrictedCharactersInFileName(testString);
        assertTrue("String " + testString + " should have reduced to " + expectedString + " but instead reduced to " + resultString,
                   expectedString.equals(resultString));

        testString = "LastCharBad*";
        expectedString = "LastCharBad.";
        resultString = PathUtils.replaceRestrictedCharactersInFileName(testString);
        assertTrue("String " + testString + " should have reduced to " + expectedString + " but instead reduced to " + resultString,
                   expectedString.equals(resultString));

        testString = "??";
        assertTrue("Null should have been returned, resulting string is '..'",
                   PathUtils.replaceRestrictedCharactersInFileName(testString) == null);

        testString = "?.";
        assertTrue("Null should have been returned, resulting string is '..'",
                   PathUtils.replaceRestrictedCharactersInFileName(testString) == null);

        testString = "?";
        assertTrue("Null should have been returned, resulting string is '.'",
                   PathUtils.replaceRestrictedCharactersInFileName(testString) == null);

        testString = "..";
        assertTrue("Strings should be equal, supplied string was '..'",
                   testString.equals(PathUtils.replaceRestrictedCharactersInFileName(testString)));

        StringBuilder sb = new StringBuilder();
        sb.append("First");
        sb.append((char) 18); // Control character
        sb.append("Last");
        testString = sb.toString();
        expectedString = "First.Last";
        assertTrue("String should have had control character replaced", expectedString.equals(PathUtils.replaceRestrictedCharactersInFileName(testString)));
    }

}
