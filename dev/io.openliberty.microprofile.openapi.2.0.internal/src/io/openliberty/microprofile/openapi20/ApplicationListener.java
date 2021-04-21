/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

import io.openliberty.microprofile.openapi20.utils.LoggingUtils;

/**
 * The ApplicationListener class monitors the OL instance for applications starting/stopping and adds/removes them
 * to/from the {@link ApplicationRegistry}, as appropriate.
 */
@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ApplicationListener implements ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(ApplicationListener.class);
    
    @Reference
    private ApplicationRegistry appRegistry;

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        try {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application starting process started: " + appInfo);
            }
            
            appRegistry.addApplication(appInfo);
            
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application starting process ended: " + appInfo);
            }
        } catch (Throwable e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to process application: " + e.getMessage());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {}

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        try {
            appRegistry.removeApplication(appInfo);
        } catch (Throwable e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to remove application: " + e.getMessage());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {}
}