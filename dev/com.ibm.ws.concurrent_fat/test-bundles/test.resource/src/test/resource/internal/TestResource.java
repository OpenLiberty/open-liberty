/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.resource.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A resource factory that runs a test that needs to access the service registry.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { ResourceFactory.class },
           property = { "jndiName=testresource/testServiceRankings", "creates.objectClass=java.lang.Object" })
public class TestResource implements ResourceFactory {
    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    @Deactivate
    protected void deactivate() {
    }

    // Used by test.concurrent.app.EEConcurrencyUtilsTestServlet.testServiceRankings
    // in order to perform a test that queries the service registry.
    // An exception is returned as the result if the test fails. The message "SUCCESS" is returned if successful.
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    BundleContext bundleContext = context.getBundleContext();

                    ServiceReference<ExecutorService> execSvcRef = bundleContext.getServiceReference(ExecutorService.class);
                    String componentName = (String) execSvcRef.getProperty("component.name");
                    if (!"com.ibm.ws.threading".equals(componentName))
                        throw new Exception("Unexpected ExecutorService with highest service.ranking: " + execSvcRef);

                    ServiceReference<ScheduledExecutorService> schedExecSvcRef = bundleContext.getServiceReference(ScheduledExecutorService.class);
                    componentName = (String) schedExecSvcRef.getProperty("component.name");
                    if (!"com.ibm.ws.threading.internal.ScheduledExecutorImpl".equals(componentName))
                        throw new Exception("Unexpected ScheduledExecutorService with highest service.ranking: " + schedExecSvcRef);

                    ServiceReference<?> mgdExecSvcRef = bundleContext.getServiceReference("jakarta.enterprise.concurrent.ManagedExecutorService");
                    if (mgdExecSvcRef == null)
                        mgdExecSvcRef = bundleContext.getServiceReference("javax.enterprise.concurrent.ManagedExecutorService");

                    String displayId = (String) mgdExecSvcRef.getProperty("config.displayId");
                    if (!"managedExecutorService[DefaultManagedExecutorService]".equals(displayId))
                        throw new Exception("Unexpected ManagedExecutorService with highest service.ranking: " + displayId);

                    ServiceReference<?> mgdSchedExecSvcRef = bundleContext.getServiceReference("java.enterprise.concurrent.ManagedScheduledExecutorService");
                    if (mgdSchedExecSvcRef == null)
                        mgdSchedExecSvcRef = bundleContext.getServiceReference("jakarta.enterprise.concurrent.ManagedScheduledExecutorService");

                    displayId = (String) mgdSchedExecSvcRef.getProperty("config.displayId");
                    if (!"managedScheduledExecutorService[DefaultManagedScheduledExecutorService]".equals(displayId))
                        throw new Exception("Unexpected ManagedScheduledExecutorService with highest service.ranking: " + displayId);

                    return "SUCCESS";
                }
            });
        } catch (PrivilegedActionException x) {
            return x.getCause();
        }
    }
}