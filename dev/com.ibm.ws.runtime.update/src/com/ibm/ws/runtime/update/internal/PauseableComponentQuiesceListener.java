/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.update.internal;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentException;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * TODO Find a better place for this to live
 * This ServerQuiesceListener implementation will pause all pausable components.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class PauseableComponentQuiesceListener implements ServerQuiesceListener {

    private static final TraceComponent tc = Tr.register(PauseableComponentQuiesceListener.class);
    private BundleContext bundleContext = null;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener#serverStopping()
     */
    @Override
    public void serverStopping() {
        if (bundleContext != null) {
            try {
                Collection<ServiceReference<PauseableComponent>> refs = bundleContext.getServiceReferences(PauseableComponent.class, null);
                for (ServiceReference<PauseableComponent> ref : refs) {

                    PauseableComponent pc = bundleContext.getService(ref);
                    if (!pc.isPaused()) {
                        try {
                            pc.pause();
                        } catch (PauseableComponentException ex) {
                            Tr.warning(tc, "warn.did.not.pause.on.shutdown", ex.getMessage());
                        }
                    }
                }
            } catch (InvalidSyntaxException e) {
                // Should never happen, FFDC and return
                return;
            }
        }

    }

    protected void activate(BundleContext ctx) {
        this.bundleContext = ctx;
    }

}
