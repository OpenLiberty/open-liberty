/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.adaptable.module.internal;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.Notifier;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.Notifier.NotificationListener;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * This tests the {@link InterpretedNotifier}.
 */
public class InterpretedNotifierTest {

    /**
     * This test makes sure that if an interpreted notifier is constructed for a single fake root then it uses the right paths in its registration and notification.
     * 
     * @throws Exception
     */
    @Test
    public void testNotificationPaths() throws Exception {
        // Create the mock artifact API structure
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);

        // Use this to create an interpreted container, only want one root
        InterpretedContainer interpretedContainer = createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // We'll register /c against the fake root at /a/b. First though need to set up a test to make sure that we register against the artifact API notifier at b with path /a/b/c
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b/c"), rootOverlay);

        // Now we need to add a mock for the callback, make sure the path has been corrected to just /c even though it is /a/b/c on the artifact API
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.singleton("/z"))),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/x"))),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c"))));
            }
        });

        /*
         * Now run the test, adapt to the notifier and register the mock listener that we have set up to test the callback
         */
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/c")), notificationListener);

        /*
         * When we register against the artifact API notification framework we have a custom matcher that grabs the listener registered so that we can now callback to it as though
         * /a/b/c was changed, /a/b/z was added and /a/b/x was removed
         */
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/z")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/x")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c")));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if an interpreted notifier is constructed for a single fake root then it uses the right paths in its registration and notification when it is the
     * root element that is changing.
     * 
     * @throws Exception
     */
    @Test
    public void testNotificationPathsForRoot() throws Exception {
        // Create the mock artifact API structure
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);

        // Use this to create an interpreted container, only want one root
        InterpretedContainer interpretedContainer = createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // We'll register /c against the fake root at /a/b. First though need to set up a test to make sure that we register against the artifact API notifier at b with path /a/b/c
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        // Now we need to add a mock for the callback, make sure the path has been corrected to just /c even though it is /a/b/c on the artifact API
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/"))));
            }
        });

        /*
         * Now run the test, adapt to the notifier and register the mock listener that we have set up to test the callback. Use root as that is what we are testing here
         */
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);

        /*
         * When we register against the artifact API notification framework we have a custom matcher that grabs the listener registered so that we can now callback to it as though
         * /a/b was changed (this is the fake root)
         */
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b")));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if you register a Container that has a different root than the one used by the notifier then it will fail.
     * 
     * @throws Exception
     */
    @Test
    public void testWrongRoot() throws Exception {
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, twoRootsStructureHelper);

        Container bInterpreted = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        Notifier testObject = interpretedContainer.adapt(Notifier.class);
        Container dInterpreted = bInterpreted.getEntry("/c/d").adapt(Container.class);
        Container eInterpreted = dInterpreted.getEntry("/e").adapt(Container.class);
        try {
            testObject.registerForNotifications(new DefaultNotification(eInterpreted, Collections.singleton("/a/b/")), mockery.mock(NotificationListener.class));
            fail("The registration was allowed for a container with a different root to the notifiers root");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * This test makes sure that if a modified event in a second root is triggered then it just gets passed back to the callback as though the fake artifact root had changed and
     * doesn't delve into the second root.
     * 
     * @throws Exception
     */
    @Test
    public void testModifiedEventInOtherRoot() throws Exception {
        // We need to set up the fake root with the structure helper to create two roots
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, twoRootsStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        // The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "d" even though it was "e" that was notified against
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c/d"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/e")));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if a deleted event in a second root is triggered then it just gets passed back to the callback as though the fake artifact root had changed and
     * doesn't delve into the second root.
     * 
     * @throws Exception
     */
    @Test
    public void testDeletedEventInOtherRoot() throws Exception {
        // We need to set up the fake root with the structure helper to create two roots
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, twoRootsStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        /*
         * The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "d" even though it was "e" that was notified against and it was a
         * delete
         */
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c/d"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/e")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if an added event in a second root is triggered then it just gets passed back to the callback as though the fake artifact root had changed and
     * doesn't delve into the second root.
     * 
     * @throws Exception
     */
    @Test
    public void testAddedEventInOtherRoot() throws Exception {
        // We need to set up the fake root with the structure helper to create two roots
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, twoRootsStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        /*
         * The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "d" even though it was "e" that was notified against and it was a
         * added
         */
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c/d"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/e/f")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if an added, removed and modified event in a second root is triggered then it just gets passed back to the callback as though the fake artifact
     * root had changed and doesn't delve into the second root. It also tests that it is grouped into a single modified event rather than multiple.
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleEventsInOtherRoot() throws Exception {
        // We need to set up the fake root with the structure helper to create two roots
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, twoRootsStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        /*
         * The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "d" even though it was "e" that was notified against and it was a
         * added
         */
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c/d"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);
        Collection<String> modifiedStrings = new HashSet<String>();
        modifiedStrings.add("/a/b/c/d/e");
        modifiedStrings.add("/a/b/c/d/f");
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/f")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/e")),
                                                               new DefaultArtifactNotification(rootOverlay, modifiedStrings));

        // Make sure the right paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if an interpreted notifier is constructed for a single fake root then it uses the right paths in its registration and notification when the
     * registration happens on a sibling to the notification path.
     * 
     * @throws Exception
     */
    @Test
    public void testNotificationPathsOnSibling() throws Exception {
        // Create the mock artifact API structure
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);

        // Use this to create an interpreted container, only want one root
        InterpretedContainer interpretedContainer = createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // We'll register /c against the fake root at /a/b. First though need to set up a test to make sure that we register against the artifact API notifier at b with path /a/b
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        // Now we need to add a mock for the callback, make sure the path has been corrected to just /c even though it is /a/b/c on the artifact API
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c"))));
            }
        });

        /*
         * Now run the test, adapt to the notifier and register the mock listener that we have set up to test the callback. Note that we are adapting "n" which is a sibling of "c"
         * which is the path that the notification is going to come in on. This should be ok though as we are registering against the root which is shared by n and c
         */
        Container siblingContainer = interpretedRoot.getEntry("n").adapt(Container.class);
        Notifier testObject = siblingContainer.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);

        /*
         * When we register against the artifact API notification framework we have a custom matcher that grabs the listener registered so that we can now callback to it as though
         * /a/b/c was changed
         */
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c")));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This test makes sure that if two fake roots are directly under each other then changes to the second root have the right path for the modified in the first root.
     * 
     * @throws Exception
     */
    @Test
    public void testEventOnAdjacentRoots() throws Exception {
        // We need to set up the fake root with the structure helper to create two adjacent roots
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, adjacentRootsStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("/a/b"), rootOverlay);

        /*
         * The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "c" even though it was "f" that was notified against and it was an
         * added
         */
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("/")), notificationListener);
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c/d/e/f")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This tests that if there is a listener registered on the root adaptable and an event happens in an interpreted root then you still get the effect of making it look like a
     * modified call on the fake root.
     */
    @Test
    public void testRootNotifierWithEventOnFakeRoot() throws Exception {
        // We need to set up the fake root with the structure helper to create a single fake root
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        final InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // We also need to mock the artifact notifier to expect a registration
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/", Collections.singleton("/"), rootOverlay);

        /*
         * The final bit of setup is to add a mock to make sure that the callback only tells us about a change to "c" even though it was "f" that was notified against and it was an
         * added
         */
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedContainer, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedContainer, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedContainer, Collections.singleton("/a/b"))));
            }
        });

        // Time to run the test... grab the notifier, register against it then use the artifact listener grabbed by the matcher above to do a callback on the nested root artifact
        Notifier testObject = interpretedContainer.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedContainer, Collections.singleton("/")), notificationListener);
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/n")),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * Test to make sure you get the same notifier object on multiple calls to apdapt.
     * 
     * @throws UnableToAdaptException
     */
    @Test
    public void testSameNotifierOnMultipleCalls() throws UnableToAdaptException {
        // Create a mock artifact layer to use in the test
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);
        final InterpretedContainer interpretedContainer = this.createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // Try adapting the root and a child of the root
        Container root = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        Notifier rootNotifier = root.adapt(Notifier.class);
        Container child = root.getEntry("/c").adapt(Container.class);
        Notifier childNotifier = child.adapt(Notifier.class);
        assertSame("The two notifiers were different for root and a child", rootNotifier, childNotifier);
    }

    /**
     * This test makes sure that if a path has ! on the front then it is converted to the right path to pass into the artifact layer
     * 
     * @throws Exception
     */
    @Test
    public void testNonRecursiveMonitor() throws Exception {
        // Create the mock artifact API structure
        final Mockery mockery = new Mockery();
        final OverlayContainer rootOverlay = this.mockArtifactDataStructure(mockery);

        // Use this to create an interpreted container, only want one root
        InterpretedContainer interpretedContainer = createInterpretedContainer(mockery, rootOverlay, oneRootStructureHelper);

        // We'll register !/c against the fake root at /a/b. Therefore the mocker needs to make sure the "!" appears at the front of the whole string
        final CaptureNotificationListener notificationListenerMatcher = this.mockRegisterAgainstArtifactNotifier(mockery, "/a/b", Collections.singleton("!/a/b/c"), rootOverlay);

        // Now we need to add a mock for the callback, make sure the path has been corrected to just /c even though it is /a/b/c on the artifact API
        final Container interpretedRoot = interpretedContainer.getEntry("/a/b").adapt(Container.class);
        final NotificationListener notificationListener = mockery.mock(NotificationListener.class);
        mockery.checking(new Expectations() {
            {
                oneOf(notificationListener).notifyEntryChange(with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.<String> emptySet())),
                                                              with(new NotificationMatcher(interpretedRoot, Collections.singleton("/c"))));
            }
        });

        /*
         * Now run the test, adapt to the notifier and register the mock listener that we have set up to test the callback. The point of this test is to use ! on the path, it
         * should be moved to the front of the whole path
         */
        Notifier testObject = interpretedRoot.adapt(Notifier.class);
        testObject.registerForNotifications(new DefaultNotification(interpretedRoot, Collections.singleton("!/c")), notificationListener);

        /*
         * Make sure the callback is correct without any ! in it
         */
        notificationListenerMatcher.listener.notifyEntryChange(new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.<String> emptySet()),
                                                               new DefaultArtifactNotification(rootOverlay, Collections.singleton("/a/b/c")));

        // Make sure the righ paths were passed into the calls
        mockery.assertIsSatisfied();
    }

    /**
     * This method will mock a call to the notifier registered against the <code>root</code> container. It will use {@link Expectations#oneOf(Object)} so it will only work for one
     * {@link ArtifactNotifier#registerForNotifications(ArtifactNotification, ArtifactListener)} call.
     * 
     * @param mockery The mock context being used
     * @param expectedRootArtifactPath The path to the artifact container that is expected to be the root of the register call
     * @param expectedPaths The paths that are expected to be registered against the notifier
     * @param root The root of the artifact API
     * @return The {@link CaptureNotificationListener} that will capture what listener was registered against the notifier which is useful for performing callbacks
     */
    private CaptureNotificationListener mockRegisterAgainstArtifactNotifier(final Mockery mockery, final String expectedRootArtifactPath, final Collection<String> expectedPaths,
                                                                            final ArtifactContainer root) {
        final ArtifactNotifier notifier = root.getArtifactNotifier();
        final ArtifactContainer bContainer = ("/".equals(expectedRootArtifactPath)) ? root : root.getEntry(expectedRootArtifactPath).convertToContainer();
        final CaptureNotificationListener notificationListenerMatcher = new CaptureNotificationListener();
        mockery.checking(new Expectations() {
            {
                oneOf(notifier).registerForNotifications(with(new ArtifactNotificationMatcher(bContainer, expectedPaths)), with(notificationListenerMatcher));
            }
        });
        return notificationListenerMatcher;
    }

    /** This structure helper creates a single root at "b" (to be used with the artifact structure created by {@link #mockArtifactDataStructure(Mockery)}) */
    private final StructureHelper oneRootStructureHelper = new StructureHelper() {

        @Override
        public boolean isRoot(ArtifactContainer e) {
            String containerName = e.getName();
            if ("b".equals(containerName)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isValid(ArtifactContainer e, String path) {
            if (path.contains("b")) {
                // paths with b in are valid if they are trying to get b directly
                if (!path.endsWith("b")) {
                    // They are also valid if we are at b already
                    if (!"b".equals(e.getName())) {
                        return false;
                    }
                }
            }
            return true;
        }

    };

    /** This structure helper creates a two roots, one at "b" and one at "d" (to be used with the artifact structure created by {@link #mockArtifactDataStructure(Mockery)}) */
    private final StructureHelper twoRootsStructureHelper = new StructureHelper() {

        @Override
        public boolean isRoot(ArtifactContainer e) {
            String containerName = e.getName();
            if ("b".equals(containerName) || "d".equals(containerName)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isValid(ArtifactContainer e, String path) {
            if (path.contains("b")) {
                // paths with b in are valid if they are trying to get b directly
                if (!path.endsWith("b")) {
                    // They are also valid if we are at b or d already
                    if (!"b".equals(e.getName()) && !"d".equals(e.getName())) {
                        return false;
                    }
                }
            }
            // Paths under d are only valid if trying to get d or are at d
            if (path.contains("d")) {
                if (!path.endsWith("d")) {
                    if (!"d".equals(e.getName())) {
                        return false;
                    }
                }
            }
            return true;
        }

    };

    /** This structure helper creates a two roots, one at "b" and one at "c" (to be used with the artifact structure created by {@link #mockArtifactDataStructure(Mockery)}) */
    private final StructureHelper adjacentRootsStructureHelper = new StructureHelper() {

        @Override
        public boolean isRoot(ArtifactContainer e) {
            String containerName = e.getName();
            if ("b".equals(containerName) || "c".equals(containerName)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isValid(ArtifactContainer e, String path) {
            if (path.contains("b")) {
                // paths with b in are valid if they are trying to get b directly
                if (!"b".equals(path) && !"/a/b".equals(path) && !"a/b".equals(path)) {
                    // They are also valid if we are at b or c already
                    if (!"b".equals(e.getName()) && !"c".equals(e.getName())) {
                        return false;
                    }
                }
            }
            // Paths under c are only valid if trying to get c or are at c
            if (path.contains("c")) {
                if (!"c".equals(path) && !"/a/b/c".equals(path) && !"a/b/c".equals(path)) {
                    if (!"c".equals(e.getName())) {
                        return false;
                    }
                }
            }
            return true;
        }

    };

    /**
     * This method creates an interpreted container over the rootOverlay.
     * 
     * @param mockery The mockery object for mocking the FactoryHolder class
     * @param rootOverlay The overlay to use as the root artifact container object
     * @param structureHelper The structure helper to provide mock roots for the overlay
     * @return The InterpretedContainer that was created
     * @throws UnableToAdaptException If we were unable to adapt to interpreted container
     */
    private InterpretedContainer createInterpretedContainer(final Mockery mockery, final OverlayContainer rootOverlay, final StructureHelper structureHelper) throws UnableToAdaptException {
        Container adpatableRoot = new AdaptableContainerImpl(rootOverlay, mockery.mock(FactoryHolder.class));
        InterpretedContainer interpretedContainer = adpatableRoot.adapt(InterpretedContainer.class);
        interpretedContainer.setStructureHelper(structureHelper);
        return interpretedContainer;
    }

    /**
     * <p>
     * This method will create a mock data structure in the artifact API. All supported method calls are registered using an "allowing" so can be called 0 or more times with no
     * validation that they are called. It will have the following structure where each entry is also a container:
     * </p>
     * <code>
     * &#47;<br/>
     * |-&gt;a<br/>
     * |&nbsp;&nbsp;|-&gt;b<br/>
     * |&nbsp;&nbsp;|&nbsp;&nbsp;|-&gt;c<br/>
     * |&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;&nbsp;|-&gt;d<br/>
     * |&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;&nbsp;|-&gt;e<br/>
     * |&nbsp;&nbsp;|&nbsp;&nbsp;|-&gt;n<br/>
     * </code>
     * <p>
     * The following calls are supported on the root OverlayContainer:
     * </p>
     * <ul>
     * <li>{@link OverlayContainer#isRoot()}: returns <code>true</code></li>
     * <li>{@link OverlayContainer#getRoot()}: returns itself</li>
     * <li>{@link OverlayContainer#getEntry(String)}: works for all entries</li>
     * <li>{@link OverlayContainer#getPath()}: returns "/"</li>
     * <li>{@link OverlayContainer#getName()}: returns "/"</li>
     * <li>{@link OverlayContainer#getArtifactNotifier()}: returns a mock {@link ArtifactNotifier}</li>
     * <li>{@link OverlayContainer#getEnclosingContainer()}: returns <code>null</code></li>
     * </ul>
     * <p>
     * For each entry in the structure the following method calls are mocked:
     * </p>
     * <ul>
     * <li>{@link ArtifactEntry#convertToContainer()}: returns the container</li>
     * <li>{@link ArtifactEntry#getPath()}: returns the path</li>
     * </ul>
     * <p>
     * For each container in the structure the following method calls are mocked:
     * </p>
     * <ul>
     * <li>{@link ArtifactContainer#getName()}: returns the name</li>
     * <li>{@link ArtifactContainer#getPath()}: returns the path</li>
     * <li>{@link ArtifactContainer#isRoot()}: returns false</li>
     * <li>{@link ArtifactContainer#getArtifactNotifier()}: returns the same notifier as the root overlay</li>
     * <li>{@link ArtifactContainer#getEntryInEnclosingContainer()}: returns the entry for this container</li>
     * <li>{@link ArtifactContainer#getEnclosingContainer()}: returns its parent container</li>
     * <li>{@link ArtifactContainer#getEntry()}: relative paths work for all direct children, absolute paths work for all entries</li>
     * </ul>
     * 
     * @param mockery The mockery object for creating mocks
     * @return
     */
    private OverlayContainer mockArtifactDataStructure(final Mockery mockery) {
        final OverlayContainer root = mockery.mock(OverlayContainer.class, "root");
        final ArtifactNotifier notifier = mockery.mock(ArtifactNotifier.class);
        mockery.checking(new Expectations() {
            {
                allowing(root).isRoot();
                will(returnValue(true));
                allowing(root).getRoot();
                will(returnValue(root));
                allowing(root).getName();
                will(returnValue("/"));
                allowing(root).getPath();
                will(returnValue("/"));
                allowing(root).getArtifactNotifier();
                will(returnValue(notifier));
                allowing(root).getEnclosingContainer();
                will(returnValue(null));
            }
        });
        // Now add containers to it
        ArtifactContainer aContainer = this.mockContainerEntry("a", root, root, mockery);
        ArtifactContainer bContainer = this.mockContainerEntry("b", aContainer, root, mockery);
        ArtifactContainer cContainer = this.mockContainerEntry("c", bContainer, root, mockery);
        ArtifactContainer nContainer = this.mockContainerEntry("n", bContainer, root, mockery);
        ArtifactContainer dContainer = this.mockContainerEntry("d", cContainer, root, mockery);
        ArtifactContainer eContainer = this.mockContainerEntry("e", dContainer, root, mockery);

        // You should be able to call using a root path to any other container, so on b I should be able to call getEntry("/a/b/c"); to get c. Set this up now.
        mockRootCalls(mockery, aContainer, bContainer, cContainer, dContainer, eContainer, nContainer);
        return root;
    }

    /**
     * <p>
     * This method will register calls to all of the containers to get entries for all of the other containers from the root so that for two containers the following will work:
     * </p>
     * <code>
     * ArtifactContainer container1 = ...;</br>
     * ArtifactContainer container2 = ...;</br>
     * assert container1 == container2.getEntry(container1.getPath()).convertToContainer();</br>
     * </code>
     * 
     * @param mockery The mock context to register the allowing calls to
     * @param containers The containers to register against each other
     */
    private void mockRootCalls(Mockery mockery, ArtifactContainer... containers) {
        if (containers == null || containers.length == 0) {
            return;
        }

        // Do a double loop as we need to register every container against every other container
        for (int i = 0; i < containers.length; i++) {
            for (int j = 0; j < containers.length; j++) {
                final ArtifactEntry entryToGet = containers[j].getEntryInEnclosingContainer();
                final ArtifactContainer containerToRegisterAgainst = containers[i];
                final String containerPath = entryToGet.getPath();
                mockery.checking(new Expectations() {
                    {
                        allowing(containerToRegisterAgainst).getEntry(containerPath);
                        will(returnValue(entryToGet));
                    }
                });
            }
        }
    }

    /**
     * <p>
     * This method will create a container and entry within a data structure. The two will act as a pair such that the following will pass:
     * </p>
     * <code>
     * ArtifactEntry entry = ...;<br/>
     * ArtifactContainer container = ...;<br/>
     * assert entry.convertToContainer() == container;<br/>
     * assert entry == container.getEntryInEnclosingContainer();<br/>
     * assert entry.getPath() == container.getPath();<br/>
     * </code>
     * 
     * <p>
     * The following methods are mocked on the entry:
     * </p>
     * <ul>
     * <li>{@link ArtifactEntry#convertToContainer()}: returns the container</li>
     * <li>{@link ArtifactEntry#getPath()}: returns the path</li>
     * </ul>
     * <p>
     * The following methods are mocked on the container:
     * </p>
     * <ul>
     * <li>{@link ArtifactContainer#getName()}: returns the name</li>
     * <li>{@link ArtifactContainer#getPath()}: returns the path</li>
     * <li>{@link ArtifactContainer#isRoot()}: returns false</li>
     * <li>{@link ArtifactContainer#getArtifactNotifier()}: returns the same notifier as the root container</li>
     * <li>{@link ArtifactContainer#getEnclosingContainer()}: returns <code>parent</code></li>
     * <li>{@link ArtifactContainer#getEntryInEnclosingContainer()}: returns the entry</li>
     * </ul>
     * 
     * <p>
     * In addition support will be added for a {@link ArtifactContainer#getEntry(String)} call for the (relative) name of this entry on the <code>parent</code> container and
     * (absolute) path on the <code>root</code> container.
     * </p>
     * 
     * @param name The name to give this entry and container, also used for the path
     * @param parent The direct parent of this entry
     * @param root The root of this entry
     * @param mockery The mock context to create the mocks in
     * @return The container
     */
    private ArtifactContainer mockContainerEntry(final String name, final ArtifactContainer parent, final ArtifactContainer root, Mockery mockery) {
        final ArtifactEntry entry = mockery.mock(ArtifactEntry.class, name + "Entry");
        final ArtifactContainer container = mockery.mock(ArtifactContainer.class, name + "Container");
        final String parentPath = parent.getPath();
        final String entryPath = "/".equals(parentPath) ? parentPath + name : parentPath + "/" + name;
        mockery.checking(new Expectations() {
            {
                // Entry
                allowing(entry).convertToContainer();
                will(returnValue(container));
                allowing(entry).getPath();
                will(returnValue(entryPath));

                // Container
                allowing(container).getName();
                will(returnValue(name));
                allowing(container).isRoot();
                will(returnValue(false));
                allowing(container).getArtifactNotifier();
                will(returnValue(root.getArtifactNotifier()));
                allowing(container).getEntryInEnclosingContainer();
                will(returnValue(entry));
                allowing(container).getPath();
                will(returnValue(entryPath));
                allowing(container).getEnclosingContainer();
                will(returnValue(parent));

                // Parent and root
                allowing(parent).getEntry(name);
                will(returnValue(entry));
                allowing(root).getEntry(entryPath);
                will(returnValue(entry));
            }
        });
        return container;
    }

    /**
     * Class for matching {@link ArtifactNotification} objects
     */
    private class ArtifactNotificationMatcher extends BaseMatcher<ArtifactNotification> {

        private final ArtifactContainer expectedRoot;

        private final Collection<String> expectedPaths;

        /**
         * Creates a new matcher with the expected inputs to the notification object to match
         * 
         * @param expectedRoot The expected root container, reference equality will be used to test this object
         * @param expectedPaths The expected paths for the notification object, object equality will be used to test this object
         */
        public ArtifactNotificationMatcher(ArtifactContainer expectedRoot, Collection<String> expectedPaths) {
            super();
            this.expectedRoot = expectedRoot;
            this.expectedPaths = expectedPaths;
        }

        @Override
        public boolean matches(Object arg0) {
            if (arg0 instanceof ArtifactNotification) {
                ArtifactNotification notification = (ArtifactNotification) arg0;
                if (notification.getContainer() == expectedRoot && expectedPaths.equals(notification.getPaths())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText("expected Notification root is " + this.expectedRoot + " with paths " + this.expectedPaths);
        }
    }

    /**
     * Class for matching {@link Notification} objects
     */
    private class NotificationMatcher extends BaseMatcher<Notification> {

        private final Container expectedRoot;

        private final Collection<String> expectedPaths;

        /**
         * Creates a new matcher with the expected inputs to the notification object to match
         * 
         * @param expectedRoot The expected root container, reference equality will be used to test this object
         * @param expectedPaths The expected paths for the notification object, object equality will be used to test this object
         */
        public NotificationMatcher(Container expectedRoot, Collection<String> expectedPaths) {
            super();
            this.expectedRoot = expectedRoot;
            this.expectedPaths = expectedPaths;
        }

        @Override
        public boolean matches(Object arg0) {
            if (arg0 instanceof Notification) {
                Notification notification = (Notification) arg0;
                if (notification.getContainer() == this.expectedRoot && this.expectedPaths.equals(notification.getPaths())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText("expected Notification root is " + this.expectedRoot + " with paths " + this.expectedPaths);
        }
    }

    /**
     * This matcher will act the same as {@link Expectations#any(Class)} but will also record what {@link ArtifactListener} was registered so that it is available for callbacks.
     */
    private class CaptureNotificationListener extends BaseMatcher<ArtifactListener> {

        private ArtifactListener listener;

        /** {@inheritDoc} */
        @Override
        public boolean matches(Object arg0) {
            if (arg0 instanceof ArtifactListener) {
                listener = (ArtifactListener) arg0;
                return true;
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void describeTo(Description arg0) {
            arg0.appendText("any ArtifactListener");
        }
    }
}
