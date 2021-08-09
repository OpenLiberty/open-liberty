/*******************************************************************************
 * Copyright (c) 2014, 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.persistent.delayexec.internal;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;

/**
 * User feature service for testing config update.
 */
@Component(configurationPid = "test.delayexecution.user",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           service = { RuntimeUpdateListener.class },
           property = { "creates.objectClass=test.concurrent.persistent.delayexec.internal.TestServiceImpl", "service.ranking:Integer=25" })
public class TestServiceImpl implements RuntimeUpdateListener {
    long m_delay;
    int m_numberOfSleeps;

    /**
     * Class name constant
     */
    final String CLASS_NAME = this.getClass().getName();

    /**
     * Declarative Services method to activate this component.
     * 
     * @param bundleContext The bundle context.
     */
    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        System.out.println(CLASS_NAME + ".activate.Entry. " + "Properties: " + properties);

        String jndiName = (String) properties.get("jndiName");

        m_delay = (Long) properties.get("activateDelay");

        m_numberOfSleeps = (Integer) properties.get("numberOfNotificationSleeps");

        System.out.println(CLASS_NAME + ".activate.Exit. " + "JNDIName: " + jndiName +
                           ", activateDelay: " + m_delay + ", numberOfNotificationSleeps: " + m_numberOfSleeps);

    }

    /**
     * Declarative Services method to deactivate this component.
     * 
     * @param context context for this component
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        System.out.println(CLASS_NAME + ".deactivate.Entry. " + "ComponentContext: " + context);
        System.out.println(CLASS_NAME + ".deactivate.Exit.");
    }

    /**
     * This method is called during a configuration update. Its called for a short list of notifications. The goal
     * of this test method is to hang-up the configuration update. The problem is that if this method would hang-up on
     * the same notification as the PersistentExecutorImpl.notificationCreated method, then this method could get
     * control first (all specific notifications are "delivered" sequentially) and the PersistentExecutorImpl code would
     * not be able to "detect" the configuration update in progress so it can "defer" running tasks. So, this test is dependent
     * on the order of notifications. The "CONFIG_UPDATES_DELIVERED" notification must be delivered prior to the
     * "FEATURE_BUNDLES_RESOLVED" notification. This is to ensure that the PersistentExecutorImpl.notificationCreated is driven
     * to see the "CONFIG_UPDATES_DELIVERED" notification prior to this method hanging (sleeping) when it gets the
     * "FEATURE_BUNDLES_RESOLVED".
     * 
     * @see com.ibm.ws.runtime.update.RuntimeUpdateListener#notificationCreated(com.ibm.ws.runtime.update.RuntimeUpdateManager, com.ibm.ws.runtime.update.RuntimeUpdateNotification)
     */
    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        System.out.println(CLASS_NAME + ".notificationCreated, notified with ( " + notification.getName() + ")");

        // Delay the configuration update when we see the "FEATURE_UPDATES_COMPLETED" notification.
        if (RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED.equals(notification.getName())) {
            if (notification.isDone()) {
                // The notification is complete, that means it is the one that was used to install us.
                // Just return and ignore it, we only want to pay attention to newly created notifications
                return;
            }
            if (m_numberOfSleeps <= 0)
                return;
            else
                m_numberOfSleeps--;

            try {
                System.out.println(CLASS_NAME + ".notificationCreated about to sleep " + m_delay + "ms, for the " + m_numberOfSleeps + " time.");

                Thread.sleep(m_delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
