/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.workcontext;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.threading.WorkContext;


/**
 *
 **/
public class IIOPWorkContextInterceptor extends LocalObject implements ServerRequestInterceptor {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(IIOPWorkContextInterceptor.class);

    private static final ThreadLocal<Map<String, Serializable>> workInfoMap = new ThreadLocal<Map<String, Serializable>> (){

        @Override
        protected Map<String, Serializable> initialValue() {
            return new HashMap<>();
        }
    };

    
    public static Map<String, Serializable> getWorkInfoMap(){

       return workInfoMap.get();
    }

    public IIOPWorkContextInterceptor() {
    	
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {

                int requestId = ri.request_id();
                String operation = ri.operation();

                Map<String, Serializable> theMap = workInfoMap.get();

                theMap.clear();

                theMap.put(WorkContext.IIOP_REQUEST_ID, requestId);
                theMap.put(WorkContext.IIOP_OPERATION_NAME, operation);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "operation: " + operation + ", and requestId: " + requestId);
    }

	@Override
	public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
	}
	
    @Override
    public void destroy() {}

    /**
     * Returns the name of the interceptor.
     * <p/>
     * Each Interceptor may have a name that may be used administratively
     * to order the lists of Interceptors. Only one Interceptor of a given
     * name can be registered with the ORB for each Interceptor type. An
     * Interceptor may be anonymous, i.e., have an empty string as the name
     * attribute. Any number of anonymous Interceptors may be registered with
     * the ORB.
     *
     * @return the name of the interceptor.
     */
    @Override
    public String name() {
        return getClass().getName();
    }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest
    {
        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
    }

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest
    {
        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
    }

	@Override
	public void send_reply(ServerRequestInfo arg0) {

        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
	}
}
