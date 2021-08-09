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

import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

public class BundleArtifactNotificationTest extends ArtifactNotificationTestImpl {

    public BundleArtifactNotificationTest(String testName, PrintWriter writer) {
        super(testName, writer);
    }

    //

    @Override
    public boolean setup(
        File testDir,
        ArtifactContainerFactory acf, OverlayContainerFactory ocf) {
        return true;
    }

    @Override
    public boolean tearDown() {
        return true;
    }

    //

    @Override
    public boolean runSingleRootListenerTest() {
        return true;
    }

    //

    @Override
    public boolean runMultipleNonRootListenerTest() {
        return true;
    }

    @Override
    public boolean runMultipleOverlappingNonRootListenerTest() {
        return true;
    }

    @Override
    public boolean runMultipleOverlappingRootListenerTest() {
        return true;
    }

    @Override
    public boolean runMultipleRootListenerTest() {
        return true;
    }

    @Override
    public boolean runSingleNonRootListenerTest() {
        return true;
    }

    //

    @Override
    public boolean testNoNotificationsAfterRemovalOfListener() {
        return true;
    }

    @Override
    public boolean testAddRemoveOfExistingViaOverlayBecomesModified() {
        return true;
    }

    @Override
    public boolean testBaseAndOverlaidContentWithMaskUnmask() {
        return true;
    }

    @Override
    public boolean testMultipleListenersViaOverlay() {
        return true;
    }

    @Override
    public boolean testNestedChangeCausesEntryChange() {
        return true;
    }

    @Override
    public boolean testNonExistingUnderOverlay() {
        return true;
    }

    @Override
    public boolean testNonRecursiveMixedNotification() {
        return true;
    }

    @Override
    public boolean testNonRecursiveNotificationAtNonRoot() {
        return true;
    }

    @Override
    public boolean testNonRecursiveNotificationAtRoot() {
        return true;
    }
}
