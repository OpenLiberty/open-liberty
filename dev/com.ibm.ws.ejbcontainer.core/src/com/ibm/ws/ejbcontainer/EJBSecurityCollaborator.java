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

import java.security.Principal;

import javax.ejb.EJBAccessException;

/**
 * Security callback interface as EJB methods are being called.
 */
public interface EJBSecurityCollaborator<T>
                extends EJBRequestCollaborator<T>
{
    /**
     * {@inheritDoc}
     *
     * @throws EJBAccessException if the caller role is not allowed
     */
    @Override
    T preInvoke(EJBRequestData request)
                    throws Exception;

    /**
     * Called by the container after all method interceptors if any interceptor
     * called {@code InvocationContext.setParameters}.
     *
     * @param request the EJB request; {@link EJBRequestData#getMethodArguments} contains the updated arguments
     * @param preInvokeData the value returned by {@link #preInvoke}
     */
    void argumentsUpdated(EJBRequestData request, T preInvokeData)
                    throws Exception;

    /**
     * @param cmd the originating EJB component
     * @param request the EJB request, or null if there is no active EJB request
     * @param preInvokeData the value returned by {@link #preInvoke}, or null
     *            if there is no active EJB request
     * @return the caller identity
     */
    @Deprecated
    java.security.Identity getCallerIdentity(EJBComponentMetaData cmd,
                                             EJBRequestData request,
                                             T preInvokeData);

    /**
     * @param cmd the originating EJB component
     * @param request the EJB request, or null if there is no active EJB request
     * @param preInvokeData the value returned by {@link #preInvoke}, or null
     *            if there is no active EJB request
     * @return the caller principal
     */
    Principal getCallerPrincipal(EJBComponentMetaData cmd,
                                 EJBRequestData request,
                                 T preInvokeData);

    /**
     * @param cmd the EJB component being invoked
     * @param request the EJB request, or null if there is no active EJB request
     * @param preInvokeData the value returned by {@link #preInvoke}, or null
     *            if there is no active EJB request
     * @param roleName the role name to check
     * @param roleLink the role link for the role name, or null if unspecified
     * @return true if the current security principal is in the specified role
     */
    boolean isCallerInRole(EJBComponentMetaData cmd,
                           EJBRequestData request,
                           T preInvokeData,
                           String roleName,
                           String roleLink);

    /**
     * @return true if JACC requires EJB arguments, false by default.
     */
    boolean areRequestMethodArgumentsRequired();
}
