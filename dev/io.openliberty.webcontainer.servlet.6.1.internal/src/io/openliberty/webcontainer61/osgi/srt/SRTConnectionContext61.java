/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.srt;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.osgi.srt.SRTConnectionContext60;
import io.openliberty.webcontainer61.osgi.webapp.WebAppDispatcherContext61;
import io.openliberty.webcontainer61.srt.SRTServletRequest61;
import io.openliberty.webcontainer61.srt.SRTServletResponse61;

public class SRTConnectionContext61 extends SRTConnectionContext60 {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.osgi.srt");
    private static final String CLASS_NAME = SRTConnectionContext61.class.getName();

    /**
     * Used for pooling the SRTConnectionContext objects.
     */
    public SRTConnectionContext61 nextContext;

    @Override
    protected void init() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "init", "this [" + this + "] , request [" + _request + "]");
        }

        this._dispatchContext = new WebAppDispatcherContext61(_request);
        _request.setWebAppDispatcherContext(_dispatchContext);
    }

    @Override
    protected SRTServletRequest newSRTServletRequest() {
        return new SRTServletRequest61(this);
    }

    @Override
    protected SRTServletResponse newSRTServletResponse() {
        return new SRTServletResponse61(this);
    }
}
