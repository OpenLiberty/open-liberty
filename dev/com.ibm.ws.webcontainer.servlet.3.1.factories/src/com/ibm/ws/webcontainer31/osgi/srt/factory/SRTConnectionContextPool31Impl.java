/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.osgi.srt.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.service.util.ConcurrentObjectPool;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContextPool;
import com.ibm.ws.webcontainer31.osgi.srt.SRTConnectionContext31;

/**
 * A simple pool for SRTConnectionContext31 objects.
 */
@Component(property = { "service.vendor=IBM", "service.ranking:Integer=31", "servlet.version=3.1" })
public class SRTConnectionContextPool31Impl implements SRTConnectionContextPool {
    private final ConcurrentObjectPool<SRTConnectionContext> pool = new ConcurrentObjectPool<>(100);

    @Override
    public final SRTConnectionContext get() {
        SRTConnectionContext context = pool.get();

        if (context == null) {
            context = new SRTConnectionContext31();
        }

        return context;
    }

    @Override
    public final void put(SRTConnectionContext context) {
        pool.put(context);
    }
}
