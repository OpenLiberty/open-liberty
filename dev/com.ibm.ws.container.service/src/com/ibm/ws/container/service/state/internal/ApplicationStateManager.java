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

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

class ApplicationStateManager extends StateChangeManager<ApplicationStateListener> {
    ApplicationStateManager(String listenerRefName) {
        super(listenerRefName);
    }

    /**
     * @param info
     */
    public void fireStarting(ApplicationInfo info) throws StateChangeException {
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
        for (ApplicationStateListener listener : listeners.services()) {
            try {
                listener.applicationStopped(info);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }
}
