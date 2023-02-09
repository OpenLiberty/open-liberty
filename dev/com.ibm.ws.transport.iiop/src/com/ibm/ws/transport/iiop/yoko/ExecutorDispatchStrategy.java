/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.yoko;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.yoko.orb.OB.DispatchRequest;
import org.apache.yoko.orb.OB.DispatchStrategy;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;

import com.ibm.ws.threading.RunnableWithContext;
import com.ibm.wsspi.threading.WorkContext;
import com.ibm.ws.transport.iiop.internal.IIOPWorkContext;
// import com.ibm.ws.transport.iiop.workcontext;
import com.ibm.ws.transport.iiop.workcontext.IIOPWorkContextInterceptor;

/**
 *
 */
public class ExecutorDispatchStrategy extends LocalObject implements DispatchStrategy {

    private final Executor executor;
    final IIOPWorkContext wc = new IIOPWorkContext();

    /**
     * @param executor
     */
    public ExecutorDispatchStrategy(Executor executor) {
        this.executor = executor;
    }
    // Lh set the thread local, parms to map to place on thread local
 /*   public static void setIiopWorkContext() {

    }

    */

    // ** LH Test block'''
    //LH static ?
 /*   private static final ThreadLocal<Map<String, Serializable>> workInfoMap = new ThreadLocal<Map<String, Serializable>> (){

	// LH Why is this concurrent, isn't it accessed by single thread ?
        @Override
        protected Map<String, Serializable> initialValue() {
            return new ConcurrentHashMap<>();

        }
    };
 */
    // LH move to ExecutorDispatchStrategy since accessed by both bundles ?
/*    public static Map<String, Serializable> getWorkInfoMap(){
       return workInfoMap.get();

    }
    // ** LH End block '''

    */

    /** {@inheritDoc} */
	@Override
    public void dispatch(final DispatchRequest req) {

        //executor.execute(new RunnableWithContext() {
		// Extract threadlocal info ( operation ) from the iiop.workcontext and set wc
		// and remove from the iiop threadlocal.
		// Retrieving info in ReceiveRequest WorkContext

		// Merge Map from rec request into wc.
		// ..putAll()
		// wc.putAll(IIOPWorkContextInterceptor.getWorkInfoMap());

		// Mike Nov 2 using executor
		//wc.putAll(((ExecutorServiceImpl)executor).getWorkContext());
		// empty then skip
		if ( IIOPWorkContextInterceptor.getWorkInfoMap().isEmpty()) {

			executor.execute(new Runnable() {

                @Override
                public void run() {
                    req.invoke();
                }

            });
		}
		else
		{

		wc.putAll(IIOPWorkContextInterceptor.getWorkInfoMap());
		    //wc.putAll(workInfoMap.get());


		//System.out.println("Request_Id: " + workInfoMap.get().IIOP_REQUEST_ID + "\n");
		System.out.println("ExecutorDispatchStrategy-requestId: " + wc.get(WorkContext.IIOP_REQUEST_ID) + "\n");
		System.out.println("ExecutorDispatchStrategy-operation: " + wc.get(WorkContext.IIOP_OPERATION_NAME) + "\n");
		System.out.println("ExecutorDispatchStrategy-targetId: " + wc.get(WorkContext.IIOP_TARGET_NAME) + "\n");


		executor.execute(new RunnableWithContext() {

            @Override
            public void run() {
                req.invoke();
            }

            @Override
            public WorkContext getWorkContext() {
                return wc;
            }

            });
		}
    }

    /** {@inheritDoc} */
    @Override
    public int id() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override
    public Any info() {
        return new org.apache.yoko.orb.CORBA.Any();
    }

}
