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
package io.openliberty.webcontainer.servlet.internal.osgi.srt.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContextPool;

import io.openliberty.webcontainer60.osgi.srt.SRTConnectionContext60;

@Component(property = { "service.vendor=IBM", "service.ranking:Integer=60", "servlet.version=6.0" })
public class SRTConnectionContextPool60Impl implements SRTConnectionContextPool {
    private final ThreadLocal<com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext> head = new ThreadLocal<com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext>();

    @Override
    public final com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext get() {
        com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext context = null;
        com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext headContext = head.get();
        if (headContext != null) {
            context = headContext;
            head.set(context.nextContext);
        }

        if (context == null) {
            context = new SRTConnectionContext60();
        }

        context.nextContext = null;

        return context;
    }

    @Override
    public final void put(com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext context) {
        context.nextContext = head.get();
        head.set(context);
    }
}
