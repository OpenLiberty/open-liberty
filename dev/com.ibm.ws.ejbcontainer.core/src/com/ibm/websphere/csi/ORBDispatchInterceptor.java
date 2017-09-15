/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * The <tt>ORBDispatchInterceptor</tt> is responsible for setting and restoring
 * the context class loader around invocations to a servant/tie.
 */
public interface ORBDispatchInterceptor
{
    /**
     * Called prior to argument deserialization. This object can return an
     * object that will be passed as a parameter to the corresponding
     * postInvokeORBDispatch call.
     * 
     * @param object the IDL Servant or RMI Tie
     * @param operation the operation being invoked
     * @return the object passed to postInvokeORBDispatch
     */
    public Object preInvokeORBDispatch(Object object, String operation);

    /**
     * Called after return value serialization. In order for the context class
     * loader to be correct for co-located stubs, the context class loader must
     * have been reset prior to return value serialization (usually done by a
     * wrapper to the actual servant). This method should restore the context
     * class loader in the event of a failure prior to that occuring (during
     * argument deserialization, for example).
     * 
     * @param the object returned from {@link postInvokeORBDispatch}
     */
    public void postInvokeORBDispatch(Object cookie);
}
