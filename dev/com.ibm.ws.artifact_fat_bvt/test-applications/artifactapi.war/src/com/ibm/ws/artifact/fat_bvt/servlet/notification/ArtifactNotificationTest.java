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

import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

public interface ArtifactNotificationTest {
    String getTestName();

    void println(String text);
    void printStackTrace(Throwable th);

    //

    boolean setup(
        File testDir,
        ArtifactContainerFactory acf, OverlayContainerFactory ocf);

    boolean tearDown();

    //

    /**
     * Test a single listener with path of "/" with add/remove/modified
     */
    boolean runSingleRootListenerTest();

    /**
     * Test multiple listeners with path of "/" with add/remove/modified
     */
    boolean runMultipleRootListenerTest();

    /**
     * Test a single listener with path deeper than "/" with add/remove/modified
     */
    boolean runSingleNonRootListenerTest();

    /**
     * Test multiple listeners with (non-overlapping) paths deeper than "/" with add/remove/modified
     */
    boolean runMultipleNonRootListenerTest();

    /**
     * Test a multiple listeners with (overlapping) paths where at least one path is "/" with add/remove/modified
     */
    boolean runMultipleOverlappingRootListenerTest();

    /**
     * Test a multiple listeners with (overlapping) paths deeper than "/" with add/remove/modified
     */
    boolean runMultipleOverlappingNonRootListenerTest();

    /**
     * Test non recursive notification at root
     */
    boolean testNonRecursiveNotificationAtRoot();

    /**
     * Test non recursive notification at non root
     */
    boolean testNonRecursiveNotificationAtNonRoot();

    /**
     * Test non recursive notification at root
     */
    boolean testNonRecursiveMixedNotification();

    /**
     * Test that notifications stop after the listener is removed..
     */
    boolean testNoNotificationsAfterRemovalOfListener();

    /**
     * Test that adding/removing to an overlay of content present in the base, becomes a modified notificaton
     */
    boolean testAddRemoveOfExistingViaOverlayBecomesModified();

    /**
     * Test that add/remove/modify under an overlay are passed through, with the container becoming the overlay.
     */
    boolean testNonExistingUnderOverlay();

    /**
     * Test multiple listeners via overlay (some overlapping, some non-overlapping paths).
     */
    boolean testMultipleListenersViaOverlay();

    /**
     * Test nested change causes entry change
     */
    boolean testNestedChangeCausesEntryChange();

    /**
     * Test mask/unmask with base & overlaid content.
     * 
     * mask paths within the overlay, and confirm add remove for existing content, and nothing for non-existing
     * confirm masks causes events on mask/unmask
     * confirm masks affect events for affected paths after mask/unmask
     * confirm masks affect events for overlay-only, base-only, and both, content.
     */
    boolean testBaseAndOverlaidContentWithMaskUnmask();
}
