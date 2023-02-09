/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
// LH change to public, review the effects of LocalObject
public class IIOPWorkContextInterceptor extends LocalObject implements ServerRequestInterceptor {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(IIOPWorkContextInterceptor.class);


    //LH static ?
    private static final ThreadLocal<Map<String, Serializable>> workInfoMap = new ThreadLocal<Map<String, Serializable>> (){

	// LH Why is this concurrent, isn't it accessed by single thread ?
        @Override
        protected Map<String, Serializable> initialValue() {
            //return new ConcurrentHashMap<>();
            return new HashMap<>();
        }
    };

    // LH move to ExecutorDispatchStrategy since accessed by both bundles ?
    public static Map<String, Serializable> getWorkInfoMap(){
/** Clears out the thread local
	Map<String, Serializable> threadWorkInfo = workInfoMap.get();
	workInfoMap.remove();
	return threadWorkInfo;
 */
       return workInfoMap.get();

    }


    // LH class created , keep .
    public IIOPWorkContextInterceptor() {

    }

    // LH Gathering info ( ExecutorDispatherStrategy ) workContext later
    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {


//        try {

                int requestId = ri.request_id();
                String operation = ri.operation();
                // String targetId = ri.target_most_derived_interface();

                // Set ExecutorDispatchStrategy new map, pass to ExecutorDispatchStrategy
                // setIiopWorkContext

                // ExecutorDispatchStrategy.setIiopWorkContext();

                //ExecutorDispatchStrategy.getworkInfoMap().put(WorkContext.IIOP_REQUEST_ID, requestId);
                //workInfoMap.get().put(WorkContext.IIOP_REQUEST_ID, requestId);
                //workInfoMap.get().put(WorkContext.IIOP_OPERATION_NAME, operation);
                //IIOPContext.setIiopWorkContext();
                //IIOPContext.setIiopWorkContext().put(WorkContext.IIOP_REQUEST_ID, requestId);
                Map<String, Serializable> theMap = workInfoMap.get();

                // Prepare for WorkContext use. Do we need this here since we have it in send_xxxx ?
                theMap.clear();

                theMap.put(WorkContext.IIOP_REQUEST_ID, requestId);
                theMap.put(WorkContext.IIOP_OPERATION_NAME, operation);
                // theMap.put(WorkContext.IIOP_TARGET_NAME, targetId);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "operation: " + operation + ", and requestId: " + requestId);

                // debug: What's in the map
                System.out.println("IIOPWorkContextInterceptor - requestId: " + theMap.get(WorkContext.IIOP_REQUEST_ID) + "\n");
                System.out.println("IIOPWorkContextInterceptor - operation: " + theMap.get(WorkContext.IIOP_OPERATION_NAME) + "\n");
                // System.out.println("IIOPWorkContextInterceptor - targetId: " + theMap.get(WorkContext.IIOP_TARGET_NAME) + "\n");

                //        } //catch (SystemException e) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//                Tr.debug(tc, "Could not suspend transaction", e);
//        }
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
	System.out.println("send_other - release WorkinfoMap");
        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
    }

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest
    {
	System.out.println("send_exception - release WorkinfoMap");
        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
    }

	@Override
	// public void receive_request(ServerRequestInfo arg0) throws ForwardRequest {
	public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
	// Don't think we have to do anything here.
		System.out.println("receive_request - target_most_derived_interface");
		String targetId = ri.target_most_derived_interface();
		//theMap.put(WorkContext.IIOP_TARGET_NAME, targetId);
		System.out.println("IIOPWorkContextInterceptor - targetId: " + targetId + "\n");
		// System.out.println("IIOPWorkContextInterceptor - targetId: " + theMap.get(WorkContext.IIOP_TARGET_NAME) + "\n");

	}

	@Override
	public void send_reply(ServerRequestInfo arg0) {

		System.out.println("send_reply - release WorkinfoMap");
        // Release the contents of the WorkInfoMap
        workInfoMap.get().clear();
	}
}
