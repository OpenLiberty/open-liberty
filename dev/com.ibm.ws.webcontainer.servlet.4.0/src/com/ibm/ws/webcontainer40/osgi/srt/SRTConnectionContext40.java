/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.srt;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.ws.webcontainer40.srt.SRTServletResponse40;

public class SRTConnectionContext40 extends com.ibm.ws.webcontainer31.osgi.srt.SRTConnectionContext31 {

    /**
     * Used for pooling the SRTConnectionContext31 objects.
     */
    public SRTConnectionContext40 nextContext;

    @Override
    protected void init() {
        this._dispatchContext = new WebAppDispatcherContext40(_request);
        _request.setWebAppDispatcherContext(_dispatchContext);
    }

    @Override
    protected SRTServletRequest newSRTServletRequest() {
        return new SRTServletRequest40(this);
    }

    @Override
    protected SRTServletResponse newSRTServletResponse() {
        return new SRTServletResponse40(this);
    }

}
