/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_APP_TYPE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_CONFIG_NAMESPACE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.DefaultApplicationMonitoringInformation;

@Component(property = { "service.vendor=IBM", "type=" + SPRING_APP_TYPE })
public class SpringBootHandler implements ApplicationHandler<DeployedAppInfo> {
    private static final TraceComponent tc = Tr.register(SpringBootHandler.class);

    @Reference(target = "(type=" + SPRING_APP_TYPE + ")")
    private DeployedAppInfoFactory deployedAppFactory;

    @Reference
    private FutureMonitor futureMonitor;

    private final AtomicReference<String> applicationActivated = new AtomicReference<String>();

    @Activate
    protected void activate(BundleContext context) {
        FrameworkWiring fwkWiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        // Find all bundles left over from previous run that provide spring boot config
        Collection<BundleCapability> configs = fwkWiring.findProviders(new Requirement() {
            @Override
            public Resource getResource() {
                return null;
            }

            @Override
            public String getNamespace() {
                return SPRING_BOOT_CONFIG_NAMESPACE;
            }

            @Override
            public Map<String, String> getDirectives() {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Collections.emptyMap();
            }
        });

        // We uninstall left over configs from previous shutdown to give config admin a
        // chance to clear out the configurations now before we start any applications.
        configs.forEach((c) -> {
            try {
                c.getRevision().getBundle().uninstall();
            } catch (BundleException e) {
                // AUTO FFDC here
            }
        });
    }

    @Override
    public Future<Boolean> install(final ApplicationInformation<DeployedAppInfo> applicationInformation) {
        SpringBootApplicationImpl springBootApplication = (SpringBootApplicationImpl) applicationInformation.getHandlerInfo();
        if (springBootApplication == null) {
            IllegalStateException ise = new IllegalStateException("No SpringBootApplication found.");
            return futureMonitor.createFutureWithResult(Boolean.class, ise);
        }
        if (springBootApplication.getError() != null) {
            return futureMonitor.createFutureWithResult(Boolean.class, springBootApplication.getError());
        }
        Future<Boolean> result = futureMonitor.createFuture(Boolean.class);
        String current = applicationActivated.getAndUpdate((s) -> {
            if (s == null) {
                return applicationInformation.getName();
            }
            return s;
        });
        if (current != null) {
            IllegalStateException error = new IllegalStateException(Tr.formatMessage(tc, "error.multiple.applications.not.allowed", applicationInformation.getName(), current));
            return futureMonitor.createFutureWithResult(Boolean.class, error);
        }
        springBootApplication.setApplicationActivated(applicationActivated);
        futureMonitor.onCompletion(result, new CompletionListener<Boolean>() {

            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                if (!result) {
                    springBootApplication.uninstallApp();
                }
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                CountDownLatch readyLatch = springBootApplication.getApplicationReadyLatch();
                readyLatch.countDown();
                readyLatch.countDown();
                springBootApplication.uninstallApp();
            }
        });

        if (!springBootApplication.deployApp(result)) {
            //Setting to false just in case the result wasn't set already
            futureMonitor.setResult(result, false);
        }
        return result;

    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        SpringBootApplicationImpl deployedApp = (SpringBootApplicationImpl) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return futureMonitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return futureMonitor.createFutureWithResult(success);
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        Container originalContainer = applicationInformation.getContainer();
        try {
            applicationInformation.setHandlerInfo(deployedAppFactory.createDeployedAppInfo(applicationInformation));
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        SpringBootApplicationImpl springBootApplication = (SpringBootApplicationImpl) applicationInformation.getHandlerInfo();
        if (springBootApplication.getError() != null) {
            return null;
        }

        // Only monitor the boot lib directory in the container
        Notification bootInfNotification = new DefaultNotification(applicationInformation.getContainer(), springBootApplication.getSpringBootManifest().getSpringBootLib());
        Notification metaInfNotification = new DefaultNotification(applicationInformation.getContainer(), "/META-INF");
        Collection<Notification> notifications = new HashSet<Notification>();
        notifications.add(bootInfNotification);
        notifications.add(metaInfNotification);

        if (originalContainer != applicationInformation.getContainer()) {
            Notification oldBoot = new DefaultNotification(originalContainer, springBootApplication.getSpringBootManifest().getSpringBootLib());
            Notification oldMeta = new DefaultNotification(originalContainer, "/META-INF");
            notifications.add(oldBoot);
            notifications.add(oldMeta);
        }
        return new DefaultApplicationMonitoringInformation(notifications, false);
    }
}
