/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.suite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class ReactiveConcurrentWorkTest extends FATServletClient {

    public static final String APP_NAME = "ReactiveConcurrentWorkTest";

    private static final long FIVE_MINS = 5 * 60 * 1000;

    @Server("ReactiveStreamsConcurrentWorkServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name APP_NAME.war once it's written to a file
        // Include the 'APP_NAME.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.reactive.streams.test.concurrent");

        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Issue a slightly larger burst of work to Liberty
     * this is used to enable observation of how worker threads
     * are used.
     *
     * @throws Exception
     */
    @Test
    public void testReactiveParallelClient() throws Exception {

        int count = 3 * Runtime.getRuntime().availableProcessors();
        final ExecutorService executor = Executors.newFixedThreadPool(count);

        System.out.println(" Going to run " + count + " in parallel");

        Collection<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Callable<String> callableTask = () -> {
                HttpUtils.findStringInReadyUrl(server, "/ReactiveConcurrentWorkTest/parallel?message=parallel" + count, FATServletClient.SUCCESS);
                String msg = "Run on thread " + Thread.currentThread().getName();
                System.out.println(msg);
                return msg;
            };
            tasks.add(callableTask);
        }

        try {
            List<Future<String>> futures = executor.invokeAll(tasks);
            String result = waitForAllFuturesToEndInClient(futures);
            System.out.println(result);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * We don't worry about the order of the results at all.
     *
     * @param futures
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws ExecutionException
     */
    private String waitForAllFuturesToEndInClient(List<Future<String>> futures) throws InterruptedException, ExecutionException, TimeoutException {
        String result = "";
        for (Iterator<Future<String>> iterator = futures.iterator(); iterator.hasNext();) {
            Future<String> future = iterator.next();
            result = result + future.get(5, TimeUnit.MINUTES) + " "; //takes ~ 20 seconds for whole testcase usually
        }
        return result;
    }

    /**
     * This sends a single request to the server. In handling this single request
     * the testcase creates a number of streams each of which have a chain of processors.
     * Within which we can observe how threads and context are used.
     *
     * @throws Exception
     */
    @Test
    public void testReactiveParallelInServer() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/ReactiveConcurrentWorkTest/parallelInServer?message=single1", FATServletClient.SUCCESS);
    }

}
