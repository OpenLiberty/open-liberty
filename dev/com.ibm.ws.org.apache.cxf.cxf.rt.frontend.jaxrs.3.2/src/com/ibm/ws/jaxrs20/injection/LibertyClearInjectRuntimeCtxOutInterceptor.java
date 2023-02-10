/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.injection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.interceptor.JAXRSOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * @param <T>
 *
 */
public class LibertyClearInjectRuntimeCtxOutInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {

    /**
     * we should clear InjectionRuntimeContext after JAXRSOutInterceptor
     * The last provider should be MessageBodyWriter
     *
     * @param phase
     */
    public LibertyClearInjectRuntimeCtxOutInterceptor(String phase) {
        super(phase);

        addAfter(JAXRSOutInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        //clear InjectionRuntimeContext on server-side only
        if (!MessageUtils.isRequestor(message)) {
            InjectionRuntimeContextHelper.removeRuntimeContext();
        }
    }

    @Override
    public void handleFault(Message message) {
        //clear InjectionRuntimeContext on server-side only
        if (!MessageUtils.isRequestor(message)) {
            InjectionRuntimeContextHelper.removeRuntimeContext();
        }
    }
}
