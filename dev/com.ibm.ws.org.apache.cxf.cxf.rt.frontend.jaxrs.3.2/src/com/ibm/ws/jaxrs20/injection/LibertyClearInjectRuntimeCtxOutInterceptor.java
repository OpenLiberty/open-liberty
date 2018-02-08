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
package com.ibm.ws.jaxrs20.injection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.interceptor.JAXRSOutInterceptor;
import org.apache.cxf.message.Message;
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
        //clear InjectionRuntimeContext
        InjectionRuntimeContextHelper.removeRuntimeContext();
    }
}
