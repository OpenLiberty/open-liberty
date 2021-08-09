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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

public class FileArtifactNotificationTest extends ArtifactNotificationTestImpl {

    public FileArtifactNotificationTest(String testName, PrintWriter writer) {
        super(testName, writer);
    }

    //

    private File baseDir = null;
    private File dataDir = null;
    private File cacheDir = null;
    private File overlayDir = null;
    private File overlayCacheDir = null;
    private ArtifactContainer rootContainer = null;
    private OverlayContainer overlayContainer = null;

    private final int INTERVAL = 200;
    private final int SLEEP_DELAY = 30 * 1000;

    @Override
    public boolean setup(
        File testDir,
        ArtifactContainerFactory acf, OverlayContainerFactory ocf) {

        File base = createTempDir( getTestName() );
        if (base != null) {
            this.baseDir = base;
            //add a request to kill the dir when we're done.. 
            //we'll also tidy it up in tearDown, but this way makes setup easier.
            this.baseDir.deleteOnExit();

            this.dataDir = new File(baseDir, "DATA");
            this.cacheDir = new File(baseDir, "CACHE");
            this.overlayDir = new File(baseDir, "OVERLAY");
            this.overlayCacheDir = new File(baseDir, "OVERLAYCACHE");

            boolean mkdirsOk = true;
            mkdirsOk &= dataDir.mkdirs();
            mkdirsOk &= cacheDir.mkdirs();
            mkdirsOk &= overlayDir.mkdirs();
            mkdirsOk &= overlayCacheDir.mkdirs();

            if (mkdirsOk) {
                this.rootContainer = acf.getContainer(cacheDir, dataDir);
                if (rootContainer == null) {
                    println("FAIL: unable to create ArtifactContainer for root File in File Artifact Notifier testcase. (test case issue, not code issue)");
                    return false;
                }
                this.overlayContainer = ocf.createOverlay(OverlayContainer.class, rootContainer);
                if (overlayContainer == null) {
                    println("FAIL: unable to create unable to create OverlayContainer for root File in for File Artifact Notifier testcase. (test case issue, not code issue)");
                    return false;
                }
                this.overlayContainer.setOverlayDirectory(overlayCacheDir, overlayDir);
            } else {
                println("FAIL: unable to create dirs for File Artifact Notifier testcase. (test case issue, not code issue)");
                return false;
            }

            return true;
        } else {
            println("FAIL: unable to create temp dir for File Artifact Notifier testcase. (test case issue, not code issue)");
            return false;
        }
    }

    @Override
    public boolean tearDown() {
        return removeFile(baseDir);
    }

