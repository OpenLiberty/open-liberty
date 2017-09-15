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
//%Z% %I% %W% %G% %U% [%H% %T%]
/*
 * Change History:
 *
 * Reason         Version  Date        User id     Description
 * --------------------------------------------------------------------------------
 * F000896.23216	8.0		06/11/2010	shighbar	Add in removal of excess servers in topology to conserve resources & keep more legacy log history for legacy test cases PD.
 * 681388			8.0		12/07/2010	shighbar	Override startup of server resources to correct issues seen in z/OS and make bucket efficient.
 */
package com.ibm.ws.fat.hpel.setup;

import java.util.List;
import java.util.Vector;

import junit.framework.Test;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.Cell;
import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.Server;
import com.ibm.websphere.simplicity.Topology;
import com.ibm.websphere.simplicity.WebSphereVersion;
import com.ibm.ws.fat.ExpensiveTestSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;
import com.ibm.ws.fat.util.CommonActions;

/**
 * Setups key resources for HPEL test cases.
 * 
 * @author Scott Highbarger
 * 
 */
public class HpelSetup extends ExpensiveTestSetup {

    /**
     * The name of the server configured by this fixture
     */
    public static final String SERVER_NAME = "HpelServer";
    public static final String SERVER2_NAME = "HpelServer2";

    /**
     * The name of the application configured by this fixture
     */
    public static final String APP_NAME = "HpelFat";

    /**
     * The name of the EAR file for the application.
     */
    public static final String EAR_NAME = "HpelFatEAR.ear";

    /**
     * Required for compatibility with JUnit/Phased JUnit/command-line invocation of TestSetup.
     * 
     * @param test
     *            The Test or TestSuite to enclose with this test fixture
     */
    public HpelSetup(Test test) {
        super(test);
    }

    /**
     * The amount of nodes the the cell must have for this test bucket to execute.
     * 
     */
    @Override
    public int requiredNodesPerCell() {
        return 1;
    }

    /**
     * Provides the amount of time that the harness waits before starting tests. The harness will wait this long to allow the environment to settle.
     * 
     */
    @Override
    public long getStartUpWaitTime() {
        return TWENTY_SECONDS;
    }

    /**
     * Setup resources which the test bucket requires, including the creation of application servers and installation of test applications.
     * 
     */
    @Override
    public void setUpResources() throws Exception {
        // Simplicity in Liberty at the moment sets up just one server located in publish/servers
        Topology.init();

//        // initialize common variables
//        WsadminLib wsadminlib = new WsadminLib(getCellUnderTest());
//
//        ApplicationServer server = CommonActions.removeAndCreateApplicationServer(wsadminlib, getNodeUnderTest(), SERVER_NAME);
//        SimpleApplication hpelApp = new SimpleApplication();
//        hpelApp.setName(APP_NAME);
//        hpelApp.setEar(new File(Props.getProperty(Props.DIR_BUILD_EARS), EAR_NAME));
//        hpelApp.install(server);
//
//        //Some test cases will require two servers.
//        ApplicationServer server2 = CommonActions.removeAndCreateApplicationServer(wsadminlib, getNodeUnderTest(), SERVER2_NAME);
//        SimpleApplication hpelApp2 = new SimpleApplication();
//        hpelApp2.setName(APP_NAME + "2");
//        hpelApp2.setEar(new File(Props.getProperty(Props.DIR_BUILD_EARS), EAR_NAME));
//        hpelApp2.install(server2);
//
//        // Remove other servers in topology to conserve resources.
//        Set<Server> keepServers = new HashSet<Server>();
//        keepServers.add(server);
//        keepServers.add(server2);
//        CommonTasks.removeUnusedServers(getCellUnderTest(), keepServers);
//
//        //Expand the number of historical logs to keep for legacy server side tests.
//        wsadminlib.setServerSysout(server.getNodeName(), server.getName(), 5, 25);
//
//        // save and synchronize changes
//        wsadminlib.save();
    }

    /**
     * Intended to return a list of all the servers which are being tested. At this time this method will only return a
     * single server, and it equivalent to calling the getServerUnderTest() method.
     * 
     */
    @Override
    public List<? extends Server> getServersUnderTest() throws Exception {
        Vector<Server> myServers = new Vector<Server>(0, 1);
        myServers.addElement(getServerUnderTest());
        Server secondServer = getServer2UnderTest();
        if (secondServer != null) {
            myServers.addElement(secondServer); // the 2nd server configured by setUpResources
        }
        return myServers;
    }

    /**
     * Clean up resources created by the test bucket.
     * 
     */
    @Override
    public void tearDownResources() throws Exception {
        super.tearDownResources();
        // WsadminLib wsadminlib = new WsadminLib(getCellUnderTest());
        // CommonActions.deleteServer(wsadminlib, getNodeUnderTest(), SERVER_NAME);
        // wsadminlib.save();
    }

