/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.ArrayList;
import java.util.logging.Level;

import javax.servlet.http.HttpSessionListener;

import com.ibm.websphere.servlet.session.IBMSessionListener;
import com.ibm.ws.session.http.HttpSessionObserver;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.IProtocolAdapter;
import com.ibm.wsspi.session.ISession;

public class WasHttpSessionObserver extends HttpSessionObserver {

    // For logging.
    private static final String methodClassName = "WasHttpSessionObserver";

    // boolean to tell us if we should scan to listeners to find the
    // IBMSessionListener
    // and then invoke call sessionRemovedFromCache. This boolean provides a
    // performance improvement.
    private boolean doesContainIBMSessionListener = false;

    /*
     * constructor
     */
    public WasHttpSessionObserver(ArrayList listeners, IProtocolAdapter adapter) {
        super(listeners, adapter);
        // listeners is an empty ArrayList and therefore we can't set the
        // doesContainIBMSessionListener variable here.
        // setDoesContainIBMSessionListener(boolean) should be called
    }
    
    // Should call the new HttpSessionObserver constructor
    public WasHttpSessionObserver(ArrayList listeners, ArrayList idListeners, IProtocolAdapter adapter) {
        super(listeners, idListeners, adapter);
        // listeners is an empty ArrayList and therefore we can't set the
        // doesContainIBMSessionListener variable here.
        // setDoesContainIBMSessionListener(boolean) should be called
    }

    /*
     * Handles the IBM Extension to the HttpSessionListener, IBSessionListener
     * Calls apps sessionRemovedFromCache method
     */
    public void sessionCacheDiscard(Object value) {
        HttpSessionListener listener = null;

        if (doesContainIBMSessionListener) {
            for (int i = _sessionListeners.size() - 1; i >= 0; i--) {
                listener = (HttpSessionListener) _sessionListeners.get(i);
                if (listener != null && listener instanceof IBMSessionListener) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "sessionCacheDiscard", "Calling sessionCacheDiscard on listener:" + listener);
                    }
                    ((IBMSessionListener) listener).sessionRemovedFromCache(((ISession) value).getId());
                }
            }
        }
    }

    /*
     * Setter for doesContainIBMSessionListener boolean.
     */
    public void setDoesContainIBMSessionListener(boolean value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "setDoesContainIBMSessionListener", "" + value);
        }
        doesContainIBMSessionListener = value;
    }

    /*
     * Getter for doesContainIBMSessionListener variable
     */
    public boolean getDoesContainIBMSessionListener() {
        return doesContainIBMSessionListener;
    }
}
