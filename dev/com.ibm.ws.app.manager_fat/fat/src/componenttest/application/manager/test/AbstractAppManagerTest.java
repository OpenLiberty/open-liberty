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
package componenttest.application.manager.test;

import static com.ibm.websphere.simplicity.ShrinkHelper.addDirectory;
import static com.ibm.websphere.simplicity.ShrinkHelper.buildDefaultApp;

import java.io.FileNotFoundException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyFileManager;

/**
 *
 */
public abstract class AbstractAppManagerTest {

    protected static final String DERBY_DIR = "derby";
    protected static final String DROPINS_DIR = "dropins(fish)";
    //the time in seconds to wait for apps at a URL before giving up
    protected static final int CONN_TIMEOUT = 30;

    protected final static String PUBLISH_FILES = "publish/files";
    protected final static String PUBLISH_UPDATED = PUBLISH_FILES + "/updatedApplications";

    // Applications
    protected static final String TEST_WAR_APPLICATION = "testWarApplication.war";
    protected static final String APP_J2EE_EAR = "app-j2ee.ear";
    protected static final String SNOOP_WAR = "snoop.war";
    protected static final String DATA_SOURCE_APP_EAR = "DataSourceApp.ear";
    protected static final String SLOW_APP = "slowapp.war";

    // Bundles
    protected static final String BUNDLE_NAME = "test.app.notifications.jar";

    protected abstract Class<?> getLogClass();

    /**
     * Returns true if the file is successfully deleted or it doesn't exist
     *
     * @param machine
     * @param fileToDeleteAbsPath
     */
    protected boolean deleteFile(Machine machine, String fileToDeleteAbsPath) throws Exception {
        final String method = "deleteFile";
        Log.info(getLogClass(), method, "Deleting file '" + fileToDeleteAbsPath + "\'");
        try {
            RemoteFile fileToDelete = LibertyFileManager.getLibertyFile(machine, fileToDeleteAbsPath);
            if (fileToDelete.exists()) {
                boolean deleted = fileToDelete.delete();
                if (!deleted) {
                    Log.info(getLogClass(), method, "File \'" + fileToDeleteAbsPath
                                                    + "\' was not able to be deleted");
                }
                return deleted;
            } else {
                Log.info(getLogClass(), method, "File \'" + fileToDeleteAbsPath
                                                + "\' does not exist so cannot be deleted");
                return true;
            }
        } catch (FileNotFoundException e) {
            Log.info(getLogClass(), method, "File \'" + fileToDeleteAbsPath
                                            + "\' does not exist so cannot be deleted");
            return true;
        }

    }

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war1 = buildDefaultApp("test-web.war", "web.*");
        addDirectory(war1, "test-applications/app-j2ee.ear/common");
        addDirectory(war1, "test-applications/app-j2ee.ear/test-web.war/resources");

        WebArchive war2 = buildDefaultApp("test-web1.war", "web.*");
        addDirectory(war2, "test-applications/app-j2ee.ear/common");
        addDirectory(war2, "test-applications/app-j2ee.ear/test-web1.war/resources");

        EnterpriseArchive j2eeApp = ShrinkWrap.create(EnterpriseArchive.class, "app-j2ee.ear").addAsModule(war1).addAsModule(war2);
        j2eeApp = (EnterpriseArchive) addDirectory(j2eeApp, "test-applications/app-j2ee.ear/startResources");

        // Copy app-j2ee.ear to publish/files
        ShrinkHelper.exportArtifact(j2eeApp, PUBLISH_FILES, true, true);

        // Add another WAR and save the file to updatedApplications
        WebArchive war3 = buildDefaultApp("test-web3.war", "com.ibm.ws.app.manager.fat.*");
        addDirectory(war3, "test-applications/app-j2ee.ear/test-web3.war/resources");

        EnterpriseArchive updated = j2eeApp.addAsModule(war3);
        updated = (EnterpriseArchive) addDirectory(updated, "test-applications/app-j2ee.ear/updatedResources");

        ShrinkHelper.exportArtifact(updated, PUBLISH_UPDATED, true, true);

        // testWarApplication.war
        WebArchive testWar = buildDefaultApp("testWarApplication.war", "test.simple.war");
        addDirectory(testWar, "test-applications/testWarApplication.war/startResources");
        ShrinkHelper.exportArtifact(testWar, PUBLISH_FILES, true, true);

        // updated testWarApplication.war
        WebArchive testWarAppUpdated = buildDefaultApp("testWarApplication.war", "test.simple.war");
        addDirectory(testWarAppUpdated, "test-applications/testWarApplication.war/updatedResources");
        ShrinkHelper.exportArtifact(testWarAppUpdated, PUBLISH_UPDATED, true, true);

        // Snoop
        WebArchive snoop = buildDefaultApp("snoop.war", "com.ibm.app.monitor", "com.ibm.ws.security.web.saml.sample");
        addDirectory(snoop, "test-applications/snoop.war/resources");
        ShrinkHelper.exportArtifact(snoop, PUBLISH_FILES, true, true);

        // DataSourceApp
        JavaArchive ejb = ShrinkHelper.buildJavaArchive("DataSourceEJB.jar", "com.ibm.ws.app.manager.fat.datasource.ejb.*");
        EnterpriseArchive dataSourceApp = ShrinkWrap.create(EnterpriseArchive.class, "DataSourceApp.ear").addAsModule(ejb);
        dataSourceApp = (EnterpriseArchive) addDirectory(dataSourceApp, "test-applications/DataSourceApp.ear/resources");
        ShrinkHelper.exportArtifact(dataSourceApp, PUBLISH_FILES, true, true);

        //Slow App
        WebArchive slow = buildDefaultApp("slowapp.war", "test.app.*");
        addDirectory(slow, "test-applications/slowapp.war/resources");
        ShrinkHelper.exportArtifact(slow, PUBLISH_FILES, true, true);

    }

}
