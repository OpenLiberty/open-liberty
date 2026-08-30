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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for updating app class binaries via MBean trigger.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class UpdatingAppClassesViaMBeanFatTest extends AbstractUpdatingAppClassesFatTest {
    private final static Class<?> CLASS = UpdatingAppClassesViaMBeanFatTest.class;
    private JMXConnector jmxConnector;
    private MBeanServerConnection mbsc;
    private ObjectName fileMonitorMBeanName;
    private final static String[] MBEAN_METHOD_SIGNATURE = new String[] { Collection.class.getName(),
                                                                         Collection.class.getName(),
                                                                         Collection.class.getName() };

    static {
        Log.info(CLASS, "<clinit>", "My Classpath: " + System.getProperty("java.class.path"));
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(CLASS, "beforeClass", "entry");

        EnterpriseArchive ear = UpdatingAppClassesFatTest.buildUpdateableApp();

        // looseConfigServer: deploy EAR exploded into loose/ dirs, use loose XML as app descriptor
        looseConfigServer = LibertyServerFactory.getLibertyServer("classloader_updateable", null, true);
        looseConfigServer.setServerConfigurationFile("Updateable/server.xml.mBeanTrigger");
        looseConfigServer.setupForRestConnectorAccess();
        UpdatingAppClassesFatTest.deployLoose(looseConfigServer, ear);
        looseConfigServer.addInstalledAppForValidation("updateableApp");
        looseConfigServer.startServer();

        // expandedAppServer: deploy EAR fully exploded on disk so updateFile() can rename into it
        expandedAppServer = LibertyServerFactory.getLibertyServer("classloader_updateable_expandedApp", null, true);
        expandedAppServer.setServerConfigurationFile("Updateable/server.xml.mBeanTrigger2");
        expandedAppServer.setupForRestConnectorAccess();
        UpdatingAppClassesFatTest.deployExpanded(expandedAppServer, ear);
        expandedAppServer.addInstalledAppForValidation("updateableApp");
        expandedAppServer.startServer();

        Log.info(CLASS, "beforeClass", "exit");
    }

    @Override
    protected void setUp(LibertyServer server) throws Exception {
        if (!server.isJava2SecurityEnabled()) {
            int jmxPort = testName.getMethodName().endsWith("loose") ? server.getHttpDefaultSecurePort() : server.getHttpSecondarySecurePort();
            jmxConnector = server.getJMXRestConnector(jmxPort);
            mbsc = jmxConnector.getMBeanServerConnection();
            fileMonitorMBeanName = new ObjectName("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean");
        }
        Log.info(CLASS, "setUp", "Successfully connected to JMX connector server and located the file monitor MBean");
    }

    @Override
    protected void tearDown(LibertyServer server) throws Exception {
        if (!server.isJava2SecurityEnabled()) {
            if (jmxConnector != null) {
                jmxConnector.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see io.openliberty.classloading.base.fat.AbstractUpdatingAppClassesFatTest#updateFile(java.lang.String, java.lang.String)
     */
    @Override
    protected void updateFile(LibertyServer server, String dest, String src) throws Exception {
        server.copyFileToLibertyServerRoot(dest, src);
        List<String> fullPathList = new ArrayList<String>();
        String modifyPath = server.getServerRoot() + "/" + dest + src.substring(src.lastIndexOf('/'));
        Log.info(CLASS, "updateFile", "modifyPath: " + modifyPath);
        fullPathList.add(modifyPath);
        Object[] params = new Object[] { null, fullPathList, null };
        mbsc.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);
    }

}
