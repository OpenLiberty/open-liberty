/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.timedexit.internal;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 */
public class TimedExitComponent implements org.osgi.framework.BundleActivator {

    private final TimedExitThread mash = new TimedExitThread();

    private static TraceComponent tc = Tr.register(TimedExitComponent.class);

    @Override
    public void start(BundleContext context) {
        try {
            Tr.audit(tc, "TE9900.timedexit.enabled");

            String timeoutProperty = System.getProperty("com.ibm.ws.timedexit.timetolive");
            if (timeoutProperty != null) {
                long timeout = Long.parseLong(timeoutProperty);
                mash.setTimeout(timeout);
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        } catch (NumberFormatException e) {
            // Ignore this, we'll just not bother to change the timeout from it's default
        }

        mash.start();

    }

    @Override
    public void stop(BundleContext context) {
        // We used to cancel the countdown - but that's too early since we've seen hangs in the
        // shutdown processing!

        if (!FrameworkState.isStopping()) {
            Throwable ex = new RequiredTimedExitFeatureStoppedException(
                            "The timedexit-1.0 feature is being stopped before the server is stopping.  " +
                                            "It must be enabled during ALL FAT bucket runs; make sure that" +
                                            " fatTestPorts.xml is included in the server.xml.");
            FFDCFilter.processException(ex, getClass().getName(), "stop");
        }

    }

}
