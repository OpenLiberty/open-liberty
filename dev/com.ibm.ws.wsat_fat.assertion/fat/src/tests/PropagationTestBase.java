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
package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.topology.utils.HttpUtils;

/**
 * Shared helpers for tests that drive {@code ConditionalPropagationServlet} in the
 * {@code assertion} test application.
 *
 * <p>Propagation is observed two ways, both reported by the endpoint ({@code TxProbe}):
 * <ul>
 * <li>{@code tx=ACTIVE|NONE} in the reply - did the endpoint import a WS-AT coordination context?</li>
 * <li>A row keyed by a unique key is inserted into an XA database. It persists after the client
 * <b>commits</b> in every case, but after the client <b>rolls back</b> it persists only if the
 * endpoint was <i>not</i> enlisted in the client's transaction.</li>
 * </ul>
 */
public abstract class PropagationTestBase extends WSATTest {

    protected static final String REQUIRED = "Required";
    protected static final String OPTIONAL = "Optional";
    protected static final String NONE = "None";
    /** Optional endpoint called through a {@code Dispatch} client created without a WSDL. */
    protected static final String DISPATCH = "Dispatch";
    /** Required endpoint called through a {@code Dispatch} client created without a WSDL. */
    protected static final String DISPATCH_REQUIRED = "DispatchRequired";

    protected static final String COMMIT = "GlobalCommit";
    protected static final String ROLLBACK = "GlobalRollback";
    protected static final String NO_TRAN = "NoTran";

    private static final String SERVLET = "/assertion/ConditionalPropagationServlet";

    /** Unique, traceable row key for this test invocation. */
    protected String newKey() {
        String name = testName.getMethodName();
        if (name.length() > 30) {
            name = name.substring(0, 30);
        }
        return name + "-" + Long.toString(System.nanoTime(), 36);
    }

    /** Create the probe table on the server at {@code baseUrl}. */
    protected void initTable(String baseUrl) {
        assertTrue(get(baseUrl, "op=init").startsWith("INIT:"));
    }

    /**
     * Run {@code service} under {@code tran} from the application at {@code clientBaseUrl}, targeting the
     * endpoints hosted at {@code targetBaseUrl}. Returns the servlet's single result line.
     */
    protected String call(String clientBaseUrl, String targetBaseUrl, String service, String tran, String key) {
        return get(clientBaseUrl, "op=invoke&service=" + service + "&tran=" + tran + "&key=" + enc(key) + "&baseurl=" + enc(targetBaseUrl));
    }

    /** Number of rows persisted at {@code baseUrl} for {@code key}. */
    protected int count(String baseUrl, String key) {
        final String r = get(baseUrl, "op=count&key=" + enc(key));
        assertTrue("Unexpected count response: " + r, r.startsWith("COUNT:"));
        return Integer.parseInt(r.substring("COUNT:".length()).trim());
    }

    /**
     * Invoke and assert the call succeeded, whether the endpoint saw a global transaction, and
     * whether its database row survived the client's completion.
     *
     * @param propagated  true if the WS-AT context must have been attached (endpoint enlisted)
     * @param commit      true if the client committed, false if it rolled back
     */
    protected void assertOutcome(String clientBaseUrl, String targetBaseUrl, String service, boolean propagated, boolean commit) {
        final String key = newKey();
        final String r = call(clientBaseUrl, targetBaseUrl, service, commit ? COMMIT : ROLLBACK, key);
        final String label = service + (commit ? " commit" : " rollback") + ": ";

        assertTrue(label + "expected OK but got: " + r, r.startsWith("OK:"));
        assertTrue(label + "expected the endpoint to " + (propagated ? "" : "NOT ") + "see a global transaction (WS-AT context "
                   + (propagated ? "attached" : "suppressed") + ") but got: " + r,
                   r.endsWith(propagated ? "tx=ACTIVE" : "tx=NONE"));

        // Enlisted endpoint: row follows the client (1 on commit, 0 on rollback).
        // Non-enlisted endpoint: committed locally, so the row always persists.
        final int expectedRows = (propagated && !commit) ? 0 : 1;
        assertEquals(label + "unexpected number of persisted rows", expectedRows, count(targetBaseUrl, key));
    }

