/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EventLoggingElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Verifies that EJB invocations produce correct event log entries for all four EJB event types
 * across three packaging scenarios:
 *
 * <ol>
 * <li>EJBEventApp.ear — EJB JAR inside EAR; contextInfo is 4-part
 * (appName | moduleName | beanName | methodName)</li>
 * <li>EJBEventWar.war — standalone WAR with embedded beans; contextInfo is
 * 3-part (moduleName | beanName | methodName)</li>
 * <li>EJBEventJar.jar — standalone EJB JAR; contextInfo is 3-part
 * (moduleName | beanName | methodName)</li>
 * </ol>
 *
 * <p><b>Timer coverage:</b>
 * <ul>
 * <li>EAR and WAR use PROGRAMMATIC single-action timers (triggerTimer())</li>
 * <li>Standalone JAR uses an AUTOMATIC &#64;Schedule timer that fires every 1s
 * and cancels itself after the first invocation</li>
 * </ul>
 *
 * <p><b>Async interface coverage:</b>
 * <ul>
 * <li>EAR: &#64;Asynchronous on LOCAL interface</li>
 * <li>WAR: &#64;Asynchronous on REMOTE interface</li>
 * <li>JAR: &#64;Asynchronous on REMOTE interface (required — JAR and its driver WAR
 * are separate applications with separate class loaders)</li>
 * </ul>
 *
 * <p><b>BEGIN/END pairing and request-ID correctness:</b>
 * For event types that run on their own thread (async, mdb, remote, timer) the tests assert
 * that exactly one BEGIN and one END entry exist for each invocation, that both entries carry
 * the same request ID, and that distinct invocations carry distinct request IDs.
 *
 * <p><b>Failure-path coverage:</b>
 * Three tests verify that event log entries are still produced correctly when a bean method
 * throws — both checked application exceptions and unchecked system exceptions — and that
 * the request context is cleaned up so that the next invocation receives a fresh request ID.
 *
 * <p><b>Invocations that must never be logged:</b>
 * The following are explicitly asserted to produce no event log entries, because the
 * request timing collaborator only instruments business-method dispatches on the component
 * object, not container-level or local-interface calls:
 * <ul>
 * <li>Local interface calls ({@code doLocal}, {@code doLocalObject})</li>
 * <li>EJB home methods ({@code home.create()}, {@code home.remove(Handle)})</li>
 * <li>Container accessor methods ({@code getHandle()})</li>
 * </ul>
 *
 * <p><b>eventTypes filter and logMode configuration:</b>
 * A group of tests reconfigures {@code <eventLogging>} dynamically and verifies that:
 * <ul>
 * <li>{@code logMode="entryExit"} produces both BEGIN and END entries (the server default
 * produces END entries only)</li>
 * <li>Setting {@code eventTypes} to a single EJB type (e.g. {@code websphere.ejb.async})
 * gates logging to exactly that type, with no entries for the other three types</li>
 * <li>Listing all four types explicitly is equivalent to {@code eventTypes="all"}</li>
 * <li>Removing the {@code <eventLogging>} element entirely suppresses all EJB event entries
 * (the servlet-only default applies)</li>
 * </ul>
 *
 * <p><b>Dynamic feature toggle under load:</b>
 * One FULL-mode test removes and re-adds the {@code eventLogging-1.0} feature several times
 * while ten EJB interval timers keep invocations continuously in flight. Each cycle verifies
 * that no event is logged after the feature is removed, events resume after it is re-added,
 * the application is never restarted, and no FFDC is recorded for failures in the request
 * timing collaborator hooks.
 *
 * <p>All tests run under EE7 by default and are repeated for EE8–EE11 in FULL mode
 * (subject to minimum Java version constraints).
 */
@RunWith(FATRunner.class)
public class EJBEventLoggingTest extends FATServletClient {

    private static final String SERVER = "EJBEventLoggingServer";

    @Server(SERVER)
    public static LibertyServer server;

