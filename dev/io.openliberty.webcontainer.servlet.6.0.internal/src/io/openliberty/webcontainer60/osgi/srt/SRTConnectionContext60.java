/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.osgi.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.ws.webcontainer40.osgi.srt.SRTConnectionContext40;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.srt.SRTServletRequest60;
import io.openliberty.webcontainer60.srt.SRTServletResponse60;

public class SRTConnectionContext60 extends SRTConnectionContext40 {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.osgi.srt");
    private static final String CLASS_NAME = SRTConnectionContext60.class.getName();

    /**
     * Used for pooling the SRTConnectionContext objects.
     */
    public SRTConnectionContext60 nextContext;

    @Override
    protected void init() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "init", "this [" + this + "] , request [" + _request + "]");
        }

        //Reuse 4.0 until there is something new in servlet 6.0 dispatch context
        this._dispatchContext = new WebAppDispatcherContext40(_request);
        _request.setWebAppDispatcherContext(_dispatchContext);
    }

    @Override
    protected SRTServletRequest newSRTServletRequest() {
        return new SRTServletRequest60(this);
    }

    @Override
    protected SRTServletResponse newSRTServletResponse() {
        return new SRTServletResponse60(this);
    }
}
