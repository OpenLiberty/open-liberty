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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.DynamicMBean;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;

@Component(service = { RuntimeUpdateNotificationMBean.class, DynamicMBean.class, RuntimeUpdateListener.class },
           immediate = false,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "jmx.objectname=" + RuntimeUpdateNotificationMBean.OBJECT_NAME)
public class RuntimeUpdateNotificationMBeanImpl extends
             StandardEmitterMBean implements
             RuntimeUpdateNotificationMBean, RuntimeUpdateListener {
    
    private static final TraceComponent tc = Tr.register(RuntimeUpdateNotificationMBeanImpl.class);
    
    private final AtomicLong sequenceNum = new AtomicLong();
    
    public RuntimeUpdateNotificationMBeanImpl() {
        super(RuntimeUpdateNotificationMBean.class, false, new NotificationBroadcasterSupport((Executor) null,
                new MBeanNotificationInfo(new String[] { RUNTIME_UPDATE_NOTIFICATION_TYPE },
                        Notification.class.getName(),
                        "")));
    }
    
    //
    // RuntimeUpdateListener methods.
    //

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager,
            RuntimeUpdateNotification notification) {
        final String name = notification.getName();
        notification.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                sendNotification(createNotification(name, result, null));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Notification sent on successful completion: " + name + '.');
                }
            }
            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                sendNotification(createNotification(name, Boolean.FALSE, t));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Notification sent on failed completion: " + name + '.', t);
                }
            }
        });
    }
    
    private Notification createNotification(String name, Boolean result, Throwable t) {
        Notification n = new Notification(RUNTIME_UPDATE_NOTIFICATION_TYPE, 
                this,
                sequenceNum.incrementAndGet(),
                System.currentTimeMillis());
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(RUNTIME_UPDATE_NOTIFICATION_KEY_NAME, name);
        map.put(RUNTIME_UPDATE_NOTIFICATION_KEY_STATUS, result);
        map.put(RUNTIME_UPDATE_NOTIFICATION_KEY_MESSAGE, t != null ? t.getMessage() : null);
        n.setUserData(map);
        return n;
    }
}
