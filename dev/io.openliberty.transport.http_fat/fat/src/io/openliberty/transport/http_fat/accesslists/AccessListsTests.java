/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat.accesslists;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import app1.web.Backend;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Test are named either descriptively or based on the historical testcase name from
 * traditional WebSphere that has the same configuration.
 *
 * The template server.xml files can be found in:
 * ./publish/files/<ClassName>_<testMethodName>_server.xml and finished files at
 * build/libs/autoFVT/output/servers/AccessLists-DD-MM-YYYY-HH-MM-SS/server.xml
 *
 */
@RunWith(FATRunner.class)
public class AccessListsTests extends HttpTest {

    static final Logger LOG = Logger.getLogger(AccessListsTests.class.getName());
    public static final String APP_NAME = "app1";

    @Server("AccessLists")
    @TestServlet(servlet = Backend.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    /**
     * Setup test app, find out address/host/port of this test client
     * for substituting into the server.xml used in tests and prestart
     * the test server once
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(clientInfoServer, APP_NAME, "app1.web");
        ShrinkHelper.defaultApp(server, APP_NAME, "app1.web");
        setClientInfo();
        server.startServer();
    }

    /**
     * Stop the server, the FAT scaffolding will check the logs when wrapping up
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test that the basic server.xml replacement mechanism is working
     * Will assert if not successful and relies on file system but not on network
     *
     * @throws Exception
     */
    @Test
    public void testConfigReplace() throws Exception {
        Utils.setUpServer(server, this);
    }

    /**
     * Test that the server.xml replacement mechanism with keyword substitution is working
     *
     * @throws Exception
     */
    @Test
    public void testConfigReplaceSubs() throws Exception {
        // Will assert if not successful (checks network access)
        checkAccessAllowed();
    }

    /**
     * Specifically exclude the test client
     *
     * <tcpOptions addressExcludeList="0.0.0.1, 127.1.1.1, CLIENT_ADDR"/>
     *
     * @throws Exception
     */
    @Test
    public void testLocalHostExcludedInList() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressIncludeList="1.1.1.1, 0.0.0.0, 127.127.127.127, CLIENT_ADDR, 2.2.2.2, 11.22.33.44,
     * 255.255.255.255, 2.4.8.16, 9.9.9.9, 10.10.10.10" />
     *
     * @throws Exception
     */
    @Test
    public void testA10() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressIncludeList="*.*.*.255, 127.*.*.0, 1.0.0.*, 127.*.127.*, 255.0.0.*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA11() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressIncludeList= "*:0:0:0:0:0:0:*, INSERT_A12, *:*:*:*:*:*:*:7777,
     * 5555:4444:3333:*:007F:2222:1111:0000, 0:*:2:0:F:0:0:3"/>
     *
     * @throws Exception
     */
    @Test
    public void testA12() throws Exception {
        String connectAddr = clientDetails.getProperty("CLIENT_ADDR");
        String insertA12 = Utils.convertIP4toIP6(connectAddr);
        insertA12 = putInWildCards(insertA12, 7, "*");
        clientDetails.put("INSERT_A12", insertA12);
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameIncludeList="www.ibm.com, www.rational.com,
     * a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.27.28.29.30.31.32.33.34.35.36.37.38.39.40.41.42.43.44.45.46.47.48.49.50.51.52.53.54.55.56.57.58.59.60.61.62.63.64.65.66.67.68.69.70.71.72.73.74.75.76.77.78.79.80.81.82.83.84.85.86.87.88.89.90.91.2.93.94.95.96.97.98.99.100.101.102.103.104.105.106.107.108.109.110.111"/>
     *
     * @throws Exception
     */
    @Test
    public void testA13() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameIncludeList="www.ibm.com, INSERT_A14,
     * a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.27.28.29.30.31.32.33.34.35.36.37.38.39.40.41.42.43.44.45.46.47.48.49.50"/>
     *
     * @throws Exception
     */
    @Test
    public void testA14() throws Exception {
        String connectHost = clientDetails.getProperty("CLIENT_HOST");
        String insertA14 = putInWildCards(connectHost, 1, "*");
        clientDetails.put("INSERT_A14", insertA14);
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressIncludeList="*.*.*.255, 127.*.*.0, 1.0.0.*, 127.*.127.*, CLIENT_ADDR"
     * hostNameIncludeList="www.ibm.com, www.NoMatch.com, a.b.c.d.e.f.g.h.i"/>
     *
     * @throws Exception
     */
    @Test
    public void testA15() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameIncludeList="xyz.1.0.1, w3.ibm.com, INSERT_A16A" addressIncludeList="0.0.0.1,
     * 127.1.1.1, 0:0:0:0:007F:0:0001:0001" addressExcludeList="*.*.*.255, 127.*.*.0, 1.0.0.*"
     * hostNameExcludeList="www.ibm.com, CLIENT_HOST, a.b.c.d.e.f.g.h.i"/>
     *
     * @throws Exception
     */
    @Test
    public void testA16A() throws Exception {
        String connectHostName = clientDetails.getProperty("CLIENT_HOST");
        String insert = putInWildCards(connectHostName, 1, "*");
        clientDetails.put("INSERT_A16A", insert);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameIncludeList="xyz.1.0.1, w3.ibm.com, INSERT_A16B" hostNameExcludeList="www.ibm.com,
     * noMatch, a.b.c.d.e.f.g.h.i" addressIncludeList="0.0.0.1, 127.1.1.1, 0:0:0:0:007F:0:0001:0001"
     * addressExcludeList="*.*.*.255, 127.*.*.0, 1.0.0.*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA16B() throws Exception {
        String connectHostName = clientDetails.getProperty("CLIENT_HOST");
        String insertA16B = putInWildCards(connectHostName, 1, "*");
        clientDetails.put("INSERT_A16B", insertA16B);
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="" hostNameExcludeList="" addressIncludeList="" hostNameIncludeList=""/>
     *
     * @throws Exception
     */
    @Test
    public void testA17() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="0.0.0.1, 127.1.1.1, 127.0.0.0"/>
     *
     * @throws Exception
     */
    @Test
    public void testA1A() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="0:0:0:0:0:0:0:1, 0:0:0:0:007F:1:1:1, 007F:0:0:1:0:0:0:0"/>
     *
     * @throws Exception
     */
    @Test
    public void testA1B() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="INSERT_A2A"/>
     *
     * @throws Exception
     */
    @Test
    public void testA2A() throws Exception {
        String connectAddr = clientDetails.getProperty("CLIENT_ADDR");
        String insertA2A = "0.0.0.1, 127.1.1.1,  " + connectAddr + ", 9.9.9.9";
        clientDetails.put("INSERT_A2A", insertA2A);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="INSERT_A2B"/>
     *
     * @throws Exception
     */
    @Test
    public void testA2B() throws Exception {
        String connectAddr = clientDetails.getProperty("CLIENT_ADDR");
        String insertA2B = "0.0.0.1, 127.1.1.1, " + Utils.convertIP4toIP6(connectAddr) + ", 0:0:0:0:0009:0009:0009:0009";
        clientDetails.put("INSERT_A2B", insertA2B);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="0.0.0.1, *.0.0.2, 127.0.1.*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA3A() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList= "0:0:0:0:0:0:0:1, *:*:*:*:255:255:255:255, 0:0:0:0:007F:255:255:*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA3B() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList="INSERT_A4A, *.*.*.255, 2.2.2.2, 3.3.3.3 "/>
     *
     * @throws Exception
     */
    @Test
    public void testA4A() throws Exception {
        String connectAddr = clientDetails.getProperty("CLIENT_ADDR");
        String insert = Utils.putInWildCards(connectAddr, 1, "*");
        insert = putInWildCards(insert, 2, "*");
        insert = putInWildCards(insert, 3, "*");
        clientDetails.put("INSERT_A4A", insert);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressExcludeList= "0.0.0.1, 127.1.1.1, INSERT_A4B, *:*:*:*:255:255:*:*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA4B() throws Exception {
        String connectAddr = clientDetails.getProperty("CLIENT_ADDR");
        String insert = Utils.convertIP4toIP6(connectAddr);
        insert = putInWildCards(insert, 1, "*");
        insert = putInWildCards(insert, 2, "*");
        insert = putInWildCards(insert, 3, "*");
        insert = putInWildCards(insert, 4, "*");
        insert = putInWildCards(insert, 6, "*");
        insert = putInWildCards(insert, 7, "*");
        insert = putInWildCards(insert, 8, "*");
        clientDetails.put("INSERT_A4B", insert);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameExcludeList="NoMatch.com"/>
     *
     * @throws Exception
     */
    @Test
    public void testA5() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameExcludeList="NoMatch.com, www.ibm.com, www.dell.com, CLIENT_HOST,
     * www.Bill.com, www.entry6.com, www.microsoft.com, www.12345678.com, www.coastalfcu.org,
     * www.lastone.com"/>
     *
     * @throws Exception
     */
    @Test
    public void testA6() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameExcludeList="*.no.match, *.raleigh.nope.com, *.not.this.one,
     * *.nome.ibm.com, *.NoNoNo"/>
     *
     * @throws Exception
     */
    @Test
    public void testA7() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameExcludeList="INSERT_A8A, one.two.three, edge.noMatch.ibm.com, hi.good.bye.com, 5.5.5.5"/>
     *
     * @throws Exception
     */
    @Test
    public void testA8A() throws Exception {
        String connectHostName = clientDetails.getProperty("CLIENT_HOST");
        String insertA8A = putInWildCards(connectHostName, 1, "*");
        clientDetails.put("INSERT_A8A", insertA8A);
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions hostNameExcludeList="*"/>
     *
     * @throws Exception
     */
    @Test
    public void testA8B() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * <tcpOptions addressIncludeList="0:0:0:0:0:0:0:0"/>
     *
     * @throws Exception
     */
    @Test
    public void testA9() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Both host and address include lists present, test host included
     *
     * @throws Exception
     */
    @Test
    public void testBothIncludesHostIn() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Both host and address include lists present, test address included
     *
     * @throws Exception
     */
    @Test
    public void testBothIncludesAddrIn() throws Exception {
        checkAccessAllowed();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Both host and address exclude lists present, test host in host exclude only
     *
     * @throws Exception
     */
    @Test
    public void testBothExcludesHostIn() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Both host and address exclude lists present, test addr in address exclude only
     *
     * @throws Exception
     */
    @Test
    public void testBothExcludesAddrIn() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Test machine in address include but host exclude list - denied
     *
     * @throws Exception
     */
    @Test
    public void testIncAddrExcHost() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Client address in both include and exclude lists - denied
     *
     * @throws Exception
     */
    @Test
    public void testIncAndExcAddr() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Client hostname in both include and exclude lists - denied
     *
     * @throws Exception
     */
    @Test
    public void testIncAndExcHost() throws Exception {
        checkAccessDenied();
    }

    /**
     * See ./publish/files/<ClassName>_<testMethodName>_server.xml for the tested config
     *
     * Client hostname included but address excluded - denied
     *
     * @throws Exception
     */
    @Test
    public void testIncHostExcAddr() throws Exception {
        checkAccessDenied();
    }

    /**
     * Check that access is allowed
     * This will replace the server's config file with the specific one
     * for this test and then confirm that the back end application can
     * be reached.
     *
     * @throws Exception
     */
    private void checkAccessAllowed() throws Exception {

        String result = null;
        Throwable problem = null;

        Utils.setUpServer(server, this);
        try {
            result = checkAccessAllowed(server);
            if (result.contains("Backend")) {
                return; //Success
            }
        } catch (Throwable t) {
            problem = t;
            debug("Unexpected exception during access for server: " + server.getServerName() + " :" + t.getMessage());
        }
        if (problem == null) {
            debug("Unexpected return text during access for server: " + server.getServerName() + " of:" + result + " (should be \"Backend\")");
        }

        debug("Server configuration: " + server.getServerConfiguration());
        debug("Lets see what happens with a nothing more that a restart...");
        try {
            try {
                // We have an error but first try waiting
                Thread.sleep(2000);
                result = checkAccessAllowed(server);
                if (result.contains("Backend")) {
                    debug("The problem is a timing issue as waiting for 2 seconds fixed it");
                    Assert.fail("Rewrite of server config server.xml did not become properly live before return from function but did after sleep");
                }
            } catch (Throwable t) {
                debug("Unexpected exception on retry after sleeping for two seconds: " + server.getServerName() + " :" + t.getMessage() + server.getServerConfiguration());
                debug("Will try server restart next");
            }

            server.restartServer(); // Will validate apps started by default
            result = checkAccessAllowed(server);
        } catch (Throwable t) {
            debug("Unexpected exception on retry for config rewrite followed by server restart: " + server.getServerName() + " :" + t.getMessage());
        } finally {
            if (result != null && result.contains("Backend")) {
                Assert.fail("Rewrite of server config server.xml did not become properly live without server reboot but worked after");
            } else {
                Assert.fail("Rewrite of server config server.xml did not become properly live even with server reboot");
            }
        }
    }

    /**
     * Check that access is denied
     * This will replace the server's config file with the specific one
     * for this test and then confirm that the back end application cannot
     * be reached.
     *
     * @throws Exception
     */
    private void checkAccessDenied() throws Exception {
        Utils.setUpServer(server, this);
        checkAccessDenied(server);
    }

}
