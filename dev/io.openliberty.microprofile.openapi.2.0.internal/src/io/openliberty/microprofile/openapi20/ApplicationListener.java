/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

import io.openliberty.microprofile.openapi20.utils.LoggingUtils;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ApplicationListener implements ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(ApplicationListener.class);
    private ApplicationProcessor appProcessor = null;

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        if (appProcessor != null) {
            try {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Application starting process started: " + appInfo);
                }
                appProcessor.addApplication(appInfo);
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Application starting process ended: " + appInfo);
                }
            } catch (Throwable e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to process application: " + e.getMessage());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {}

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        if (appProcessor != null) {
            try {
                appProcessor.removeApplication(appInfo);
            } catch (Throwable e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to remove application: " + e.getMessage());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {}

    @Reference(service = ApplicationProcessor.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
    protected void setApplicationProcessor(ApplicationProcessor appProcessor) {
        this.appProcessor = appProcessor;
    }

    protected void unsetApplicationProcessor(ApplicationProcessor appProcessor) {
        if (this.appProcessor == appProcessor) {
            this.appProcessor = null;
        }
    }
}