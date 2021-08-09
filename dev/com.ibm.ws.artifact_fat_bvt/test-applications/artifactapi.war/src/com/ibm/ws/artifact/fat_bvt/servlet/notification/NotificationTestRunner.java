/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet.notification;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

public class NotificationTestRunner {

    private static class NotificationTestThread extends Thread {
        public NotificationTestThread(ArtifactNotificationTest test) {
            super( test.getTestName() );

            this.test = test;
            this.result = null;
        }

        //

        private final ArtifactNotificationTest test;

        public ArtifactNotificationTest getTest() {
            return test;
        }

        //

        protected volatile Boolean result;

        protected void setResult(boolean result) {
            this.result = Boolean.valueOf(result);
        }

        public Boolean getResult() {
            return this.result;
        }
    }

    private NotificationTestThread createTestThread(
        final ArtifactNotificationTest notificationTest,
        final File testDir,
        final ArtifactContainerFactory acf,
        final OverlayContainerFactory ocf) {

        return new NotificationTestThread(notificationTest) {
            @Override
            public void run() {
                ArtifactNotificationTest useTest = getTest();

                boolean primResult;

                try {
                    primResult = useTest.setup(testDir, acf, ocf);

                    if ( primResult ) {
                        primResult = useTest.runSingleNonRootListenerTest();

                        if ( primResult ) {
                            useTest.tearDown();
                        }
                    }

                } catch ( Throwable t ) {
                    primResult = false;

                    useTest.println("FAIL: Execption " + t.getMessage());
                    useTest.printStackTrace(t);
                }

                setResult(primResult);
            }
        };
    }

    public void runNotificationTests(
        File testDir,
        ArtifactContainerFactory acf, OverlayContainerFactory ocf,
        PrintWriter out) {

        ArtifactNotificationTest[] notifierTests = new ArtifactNotificationTest[] {
            new FileArtifactNotificationTest("fileTest", out),
            new JarArtifactNotificationTest("jarTest", out),
            new BundleArtifactNotificationTest("bundleTest", out),
            new LooseArtifactNotificationTest("looseTest", out)
        };

        List<NotificationTestThread> testThreads = new ArrayList<NotificationTestThread>();
        for ( ArtifactNotificationTest notifierTest : notifierTests ) {
            testThreads.add( createTestThread(notifierTest, testDir, acf, ocf) );
        }

        // Used to be 60s, but that's not enough for some machines.
        int waitMs = 120 * 1000;
        int pollMs = 50;

        long startMs = System.currentTimeMillis();
        long endMs = startMs + waitMs;

        for ( Thread testThread : testThreads ) {
            testThread.start();
        }

        boolean allPassed = true;

        while ( !testThreads.isEmpty() && System.currentTimeMillis() < endMs ) {
            try {
                Thread.sleep(pollMs);
            } catch ( InterruptedException e ) {
                // Ignore
            }
            long durationMs = System.currentTimeMillis() - startMs;

            List<NotificationTestThread> completedTestThreads =
                new ArrayList<NotificationTestThread>();
            List<NotificationTestThread> incompleteTestThreads =
                new ArrayList<NotificationTestThread>();
            for ( NotificationTestThread testThread : testThreads ) {
                if ( testThread.getResult() != null ) {
                    completedTestThreads.add(testThread);
                } else {
                    incompleteTestThreads.add(testThread);
                }
            }
            testThreads = incompleteTestThreads;

            for ( NotificationTestThread completedTestThread : completedTestThreads ) {
                String testName = completedTestThread.getName();
                boolean testResult = completedTestThread.getResult().booleanValue();

                out.println("Test [ " + testName + " ] Result [ " + Boolean.valueOf(testResult) + " ] in [ " + Long.valueOf(durationMs) + " ] ms");

                if ( !testResult ) {
                    allPassed = false;
                }
            }
        }

        long finalDurationMs = System.currentTimeMillis() - startMs;

        for ( NotificationTestThread incompleteTestThread : testThreads ) {
            String testName = incompleteTestThread.getName();
            out.println("Test [ " + testName + " ] Result [ false (timed out) ] in [ " + Long.valueOf(finalDurationMs) + " ] ms");
        }

        String resultText;
        if ( testThreads.isEmpty() ) {
            if ( allPassed ) {
                resultText = "PASS: All tests completed on time; all completed tests were successful";
            } else {
                resultText = "FAIL: All tests completed on time; at least one completed test failed";
            }
        } else {
            if ( allPassed ) {
                resultText = "FAIL: At least one test did not complete; all completed tests were successful";
            } else {
                resultText = "FAIL: At least one test did not complete; at least one completed test failed";
            }
        }
        out.println(resultText);
    }
}