    @Override
    public boolean testNoNotificationsAfterRemovalOfListener() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wanted = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener test = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wanted, test);

        boolean pass = true;
        File createTest = null;
        try {
            //make a file
            createTest = new File(dataDir, "test");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }
            //reset the sets
            invocable.add(test);
            invoked.clear();

            //clear us from the listener set..
            an.removeListener(test);

            //remove the file..
            if (!removeFile(createTest)) {
                println("FAIL: unable to remove file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 0, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was invoked unexpectedly");
                pass = false;
            }
            //reset the sets
            invocable.add(test);
            invoked.clear();

            //try with 2 listeners.. 
            NotificationListener test2 = new NotificationListener(invocable, invoked);
            an.registerForNotifications(wanted, test2);
            an.registerForNotifications(wanted, test);

            //make sure 2 is in the set.
            invocable.add(test2);

            //create the file..
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listeners were not invoked as expected");
                pass = false;
            }
            //reset the sets for after removal..
            invocable.add(test);
            invocable.add(test2);
            invoked.clear();

            //remove one of the listeners.. 
            an.removeListener(test2);

            //remove the file..
            if (!removeFile(createTest)) {
                println("FAIL: unable to remove file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: remaining listener was not invoked as expected invoked :" + invoked + " invocable :" + invocable);
                pass = false;
            }

            //deregister the last listener.. (just being clean here) 
            an.removeListener(test);

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            io.printStackTrace();
            pass = false;
        }
        //kill that test file if somehow it's still there. 
        removeFile(createTest);
        return pass;
    }

    @Override
    public boolean runMultipleNonRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/one"));
        ArtifactNotification wantedTwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/two"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);
        an.registerForNotifications(wantedTwo, testtwo);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //make a file
            nested = new File(createTest, "test");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        }
        //kill that test file if somehow it's still there. 
        removeFile(createTest);
        removeFile(nested);
        removeFile(other);
        return pass;
    }

    @Override
    public boolean runMultipleOverlappingNonRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/one"));
        ArtifactNotification wantedTwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/one/two"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);
        an.registerForNotifications(wantedTwo, testtwo);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected [i] invocable:" + invocable + " invoked:" + invoked);
                pass = false;
            }

            //reset the sets
            invocable.clear();
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected [ii] invocable:" + invocable + " invoked:" + invoked);
                pass = false;
            }

            //reset the sets
            invocable.clear();
            testone.getAdded().clear();
            testtwo.getAdded().clear();
            testone.getRemoved().clear();
            testtwo.getRemoved().clear();
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 0, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was invoked as unexpectedly [iii] invocable:" + invocable + " invoked:" + invoked);
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        }
        //kill that test file if somehow it's still there. 
        removeFile(createTest);
        removeFile(nested);
        removeFile(other);
        return pass;
    }

    @Override
    public boolean runMultipleOverlappingRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        ArtifactNotification wantedTwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/one"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);
        an.registerForNotifications(wantedTwo, testtwo);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        }
        //kill that test file if somehow it's still there. 
        removeFile(createTest);
        removeFile(nested);
        removeFile(other);
        return pass;
    }

    @Override
    public boolean runMultipleRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        ArtifactNotification wantedTwo = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);
        an.registerForNotifications(wantedTwo, testtwo);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        }
        //kill that test file if somehow it's still there. 
        removeFile(createTest);
        removeFile(nested);
        removeFile(other);
        return pass;
    }

    @Override
    public boolean runSingleNonRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/one"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 0, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean runSingleRootListenerTest() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("/"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testAddRemoveOfExistingViaOverlayBecomesModified() {
        ArtifactNotifier on = getNotifier(overlayContainer);
        if (on == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/"));
        on.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        //don't register yet.. 

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {

            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            createTest = new File(dataDir, "other");
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }

            //register to listen against overlay
            on.registerForNotifications(wantedOne, testone);

            //override the file via the overlay.
            ArtifactEntry e = rootContainer.getEntry("/other");
            if (e == null) {
                println("FAIL: unable to obtaine created entry via overlay (overlay issue, not notifier issue)");
                pass = false;
            }
            overlayContainer.addToOverlay(e, "/one", false); //set entry into overlay at path "/one"            

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] {}, new String[] { "/one" })) {
                println("FAIL: add of existing did not become modified. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlayContainer.removeFromOverlay("/one");

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] {}, new String[] { "/one" })) {
                println("FAIL: remove of existing did not become modified. " + testone);
                pass = false;
            }

        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            on.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testBaseAndOverlaidContentWithMaskUnmask() {
        ArtifactNotifier on = getNotifier(overlayContainer);
        if (on == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/"));
        on.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        //don't register yet.. 

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {

            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            createTest = new File(dataDir, "other");
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }

            //override the file via the overlay.
            ArtifactEntry e = rootContainer.getEntry("/other");
            if (e == null) {
                println("FAIL: unable to obtaine created entry via overlay (overlay issue, not notifier issue)");
                pass = false;
            }
            overlayContainer.addToOverlay(e, "/one", false); //set entry into overlay at path "/one"

            //register to listen against overlay
            on.registerForNotifications(wantedOne, testone);

            overlayContainer.mask("/one");

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/one" }, new String[] {})) {
                println("FAIL: mask of existing did not become removed. " + testone);
                pass = false;
            } else if (!testone.getRemoved().get(0).getPaths().contains("/one")) {
                println("FAIL: Incorrect path in notification, expected /one in removed. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlayContainer.unMask("/one");

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/one" }, new String[] {}, new String[] {})) {
                println("FAIL: unmask of existing did not become added. " + testone);
                pass = false;
            } else if (!testone.getAdded().get(0).getPaths().contains("/one")) {
                println("FAIL: Incorrect path in notification, expected /one in added. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlayContainer.mask("/other");

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/other" }, new String[] {})) {
                println("FAIL: mask of existing did not become removed. " + testone);
                pass = false;
            } else if (!testone.getRemoved().get(0).getPaths().contains("/other")) {
                println("FAIL: Incorrect path in notification, expected /other in removed. " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            overlayContainer.unMask("/other");

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/other" }, new String[] {}, new String[] {})) {
                println("FAIL: unmask of existing did not become added. " + testone);
                pass = false;
            } else if (!testone.getAdded().get(0).getPaths().contains("/other")) {
                println("FAIL: Incorrect path in notification, expected /other in added. " + testone);
                pass = false;
            }

        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            on.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testMultipleListenersViaOverlay() {
        ArtifactNotifier on = getNotifier(overlayContainer);
        if (on == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/one"));
        ArtifactNotification wantedTwo = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/two"));
        ArtifactNotification wantedThree = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/one/test"));
        on.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        NotificationListener testtwo = new NotificationListener(invocable, invoked);
        NotificationListener testthree = new NotificationListener(invocable, invoked);

        on.registerForNotifications(wantedOne, testone);
        on.registerForNotifications(wantedTwo, testtwo);
        on.registerForNotifications(wantedThree, testthree);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY) || !invoked.contains(testone)) {
                println("FAIL: listener was not invoked as expected invoked:" + invoked + " invocable:" + invocable);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invocable.add(testthree);
            invoked.clear();

            //make a file
            nested = new File(createTest, "test");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 2, INTERVAL, SLEEP_DELAY) || !invocable.contains(testtwo)) {
                println("FAIL: listener was not invoked as expected invoked:" + invoked + " invocable:" + invocable);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invocable.add(testtwo);
            invocable.add(testthree);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY) || !invoked.contains(testtwo)) {
                println("FAIL: listener was not invoked as expected invoked:" + invoked + " invocable:" + invocable);
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            on.removeListener(testone);
            on.removeListener(testtwo);
            on.removeListener(testthree);
        }
        return pass;
    }

    @Override
    public boolean testNestedChangeCausesEntryChange() {
        //this one is a no-op for file, since any nested container is 
        //represented by a file, and thus will cause a change on that file anyways.
        println("Nested Causes Entry Change (No-Op for File)");
        return true;
    }

    @Override
    public boolean testNonExistingUnderOverlay() {
        ArtifactNotifier on = getNotifier(overlayContainer);
        if (on == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(overlayContainer, Collections.<String> singleton("/"));
        on.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        on.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {

            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] { "/one" }, new String[] {}, new String[] {})) {
                println("FAIL: create did not become added for overlay " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            if (!removeFile(createTest)) {
                println("FAIL: unable to delete file created to test overlay passthru (testcase issue, not a notification error)");
                pass = false;
            }

            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            if (!verifyCounts(testone, new String[] {}, new String[] { "/one" }, new String[] {})) {
                println("FAIL: delete did not become removed for overlay " + testone);
                pass = false;
            }

        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            on.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testNonRecursiveMixedNotification() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Arrays.asList(new String[] { "/", "!/" }));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testNonRecursiveNotificationAtNonRoot() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("!/one"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked once as expected " + invoked);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was invoked when not expected " + invoked);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 0, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked once as expected " + invoked);
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            an.removeListener(testone);
        }
        return pass;
    }

    @Override
    public boolean testNonRecursiveNotificationAtRoot() {
        ArtifactNotifier an = getNotifier(rootContainer);
        if (an == null)
            return false;

        ArtifactNotification wantedOne = new DefaultArtifactNotification(rootContainer, Collections.<String> singleton("!/"));
        an.setNotificationOptions(INTERVAL, false);

        Set<ArtifactListener> invocable = Collections.synchronizedSet(new HashSet<ArtifactListener>());
        Set<ArtifactListener> invoked = Collections.synchronizedSet(new HashSet<ArtifactListener>());

        NotificationListener testone = new NotificationListener(invocable, invoked);
        an.registerForNotifications(wantedOne, testone);

        boolean pass = true;
        File createTest = null;
        File nested = null;
        File other = null;
        try {
            //make a file
            createTest = new File(dataDir, "one");
            if (!createTest.mkdirs()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            nested = new File(createTest, "two");
            if (!nested.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            //should not trigger, two is nested too far for !/
            if (!waitForInvoked(invoked, 0, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected");
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();

            //make a file
            other = new File(dataDir, "two");
            if (!other.createNewFile()) {
                println("FAIL: could not create test file for File Notifier test (test case issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected " + testone);
                pass = false;
            }

            //reset the sets
            invocable.add(testone);
            invoked.clear();
            testone.getAdded().clear();
            testone.getRemoved().clear();
            testone.getModified().clear();

            if (!removeFile(dataDir)) {
                println("FAIL: unable to remove base data dir to test root deletion (test issue, not code issue)");
                pass = false;
            }
            if (!waitForInvoked(invoked, 1, INTERVAL, SLEEP_DELAY)) {
                println("FAIL: listener was not invoked as expected " + testone);
                pass = false;
            }

            boolean found = false;
            for (ArtifactNotification z : testone.getRemoved()) {
                if (z.getPaths().contains("/")) {
                    found = true;
                }
            }
            if (!found) {
                println("FAIL: delete of root did not cause / notify for !/ listener " + testone);
                pass = false;
            }

            //end, dir should be clean now too.
        } catch (IOException io) {
            println("FAIL: io exception during File Notifier testcase");
            printStackTrace(io);
            pass = false;
        } finally {
            //kill that test file if somehow it's still there. 
            removeFile(createTest);
            removeFile(nested);
            removeFile(other);

            an.removeListener(testone);
        }
        return pass;
    }

}
