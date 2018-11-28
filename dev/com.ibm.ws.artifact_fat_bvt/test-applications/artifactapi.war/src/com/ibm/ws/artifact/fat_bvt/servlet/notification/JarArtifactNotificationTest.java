/*******************************************************************************
 * Copyright (c) 2012,2018 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

public class JarArtifactNotificationTest extends ArtifactNotificationTestImpl {

    private static final int NOTIFICATION_INTERVAL = 200;
    private static final int NOTIFICATION_SLEEP_DELAY = 30 * 1000;

    //

    public JarArtifactNotificationTest(String testName, PrintWriter writer) {
        super(testName, writer);
    }

    //

    private File sourceTestDir;
    private File sourceTestJar;

    private ArtifactContainerFactory acf;
    private OverlayContainerFactory ocf;

    private File testDir;
    private File testJar;

    private File testCacheDir;
    private File testOverlayDir;
    private File testOverlayCacheDir;

    @Override
    public boolean setup(
        File testDir,
        ArtifactContainerFactory acf, OverlayContainerFactory ocf) {

        this.sourceTestDir = testDir;
        this.sourceTestJar = new File(this.sourceTestDir, "c/b.jar");

        this.acf = acf;
        this.ocf = ocf;

        this.testDir = createTempDir( getTestName() );
        if ( this.testDir == null ) {
            println("FAIL: Failed to create test directory [ " + getTestName() + " ]");
            return false;
        }

        this.testJar = new File(this.testDir, "TEST.JAR");
        boolean didCopy = copyFile(sourceTestJar, this.testJar);
        if ( !didCopy ) {
            println("FAIL: Failed to copy [ " + sourceTestJar.getAbsolutePath() + " ]" +
                    " to [ " + this.testJar.getAbsolutePath() + " ]");
            return false;
        }

        this.testCacheDir = new File(this.testDir, "CACHE");
        this.testOverlayDir = new File(this.testDir, "OVERLAY");
        this.testOverlayCacheDir = new File(this.testDir, "OVERLAYCACHE");

        if ( !testCacheDir.mkdirs() ||
             !testOverlayDir.mkdirs() ||
             !testOverlayCacheDir.mkdirs() ) {

            println("FAIL: Failed to create cache and overlay directories");
            return false;
        }

        return true;
    }

    @Override
    public boolean tearDown() {
        return removeFile(testDir);
    }

    //

    @Override
    public boolean runSingleNonRootListenerTest() {
        String testJarPath = testJar.getAbsolutePath();
        println("Test Jar [ " + testJarPath + " ]");

        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        if ( rootContainer == null ) {
            println("FAIL: Null root container");
            return false;
        }
        ArtifactNotifier rootNotifier = getNotifier(rootContainer);
        if ( rootNotifier == null ) {
            println("FAIL: Null root notifier");
            return false;
        }

        rootNotifier.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Collection<String> jarRegistrations = Collections.<String> singleton("/ba");

        println("Test JAR Registrations:");
        for ( String jarEntry : jarRegistrations ) {
            println("  [ " + jarEntry + " ]");
        }

        ArtifactNotification wanted = new DefaultArtifactNotification(rootContainer, jarRegistrations); 

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        NotificationListener listener = new NotificationListener(invocable, invoked);

        rootNotifier.registerForNotifications(wanted, listener);

        // Because closes will be delayed, the initial close on the test JAR
        // may not work.
        //
        // Put in a delay of twice the maximum reap interval to make sure any
        // pending closes are processed.

        if ( !removeFile(testJar) ) {
            println("Initial remove failed [ " + testJarPath + " ]");

            long maxInterval = ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX / ZipCachingProperties.NANO_IN_MILLI;

            println("Zip Cache Long Interval [ " + Long.valueOf(maxInterval) + " ] ms");

            long startMs = System.currentTimeMillis();
            try {
                Thread.sleep(maxInterval * 2);
            } catch ( InterruptedException e ) {
                // Ignore
            }
            long endMs = System.currentTimeMillis();

            println("Waited for zip to release; requested [ " + Long.valueOf(maxInterval * 2) + " ] ms;" +
                    " actual [ " + Long.valueOf(endMs - startMs) + " ] ms");

            // Loop through a few times. Some external file scanner may have obtained a file handle temporarily
            boolean removed = false;
        	for ( int i = 1; (!removed && i < 5); i++) {
        		if ( !removeFile(testJar) ) {            
        			println("FAIL: Delayed remove " + i + " failed [ " + testJarPath + " ]");        		               
        		} else {
        			println("Delayed remove [ " + testJarPath + " ]");
        			removed = true;
        		}
        	}
        	if ( !removed ) {
        		println("FAIL: Tried to delete [ " + testJarPath + " ] 5 times but failed.");
        		return false;
        	}

        } else {
            println("Initial remove [ " + testJarPath + " ]");
        }

        boolean pass = true;

        boolean invokedCorrectly = waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY);

        println("Invocations:");
        for ( ArtifactListener invokedListener : invoked ) {
            NotificationListener notificationListener = (NotificationListener) invokedListener;

            Collection<ArtifactNotification> added = notificationListener.getAdded();
            for ( ArtifactNotification notification : added ) {
                for ( String path : notification.getPaths() ) {
                    println("  Added [ " + path + " ]");
                }
            }

            Collection<ArtifactNotification> removed = notificationListener.getRemoved();
            for ( ArtifactNotification notification : removed ) {
                for ( String path : notification.getPaths() ) {
                    println("  Removed [ " + path + " ]");
                }
            }

            Collection<ArtifactNotification> modified = notificationListener.getModified();
            for ( ArtifactNotification notification : modified ) {
                for ( String path : notification.getPaths() ) {
                    println("  Modified [ " + path + " ]");
                }
            }
        }

        if ( !invokedCorrectly ) {
            println("FAIL: Notification on [ /ba] was [ " + Integer.valueOf(invoked.size()) + " ] but should have been [ 1 ]");
            pass = false;
        }

        return pass;
    }

    //
    
    @Override
    public boolean testNoNotificationsAfterRemovalOfListener() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if ( an == null ) {
            return false;
        }

        ArtifactNotification wanted =
            new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener test = new NotificationListener(invocable, invoked);

        boolean pass = true;

        //add then remove the listener.. 
        an.registerForNotifications(wanted, test);
        an.removeListener(test);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 0, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: removed listener was invoked unexpectedly!");
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean runMultipleNonRootListenerTest() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/ba"));
        ArtifactNotification wantedtwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/bb"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);
        an.registerForNotifications(wantedtwo, testtwo);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 2, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: non root listener was not invoked!");
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean runMultipleOverlappingNonRootListenerTest() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/ba"));
        ArtifactNotification wantedtwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/ba/baa"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);
        an.registerForNotifications(wantedtwo, testtwo);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 2, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for multiple listener invocation.. expected both, got " + invoked.size() + " one? " + invoked.contains(testone) + " two? "
                        + invoked.contains(testtwo));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean runMultipleOverlappingRootListenerTest() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        ArtifactNotification wantedtwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/ba"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);
        an.registerForNotifications(wantedtwo, testtwo);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 2, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for multiple listener invocation.. expected both, got " + invoked.size() + " one? " + invoked.contains(testone) + " two? "
                        + invoked.contains(testtwo));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean runMultipleRootListenerTest() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        ArtifactNotification wantedtwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);
        an.registerForNotifications(wantedtwo, testtwo);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 2, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for multiple listener invocation.. expected both, got " + invoked.size() + " one? " + invoked.contains(testone) + " two? "
                        + invoked.contains(testtwo));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean runSingleRootListenerTest() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for single listener invocation.. expected one, got " + invoked.size() + " one? " + invoked.contains(testone));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean testAddRemoveOfExistingViaOverlayBecomesModified() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        OverlayContainer overlay = ocf.createOverlay(OverlayContainer.class, rootContainer);
        overlay.setOverlayDirectory(this.testOverlayCacheDir, this.testOverlayDir);
        ArtifactNotifier an = getNotifier(overlay);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(overlay, Collections.<String> singleton("/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;
        try {
            an.registerForNotifications(wantedone, testone);

            ArtifactEntry ba = overlay.getEntry("/ba");
            overlay.addToOverlay(ba, "/bb", false);

            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected for addToOverlay");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/bb/baa", "/bb/baa/baa1.txt", "/bb/baa/baa2.txt" }, new String[] {}, new String[] { "/bb" })) {
                println("FAIL: add of existing did not become modified. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlay.removeFromOverlay("/bb");
            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected for removeFromOverlay");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/bb/baa", "/bb/baa/baa1.txt", "/bb/baa/baa2.txt" }, new String[] { "/bb" })) {
                println("FAIL: add of existing did not become modified. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            if (!removeFile(testJar)) {
                println("FAIL: Unable to remove file for root notifier removal (test issue not code issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected for removeFromOverlay");
                pass = false;
            }

        } finally {
            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testBaseAndOverlaidContentWithMaskUnmask() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        OverlayContainer overlay = ocf.createOverlay(OverlayContainer.class, rootContainer);
        overlay.setOverlayDirectory(this.testOverlayCacheDir, this.testOverlayDir);
        ArtifactNotifier an = getNotifier(overlay);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(overlay, Collections.<String> singleton("/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;
        try {
            ArtifactEntry ba = overlay.getEntry("/ba");
            overlay.addToOverlay(ba, "/bb", false);

            //register to listen against overlay
            an.registerForNotifications(wantedone, testone);

            overlay.mask("/ba");

            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/ba/baa/baa2.txt", "/ba/baa", "/ba", "/ba/baa/baa1.txt" }, new String[] {})) {
                println("FAIL: mask of existing did not become removed. " + testone);
                pass = false;
            } else if (!testone.getRemoved().get(0).getPaths().contains("/ba")) {
                println("FAIL: Incorrect path in notification, expected /ba in removed. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlay.unMask("/ba");

            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/ba/baa/baa2.txt", "/ba/baa", "/ba", "/ba/baa/baa1.txt" }, new String[] {}, new String[] {})) {
                println("FAIL: unmask of existing did not become added. " + testone);
                pass = false;
            } else if (!testone.getAdded().get(0).getPaths().contains("/ba")) {
                println("FAIL: Incorrect path in notification, expected /ba in added. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlay.mask("/bb");

            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/bb/baa", "/bb/a.jar", "/bb/baa/baa1.txt", "/bb", "/bb/baa/baa2.txt" }, new String[] {})) {
                println("FAIL: mask of existing did not become removed. " + testone);
                pass = false;
            } else if (!testone.getRemoved().get(0).getPaths().contains("/bb")) {
                println("FAIL: Incorrect path in notification, expected /bb in removed. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlay.unMask("/bb");

            if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/bb/baa", "/bb/a.jar", "/bb/baa/baa1.txt", "/bb", "/bb/baa/baa2.txt" }, new String[] {}, new String[] {})) {
                println("FAIL: unmask of existing did not become added. " + testone);
                pass = false;
            } else if (!testone.getAdded().get(0).getPaths().contains("/bb")) {
                println("FAIL: Incorrect path in notification, expected /bb in added. " + testone);
                pass = false;
            }

        } finally {
            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testMultipleListenersViaOverlay() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        OverlayContainer overlay = ocf.createOverlay(OverlayContainer.class, rootContainer);
        overlay.setOverlayDirectory(this.testOverlayCacheDir, this.testOverlayDir);
        ArtifactNotifier an = getNotifier(overlay);
        if (an == null)
            return false;

        ArtifactNotification wantedone =
            new DefaultArtifactNotification(overlay, Collections.<String> singleton("/"));

        @SuppressWarnings("unused")
        ArtifactNotification wantedtwo =
            new DefaultArtifactNotification(overlay, Collections.<String> singleton("/"));

        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);

        boolean pass = true;
        try {
            ArtifactEntry ba = overlay.getEntry("/ba");
            overlay.addToOverlay(ba, "/addedtooverlay", false);

            an.registerForNotifications(wantedone, testone);
            an.registerForNotifications(wantedone, testtwo);

            overlay.removeFromOverlay("/addedtooverlay");
            if (!waitForInvoked(invoked, 2, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

        } finally {
            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testNestedChangeCausesEntryChange() {
        //nested content for a jar/zip are ZipEntries within the zip structure,
        //which cannot be altered except by deleting the entire jar, which causes
        //notifications against all entries inside.
        //this means that the only way to change a nested container is to delete the 
        //current zip, which is already tested above.
        println("Nested Causes Entry Change (No-Op for Jar)");
        return true;
    }

    @Override
    public boolean testNonExistingUnderOverlay() {
        //the only tests that can modify existing under overlay are removes, which have been
        //driven by the other overlay tests within this class, so this test case can be a no-op.
        println("Non Existing Under Overlay (No-Op for Jar)");
        return true;
    }

    @Override
    public boolean testNonRecursiveMixedNotification() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Arrays.asList(new String[] { "/", "!/" }));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedOne, testone);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for single listener invocation.. expected one, got " + invoked.size() + " one? " + invoked.contains(testone));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean testNonRecursiveNotificationAtNonRoot() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/bb"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);

        //kill the jar file 
        removeFile(testJar);
        if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for single listener invocation.. expected one, got " + invoked.size() + " one? " + invoked.contains(testone));
            pass = false;
        }
        return pass;
    }

    @Override
    public boolean testNonRecursiveNotificationAtRoot() {
        ArtifactContainer rootContainer = acf.getContainer(testCacheDir, testJar);
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedone = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("!/"));
        an.setNotificationOptions(NOTIFICATION_INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);

        boolean pass = true;

        an.registerForNotifications(wantedone, testone);

        removeFile(testJar);
        if (!waitForInvoked(invoked, 1, NOTIFICATION_INTERVAL, NOTIFICATION_SLEEP_DELAY)) {
            println("FAIL: incorrect count for single listener invocation.. expected one, got " + invoked.size() + " one? " + invoked.contains(testone));
            pass = false;
        }
        boolean found = false;
        for (ArtifactNotification z : testone.getRemoved()) {
            if (z.getPaths().contains("/")) {
                found = true;
            }
        }
        if (!found) {
            println("FAIL: delete of root did not cause / notifiy for !/ listener");
            pass = false;
        }
        return pass;
    }

}