    /**
     * Make the calls named by {@code seq} (underscore separated REQ | OPT | NONE) in one global transaction and
     * assert, for each call, whether the endpoint saw a global transaction and whether its row survived.
     *
     * @param always  true if every call is propagated ({@code propagation="always"}); false for
     *                {@code check_wsdl}, where only REQ and OPT calls are propagated
     * @param commit  true if the client committed, false if it rolled back
     */
    protected void assertMixedOutcome(String clientBaseUrl, String targetBaseUrl, String seq, boolean always, boolean commit) {
        final String key = newKey();
        final String r = get(clientBaseUrl, "op=mixed&seq=" + seq + "&tran=" + (commit ? COMMIT : ROLLBACK)
                                            + "&key=" + enc(key) + "&baseurl=" + enc(targetBaseUrl));
        final String label = seq + (commit ? " commit" : " rollback") + ": ";
        assertTrue(label + "expected OK but got: " + r, r.startsWith("OK:"));

        final String[] steps = seq.split("_");
        final String[] replies = r.substring("OK:".length()).split("\\|");
        assertEquals(label + "unexpected number of replies in: " + r, steps.length, replies.length);

        for (int i = 0; i < steps.length; i++) {
            final boolean propagated = always || !"NONE".equals(steps[i]);
            assertTrue(label + "call " + i + " (" + steps[i] + ") expected the endpoint to " + (propagated ? "" : "NOT ")
                       + "see a global transaction but got: " + replies[i],
                       replies[i].endsWith(propagated ? "tx=ACTIVE" : "tx=NONE"));
            final int expectedRows = (propagated && !commit) ? 0 : 1;
            assertEquals(label + "unexpected number of persisted rows for call " + i + " (" + steps[i] + ")",
                         expectedRows, count(targetBaseUrl, key + "-" + i));
        }
    }

    /** Run the fault isolation scenario; see {@code ConditionalPropagationServlet}. Returns the single result line. */
    protected String callFaultIsolation(String clientBaseUrl, String targetBaseUrl, String key) {
        return get(clientBaseUrl, "op=fault&key=" + enc(key) + "&baseurl=" + enc(targetBaseUrl));
    }

    /**
     * Call {@code service} from an EJB or CDI bean (see {@code CmtDriver}) with the caller in {@code tran}.
     *
     * @param kind  EJB | BMT | CDI
     * @param attr  transaction attribute of the bean method (ignored for BMT)
     */
    protected String callCmt(String clientBaseUrl, String targetBaseUrl, String kind, String attr, String service, String tran,
                             boolean rollback, String key) {
        return get(clientBaseUrl, "op=cmt&kind=" + kind + "&attr=" + attr + "&service=" + service + "&tran=" + tran
                                  + "&rollback=" + rollback + "&key=" + enc(key) + "&baseurl=" + enc(targetBaseUrl));
    }

    protected static void assertException(String message, String result) {
        assertNotNull(message, result);
        assertTrue(message + " - expected EXCEPTION but got: " + result, result.startsWith("EXCEPTION:"));
    }

    private String get(String baseUrl, String query) {
        final String method = "get";
        try {
            final String urlStr = baseUrl + SERVLET + "?" + query;
            Log.info(getClass(), method, urlStr);
            final HttpURLConnection con = getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            final BufferedReader br = HttpUtils.getConnectionStream(con);
            final String result = br.readLine();
            Log.info(getClass(), method, "result=" + result);
            assertNotNull("No response from " + urlStr, result);
            return result;
        } catch (Exception e) {
            fail("HTTP request failed: " + query + ": " + e);
            return null; // unreachable
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
