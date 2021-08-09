/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

public interface EntityHelper
{
    EJBObject getBean(EJSHome home, Object primaryKey)
                    throws RemoteException;

    EJBObject getBean(EJSHome home, String type, Object primaryKey, Object data)
                    throws FinderException,
                    RemoteException;

    EJBLocalObject getBean_Local(EJSHome home, Object primaryKey)
                    throws RemoteException;

    @SuppressWarnings("rawtypes")
    Enumeration getCMP20Enumeration(EJSHome home, Enumeration keys)
                    throws FinderException,
                    RemoteException;

    @SuppressWarnings("rawtypes")
    Enumeration getCMP20Enumeration_Local(EJSHome home, Enumeration keys)
                    throws FinderException,
                    RemoteException;

    @SuppressWarnings("rawtypes")
    Collection getCMP20Collection(EJSHome home, Collection keys)
                    throws FinderException,
                    RemoteException;

    @SuppressWarnings("rawtypes")
    Collection getCMP20Collection_Local(EJSHome home, Collection keys)
                    throws FinderException,
                    RemoteException;

    /**
     * Internal method that provides common implementation of both
     * activateBean and activateBean_Local. <p>
     * 
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     * @param pkeyCopyRequired true if the primary key was provided by
     *            customer code and needs to be copied if the
     *            EJB Container will use it as a key into
     *            internal structures (like EJB Cache).
     * 
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key).
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key.
     */
    // f111627 d215317
    EJSWrapperCommon activateBean_Common(EJSHome home, Object primaryKey, boolean pkeyCopyRequired)
                    throws FinderException,
                    RemoteException;

    EntityBeanO getFindByPrimaryKeyEntityBeanO(EJSHome home)
                    throws RemoteException;

    EntityBeanO getFinderEntityBeanO(EJSHome home)
                    throws RemoteException;

    void discardFinderEntityBeanO(EJSHome home, EntityBeanO beanO)
                    throws RemoteException;

    void releaseFinderEntityBeanO(EJSHome home, EntityBeanO beanO)
                    throws RemoteException;

    BeanManagedBeanO getFinderBeanO(EJSHome home)
                    throws RemoteException;

    void releaseFinderBeanO(EJSHome home, BeanManagedBeanO beanO)
                    throws RemoteException;

    EntityBeanO getHomeMethodEntityBeanO(EJSHome home)
                    throws RemoteException;

    void releaseHomeMethodEntityBeanO(EJSHome home, EntityBeanO beanO)
                    throws RemoteException;

    /**
     * Implementation of EJSHome.remove for entity beans.
     * 
     * @param primaryKey the <code>Object</code> containing the primary key of
     *            EJB to remove from this home <p>
     */
    BeanId remove(EJSHome home, Object primaryKey)
                    throws RemoteException,
                    RemoveException,
                    FinderException;

    /**
     * This gets a EJBMethodInfoImpl from the thread stack. Must be called together
     * with putPMMethodInfo to ensure that the MethodInfo is returned back to
     * the stack.
     * 
     * @return initialized EJBMethodInfoImpl off the thread stack
     */
    public EJBMethodInfoImpl getPMMethodInfo(EJSDeployedSupport s, //130230
                                             EJSWrapperBase wrapper, int methodId,
                                             String methodSignature);

    public boolean hasMethodLevelAccessIntentSet(EJSHome ejbHome);
}
