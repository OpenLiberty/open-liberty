/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.location.ExistingResourceException;
import com.ibm.wsspi.kernel.service.location.ResourceMismatchException;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
@RunWith(JMock.class)
public class LocalFileResourceTest {
    static SharedOutputManager outputMgr;

    static File tempDirectory;
    static String NORMALIZED_ROOT;

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

        File f = Utils.createTempFile("LocalFileResourceTest", "tmp");
        f.delete();
        f.mkdir();

        tempDirectory = f;
        System.out.println("Using tmp directory: " + tempDirectory.getAbsolutePath());
        NORMALIZED_ROOT = PathUtils.normalize(tempDirectory.getAbsolutePath() + "/");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Utils.recursiveClean(tempDirectory);
        outputMgr.restoreStreams();
    }

    Mockery context = new JUnit4Mockery();

    LocalFileResource commonFile = LocalFileResource.newResource(NORMALIZED_ROOT + "commonFile", null);
    LocalFileResource commonDir = LocalFileResource.newResource(NORMALIZED_ROOT + "commonDir/", null);

    @After
    public void tearDown() {
        assertTrue("Resource should be deletable or not exist after all tests", commonFile.delete() || !commonFile.exists());
        SymbolRegistry.getRegistry().clear();
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#newResource(String, String)}
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testConstructResourceNoPath() {
        LocalFileResource.newResource(null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#newResource(String, String)}
     * 
     * @throws IOException
     */
    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testConstructResourceBadFile() throws IOException {
        String name = "testConstructResourceBadFile/";
        File file = new File(tempDirectory, name);
        file.createNewFile(); // create file
        file.deleteOnExit();

        LocalFileResource.newResource(NORMALIZED_ROOT + name, null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#newResource(String, String)}
     * 
     * @throws IOException
     */
    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testConstructResourceBadDirectory() throws IOException {
        String name = "testConstructResourceBadDirectory";
        File dir = new File(tempDirectory, name);
        dir.mkdir();
        dir.deleteOnExit();

        LocalFileResource.newResource(NORMALIZED_ROOT + name, null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#newResourceFromResource(String, String, LocalFileResource)}
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testConstructResourceFromResourceNoPath() {
        LocalFileResource.newResourceFromResource(null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#create(com.ibm.wsspi.kernel.service.location.WsResource.Type)} .
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#delete()} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#exists()} ,
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChild(String)} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChildren()}
     * , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChildren(String)} ,
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getLastModified()} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getName()} ,
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getParent()} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#setLastModified()} ,
     * 
     * @throws IOException
     */
    @Test
    public void testFileCreateDeleteExistsDoNotThrow() throws IOException {
        final String m = "testFileCreateDeleteExistsDoNotThrow";
        try {
            String parentName = "testFileOperationsParent/";
            String fileName = "testFileOperationsFile";
            String badFileName = "testFileOperationsBadFile/";

            File dir = new File(tempDirectory, parentName);
            dir.deleteOnExit();

            SymbolicRootResource root = new SymbolicRootResource(NORMALIZED_ROOT + parentName, "A", null);

            File file = new File(dir, fileName);
            file.deleteOnExit();

            File badFile = new File(file, badFileName);
            badFile.deleteOnExit();

            LocalFileResource parent = LocalFileResource.newResource(root.getNormalizedPath(), null, root);
            assertTrue("parent should be a directory" + parent, parent.isType(WsResource.Type.DIRECTORY));

            LocalFileResource res = LocalFileResource.newResource(root.getNormalizedPath() + fileName, null, root);
            assertTrue("res should be a file: " + res, res.isType(WsResource.Type.FILE));

            LocalFileResource badRes = LocalFileResource.newResource(res.getNormalizedPath() + '/' + badFileName, null, root);
            assertTrue("badRes should be a directory: " + badRes, badRes.isType(WsResource.Type.DIRECTORY));

            // --- Pre-check for exists ----

            assertFalse("Parent directory resource should not exist before create", parent.exists());

            assertFalse("File resource should not exist before create", res.exists());

            assertFalse("File resource should not exist before create", badRes.exists());

            // --- Create the file ---

            assertTrue("Resource should successfully be created as a file", res.create());

            assertTrue("Created resource should be a file", res.isType(WsResource.Type.FILE));
            assertFalse("False should be returned if type is null", res.isType(null));

            assertTrue("Parent directory resource should exist after create", parent.exists());

            assertTrue("Resource should exist after create", res.exists());

            // -- Try to create the already existing file
            assertFalse("False should be returned if file already exists", res.create());

            // -- Check and set last modified time

            assertTrue("Last modified time should be non-zero", res.getLastModified() != 0);

            assertTrue("Should be able to set the last modified time", res.setLastModified(1234567890L));

            assertEquals("URI of parent should match URI from file.getParent", res.getParent().toExternalURI(), parent.toExternalURI());

            assertNull("File has no children (getChild), should return null", res.getChild("not null"));

            assertFalse("File has no children (getChildren()), iterator should be empty", res.getChildren().hasNext());

            assertFalse("File has no children (getChildren(String)), iterator should be empty", res.getChildren("not null").hasNext());

            assertEquals("File name should match file name", file.getName(), res.getName());

            // --- Try to create child of file ---

            assertFalse("Can not create a file with a file as parent", badRes.create());

            assertFalse("File resource should not exist after failed create", badRes.exists());

            // --- Try to delete file ---

            assertTrue("Resource should successfully be deleted", res.delete());

            assertFalse("Non-existent file should not be deleted", badRes.delete());

            // --- Post-check existence ---

            assertFalse("Resource should not exist after delete", res.exists());

            assertTrue("Parent directory resource should still exist after file is deleted", parent.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#create(com.ibm.wsspi.kernel.service.location.WsResource.Type)} .
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#delete()} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#exists()} ,
     * {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChild(String)} , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChildren()}
     * , {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChildren(String)} ,
     */
    @Test
    public void testDirectoryCreateDeleteExistsShouldNotThrow() {
        final String m = "testDirectoryCreateDeleteExistsShouldNotThrow";

        try {
            String parentName = "testDirectoryOperations/";
            String fileName = "testDirectoryOperationsFile";

            File dir = new File(tempDirectory, parentName);
            dir.deleteOnExit();

            File file = new File(tempDirectory, "testDirectoryOperationsFile");
            file.deleteOnExit();

            SymbolicRootResource root = new SymbolicRootResource(NORMALIZED_ROOT + parentName, "A", null);

            LocalFileResource directoryRes = LocalFileResource.newResource(root.getNormalizedPath() + parentName, null, root);
            assertTrue("directoryRes should be a directory" + directoryRes, directoryRes.isType(WsResource.Type.DIRECTORY));

            LocalFileResource fileRes = LocalFileResource.newResource(directoryRes.getNormalizedPath() + fileName, null, root);
            assertTrue("fileRes should be a file" + fileRes, fileRes.isType(WsResource.Type.FILE));

            // --- Pretest exists ---

            assertFalse("Directory should not exist before create", directoryRes.exists());
            assertFalse("File should not exist before create", fileRes.exists());
            assertTrue("toString contains 'testDirectoryOperations/'", directoryRes.toString().contains(parentName));

            // --- Create directory (with parent as directory, and parent as file) ---

            assertTrue("Resource should successfully be created as a directory", directoryRes.create());
            assertTrue("Directory should exist after create", directoryRes.exists());
            assertEquals("Directory name should match file name", dir.getName() + '/', directoryRes.getName());

            assertTrue("File should successfully be created", fileRes.create());
            assertTrue("Child file should exist after create", fileRes.exists());

            // --- Test getChild/getChildren ---

            Iterator<String> children = directoryRes.getChildren();
            assertNotNull("Directory can have children", children);
            assertTrue("Directory has at least one child", children.hasNext());

            String name = children.next();
            assertEquals("Name of child should match name of file", fileRes.getName(), name);

            WsResource child = directoryRes.getChild(name);
            assertNotNull("Child should exist", child);
            assertEquals("Child should match file", fileRes, child);

            child = directoryRes.getChild("notexist");
            assertNull("Child should not exist", child);

            children = directoryRes.getChildren("nomatch");
            assertNotNull("Directory can have children", children);
            assertFalse("No children match 'nomatch'", children.hasNext());

            children = directoryRes.getChildren(name);
            assertNotNull("Directory can have children", children);
            assertTrue("At least one child matches specified name", children.hasNext());

            // --- Delete directory ---

            assertFalse("Should not be able to delete non-empty directory", directoryRes.delete());

            assertTrue("Directory should exist after faild delete", directoryRes.exists());

            assertTrue("Should be able to delete file", fileRes.delete());

            assertFalse("File should not exist after delete", fileRes.exists());

            assertTrue("Should be able to delete directory after file is deleted", directoryRes.delete());

            assertFalse("Directory should not exist after delete", directoryRes.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChild()}
     */
    @Test
    public void testGetChildWithNullName() {
        assertNull("No child should be returned for null name", commonDir.getChild(null));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChild()}
     */
    @Test
    public void testGetChildWithNoRoot() {
        assertNull("No child should be returned when there is no root", commonDir.getChild("child"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getChild()}
     */
    @Test
    public void testGetChildWithFile() {
        assertNull("No child should be returned when resource is a file", commonFile.getChild("child"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#getParent()}
     */
    @Test
    public void testGetParentNoRoot() throws Exception {
        assertNull("No parent should be returned when resource has no root", commonFile.getParent());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#equals(Object)}
     */
    @Test
    public void testEqualsHashCode() throws Exception {
        String dirName = "testEqualsDirectory/";
        String fileName = "testEqualsFile";

        File dir = new File(tempDirectory, dirName);
        dir.mkdir();
        dir.deleteOnExit();

        File tmpFile = new File(tempDirectory, fileName);
        tmpFile.createNewFile();
        tmpFile.deleteOnExit();

        LocalFileResource dirRes = LocalFileResource.newResource(NORMALIZED_ROOT + dirName, null);
        LocalFileResource dirRes2 = LocalFileResource.newResource(NORMALIZED_ROOT + dirName, null);

        LocalFileResource fileRes = LocalFileResource.newResource(NORMALIZED_ROOT + fileName, null);
        LocalFileResource fileRes2 = LocalFileResource.newResource(NORMALIZED_ROOT + fileName, null);

        // --- Contract for equals ---

        assertFalse("equals(null) must return false", dirRes.equals(null));

        assertTrue("Reflexive: x.equals(x) is true (directory)", dirRes.equals(dirRes));
        assertTrue("Reflexive: x.equals(x) is true (file)", fileRes.equals(fileRes));

        assertTrue("Same resource should be equal (directory=" + dirRes + ",directory2=" + dirRes2 + ")", dirRes.equals(dirRes2));
        assertEquals("Symmetric (equal): x.equals(y) must be the same as y.equals(x)", dirRes.equals(dirRes2), dirRes2.equals(dirRes));

        assertTrue("Same resource should be equal (file)", fileRes.equals(fileRes2));
        assertEquals("Symmetric (equal): x.equals(y) must be the same as y.equals(x)", fileRes.equals(fileRes2), fileRes2.equals(fileRes));

        assertFalse("Different resources should not be equal (directory/file)", dirRes.equals(fileRes));
        assertEquals("Symmetric (not equal): x.equals(y) must be the same as y.equals(x) (directory/file)", dirRes.equals(fileRes), fileRes.equals(dirRes));

        assertFalse("File and Resource should not be equal (different class)", tmpFile.equals(fileRes));

        // --- Contract for hashCode ---

        assertEquals("Same resource should have same hashCode (directory)", dirRes.hashCode(), dirRes.hashCode());
        assertEquals("Same resource should have same hashCode (file)", fileRes.hashCode(), fileRes.hashCode());
        assertEquals("Equal resource should have same hashCode (directory)", dirRes.hashCode(), dirRes2.hashCode());
        assertEquals("Equal resource should have same hashCode (file)", fileRes.hashCode(), fileRes2.hashCode());

        assertTrue("Resource with different file should have different hashCode", dirRes.hashCode() != fileRes.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#put(ReadableByteChannel)}
     */
    @Test
    public void testPutUnopenChannel() throws Exception {
        final ReadableByteChannel rbc = context.mock(ReadableByteChannel.class);

        context.checking(new Expectations()
        {
            {
                oneOf(rbc).isOpen();
                will(returnValue(false));
            }
        });

        assertFalse("Resource should not exist before put", commonFile.exists());
        commonFile.put(rbc);
        assertFalse("Resource should not exist after put of closed channel", commonFile.exists());
    }

    static class DummyInputStream extends InputStream implements ReadableByteChannel {
        int available_i = 0;
        int available[];

        int read_i = 0;
        int read[];

        int open_i = 0;
        int close_i = 0;

        boolean open = true;

        DummyInputStream() {
            open = false;
        }

        DummyInputStream(int available, int read) {
            this.available = new int[] { available };
            this.read = new int[] { read };
        }

        DummyInputStream(int available[], int read[]) {
            this.available = available;
            this.read = read;
        }

        @Override
        public int available() throws IOException {
            return available[available_i++];
        }

        @Override
        public int read() throws IOException {
            if (read_i < read.length) {
                return read[read_i++];
            } else {
                return -1;
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int r = read();

            if (r > 0)
                dst.put((byte) 1);

            return r;
        }

        @Override
        public boolean isOpen() {
            open_i++;
            return open;
        }

        @Override
        public void close() {
            close_i++;
        }
    }

    @Test
    public void testPutNullStream() throws Exception {
        commonFile.put((InputStream) null);
        assertFalse("Resource should not exist after put of null input stream", commonFile.exists());
    }

    @Test
    public void testPutClosedChannel() throws Exception {
        DummyInputStream dis = new DummyInputStream();
        commonFile.put((ReadableByteChannel) dis);
        assertFalse("Resource should not exist after put of null input stream", commonFile.exists());
        assertEquals("read should not have been called", 0, dis.read_i);
        assertEquals("isOpen should have been called once", 1, dis.open_i);
    }

    @Test
    public void testPutEmptyStream() throws Exception {
        assertFalse("Resource should not exist before put", commonFile.exists());

        DummyInputStream dis = new DummyInputStream(new int[] { 0 }, new int[] {});
        commonFile.put((InputStream) dis);

        assertTrue("Resource should exist after put of empty input stream", commonFile.exists());
        // assertEquals("available should have been called once", 1, dis.available_i);
        assertEquals("read should not have been called once", 0, dis.read_i);
        assertEquals("close should not have been called once", 0, dis.close_i);
    }

    @Test
    public void testPutStreamToDirectory() {
        DummyInputStream dis = new DummyInputStream(1, 0);

        try {
            commonDir.put((InputStream) dis);
            fail("Missed expected IOException for writing to a directory/directory");
        } catch (IOException e) { // expected
        }

        assertEquals("available should not have been called", 0, dis.available_i);
        assertEquals("read should not have been called", 0, dis.read_i);
        assertEquals("close should not have been called", 0, dis.close_i);
    }

    @Test
    public void testPutStream() throws Exception {
        String path = "subdir" + File.separator + "testPutStream";

        // Test creation as side-effect of put (do not create explicitly)
        LocalFileResource resource = LocalFileResource.newResource(NORMALIZED_ROOT + path, null);
        assertFalse("Resource should not exist before put", resource.exists());

        DummyInputStream dis = new DummyInputStream(new int[] { 1 }, new int[] { 31 });

        resource.put((InputStream) dis);

        // assertEquals("available should have been called once", 1, dis.available_i);
        assertEquals("read should have been called once", 1, dis.read_i);
        assertEquals("close should not have been called", 0, dis.close_i);
        assertTrue("Resource should exist after put", resource.exists());

        InputStream result = resource.get();
        assertEquals("InputStream should contain 1 byte", 1, result.available());
        result.close();
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testMoveToWithNull() throws IOException {
        commonFile.moveTo(null);
    }

    @Test
    public void testMoveToSelf() throws IOException {
        commonFile.moveTo(commonFile);
    }

    @Test(expected = ResourceMismatchException.class)
    public void testMoveToDifferentType() throws IOException {
        commonFile.moveTo(commonDir);
    }

    @Test
    public void testMoveToFile() throws IOException {
        String m = "testMoveToFile";
        String dirName = m + "/";

        LocalFileResource l1 = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "1", null);
        LocalFileResource l2 = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "2", null);

        try {
            l1.create();
            assertTrue("l1 should exist after create()", l1.exists());
            assertFalse("l2 should not exist after create()", l2.exists());

            System.out.printf("%s\t%s%n", l1.exists(), l1.toRepositoryPath());
            System.out.printf("%s\t%s%n", l2.exists(), l2.toRepositoryPath());

            System.out.println("--- move l1 to l2 ");
            l1.moveTo(l2);

            System.out.printf("%s\t%s%n", l1.exists(), l1.toRepositoryPath());
            System.out.printf("%s\t%s%n", l2.exists(), l2.toRepositoryPath());

            assertFalse("l1 should not exist after move (l1 -> l2)", l1.exists());
            assertTrue("l2 should exist after move (l1 -> l2)", l2.exists());

            // Now try to move back to l1 if l1 already exists
            l1.create();
            assertTrue("l1 should exist after create()", l1.exists());

            try {
                System.out.println("--- move l2 to existing (NOT empty) l1 ");
                l2.moveTo(l1);
                fail("Should have thrown when trying to move to an existing file");
            } catch (ExistingResourceException er) {
                // expected.. YAY!
            }

            System.out.printf("%s\t%s%n", l1.toRepositoryPath(), l1.exists());
            System.out.printf("%s\t%s%n", l2.toRepositoryPath(), l2.exists());

            assertTrue("l1 should exist after failed move (l2->existing l1)", l1.exists());
            assertTrue("l2 should exist after failed move (l2->existing l1)", l2.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMoveToDirectory() throws IOException {
        String m = "testMoveToDirectory";
        String dirName = m + "/";

        LocalFileResource n1 = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "1/", null);
        LocalFileResource n1a = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "1/a/b", null);

        LocalFileResource n2 = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "2/", null);
        LocalFileResource n2a = LocalFileResource.newResource(NORMALIZED_ROOT + dirName + "2/a/b", null);

        try {
            n1a.create();
            assertTrue("n1a should exist after create()", n1a.exists());

            System.out.printf("%s\t%s%n", n1.exists(), n1.toRepositoryPath());
            System.out.printf("%s\t%s%n", n1a.exists(), n1a.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2.exists(), n2.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2a.exists(), n2a.toRepositoryPath());

            System.out.println("--- move n1 to n2 ");
            n1.moveTo(n2);

            System.out.printf("%s\t%s%n", n1.exists(), n1.toRepositoryPath());
            System.out.printf("%s\t%s%n", n1a.exists(), n1a.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2.exists(), n2.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2a.exists(), n2a.toRepositoryPath());

            assertFalse("n1 should not exist after move (n1->n2)", n1.exists());
            assertFalse("n1a should not exist after move (n1a->n2)", n1a.exists());
            assertTrue("n2 should exist after move (n1->n2)", n2.exists());
            assertTrue("n2a should exist after move (n1->n2)", n2a.exists());

            n1.create();
            assertTrue("n1 should exist after create()", n1.exists());

            try {
                System.out.println("--- move n1 to existing (NOT empty) n2 ");
                n2.moveTo(n1);
                fail("Should have thrown when trying to move to a directory that exists");
            } catch (ExistingResourceException er) {
                // expected.. YAY!
            }

            System.out.printf("%s\t%s%n", n1.exists(), n1.toRepositoryPath());
            System.out.printf("%s\t%s%n", n1a.exists(), n1a.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2.exists(), n2.toRepositoryPath());
            System.out.printf("%s\t%s%n", n2a.exists(), n2a.toRepositoryPath());

            assertTrue("n1 should exist after failed move  (n2->existing n1)", n1.exists());
            assertFalse("n1a should not exist after failed move (n2->existing n1)", n1a.exists());
            assertTrue("n2 should exist (n2->existing n1)", n2.exists());
            assertTrue("n2a should exist after failed move (n2->existing n1)", n2a.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testPutChannel() throws IOException {
        String name = "testPutChannel";

        LocalFileResource resource = LocalFileResource.newResource(NORMALIZED_ROOT + name, null);
        resource.create();

        assertTrue("Created resource should exist", resource.exists());
        assertTrue("Created resource should be a file", resource.isType(WsResource.Type.FILE));

        DummyInputStream dis = new DummyInputStream(new int[] { 1 }, new int[] { 1, 0 });

        resource.put((ReadableByteChannel) dis);

        assertEquals("available should not have been called", 0, dis.available_i);
        assertEquals("read should have been called two times", 2, dis.read_i);
        assertEquals("close should not have been called", 0, dis.close_i);
        assertTrue("Resource should exist after put", resource.exists());

        InputStream is = resource.get();
        assertEquals("InputStream should contain 1 byte", 1, is.available());
        is.close();
    }

    @Test
    public void testGetPutFileChannel() throws Exception {
        String fname1 = "testGetPut";
        String fname2 = "testGetPut2";

        LocalFileResource resource = LocalFileResource.newResource(NORMALIZED_ROOT + fname1, null);
        LocalFileResource resource2 = LocalFileResource.newResource(NORMALIZED_ROOT + fname2, null);

        Properties outP = new Properties();
        outP.put("key", "value");
        outP.put("key2", "value2");

        // Store contents in resource/file1
        OutputStream outStream = resource.putStream();
        outP.store(outStream, "Try to store properties into resource");
        outStream.close();

        // Use a channel to transfer contents from resource to resource2
        ReadableByteChannel inChannel = resource.getChannel();
        resource2.put(inChannel);
        inChannel.close();

        // read from resource2
        Properties inP = new Properties();
        InputStream inStream2 = resource2.get();
        inP.load(inStream2);
        inStream2.close();

        assertEquals(outP, inP);

        resource.delete();
        assertFalse("Resource does not exist post-test", resource.exists());
        resource2.delete();
        assertFalse("Resource does not exist post-test", resource2.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#resolveRelative(String)}
     */
    @Test
    public void testResolveWithNull() throws Exception {
        assertNull("Can not resolve relative with no URI", commonFile.resolveRelative(null));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#resolveRelative(String)}
     */
    @Test
    public void testResolveNoRoot() throws Exception {
        assertNull("Can not resolve relative when no root", commonFile.resolveRelative("relative"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.LocalFileResource#resolveRelative(String)}
     */
    @Test
    public void testResolve() throws Exception {
        String rootName = NORMALIZED_ROOT + "testResolve/";
        String homeName = "home";
        String peerName = "peer";
        String parentName = "parent/";
        String gcName = "parent/child";

        try {
            SymbolicRootResource root = new SymbolicRootResource(rootName, "A", null);

            // Navigate from home to peer, parent, parent/child
            LocalFileResource c_res = LocalFileResource.newResource(rootName + homeName, null, root);

            URI a = c_res.toExternalURI();
            URI b = new URI(null, null, peerName, null);
            URI c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource p_res = (InternalWsResource) c_res.resolveRelative(peerName);
            assertNotNull("Peer resource should be returned: c_res.resolve(" + peerName + ")", p_res);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Peer location should match calculated URI", c.getPath().endsWith(p_res.getNormalizedPath()));
            assertEquals("Peer repository path: c_res.resolve(peer) should be ${A}/" + peerName, "${A}/" + peerName, p_res.toRepositoryPath());

            b = new URI("./peer");
            c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource p_res2 = (InternalWsResource) c_res.resolveRelative("./peer");
            assertNotNull("Peer resource should be returned: c_res.resolve(./peer)", p_res2);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Peer location should match calculated URI: " + c.getPath() + " = " + p_res2.getNormalizedPath(), c.getPath().endsWith(p_res2.getNormalizedPath()));
            assertEquals("Peer repository path: c_res.resolve(peer) should be ${A}/" + peerName, "${A}/" + peerName, p_res2.toRepositoryPath());

            b = new URI(parentName);
            c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource p_res3 = (InternalWsResource) c_res.resolveRelative(parentName);
            assertNotNull("Parent resource should be returned: c_res.resolve(" + parentName + ")", p_res3);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Parent location should match calculated URI: " + c.getPath() + " = " + p_res3.getNormalizedPath(), c.getPath().endsWith(p_res3.getNormalizedPath()));
            assertEquals("Parent repository path: c_res.resolve(peer) should be ${A}/" + parentName, "${A}/" + parentName, p_res3.toRepositoryPath());

            b = new URI(gcName);
            c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource gc_res = (InternalWsResource) c_res.resolveRelative(gcName);
            assertNotNull("Child resource should be returned: c_res.resolve(" + gcName + ")", gc_res);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Child location should match calculated URI: " + c.getPath() + " = " + gc_res.getNormalizedPath(), c.getPath().endsWith(gc_res.getNormalizedPath()));
            assertEquals("Child repository path: c_res.resolve(peer) should be ${A}/" + gcName, "${A}/" + gcName, gc_res.toRepositoryPath());

            // Navigate from child...
            a = gc_res.toExternalURI();
            b = new URI("..");
            c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource gc_res_p = (InternalWsResource) gc_res.resolveRelative("..");
            assertNotNull("Root resource should be returned (child is a file): gc_res.resolve(..)", gc_res_p);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Root location should match calculated URI: " + c.getPath() + " = " + gc_res_p.getNormalizedPath(), c.getPath().endsWith(gc_res_p.getNormalizedPath()));
            assertEquals("Location should match symbolic root", root, gc_res_p);

            b = new URI(".");
            c = a.resolve(b);
            System.out.format("%s, %s -> %n \t %s %n", a, b, c);

            InternalWsResource gc_res_p1 = (InternalWsResource) gc_res.resolveRelative(".");
            assertNotNull("Parent resource should be returned (child is a file): gc_res.resolve(.)", gc_res_p1);
            // on windows, c.getPath() will return a path prefixed with a '/'...
            assertTrue("Parent location should match calculated URI: " + c.getPath() + " = " + gc_res_p1.getNormalizedPath(), c.getPath().endsWith(gc_res_p1.getNormalizedPath()));
            assertEquals("Parent location should match previous value", p_res3, gc_res_p1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testResolve", t);
        }
    }

}
