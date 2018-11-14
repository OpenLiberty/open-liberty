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
package com.ibm.ws.webcontainer40.osgi.srt.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.service.util.ConcurrentObjectPool;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContextPool;
import com.ibm.ws.webcontainer40.osgi.srt.SRTConnectionContext40;

/**
 * A simple pool for SRTConnectionContext40 objects.
 */
@Component(property = { "service.vendor=IBM", "service.ranking:Integer=40", "servlet.version=4.0" })
public class SRTConnectionContextPool40Impl implements SRTConnectionContextPool {
    private final ConcurrentObjectPool<SRTConnectionContext> pool = new ConcurrentObjectPool<>(100);

    @Override
    public final SRTConnectionContext get() {
        SRTConnectionContext context = pool.get();

        if (context == null) {
            context = new SRTConnectionContext40();
        }

        return context;
    }

    @Override
    public final void put(SRTConnectionContext context) {
        pool.put(context);
    }
}
