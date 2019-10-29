//package com.ibm.ws.request.probe.setup;
//
//import junit.framework.Test;
//
//import com.ibm.websphere.simplicity.Topology;
//import com.ibm.ws.fat.ExpensiveTestSetup;
//
//public class RequestProbeSetup extends ExpensiveTestSetup {
//
//    /**
//     * Required for compatibility with JUnit/Phased JUnit/command-line invocation of TestSetup.
//     *
//     * @param test
//     *            The Test or TestSuite to enclose with this test fixture
//     */
//    public RequestProbeSetup(Test test) {
//        super(test);
//    }
//
//    /**
//     * The amount of nodes the the cell must have for this test bucket to execute.
//     *
//     */
//    @Override
//    public int requiredNodesPerCell() {
//        return 1;
//    }
//
//    /**
//     * Provides the amount of time that the harness waits before starting tests. The harness will wait this long to allow the environment to settle.
//     *
//     */
//    @Override
//    public long getStartUpWaitTime() {
//        return TWENTY_SECONDS;
//    }
//
//    /**
//     * Setup resources which the test bucket requires, including the creation of application servers and installation of test applications.
//     *
//     */
//    @Override
//    public void setUpResources() throws Exception {
//        // Simplicity in Liberty at the moment sets up just one server located in publish/servers
//        Topology.init();
//
//    }
//
//    @Override
//    public void tearDownResources() throws Exception {
//        super.tearDownResources();
//
//    }
//
//    @Override
//    public boolean skipTest() {
//
//        return false;
//
//    }
//
//    @Override
//    public void startResources() throws Exception {
//        // We don't want to start resources, they will need to be bounced in legacy setup anyways.
//        // Also see issues trying to start both back to back using standard harness framework methods on slower z/OS
//        // systems.
//    }
//
//}
