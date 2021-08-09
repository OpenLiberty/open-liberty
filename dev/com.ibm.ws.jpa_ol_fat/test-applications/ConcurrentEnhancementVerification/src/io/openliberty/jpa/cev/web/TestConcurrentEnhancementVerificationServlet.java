/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa.cev.web;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@WebServlet(urlPatterns = "/TCEVS")
public class TestConcurrentEnhancementVerificationServlet extends JPATestServlet {
    @PersistenceUnit(unitName = "CEV")
    private EntityManagerFactory amjtaEmf;

    private CountDownLatch startSignal = new CountDownLatch(26);
    private CountDownLatch doneSignal = new CountDownLatch(26);

    @Test
    public void testJPAConcurrentEnhancement() throws Exception {
        System.out.println("Beginning test testJPAConcurrentEnhancement ...");

        startSignal = new CountDownLatch(26);
        doneSignal = new CountDownLatch(26);

        // Build the list of Entity type names (EntityA -> EntityZ)
        List<String> entityNamesList = new ArrayList<String>();
        for (int idx = 65; idx < 91; idx++) {
            entityNamesList.add("io.openliberty.jpa.cev.model.Entity" + (char) idx);
        }

        final ClassLoader cl = TestConcurrentEnhancementVerificationServlet.class.getClassLoader();
        for (String cn : entityNamesList) {
            System.out.println("<br>Starting Worker thread for " + cn);
            new Thread(new Worker(cn, cl, System.out)).start();
            startSignal.countDown();
        }

        doneSignal.await();
        System.out.println("Worker Threads Complete.");
        System.out.println("Now to verify enhancement.  Look at the interfaces associated with the entities.");
        for (String cn : entityNamesList) {
            System.out.println("Checking " + cn + " ...");
            Class entity = cl.loadClass(cn);
            Class[] ifaces = entity.getInterfaces();
            if (ifaces != null && ifaces.length > 0) {
                System.out.println("<br>Interfaces:");
                for (Class i : ifaces) {
                    System.out.println("<br>     " + i.getName());
                }
            } else {
                Assert.fail("No Interfaces Found for " + cn + ", which means it has not been enhanced.");
            }
        }
    }

    private class Worker implements Runnable {
        private String classToLoad;
        private ClassLoader cl;
        private PrintStream pw;

        Worker(String classToLoad, ClassLoader cl, PrintStream pw) {
            this.classToLoad = classToLoad;
            this.cl = cl;
            this.pw = pw;
        }

        @Override
        public void run() {
            try {
                String name = Thread.currentThread().getName();
                pw.println("<br>Worker thread " + name + " started.");
                startSignal.await();
                doClassLoad();
                pw.println("<br>Worker thread " + name + " finished.");
                doneSignal.countDown();

            } catch (InterruptedException ex) {
            } // return;
        }

        private void doClassLoad() {
            try {
                cl.loadClass(classToLoad);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
