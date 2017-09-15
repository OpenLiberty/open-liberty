/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import com.ibm.wsspi.kernel.embeddable.Server;
import com.ibm.wsspi.kernel.embeddable.Server.Result;
import com.ibm.wsspi.kernel.embeddable.ServerBuilder;
import com.ibm.wsspi.kernel.embeddable.ServerEventListener;

public class EmbeddedServerDriver implements ServerEventListener {

    public List<AssertionFailedError> doTest() {
        return doTest(null);
    }

    /**
     * @param args
     */
    public List<AssertionFailedError> doTest(File installDir) {

        // TODO: fix me to something more intelligent
        List<AssertionFailedError> failures = new ArrayList<AssertionFailedError>();

        String serverName = "embed_me";

        ServerBuilder sb = new ServerBuilder();
        Server libertyServer = sb.setName(serverName).setInstallDir(installDir).setServerEventListener(this).build();

        Result result;

        checkServerRunning(libertyServer, false, failures); // server should be stopped

        // Stop a stopped server
        Future<Result> stopFuture = libertyServer.stop();
        try {
            result = stopFuture.get();
            dumpResult("Stopping a stopped server", result);
            Assert.assertFalse("Stopping a stopped server should not be successful", result.successful());
            Assert.assertEquals("Should have a redundant operation returned", 1, result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Stop operation did not complete normally: " + e));
            e.printStackTrace();
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Stop operation could not be queued: " + e));
            e.printStackTrace();
        }

        checkServerRunning(libertyServer, false, failures); // server should be stopped

        // Start the server
        Future<Result> startFuture = libertyServer.start();
        try {
            result = startFuture.get();
            dumpResult("Starting a server", result);
            Assert.assertTrue("Result of first start attempt should be successful", result.successful());
            Assert.assertEquals("Should have an OK return code", 0, result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            e.printStackTrace();
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            e.printStackTrace();
        }

        checkServerRunning(libertyServer, true, failures); // server should be started

        // Start the started server
        Future<Result> startFuture2 = libertyServer.start();
        try {
            result = startFuture2.get();
            dumpResult("Starting a started server", result);
            Assert.assertFalse("Starting a started server should not be successful", result.successful());
            Assert.assertEquals("Should have a redundant operation returned", 1, result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Start operation did not complete normally: " + e));
            e.printStackTrace();
            startFuture.cancel(true);
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Start operation could not be queued: " + e));
            e.printStackTrace();
        }

        checkServerRunning(libertyServer, true, failures); // server should be started

        // Stop the started server
        Future<Result> stopFuture2 = libertyServer.stop();
        try {
            result = stopFuture2.get();
            dumpResult("Stopping a started server", result);
            Assert.assertTrue("Stopping a running server should be successful", result.successful());
            Assert.assertEquals("Should have an OK return code", 0, result.getReturnCode());
        } catch (AssertionFailedError e) {
            failures.add(e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            failures.add(new AssertionFailedError("Stop operation did not complete normally: " + e));
            e.printStackTrace();
        } catch (ExecutionException e) {
            failures.add(new AssertionFailedError("Stop operation could not be queued: " + e));
            e.printStackTrace();
        }

        checkServerRunning(libertyServer, false, failures); // server should be stopped

        return failures;
    }

    private boolean checkServerRunning(Server server, boolean expectRunning, List<AssertionFailedError> failures) {
        try {
            if (expectRunning) {
                Assert.assertTrue("Server should be running", server.isRunning());
            } else {
                Assert.assertFalse("Server should not be running", server.isRunning());
            }
        } catch (AssertionFailedError e) {
            failures.add(e);
        }
        return false;
    }

    private void dumpResult(String msg, Result result) {
        System.out.println(msg + " -- Result: success=" + result.successful() + ", rc=" + result.getReturnCode() + ", ex=" + result.getException());
    }

    @Override
    public void serverEvent(ServerEvent event) {
        System.out.println("NEW SERVER EVENT " + event);
    }

}
