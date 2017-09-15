/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

/**
 * Callback interface as EJB methods are being called.
 */
public interface EJBRequestCollaborator<T>
{
    /**
     * Called by container prior to executing an EJB method (see {@link EJBMethodInterface} for the possible method types). The timing of
     * the method call depends on how the collaborator is registered with the
     * container. If this method returns successfully, then {@link #postInvoke} will be called.
     * 
     * @param request the EJB request
     * @return a value that should be passed to {@link #postInvoke}
     */
    T preInvoke(EJBRequestData request)
                    throws Exception;

    /**
     * Called by the container after executing an EJB method. This method will
     * only be called if {@link #preInvoke} returns successfully.
     * 
     * @param request the EJB request
     * @param preInvokeResult the value returned by {@link #preInvoke}
     */
    void postInvoke(EJBRequestData request, T preInvokeResult)
                    throws Exception;
}
