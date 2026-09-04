/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class LibraryChangeListenerTest {

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("classloader_FAT_Server");

    @BeforeClass
    public static void setupClass() throws Exception {
        server.installSystemBundle("library.listener");
        server.installSystemFeature("library.listener-1.0");
        server.setServerConfigurationFile("LibraryChangeListener/server.xml.topLevel");
        server.startServer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server != null) {
            server.stopServer(true);
            server.uninstallSystemFeature("library.listener-1.0");
            server.uninstallSystemBundle("library.listener");
        }
    }

    @Before
    public void setup() throws Exception {
        server.setMarkToEndOfLog();
    }

    /**
     * Changes the filesystem content (i.e. JAR file)
     * of the library, and verifies we are notified.
     */
    @Test
    @Ignore
    // for now, since it seems to be failing - investigating...
    public void topLevelChangeFilesetContent() throws Throwable {
        testContentChange("LibraryChangeListener/server.xml.topLevel");
    }

    /**
     * Changes the config of the library to a
     * different file set, and verifies we are
     * notified.
     */
    @Test
    public void topLevelChangeFilesetDir() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.topLevel", "LibraryChangeListener/server.xml.topLevel-changeFilesetDir");
    }

    /**
     * Changes the config of the library to add
     * a new file set, and verifies we are
     * notified. Then deletes the old file set
     * and verifies we are notified.
     */
    @Test
    public void topLevelAddAndRemoveFileset() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.topLevel", "LibraryChangeListener/server.xml.topLevel-addNewFileset");
        testConfigChange("LibraryChangeListener/server.xml.topLevel-addNewFileset", "LibraryChangeListener/server.xml.topLevel-removedOldFileset");
    }

    /**
     * Changes the config of the library by
     * changing the ID of the library. The
     * runtime should handle this change by
     * removing the current instance of the
     * library, and creating a new one.
     */
    @Test
    public void topLevelChangeID() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.topLevel", "LibraryChangeListener/server.xml.topLevel-changeID");
    }

    /**
     * Changes the filesystem content (i.e. JAR file)
     * of the library, and verifies we are notified.
     */
    @Test
    @Ignore
    // for now, since it seems to be failing - investigating...
    public void embeddedElementChangeFilesetContent() throws Throwable {
        testContentChange("LibraryChangeListener/server.xml.embedded");
    }

    /**
     * Changes the config of the library to a
     * different file set, and verifies we are
     * notified.
     */
    @Test
    public void embeddedElementChangeFilesetDir() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.embedded", "LibraryChangeListener/server.xml.embedded-changeFilesetDir");
    }

    /**
     * Changes the config of the library to add
     * a new file set, and verifies we are
     * notified. Then deletes the old file set
     * and verifies we are notified.
     */
    @Test
    public void embeddedElementAddAndRemoveFileset() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.embedded", "LibraryChangeListener/server.xml.embedded-addNewFileset");
        testConfigChange("LibraryChangeListener/server.xml.embedded-addNewFileset", "LibraryChangeListener/server.xml.embedded-removedOldFileset");
    }

    /**
     * Changes the config of the library by
     * changing the ID of the library. The
     * runtime should handle this change by
     * removing the current instance of the
     * library, and creating a new one.
     */
    @Test
    public void embeddedElementChangeID() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.embedded-ID1", "LibraryChangeListener/server.xml.embedded-ID2");
    }

    /**
     * Changes the config of the library from
     * being embedded in another element (like
     * jdbcDriver, etc.) to being a top level
     * element.
     */
    @Test
    public void changeEmbeddedElementToTopLevel() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.embedded", "LibraryChangeListener/server.xml.topLevel");
    }

    /**
     * Changes the config of the library from
     * being a top level element in the server.xml
     * to being embedded inside another config
     * element (like jdbcDriver, app, etc.).
     */
    @Test
    public void changeTopLevelToEmbeddedElement() throws Exception {
        testConfigChange("LibraryChangeListener/server.xml.topLevel", "LibraryChangeListener/server.xml.embedded");
    }

    private void testConfigChange(final String config1, final String config2) throws Exception {
        server.setServerConfigurationFile(config1);
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config2);
        checkNotification();
    }

    private void testContentChange(String config) throws Throwable {
        Throwable throwable = null;
        try {
            server.setServerConfigurationFile(config);
            server.setMarkToEndOfLog();
            // verify that the addition of a file that meets the fileset criteria gets notified
            server.copyFileToLibertyServerRoot("SharedLibraryA", "testD.jar");
            checkNotification();
        } catch (Throwable t) {
            throwable = t;
        } finally {
            // if we've already failed, clean up and error out
            server.deleteFileFromLibertyServerRoot("SharedLibraryA/testD.jar");
            if (throwable != null) {
                throw throwable;
            } else {
                // otherwise, we need to verify that deleting the new jar also results in a notification
                checkNotification();
            }
        }
    }

    private void checkNotification() throws Exception {
        assertNotNull("No notification detected", server.waitForStringInLogUsingMark("MyLibraryChangeListener.libraryNotification"));
        server.setMarkToEndOfLog();
    }
}
