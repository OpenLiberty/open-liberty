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

    /** {@inheritDoc} */
	@Override
    public void dispatch(final DispatchRequest req) {

		// empty then skip
		if ( IIOPWorkContextInterceptor.getWorkInfoMap().isEmpty()) {
			System.out.println("ExecutorDispatchStrategy-Infomap empty " + "\n");
			executor.execute(new Runnable() {

                @Override
                public void run() {
                    req.invoke();
                }

            });
		}
		else
		{
			System.out.println("ExecutorDispatchStrategy - dispatch Map update for WorkType: " + wc.getWorkType() + "\n");

		wc.putAll(IIOPWorkContextInterceptor.getWorkInfoMap());

		//Test purposes
		System.out.println("ExecutorDispatchStrategy-requestId: " + wc.get(WorkContext.IIOP_REQUEST_ID) + "\n");
		System.out.println("ExecutorDispatchStrategy-operation: " + wc.get(WorkContext.IIOP_OPERATION_NAME) + "\n");
		System.out.println("ExecutorDispatchStrategy-targetId : " + wc.get(WorkContext.IIOP_TARGET_NAME) + "\n");

		System.out.println("ExecutorDispatchStrategy-dispatch execute RunnableWithContext" + "\n");
		executor.execute(new RunnableWithContext() {

            @Override
            public void run() {
                req.invoke();
            }

            @Override
            public WorkContext getWorkContext() {
		System.out.println("ExecutorDispatchStrategy - dispatch Map update in getWorkContext ");
		// possible fix
		wc.putAll(IIOPWorkContextInterceptor.getWorkInfoMap());
		System.out.println("ExecutorDispatchStrategy-getWorkContext- requestId: " + wc.get(WorkContext.IIOP_REQUEST_ID) + "\n");
		System.out.println("ExecutorDispatchStrategy-getWorkContext- operation: " + wc.get(WorkContext.IIOP_OPERATION_NAME) + "\n");
		System.out.println("ExecutorDispatchStrategy-getWorkContext- targetId : " + wc.get(WorkContext.IIOP_TARGET_NAME) + "\n");

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