    /*@formatter:off*/
    @ClassRule
    public static RepeatTests r = RepeatTests
        .with(FeatureReplacementAction.EE7_FEATURES().forServers(SERVER))
        .andWith(FeatureReplacementAction.EE8_FEATURES()
                     .fullFATOnly()
                     .forServers(SERVER))
        .andWith(FeatureReplacementAction.EE9_FEATURES()
                     .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)
                     .forServers(SERVER))
        .andWith(FeatureReplacementAction.EE10_FEATURES()
                     .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)
                     .forServers(SERVER))
        .andWith(FeatureReplacementAction.EE11_FEATURES()
                     .forServers(SERVER));
    /*@formatter:on*/

    // -----------------------------------------------------------------------
    // Event log pattern constants
    // -----------------------------------------------------------------------
    // The server.xml default <eventLogging> logs END entries only (logMode defaults to
    // "exit"); the tests that reconfigure <eventLogging> use logMode="entryExit" so
    // that both BEGIN and END entries are written to messages.log.
    //
    // Log line formats (see EventLoggingProbeExtension), with " # " between the fields:
    //   BEGIN requestID=<id> # eventType=<type> # contextInfo=<info>
    //   END requestID=<id> # eventType=<type> # contextInfo=<info> # duration=<n.nnn>ms
    // and <info> is "app | module | bean | method" for an EAR, "module | bean | method" otherwise.

    private static final String EAR_APP = "EJBEventApp";
    private static final String EAR_MODULE = "EJBEventEJB";
    private static final String WAR_MODULE = "EJBEventWar";
    private static final String JAR_MODULE = "EJBEventJar";

    private static final Pattern REQUEST_ID = Pattern.compile("requestID=(\\S+)");

    private static final String EJB_SERVLET_PATH = "EJBEventWeb/EJBEventServlet";
    private static final String EJB_WAR_SERVLET_PATH = "EJBEventWar/EJBEventWarServlet";
    private static final String EJB_JAR_SERVLET_PATH = "EJBEventJarWeb/EJBEventJarServlet";

    /** EJB event types that are timed. Local and local home invocations are never timed. */
    private static final List<String> EJB_TYPES = Arrays.asList("async", "mdb", "remote", "timer");

    private static final String ANY_EJB_EVENT = "(BEGIN|END) requestID=.*eventType=websphere\\.ejb";

    private static final String END_STRESS_TIMER = "END requestID=.*eventType=websphere\\.ejb\\.timer.*contextInfo=EJBEventApp.*StressTimerBean.*doStressTimeout";

    /** Application started, updated and stopped messages; none may be issued while features are toggled. */
    private static final String APP_LIFECYCLE_MESSAGES = "CWWKZ0001I|CWWKZ0003I|CWWKZ0009I";

    /** The container swallows collaborator failures but records an FFDC from these two methods. */
    private static final String COLLABORATOR_FFDC = "FFDC1015I.*EJSContainer\\.notifyRequestTiming";

    private static final int STRESS_CYCLES = 3;

    /** How long to watch for events after eventLogging has been removed. */
    private static final long QUIET_PERIOD_MS = 5000;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void setUp() throws Exception {
        // --- EJBEventApp.ear (EJB JAR + WAR inside EAR) ---
        // permissions.xml grants PropertyPermission for the Yoko ORB stub's System.getProperties()
        JavaArchive ejbJar = ShrinkHelper.buildJavaArchive("EJBEventEJB.jar",
                                                           "io.openliberty.event.logging.ejb.fat.ejb.ear.");
        WebArchive ejbWeb = ShrinkHelper.buildDefaultApp("EJBEventWeb.war",
                                                         "io.openliberty.event.logging.ejb.fat.web.ear.");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "EJBEventApp.ear");
        ear.addAsModule(ejbJar).addAsModule(ejbWeb);
        ear = (EnterpriseArchive) ShrinkHelper.addDirectory(ear, "test-applications/EJBEventApp.ear/resources");
        ShrinkHelper.exportAppToServer(server, ear, DeployOptions.SERVER_ONLY);

        // --- EJBEventWar.war (standalone WAR) ---
        WebArchive standaloneWar = ShrinkHelper.buildDefaultApp("EJBEventWar.war",
                                                                "io.openliberty.event.logging.ejb.fat.ejb.war.",
                                                                "io.openliberty.event.logging.ejb.fat.web.war.");
        standaloneWar = (WebArchive) ShrinkHelper.addDirectory(standaloneWar, "test-applications/EJBEventWar.war/resources");
        ShrinkHelper.exportAppToServer(server, standaloneWar, DeployOptions.SERVER_ONLY);

        // --- EJBEventJar.jar (standalone EJB JAR) + EJBEventJarWeb.war (thin driver) ---
        // Both include the shared api package so the remote interfaces are present
        // in each application's own class loader — no shared library needed.
        JavaArchive standaloneJar = ShrinkHelper.buildJavaArchive("EJBEventJar.jar",
                                                                  "io.openliberty.event.logging.ejb.fat.ejb.jar.",
                                                                  "io.openliberty.event.logging.ejb.fat.ejb.jar.api.");
        standaloneJar = (JavaArchive) ShrinkHelper.addDirectory(standaloneJar, "test-applications/EJBEventJar.jar/resources");
        ShrinkHelper.exportAppToServer(server, standaloneJar, DeployOptions.SERVER_ONLY);

        WebArchive jarDriverWar = ShrinkHelper.buildDefaultApp("EJBEventJarWeb.war",
                                                               "io.openliberty.event.logging.ejb.fat.web.jar.",
                                                               "io.openliberty.event.logging.ejb.fat.ejb.jar.api.");
        jarDriverWar = (WebArchive) ShrinkHelper.addDirectory(jarDriverWar, "test-applications/EJBEventJarWeb.war/resources");
        ShrinkHelper.exportAppToServer(server, jarDriverWar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // CNTR0020E: testEarRemoteSystemFailureEventLogged makes a bean method throw an unchecked exception
            server.stopServer("CNTR0020E");
        }
    }

    // -----------------------------------------------------------------------
    // EAR packaging tests (4-part contextInfo: EJBEventApp#EJBEventEJB#Bean#method)
    // -----------------------------------------------------------------------

    /**
     * Verifies that a {@code @Asynchronous} bean method invocation in an EAR produces exactly one
     * well-formed END event log entry of type {@code websphere.ejb.async} with the expected
     * 4-part EAR contextInfo ({@code EJBEventApp | EJBEventEJB | AsyncBean | doAsync}).
     */
    @Test
    public void testEarAsyncEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_SERVLET_PATH, "testEarAsyncEventLogged");
        assertSingleEnd("async", earContextInfo("AsyncBean", "doAsync"));
    }

    /**
     * Verifies that remote EJB invocations on both a stateless and a stateful bean in an EAR produce
     * exactly one well-formed END event log entry of type {@code websphere.ejb.remote} per business
     * method, with the expected 4-part EAR contextInfo. Also asserts that container-level methods
     * ({@code home.create()}, {@code getHandle()}) are never logged, and that exactly one
     * {@code remove} entry is produced when the stateful bean is removed via a home handle
     * (confirming the home method itself is not logged separately).
     */
    @Test
    public void testEarRemoteEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_SERVLET_PATH, "testEarRemoteEventLogged");
        // Stateless RemoteBean: doRemote + doRemoteObject are logged; home.create() and local calls are not
        assertSingleEnd("remote", earContextInfo("RemoteBean", "doRemote"));
        assertSingleEnd("remote", earContextInfo("RemoteBean", "doRemoteObject"));
        // Stateful StatefulRemoteBean: doRemote + doRemoteObject + the @Remove method doRemove are logged;
        // home.create(), getHandle() and local calls are not. home.remove(Handle) is not logged as a home
        // method, but it calls EJBObject.remove(), which is logged as a remote "remove" (see below).
        assertSingleEnd("remote", earContextInfo("StatefulRemoteBean", "doRemote"));
        assertSingleEnd("remote", earContextInfo("StatefulRemoteBean", "doRemoteObject"));
        assertSingleEnd("remote", earContextInfo("StatefulRemoteBean", "doRemove"));
        // home.create() must not appear for either bean
        assertEquals("home.create() must not be logged",
                     0, findInLog(ANY_EJB_EVENT + ".*\\bcreate\\b").size());
        // getHandle() is a container method (no application code involved)
        assertEquals("getHandle() must not be logged",
                     0, findInLog(ANY_EJB_EVENT + ".*\\bgetHandle\\b").size());
        // home.remove(Handle) is a home method and is not logged itself; it turns around and calls
        // EJBObject.remove() on the bean, which is logged. Exactly one "remove" entry means that the
        // home method added no entry of its own.
        assertSingleEnd("remote", earContextInfo("StatefulRemoteBean", "remove"));
    }

    /**
     * Verifies that a programmatic single-action timer in an EAR produces exactly one well-formed
     * END event log entry of type {@code websphere.ejb.timer} with the expected 4-part EAR
     * contextInfo ({@code EJBEventApp | EJBEventEJB | TimerBean | doTimeout}).
     */
    @Test
    public void testEarTimerEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_SERVLET_PATH, "testEarTimerEventLogged");
        assertSingleEnd("timer", earContextInfo("TimerBean", "doTimeout"));
    }

    /**
     * Verifies that a message-driven bean invocation in an EAR produces exactly one well-formed
     * END event log entry of type {@code websphere.ejb.mdb} with the expected 4-part EAR
     * contextInfo ({@code EJBEventApp | EJBEventEJB | MDBBean | onMessage}).
     */
    @Test
    public void testEarMdbEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_SERVLET_PATH, "testEarMdbEventLogged");
        assertSingleEnd("mdb", earContextInfo("MDBBean", "onMessage"));
    }

    // -----------------------------------------------------------------------
    // EAR tests where the bean method fails. The BEGIN and END must still be logged and paired,
    // and the request context must be cleaned up so that the next call gets a request ID of its own.
    // -----------------------------------------------------------------------

    /**
     * Verifies that when a remote EAR bean method throws a checked application exception, a paired
     * BEGIN and END are still logged with a consistent request ID, and that the next successful call
     * on the same bean receives a different request ID (i.e. the request context was properly cleaned
     * up after the failure).
     */
    @Test
    public void testEarRemoteFailureEventLogged() throws Exception {
        runFailureAndVerify("testEarRemoteFailureEventLogged", "remote", "RemoteBean", "doRemoteFail", "doRemote");
    }

    /**
     * The bean method throws an unchecked exception, which the container reports as a system exception:
     * it logs CNTR0020E (allowed when the server is stopped) and writes an FFDC for the exception
     * the bean threw.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void testEarRemoteSystemFailureEventLogged() throws Exception {
        runFailureAndVerify("testEarRemoteSystemFailureEventLogged", "remote", "RemoteBean", "doRemoteSystemFail", "doRemote");
    }

    /**
     * Verifies that when an {@code @Asynchronous} EAR bean method throws a checked application
     * exception (surfaced to the caller as an {@link java.util.concurrent.ExecutionException}),
     * a paired BEGIN and END are still logged with a consistent request ID, and that the next
     * successful call on the same bean receives a different request ID.
     */
    @Test
    public void testEarAsyncFailureEventLogged() throws Exception {
        runFailureAndVerify("testEarAsyncFailureEventLogged", "async", "AsyncBean", "doAsyncFail", "doAsync");
    }

    // -----------------------------------------------------------------------
    // Standalone WAR tests (3-part contextInfo: EJBEventWar#Bean#method)
    // -----------------------------------------------------------------------

    /**
     * Verifies that a {@code @Asynchronous} bean method invocation in a standalone WAR produces
     * exactly one well-formed END event log entry of type {@code websphere.ejb.async} with the
     * expected 3-part standalone contextInfo ({@code EJBEventWar | AsyncBeanInWar | doAsync}).
     */
    @Test
    public void testStandaloneWarAsyncEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_WAR_SERVLET_PATH, "testStandaloneWarAsyncEventLogged");
        assertSingleEnd("async", standaloneContextInfo(WAR_MODULE, "AsyncBeanInWar", "doAsync"));
    }

    /**
     * Verifies that a remote EJB invocation in a standalone WAR produces exactly one well-formed
     * END event log entry of type {@code websphere.ejb.remote} with the expected 3-part standalone
     * contextInfo ({@code EJBEventWar | RemoteBeanInWar | doRemote}).
     */
    @Test
    public void testStandaloneWarRemoteEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_WAR_SERVLET_PATH, "testStandaloneWarRemoteEventLogged");
        assertSingleEnd("remote", standaloneContextInfo(WAR_MODULE, "RemoteBeanInWar", "doRemote"));
    }

    /**
     * Verifies that a programmatic single-action timer in a standalone WAR produces exactly one
     * well-formed END event log entry of type {@code websphere.ejb.timer} with the expected
     * 3-part standalone contextInfo ({@code EJBEventWar | TimerBeanInWar | doTimeout}).
     */
    @Test
    public void testStandaloneWarTimerEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_WAR_SERVLET_PATH, "testStandaloneWarTimerEventLogged");
        assertSingleEnd("timer", standaloneContextInfo(WAR_MODULE, "TimerBeanInWar", "doTimeout"));
    }

    /**
     * Verifies that a message-driven bean invocation in a standalone WAR produces exactly one
     * well-formed END event log entry of type {@code websphere.ejb.mdb} with the expected 3-part
     * standalone contextInfo ({@code EJBEventWar | MDBBeanInWar | onMessage}).
     */
    @Test
    public void testStandaloneWarMdbEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_WAR_SERVLET_PATH, "testStandaloneWarMdbEventLogged");
        assertSingleEnd("mdb", standaloneContextInfo(WAR_MODULE, "MDBBeanInWar", "onMessage"));
    }

    // -----------------------------------------------------------------------
    // Standalone JAR tests (3-part contextInfo: EJBEventJar#Bean#method)
    // -----------------------------------------------------------------------

    /**
     * Verifies that a {@code @Asynchronous} remote bean method invocation in a standalone EJB JAR
     * produces exactly one well-formed END event log entry of type {@code websphere.ejb.async} with
     * the expected 3-part standalone contextInfo ({@code EJBEventJar | AsyncBeanInJar | doAsync}).
     * The async interface must be remote because the driver WAR and the JAR are separate applications
     * with separate class loaders.
     */
    @Test
    public void testStandaloneJarAsyncEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_JAR_SERVLET_PATH, "testStandaloneJarAsyncEventLogged");
        assertSingleEnd("async", standaloneContextInfo(JAR_MODULE, "AsyncBeanInJar", "doAsync"));
    }

    /**
     * Verifies that a remote EJB invocation in a standalone EJB JAR produces exactly one well-formed
     * END event log entry of type {@code websphere.ejb.remote} with the expected 3-part standalone
     * contextInfo ({@code EJBEventJar | RemoteBeanInJar | doRemote}).
     */
    @Test
    public void testStandaloneJarRemoteEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_JAR_SERVLET_PATH, "testStandaloneJarRemoteEventLogged");
        assertSingleEnd("remote", standaloneContextInfo(JAR_MODULE, "RemoteBeanInJar", "doRemote"));
    }

    /**
     * Verifies that a message-driven bean invocation in a standalone EJB JAR produces exactly one
     * well-formed END event log entry of type {@code websphere.ejb.mdb} with the expected 3-part
     * standalone contextInfo ({@code EJBEventJar | MDBBeanInJar | onMessage}).
     */
    @Test
    public void testStandaloneJarMdbEventLogged() throws Exception {
        server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
        runTest(server, EJB_JAR_SERVLET_PATH, "testStandaloneJarMdbEventLogged");
        assertSingleEnd("mdb", standaloneContextInfo(JAR_MODULE, "MDBBeanInJar", "onMessage"));
    }

    // -----------------------------------------------------------------------
    // Log-only test — no servlet call needed
    // -----------------------------------------------------------------------

    /**
     * The &#64;Schedule timer in the standalone JAR fires every second and cancels
     * itself after the first invocation. By the time the server has finished
     * starting the END entry is normally already in messages.log, so we scan from the
     * beginning of the file - no mark, no servlet call needed. In the rare case that the
     * timer has not yet fired, wait 5 seconds and look once more.
     */
    @Test
    public void testStandaloneJarTimerEventLogged() throws Exception {
        String regexp = endRegex("timer", standaloneContextInfo(JAR_MODULE, "TimerBeanInJar", "doTimeout"));
        List<String> found = server.findStringsInLogs(regexp, server.getMatchingLogFile("messages.log"));
        if (found.isEmpty()) {
            Thread.sleep(5000);
            found = server.findStringsInLogs(regexp, server.getMatchingLogFile("messages.log"));
        }
        assertEquals("Expected exactly 1 websphere.ejb.timer END entry for JAR TimerBeanInJar.doTimeout",
                     1, found.size());
    }

    // -----------------------------------------------------------------------
    // Tests that reconfigure <eventLogging> and drive EJBEventServlet.testAllEjbTypes
    // -----------------------------------------------------------------------

    /**
     * Configures {@code <eventLogging minDuration="0" logMode="entryExit" eventTypes="all">} and
     * verifies that both a BEGIN and an END entry are produced for all four timed EJB event types
     * (async, mdb, remote, timer).
     */
    @Test
    public void testBeginAndEndLogged() throws Exception {
        runAllEjbTypesAndVerify("all", "async", "mdb", "remote", "timer");
    }

    /**
     * Removes the {@code <eventLogging>} element so that the default (servlet-only) event types
     * apply, and verifies that no EJB event log entries are produced for any of the four timed
     * EJB event types.
     */
    @Test
    public void testNoEjbEventsWhenDefaultEventTypes() throws Exception {
        runAllEjbTypesAndVerify(null);
    }

    /**
     * Configures {@code eventTypes="websphere.ejb.async"} and verifies that only
     * {@code websphere.ejb.async} entries are produced; mdb, remote and timer entries
     * must be absent.
     */
    @Test
    public void testOnlyAsyncEventLogged() throws Exception {
        runAllEjbTypesAndVerify("websphere.ejb.async", "async");
    }

    /**
     * Configures {@code eventTypes="websphere.ejb.mdb"} and verifies that only
     * {@code websphere.ejb.mdb} entries are produced; async, remote and timer entries
     * must be absent.
     */
    @Test
    public void testOnlyMdbEventLogged() throws Exception {
        runAllEjbTypesAndVerify("websphere.ejb.mdb", "mdb");
    }

    /**
     * Configures {@code eventTypes="websphere.ejb.remote"} and verifies that only
     * {@code websphere.ejb.remote} entries are produced (covering both the business remote
     * interface and the remote home interface); async, mdb and timer entries must be absent.
     */
    @Test
    public void testOnlyRemoteEventLogged() throws Exception {
        runAllEjbTypesAndVerify("websphere.ejb.remote", "remote");
    }

    /**
     * Configures {@code eventTypes="websphere.ejb.timer"} and verifies that only
     * {@code websphere.ejb.timer} entries are produced; async, mdb and remote entries
     * must be absent.
     */
    @Test
    public void testOnlyTimerEventLogged() throws Exception {
        runAllEjbTypesAndVerify("websphere.ejb.timer", "timer");
    }

    /**
     * Configures all four EJB event types explicitly as a comma-separated list
     * ({@code websphere.ejb.async,websphere.ejb.mdb,websphere.ejb.remote,websphere.ejb.timer})
     * and verifies that BEGIN and END entries are produced for each type — equivalent to
     * {@code eventTypes="all"} but exercises the explicit enumeration code path.
     */
    @Test
    public void testAllEjbEventTypesExplicit() throws Exception {
        runAllEjbTypesAndVerify("websphere.ejb.async,websphere.ejb.mdb,websphere.ejb.remote,websphere.ejb.timer",
                                "async", "mdb", "remote", "timer");
    }

    /**
     * Removes and re-adds the eventLogging feature, several times, while ten EJB interval timers
     * keep timer invocations continuously in flight. This exercises setting and clearing the request
     * timing collaborator on the EJB container while requests are active.
     *
     * Each cycle verifies that:
     * <ul>
     * <li>no event is logged once eventLogging has been removed;</li>
     * <li>events are logged again once it is re-added;</li>
     * <li>the application was not restarted (no application started, updated or stopped message,
     * and the non-persistent timers still exist);</li>
     * <li>the container did not record an FFDC for a failure in the timing collaborator hooks.</li>
     * </ul>
     *
     * A request that was in flight when eventLogging was removed may have logged a BEGIN without
     * ever logging an END, so BEGIN and END entries are deliberately never paired up.
     *
     * This takes about a minute (it waits out several quiet periods and runs feature updates), so
     * it runs in FULL mode only.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testAddRemoveEventLoggingWhileEjbsActive() throws Exception {
        ServerConfiguration original = server.getServerConfiguration();
        int lifecycleMessages = countInWholeLog(APP_LIFECYCLE_MESSAGES);
        try {
            runTest(server, EJB_SERVLET_PATH, "startStressTimers");

            // eventLogging-1.0 is in the configuration, so events are expected from the start
            server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
            assertNotNull("No stress timer event was logged before eventLogging was first removed",
                          server.waitForStringInLogUsingMark(END_STRESS_TIMER));

            for (int cycle = 1; cycle <= STRESS_CYCLES; cycle++) {
                // Remove eventLogging: no EJB event may be logged from now on
                setEventLoggingFeature(false);
                server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
                Thread.sleep(QUIET_PERIOD_MS);
                List<String> unexpected = findInLog(ANY_EJB_EVENT);
                assertTrue("Cycle " + cycle + ": EJB events were logged after eventLogging was removed: " + unexpected,
                           unexpected.isEmpty());
                verifyApplicationsStillRunning(cycle, lifecycleMessages);

                // Add eventLogging back: events must start to appear again
                setEventLoggingFeature(true);
                assertNotNull("Cycle " + cycle + ": no stress timer event was logged after eventLogging was added",
                              server.waitForStringInLogUsingMark(END_STRESS_TIMER));
                verifyApplicationsStillRunning(cycle, lifecycleMessages);
            }
        } finally {
            try {
                runTest(server, EJB_SERVLET_PATH, "stopStressTimers");
                // Let any callback that was running when its timer was cancelled finish, so that its
                // event cannot land in the log being examined by the next test
                Thread.sleep(1000);
            } finally {
                updateConfigAndWait(original);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Reconfigures {@code <eventLogging>}, runs {@code EJBEventServlet.testAllEjbTypes}
     * and verifies that BEGIN and END entries appear in messages.log for exactly the
     * expected EJB event types. The original configuration is always restored.
     *
     * @param eventTypes value for the eventTypes attribute, or {@code null} to remove
     *            the {@code <eventLogging>} element so the servlet-only default applies
     * @param expectedTypes the subset of async, mdb, remote, timer expected to be logged
     */
    private void runAllEjbTypesAndVerify(String eventTypes, String... expectedTypes) throws Exception {
        ServerConfiguration original = server.getServerConfiguration();
        try {
            ServerConfiguration config = server.getServerConfiguration();
            if (eventTypes == null) {
                config.removeEventLogging();
            } else {
                EventLoggingElement eventLogging = config.getEventLogging();
                eventLogging.setMinDuration("0");
                eventLogging.setLogMode("entryExit");
                eventLogging.setEventTypes(eventTypes);
            }
            updateConfigAndWait(config);

            server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
            runTest(server, EJB_SERVLET_PATH, "testAllEjbTypes");

            verifyEjbEvents(new HashSet<String>(Arrays.asList(expectedTypes)));
        } finally {
            updateConfigAndWait(original);
        }
    }

    /**
     * Reconfigures {@code <eventLogging>} to log BEGIN and END for all types, runs a servlet method that
     * calls {@code failingMethod} (which fails) and then {@code succeedingMethod} on the same EAR bean,
     * and verifies that both calls have a paired BEGIN and END, and that their request IDs differ. The
     * original configuration is always restored.
     */
    private void runFailureAndVerify(String servletMethod, String type, String bean,
                                     String failingMethod, String succeedingMethod) throws Exception {
        ServerConfiguration original = server.getServerConfiguration();
        try {
            ServerConfiguration config = server.getServerConfiguration();
            EventLoggingElement eventLogging = config.getEventLogging();
            eventLogging.setMinDuration("0");
            eventLogging.setLogMode("entryExit");
            eventLogging.setEventTypes("all");
            updateConfigAndWait(config);

            server.setMarkToEndOfLog(server.getMatchingLogFile("messages.log"));
            runTest(server, EJB_SERVLET_PATH, servletMethod);

            String failedId = assertBeginEndPaired(type, earContextInfo(bean, failingMethod));
            String succeededId = assertBeginEndPaired(type, earContextInfo(bean, succeedingMethod));
            assertFalse("The call after the failed " + failingMethod + " reused its request ID: " + failedId,
                        failedId.equals(succeededId));
        } finally {
            updateConfigAndWait(original);
        }
    }

    private void updateConfigAndWait(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Adds or removes the eventLogging-1.0 feature (matched case-insensitively) and waits for
     * the feature update to complete.
     */
    private void setEventLoggingFeature(boolean enabled) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.removeIf(f -> f.equalsIgnoreCase("eventLogging-1.0"));
        if (enabled) {
            features.add("eventLogging-1.0");
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null, true);
    }

    /**
     * Verifies that no application was started, updated or stopped since the test began (the
     * application is expected to keep running while the feature is toggled), that the
     * non-persistent timers still exist, and that no FFDC was recorded for the timing
     * collaborator hooks in the EJB container.
     */
    private void verifyApplicationsStillRunning(int cycle, int lifecycleMessagesAtStart) throws Exception {
        assertEquals("Cycle " + cycle + ": an application was started, updated or stopped but should not have been",
                     lifecycleMessagesAtStart, countInWholeLog(APP_LIFECYCLE_MESSAGES));
        runTest(server, EJB_SERVLET_PATH, "verifyStressTimersActive");
        assertEquals("Cycle " + cycle + ": FFDC recorded for a failure in the EJB request timing collaborator hooks",
                     0, countInWholeLog(COLLABORATOR_FFDC));
    }

    /** Counts matching lines in all of messages.log, regardless of the current mark. */
    private int countInWholeLog(String regexp) throws Exception {
        return server.findStringsInLogs(regexp, server.getMatchingLogFile("messages.log")).size();
    }

    /**
     * Verifies that, since the messages.log mark, BEGIN and END entries exist for every
     * expected type, none exist for the other timed types, and that no entry exists
     * for the local and local home invocations (which are never logged).
     */
    private void verifyEjbEvents(Set<String> expectedTypes) throws Exception {
        for (String type : EJB_TYPES) {
            if (expectedTypes.contains(type)) {
                // remote produces 6 entries: stateless doRemote + doRemoteObject,
                // stateful doRemote + doRemoteObject + doRemove (@Remove) + remove (EJBObject.remove);
                // home.create() is NOT timed
                int expected = "remote".equals(type) ? 6 : 1;
                assertEquals("Expected " + expected + " BEGIN entr" + (expected == 1 ? "y" : "ies") + " for websphere.ejb." + type,
                             expected, findInLog("BEGIN requestID=.*eventType=websphere\\.ejb\\." + type + "\\b").size());
                assertEquals("Expected " + expected + " END entr" + (expected == 1 ? "y" : "ies") + " for websphere.ejb." + type,
                             expected, findInLog("END requestID=.*eventType=websphere\\.ejb\\." + type + "\\b").size());
            }
        }

        // Each expected invocation must have exactly one BEGIN and one END, with the exact context
        // info, the same request ID, and a well-formed duration on the END
        if (expectedTypes.contains("async")) {
            assertBeginEndPaired("async", earContextInfo("AsyncBean", "doAsync"));
        }
        if (expectedTypes.contains("mdb")) {
            assertBeginEndPaired("mdb", earContextInfo("MDBBean", "onMessage"));
        }
        if (expectedTypes.contains("timer")) {
            assertBeginEndPaired("timer", earContextInfo("TimerBean", "doTimeout"));
        }
        if (expectedTypes.contains("remote")) {
            // the 6 remote entries are for 6 distinct methods
            assertBeginEndPaired("remote", earContextInfo("RemoteBean", "doRemote"));
            assertBeginEndPaired("remote", earContextInfo("RemoteBean", "doRemoteObject"));
            assertBeginEndPaired("remote", earContextInfo("StatefulRemoteBean", "doRemote"));
            assertBeginEndPaired("remote", earContextInfo("StatefulRemoteBean", "doRemoteObject"));
            assertBeginEndPaired("remote", earContextInfo("StatefulRemoteBean", "doRemove"));
            assertBeginEndPaired("remote", earContextInfo("StatefulRemoteBean", "remove"));
        }

        for (String type : EJB_TYPES) {
            if (!expectedTypes.contains(type)) {
                assertTrue("Unexpected entries for websphere.ejb." + type,
                           findInLog("(BEGIN|END) requestID=.*eventType=websphere\\.ejb\\." + type + "\\b").isEmpty());
            }
        }

        // Every EJB event entry must belong to an expected type (this also rules out a websphere.ejb.local type)
        for (String line : findInLog(ANY_EJB_EVENT)) {
            boolean expected = false;
            for (String type : expectedTypes) {
                expected |= line.contains("eventType=websphere.ejb." + type);
            }
            assertTrue("Unexpected EJB event entry: " + line, expected);
        }

        // Local invocations (doLocal, doLocalObject) are never logged
        assertTrue("doLocal or doLocalObject invocation was unexpectedly logged",
                   findInLog(ANY_EJB_EVENT + ".*doLocal").isEmpty());

        // home.create() (both remote and local) is never logged — only business method
        // invocations on the component object are timed by the collaborator
        assertTrue("home.create() was unexpectedly logged",
                   findInLog(ANY_EJB_EVENT + ".*\\bcreate\\b").isEmpty());
    }

    // -----------------------------------------------------------------------
    // Exact event matching
    // -----------------------------------------------------------------------

    /** Context info of an EJB in an EAR: {@code app | module | bean | method}. */
    private static String earContextInfo(String bean, String method) {
        return EAR_APP + " | " + EAR_MODULE + " | " + bean + " | " + method;
    }

    /** Context info of an EJB in a standalone module: {@code module | bean | method}. */
    private static String standaloneContextInfo(String module, String bean, String method) {
        return module + " | " + bean + " | " + method;
    }

    /** Matches a BEGIN line for exactly this event type and context info. */
    private static String beginRegex(String type, String contextInfo) {
        return "BEGIN requestID=\\S+ # eventType=websphere\\.ejb\\." + type
               + " # contextInfo=" + Pattern.quote(contextInfo) + "(?!\\w)";
    }

    /** Matches an END line for exactly this event type and context info, ending with {@code duration=n.nnnms}. */
    private static String endRegex(String type, String contextInfo) {
        return "END requestID=\\S+ # eventType=websphere\\.ejb\\." + type
               + " # contextInfo=" + Pattern.quote(contextInfo) + " # duration=\\d+\\.\\d{3}ms";
    }

    /** Asserts exactly one well-formed END entry (exact context info and duration) since the mark. */
    private void assertSingleEnd(String type, String contextInfo) throws Exception {
        assertEquals("Expected exactly 1 websphere.ejb." + type + " END entry for " + contextInfo,
                     1, findInLog(endRegex(type, contextInfo)).size());
    }

    /**
     * Asserts exactly one BEGIN and one END entry since the mark for this event, and that both carry
     * the same request ID. Only valid for events that run on their own thread (async, timer, mdb and
     * remote), so that each has a request ID of its own.
     *
     * @return the request ID shared by the BEGIN and END entries
     */
    private String assertBeginEndPaired(String type, String contextInfo) throws Exception {
        List<String> begins = findInLog(beginRegex(type, contextInfo));
        List<String> ends = findInLog(endRegex(type, contextInfo));
        assertEquals("Expected exactly 1 websphere.ejb." + type + " BEGIN entry for " + contextInfo, 1, begins.size());
        assertEquals("Expected exactly 1 websphere.ejb." + type + " END entry for " + contextInfo, 1, ends.size());
        String requestId = requestIdOf(begins.get(0));
        assertEquals("BEGIN and END entries for " + contextInfo + " have different request IDs",
                     requestId, requestIdOf(ends.get(0)));
        return requestId;
    }

    private static String requestIdOf(String line) {
        Matcher m = REQUEST_ID.matcher(line);
        assertTrue("No request ID in: " + line, m.find());
        return m.group(1);
    }

    private List<String> findInLog(String regexp) throws Exception {
        return server.findStringsInLogsUsingMark(regexp, server.getMatchingLogFile("messages.log"));
    }
}
