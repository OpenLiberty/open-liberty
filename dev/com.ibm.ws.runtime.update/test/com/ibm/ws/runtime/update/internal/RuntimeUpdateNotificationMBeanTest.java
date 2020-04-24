/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.internal;

import static com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_MESSAGE;
import static com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_NAME;
import static com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_STATUS;
import static com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_TYPE;
import static com.ibm.ws.runtime.update.RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;

import test.common.SharedOutputManager;

public class RuntimeUpdateNotificationMBeanTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final RuntimeUpdateManager updateManager = mock.mock(RuntimeUpdateManager.class);
    private final RuntimeUpdateNotification updateNotification = mock.mock(RuntimeUpdateNotification.class);
    @SuppressWarnings("unchecked")
    private final Future<Boolean> dummyFuture = mock.mock(Future.class);

    private static class CompletionListenerInterceptor implements RuntimeUpdateNotification {

        private CompletionListener<Boolean> completionListener;

        @Override
        public String getName() {
            return CONFIG_UPDATES_DELIVERED;
        }

        @Override
        public Future<Boolean> getFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResult(boolean result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResult(Throwable t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onCompletion(CompletionListener<Boolean> completionListener) {
            this.completionListener = completionListener;
        }

        @Override
        public void waitForCompletion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        public CompletionListener<Boolean> getCompletionListener() {
            return this.completionListener;
        }

        @Override
        public boolean ignoreOnQuiesce() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> getProperties() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setProperties(Map<String, Object> props) {
            throw new UnsupportedOperationException();

        }
    }

    private static class NotficationRecorder implements NotificationListener {

        private Notification notification;
        private Object handback;

        @Override
        public void handleNotification(Notification notification,
                                       Object handback) {
            this.notification = notification;
            this.handback = handback;
        }

        public Notification getNotification() {
            return notification;
        }

        public Object getHandback() {
            return handback;
        }
    }

    private RuntimeUpdateNotificationMBeanImpl mBean;

    @Before
    public void setUp() throws Exception {
        mBean = new RuntimeUpdateNotificationMBeanImpl();
    }

    @After
    public void tearDown() throws Exception {
        mBean = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationCreated() throws Exception {
        mock.checking(new Expectations() {
            {
                one(updateNotification).getName();
                will(returnValue(CONFIG_UPDATES_DELIVERED));

                one(updateNotification).onCompletion(with(any(CompletionListener.class)));
            }
        });
        mBean.notificationCreated(updateManager, updateNotification);
    }

    @Test
    public void testNotificationSuccessfulCompletion() {
        NotficationRecorder nr = new NotficationRecorder();
        Object handback = new Object();
        mBean.addNotificationListener(nr, null, handback);

        CompletionListenerInterceptor cli = new CompletionListenerInterceptor();
        mBean.notificationCreated(updateManager, cli);
        CompletionListener<Boolean> cl = cli.getCompletionListener();
        cl.successfulCompletion(dummyFuture, Boolean.TRUE);

        Notification n = nr.getNotification();
        assertNotNull("Not expecting null Notification", n);
        assertEquals("Notification types must match",
                     RUNTIME_UPDATE_NOTIFICATION_TYPE, n.getType());
        assertSame("Source expected to be the RuntimeUpdateNotificationMBean", mBean, n.getSource());

        Object userData = n.getUserData();
        assertNotNull("Not expecting null user data.", userData);

        Map<String, Object> expectedData = new HashMap<String, Object>();
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_NAME, CONFIG_UPDATES_DELIVERED);
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_STATUS, Boolean.TRUE);
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_MESSAGE, null);
        assertEquals("User data maps do not match.", expectedData, userData);

        assertSame("Handback object is not the same.", handback, nr.getHandback());
    }

    @Test
    public void testNotificationFailedCompletion() {
        NotficationRecorder nr = new NotficationRecorder();
        Object handback = new Object();
        mBean.addNotificationListener(nr, null, handback);

        RuntimeException r = new RuntimeException("Horrible terrible failure!");

        CompletionListenerInterceptor cli = new CompletionListenerInterceptor();
        mBean.notificationCreated(updateManager, cli);
        CompletionListener<Boolean> cl = cli.getCompletionListener();
        cl.failedCompletion(dummyFuture, r);

        Notification n = nr.getNotification();
        assertNotNull("Not expecting null Notification", n);
        assertEquals("Notification types must match",
                     RUNTIME_UPDATE_NOTIFICATION_TYPE, n.getType());
        assertSame("Source expected to be the RuntimeUpdateNotificationMBean", mBean, n.getSource());

        Object userData = n.getUserData();
        assertNotNull("Not expecting null user data.", userData);

        Map<String, Object> expectedData = new HashMap<String, Object>();
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_NAME, CONFIG_UPDATES_DELIVERED);
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_STATUS, Boolean.FALSE);
        expectedData.put(RUNTIME_UPDATE_NOTIFICATION_KEY_MESSAGE, r.getMessage());
        assertEquals("User data maps do not match.", expectedData, userData);

        assertSame("Handback object is not the same.", handback, nr.getHandback());
    }
}
