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
package com.ibm.ws.container.service.state.internal;

import java.util.Iterator;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

class ApplicationStateManager extends StateChangeManager<ApplicationStateListener> {
    ApplicationStateManager(String listenerRefName) {
        super(listenerRefName);
    }

    /**
     * @param info
     */
    public void fireStarting(ApplicationInfo info) throws StateChangeException {
        if (info != null && info.getConfigHelper() == null) {
            Iterator<ServiceAndServiceReferencePair<ApplicationStateListener>> iterator = listeners.getServicesWithReferences();
            while (iterator.hasNext()) {
                ServiceAndServiceReferencePair<ApplicationStateListener> pair = iterator.next();
                ServiceReference<ApplicationStateListener> ref = pair.getServiceReference();
                if (ref.getProperty("includeAppsWithoutConfig") != null) {
                    ApplicationStateListener listener = pair.getService();
                    try {
                        listener.applicationStarting(info);
                    } catch (StateChangeException t) {
                        throw t;
                    } catch (Throwable t) {
                        throw new StateChangeException(t);
                    }
                }
            }
            return;
        }
        for (ApplicationStateListener listener : listeners.services()) {
            try {
                listener.applicationStarting(info);
            } catch (StateChangeException t) {
                throw t;
            } catch (Throwable t) {
                throw new StateChangeException(t);
            }
        }
    }

    /**
     * @param info
     */
    public void fireStarted(ApplicationInfo info) throws StateChangeException {
        if (info != null && info.getConfigHelper() == null) {
            Iterator<ServiceAndServiceReferencePair<ApplicationStateListener>> iterator = listeners.getServicesWithReferences();
            while (iterator.hasNext()) {
                ServiceAndServiceReferencePair<ApplicationStateListener> pair = iterator.next();
                ServiceReference<ApplicationStateListener> ref = pair.getServiceReference();
                if (ref.getProperty("includeAppsWithoutConfig") != null) {
                    ApplicationStateListener listener = pair.getService();
                    try {
                        listener.applicationStarted(info);
                    } catch (StateChangeException t) {
                        throw t;
                    } catch (Throwable t) {
                        throw new StateChangeException(t);
                    }
                }
            }
            return;
        }
        for (ApplicationStateListener listener : listeners.services()) {
            try {
                listener.applicationStarted(info);
            } catch (StateChangeException t) {
                throw t;
            } catch (Throwable t) {
                throw new StateChangeException(t);
            }
        }
    }

    /**
     * @param info
     */
    public void fireStopping(ApplicationInfo info) {
        if (info != null && info.getConfigHelper() == null) {
            Iterator<ServiceAndServiceReferencePair<ApplicationStateListener>> iterator = listeners.getServicesWithReferences();
            while (iterator.hasNext()) {
                ServiceAndServiceReferencePair<ApplicationStateListener> pair = iterator.next();
                ServiceReference<ApplicationStateListener> ref = pair.getServiceReference();
                if (ref.getProperty("includeAppsWithoutConfig") != null) {
                    ApplicationStateListener listener = pair.getService();
                    try {
                        listener.applicationStopping(info);
                    } catch (Throwable t) {
                        // Nothing (except automatically inserted FFDC).
                    }
                }
            }
            return;
        }
        for (ApplicationStateListener listener : listeners.services()) {
            try {
                listener.applicationStopping(info);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }

    /**
     * @param info
     */
    public void fireStopped(ApplicationInfo info) {
        if (info != null && info.getConfigHelper() == null) {
            Iterator<ServiceAndServiceReferencePair<ApplicationStateListener>> iterator = listeners.getServicesWithReferences();
            while (iterator.hasNext()) {
                ServiceAndServiceReferencePair<ApplicationStateListener> pair = iterator.next();
                ServiceReference<ApplicationStateListener> ref = pair.getServiceReference();
                if (ref.getProperty("includeAppsWithoutConfig") != null) {
                    ApplicationStateListener listener = pair.getService();
                    try {
                        listener.applicationStopped(info);
                    } catch (Throwable t) {
                        // Nothing (except automatically inserted FFDC).
                    }
                }
            }
            return;
        }
        for (ApplicationStateListener listener : listeners.services()) {
            try {
                listener.applicationStopped(info);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }
}
