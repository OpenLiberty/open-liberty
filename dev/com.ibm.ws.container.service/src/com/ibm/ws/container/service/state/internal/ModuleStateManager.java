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

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.state.ModuleStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

public class ModuleStateManager extends StateChangeManager<ModuleStateListener> {
    ModuleStateManager(String listenerRefName) {
        super(listenerRefName);
    }

    /**
     * @param info
     */
    public void fireStarting(ModuleInfo info) throws StateChangeException {
        for (ModuleStateListener listener : listeners.services()) {
            try {
                listener.moduleStarting(info);
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
    public void fireStarted(ModuleInfo info) throws StateChangeException {
        for (ModuleStateListener listener : listeners.services()) {
            try {
                listener.moduleStarted(info);
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
    public void fireStopping(ModuleInfo info) {
        for (ModuleStateListener listener : listeners.services()) {
            try {
                listener.moduleStopping(info);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }

    /**
     * @param info
     */
    public void fireStopped(ModuleInfo info) {
        for (ModuleStateListener listener : listeners.services()) {
            try {
                listener.moduleStopped(info);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }
}