    /**
     * Override the default requiredWebSphereVersion as HPEL currently requires and is only supported on WAS version 8.
     * 
     */
    @Override
    public WebSphereVersion requiredWebSphereVersion() {
        return new WebSphereVersion("8.0");
    }

    /**
     * Retrieve a reference to the Cell configured by this test fixture.
     * 
     * @return a reference to the Cell chosen for configuration of this test fixture
     * @throws Exception
     *             if a Simplicity error occurs
     */
    public static Cell getCellUnderTest() throws Exception {
        return Topology.getCells().get(0); // first cell in bootstrapping.properties
    }

    /**
     * Retrieve a reference to the Node configured by this test fixture.
     * 
     * @return a reference to the Node chosen for configuration of this test fixture
     * @throws Exception
     *             if a Simplicity error occurs
     */
    public static Node getNodeUnderTest() throws Exception {
        Cell cell = getCellUnderTest();
        return CommonActions.getSortedDefaultNodes(cell).get(0); // first Node by alphabetized name
    }

    /**
     * Retrieve a reference to the ApplicationServer configured by this test fixture.
     * 
     * @return a reference to the server named SERVER_NAME
     * @throws Exception
     *             if a Simplicity error occurs
     */
    public static ApplicationServer getServerUnderTest() throws Exception {
        Node node = getNodeUnderTest();
        Server server = node.getServerByName(SERVER_NAME);
        return server == null ? null : new HPELApplicationServer(server); // the server configured by setUpResources
    }

    /**
     * Retrieve a reference to the ApplicationServer configured by this test fixture.
     * 
     * @return a reference to the server named SERVER_NAME
     * @throws Exception
     *             if a Simplicity error occurs
     */
    public static ApplicationServer getServer2UnderTest() throws Exception {
        Node node = getNodeUnderTest();
        Server server = node.getServerByName(SERVER2_NAME);
        return server == null ? null : new HPELApplicationServer(server); // the 2nd server configured by setUpResources
    }

    @Override
    public boolean skipTest() {
        /*
         * May want to look into using this method, or calling super.skipTest() and overriding some of these methods:
         * requiredCells() requiredNodesPerCells() requiredWebSphereVersion() requiredWebSphereEdition()
         * requiredOperatingSystems()
         * 
         * etc. so that certain tests are only ran on z/OS or certain levels etc. as needed.
         */
        // There's not MixedCell topologies in Liberty yet.
        return false;

//        // HpelSetup is not able to work on MixedCell topologies at this time.
//        CommonTasks.writeLogMsg(Level.INFO, "Checking if we should skip HpelSetup or not.");
//        try {
//            return MixedCellTopologyHelper.isTopologyMixed();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return true; // Don't execute if we can't tell what topology we are on.
//        }
    }

    /**
     * Ensures that all the resources needed by your test fixture are running.
     * This step typically involves starting a series of servers, so the
     * "getServersUnderTest" method aims to accommodate this step. By default,
     * the startResources method calls getServersUnderTest to find a list of
     * servers, and then it starts that list of servers.
     * 
     * @throws Exception If a anything goes wrong
     */
    @Override
    public void startResources() throws Exception {
        // We don't want to start resources, they will need to be bounced in legacy setup anyways.
        // Also see issues trying to start both back to back using standard harness framework methods on slower z/OS
        // systems.
    }

    /**
     * Version of HPELApplicationServer which copies log records into logs/messages.log file to
     * be recognized as a started server.
     */
    private static class HPELApplicationServer extends ApplicationServer {

        protected HPELApplicationServer(Server delegate) throws Exception {
            super(delegate.getBackend(), delegate.getNode());
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.websphere.simplicity.ApplicationServer#start()
         */
        @Override
        public void start() throws Exception {
            // If HPEL is enabled we need start a thread copying log messages from repository into messages.log
            if (CommonTasks.isHpelEnabled(this)) {
                //88513 - Reading HPEL logs (HpelSetup.TextCopyThread code) get intermixed with the thread deleting 'logs' directory 
                //before starting the server (LibertyServer.preStartServerLogsTidy() code executed when we call super.start() in HpelSetup.start() method).
                //To avoid such problem its better to delete 'logs' directory ourself in HpelSetup.start() method before starting our reading thread. 
                //This way LibertyServer.preStartServerLogsTidy() will notice that 'logs' directory is gone already and won't do anything at all.
                getBackend().deleteFileFromLibertyServerRoot("logs");
            }
            super.start();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.websphere.simplicity.ApplicationServer#start(int)
         */
        @Override
        public void start(int mbeanWaitDuration) throws Exception {
            this.start();
        }

    }

}
