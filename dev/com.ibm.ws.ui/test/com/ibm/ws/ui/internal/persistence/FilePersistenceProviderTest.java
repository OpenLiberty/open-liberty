/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * This test DOES back-end to disk. However it does so under the build directory
 * so any effects this has will be resolved with a clean.
 */
public class FilePersistenceProviderTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WsLocationAdmin> locRef = mock.mock(ServiceReference.class);
    private final WsLocationAdmin loc = mock.mock(WsLocationAdmin.class);
    private final WsResource resource = mock.mock(WsResource.class);

    private static final String TEST_DATA_DIR_NAME = "build/libs/testdata/";
    private static final File TEST_DATA_DIR_FILE = new File(TEST_DATA_DIR_NAME);
    private static final String LAST_MODIFIED_TARGET = "lastModified";
    private static final String TOOLDATA_TARGET = "tooldata";
    private static final long LAST_MODIFIED_CREATE_TIME = getRoundedDownTime();
    private static final File LAST_MODIFIED_FILE = new File(TEST_DATA_DIR_NAME + LAST_MODIFIED_TARGET + ".json");
    private static final File TOOLDATA_TARGET_FILE = new File(TEST_DATA_DIR_NAME + TOOLDATA_TARGET + ".json");
    private static final String fakeContent = "fakeme";
    private static final String fakeContent2 = "Anotherfakeme";

    private FilePersistenceProvider persist;

    /**
     * Gets the time of the current second (dropping the millisecond precision).
     * We do this to work around a JVM problem with getting file timestamps on Linux & JDK 6.
     * 
     * @return The seconds since epoch
     */
    private static long getRoundedDownTime() {
        long t = new Date().getTime();
        long roundedT = t = t - (t % 1000L);
        System.out.println("Computed rounded down time: " + roundedT + ", was " + t);
        return roundedT;
    }

    /**
     * Create the initial working area for this test.
     */
    @BeforeClass
    public static void setupFiles() throws IOException {
        if (TEST_DATA_DIR_FILE.exists()) {
            assertTrue("FAIL: " + TEST_DATA_DIR_FILE.getAbsolutePath() + " must be a directory",
                       TEST_DATA_DIR_FILE.isDirectory());
        } else {
            assertTrue("FAIL: Could not create " + TEST_DATA_DIR_FILE.getAbsolutePath() + " directory",
                       TEST_DATA_DIR_FILE.mkdir());
        }

        assertTrue("FAIL: Could not create new last modified file: " + LAST_MODIFIED_FILE.getAbsolutePath(),
                   LAST_MODIFIED_FILE.createNewFile());
        FileUtils.writeStringToFile(LAST_MODIFIED_FILE, fakeContent, "UTF-8");

        assertEquals("FAIL: Could not write to file.", FileUtils.readFileToString(LAST_MODIFIED_FILE, "UTF-8"),
                     fakeContent);

        assertTrue("FAIL: Could not create new tool data file: " + TOOLDATA_TARGET_FILE.getAbsolutePath(),
                   TOOLDATA_TARGET_FILE.createNewFile());
        FileUtils.writeStringToFile(TOOLDATA_TARGET_FILE, fakeContent, "UTF-8");

        assertEquals("FAIL: Could not write to file.", FileUtils.readFileToString(TOOLDATA_TARGET_FILE, "UTF-8"),
                     fakeContent);
    }

    /**
     * Activate the service using with mocks.
     */
    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(FilePersistenceProvider.KEY_LOCATION_SERVICE, locRef);
                will(returnValue(loc));
            }
        });

        persist = new FilePersistenceProvider();
        persist.setLocationService(locRef);
        persist.activate(cc);
    }

    /**
     * Deactive the service.
     */
    @After
    public void tearDown() {
        persist.deactivate();
        persist.unsetLocationService(locRef);
        persist = null;

        mock.assertIsSatisfied();
    }

    /**
     * Perform test disk clean up.
     */
    @AfterClass
    public static void removeFiles() {
        assertTrue("FAIL: expected an error because of the null JSON service",
                   outputMgr.isEmptyMessageLog());

        if (LAST_MODIFIED_FILE.exists())
        {
            assertTrue("FAIL: Could not delete last modified file: " + LAST_MODIFIED_FILE.getAbsolutePath(),
                       LAST_MODIFIED_FILE.delete());
        }

        if (TOOLDATA_TARGET_FILE.exists())
        {
            assertTrue("FAIL: Could not delete tool data file: " + TOOLDATA_TARGET_FILE.getAbsolutePath(),
                       TOOLDATA_TARGET_FILE.delete());
        }
    }

    private void setMockFileLocation(final String name) {
        final File f = new File("build/libs/testdata/" + name + ".json");

        mock.checking(new Expectations() {
            {
                one(loc).resolveResource(FilePersistenceProvider.DEFAULT_PERSIST_LOCATION + name + ".json");
                will(returnValue(resource));

                one(resource).asFile();
                will(returnValue(f));
            }
        });
    }

    /**
     * We do not need to handle multiple error paths since the underlying
     * services do that for us.
     */
    @Test
    public void storeAndLoad() throws Exception {
        setMockFileLocation("testObject");

        String testObject = "myTestObject";

        try {
            persist.store("testObject", testObject);
            String loadedTestObject = persist.load("testObject", String.class);

            // Check a very basic property of the stored catalog. We can rely
            // on the POJOs being JSONable and that Jackson did the right thing.
            assertEquals("FAIL: loaded Object did not match the expected Object",
                         "myTestObject", loadedTestObject);
        } catch (IOException e) {
            assertTrue("JSON OSGi service won't be available for a unittest", e.getMessage().contains("CWWKX1066E"));
        }
    }

    @Test
    public void lastModified() throws Exception {
        setMockFileLocation(LAST_MODIFIED_TARGET);

        long lastModified = persist.getLastModified(LAST_MODIFIED_TARGET);
        assertTrue("FAIL: lastModified should be equal or greater than the time of creation. Saw: " + lastModified + ", expected: " + LAST_MODIFIED_CREATE_TIME,
                   lastModified >= LAST_MODIFIED_CREATE_TIME);
    }

    @Test
    public void lastModifiedDoesntExist() throws Exception {
        setMockFileLocation("idontexist");

        assertEquals("FAIL: lastModified should be zero when the target does not exist",
                     0L, persist.getLastModified("idontexist"));
    }

    /**
     * Tests the load, store, exists and delete method of FilePersistenceProvider.
     * The loadPlainText requires the file to be existed, if delete runs first, then the loadPlainText would fail.
     * Thus, put all the tests in a single method so that the run sequence is guaranteed.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadStoreExistsDelete() throws Exception
    {
        setMockFileLocation(TOOLDATA_TARGET);
        // Load test, the file already exists (setup step).
        loadPlainText();
        // Store new content.
        storePlainText();
        // Check if the file exists.
        exists();
        // Delete the file.
        delete();
    }

    private void loadPlainText() throws Exception {
        String s = persist.loadPlainText(TOOLDATA_TARGET);
        assertEquals("FAIL: loadPlainText should match.", fakeContent, s);
    }

    private void storePlainText() throws Exception {
        persist.storePlainText(TOOLDATA_TARGET, fakeContent2);
        assertEquals("FAIL: storePlainText is not correct.", FileUtils.readFileToString(TOOLDATA_TARGET_FILE, "UTF-8"),
                     fakeContent2);
    }

    private void exists() throws Exception {
        boolean e = persist.exists(TOOLDATA_TARGET);
        assertTrue("FAIL: exists failed.", e);
    }

    private void delete() throws Exception {
        boolean e = persist.delete(TOOLDATA_TARGET);
        assertTrue("FAIL: Delete failed.", e);
    }
}
