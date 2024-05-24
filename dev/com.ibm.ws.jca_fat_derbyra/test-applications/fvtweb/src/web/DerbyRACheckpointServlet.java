/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package web;

import java.util.Timer;

import javax.annotation.Resource;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

/**
 * Servlet
 */
@WebServlet(urlPatterns = "DerbyRACheckpointServlet", loadOnStartup = 1)
public class DerbyRACheckpointServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource(name = "java:global/env/eis/bootstrapContext", lookup = "eis/bootstrapContext")
    private BootstrapContext bc;

    /**
     * Execute an unsupported RA action during application startup.
     */
    @Override
    public void init() {
        System.out.println("--- DerbyRACheckpointServlet init starting ---");

        final String action = System.getProperty("unsupported.action", "unknown").toLowerCase();
        if ("servlet.init.create.timer".equals(action)) {
            try {
                Timer timer = bc.createTimer();
                System.out.println("--- DerbyRACheckpointServlet init createTimer: " + timer + " ---");
            } catch (NullPointerException | UnavailableException e) {
                System.out.println("--- DerbyRACheckpointServlet init createTimer failed with exception: " + e + " ---");
            }
        } else if ("servlet.init.create.timer.async".equals(action)) {
            try {
                ExecutionContext executionContext = new ExecutionContext();
                TestWork work = new TestWork(bc, action);
                FATWorkListener listener = new FATWorkListener();
                bc.getWorkManager().startWork(work, WorkManager.INDEFINITE, executionContext, listener);
                System.out.println("--- DerbyRACheckpointServlet init async work submitted ---");
            } catch (NullPointerException | WorkException we) {
                System.out.println("--- DerbyRACheckpointServlet init submit async work failed with exception: " + we + " ---");
            }
        } else if ("servlet.init.submit.work".equals(action)) {
            try {
                ExecutionContext executionContext = new ExecutionContext();
                TestWork work = new TestWork(bc, action);
                FATWorkListener listener = new FATWorkListener();
                bc.getWorkManager().doWork(work);
                System.out.println("--- DerbyRACheckpointServlet init sync work submitted ---");
            } catch (NullPointerException | WorkException we) {
                System.out.println("--- DerbyRACheckpointServlet init submit sync work failed with exception: " + we + " ---");
            }
        } else if ("servlet.init.submit.work.async".equals(action)) {
            try {
                ExecutionContext executionContext = new ExecutionContext();
                TestWork work = new TestWork(bc, action);
                FATWorkListener listener = new FATWorkListener();
                bc.getWorkManager().startWork(work, WorkManager.INDEFINITE, executionContext, listener);
                System.out.println("--- DerbyRACheckpointServlet init async work submitted ---");
            } catch (NullPointerException | WorkException we) {
                System.out.println("--- DerbyRACheckpointServlet init submit async work failed with exception: " + we + " ---");
            }
        }
        System.out.println("--- DerbyRACheckpointServlet init completed ---");
    }

    static class TestWork implements Work {

        BootstrapContext bc;
        String action;

        public TestWork(BootstrapContext _bc, String _action) {
            this.bc = _bc;
            this.action = _action;
        }

        @Override
        public void run() {
            System.out.println("--- DerbyRACheckpointServlet TestWork run ---");
            if (action.contains("create.timer")) {
                try {
                    Timer timer = bc.createTimer();
                    System.out.println("--- DerbyRACheckpointServlet TestWork createTimer: " + timer + " ---");
                } catch (NullPointerException | UnavailableException e) {
                    System.out.println("--- DerbyRACheckpointServlet TestWork createTimer failed with exception: " + e + " ---");
                }
            }
        }

        @Override
        public void release() {
            System.out.println("--- DerbyRACheckpointServlet TestWork release ---");
        }
    }
}