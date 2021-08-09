/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.rmi.PortableRemoteObject;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl; // LIDB549.20

/**
 * An <code>EJSWrapper</code> wraps an EJB and interposes
 * calls to the container before and after every method call on the
 * EJB. <p>
 *
 * The <code>EJSWrapper</code> is designed to contain a minimum amount
 * of state. Its primary state consists of a reference to the container
 * it is in and a <code>BeanId</code> instance that defines the identity
 * of its associated EJB. Note, it does not directly maintain a reference
 * to the EJB. It relies on the container to supply it with the appropriate
 * EJB. <p>
 *
 * NOTE: The deployed remote interface to a bean extends an EJSWrapper
 * instance. For that reason, EJSWrapper MUST NOT implement any
 * methods other than those in the EJBObject interface. Otherwise,
 * there would be a potential conflict between the methods on
 * the bean's remote interface and those on EJSWrapper.
 *
 * @ibm-private-in-use
 */
public class EJSWrapper extends EJSRemoteWrapper
                implements EJBObject
{
    private static final String CLASS_NAME = "com.ibm.ejs.container.EJSWrapper";

    /**
     * Get a reference to the EJBHome associated with this wrapper. <p>
     *
     * This method is part of the EJBObject interface. <p>
     *
     * @return reference <code>EJBHome</code> for this wrapper <p>
     */
    @Override
    public EJBHome getEJBHome() throws RemoteException
    {
        EJSWrapper homeWrapper = wrapperManager.getWrapper(beanId.getHome().getId()).getRemoteWrapper();
        Object homeRef = container.getEJBRuntime().getRemoteReference(homeWrapper);
        return (EJBHome) PortableRemoteObject.narrow(homeRef, EJBHome.class);
    }

    /**
     * Get handle for this wrapper. <p>
     *
     * This method is part of the EJBObject interface. <p>
     *
     * @return <code>Handle</code> for this wrapper <p>
     */
    // p116276 - add code to push/pop java namespace onto ThreadContext.
    @Override
    public Handle getHandle() throws RemoteException
    {
        ComponentMetaDataAccessorImpl cmdAccessor = null; // p125735
        try {
            cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor(); // p125735
            cmdAccessor.beginContext(bmd); // p125735
            return beanId.getHome().createHandle(beanId);
        } finally
        {
            if (cmdAccessor != null)
                cmdAccessor.endContext(); // p125735
        }
    } // getHandle

    /**
     * Get the primary key associated with this wrapper. <p>
     *
     * This method is part of the EJBObject interface. <p>
     *
     * @return an <code>Object</code> containing primary key of
     *         this wrapper <p>
     */
    @Override
    public Object getPrimaryKey() throws RemoteException
    {
        HomeInternal hi = beanId.getHome();

        if (hi.isStatelessSessionHome() || hi.isStatefulSessionHome()) {
            throw new IllegalSessionMethodException();
        } else {
            return beanId.getPrimaryKey();
        }
    } // getPrimaryKey

    /**
     * Return true iff the given EJBObject reference is identical
     * to this wrapper. <p>
     *
     * This method is part of the EJBObject interface. <p>
     */
    @Override
    public boolean isIdentical(EJBObject obj) throws RemoteException
    {
        // ACK! The following implementation only works for the simplest
        // case. We're just going to rely on the ORB's implementation
        // of object equality.

        // This really needs a under-the-covers Remote interface to
        // allow us to obtain the BeanId of the other EJBObject to do
        // the comparison correctly.

        try {
            Object thisRef = container.getEJBRuntime().getRemoteReference(this);
            return thisRef.equals(obj); // d176674
        } catch (NoSuchObjectException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".isIdentical", "151", this);
            return false;
        } catch (ClassCastException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".isIdentical", "154", this);
            return false;
        }
    } // isIdentical

    /**
     * Remove bean associated with this wrapper instance. <p>
     *
     * This method is part of the EJBObject interface. <p>
     */
    @Override
    public void remove() throws RemoteException, RemoveException
    {
        container.removeBean(this);
    } // remove

} // EJSWrapper
