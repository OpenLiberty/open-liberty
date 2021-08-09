/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.RemoveException;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * An <code>EJSLocalWrapper</code> wraps an EJB and interposes
 * calls to the container before and after every method call on the
 * EJB. <p>
 * 
 * The <code>EJSLocalWrapper</code> is designed to contain a minimum amount
 * of state. Its primary state consists of a reference to the container
 * it is in and a <code>BeanId</code> instance that defines the identity
 * of its associated EJB. Note, it does not directly maintain a reference
 * to the EJB. It relies on the container to supply it with the appropriate
 * EJB. <p>
 * 
 * NOTE: The deployed local interface to a bean extends an EJSLocaWrapper
 * instance. For that reason, EJSLocalWrapper MUST NOT implement any
 * methods other than those in the EJBObject interface. Otherwise,
 * there would be a potential conflict between the methods on
 * the bean's local interface and those on EJSLocalWrapper.
 */

public class EJSLocalWrapper extends EJSWrapperBase implements EJBLocalObject
{
    private static final String CLASS_NAME = "com.ibm.ejs.container.EJSLocalWrapper";

    /**
     * Get a reference to the EJBHome associated with this wrapper. <p>
     * This method is part of the EJBLocalObject interface. Therefore,
     * this method should only be called on wrappers that are
     * NOT a home wrapper (e.g. beanId._isHome is false).
     * 
     * @return reference <code>EJBLocalHome</code> for this wrapper.
     */
    public EJBLocalHome getEJBLocalHome() throws javax.ejb.EJBException
    {
        EJBLocalHome result = null;
        try {
            if (beanId._isHome) //d188404
            {
                throw new IllegalStateException("getEJBLocalHome is not a method of a EJBLocalHome object."); //d188404
            }
            result = (EJBLocalHome) wrapperManager
                            .getWrapper(beanId.getHome().getId()).getLocalObject();
        } catch (java.rmi.RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".getEJBLocalHome", "67", this);
            throw new EJBException(ex);
        }
        return (result);
    } // getEJBLocalHome

    /**
     * Get the primary key associated with this wrapper. <p>
     * This method is part of the EJBLocalObject interface.
     * 
     * @return an <code>Object</code> containing primary key of
     *         this wrapper.
     */
    public Object getPrimaryKey() throws javax.ejb.EJBException
    {
        HomeInternal hi = beanId.getHome();

        if (hi.isStatelessSessionHome() || hi.isStatefulSessionHome()) {
            throw new IllegalSessionMethodLocalException();
        }

        //PK26539: copy the primary key (see WASInternal_copyPrimaryKey for explanation)
        return ((EJSHome) hi).WASInternal_copyPrimaryKey(beanId.getPrimaryKey());
    } // getPrimaryKey

    /**
     * Return true iff the given EJBLocalObject reference is identical
     * to this wrapper. <p>
     * This method is part of the EJBLocalObject interface.
     * 
     * @param obj Local interface object to compare.
     */
    public boolean isIdentical(EJBLocalObject obj) throws javax.ejb.EJBException
    {
        // d114925 begin
        HomeInternal hi = beanId.getHome();

        if (hi.isStatefulSessionHome()) {
            // Session 6.9.1 - Stateful Session Bean
            //  session beans are identical if they are referring to the bean
            //  created with the same BeanId
            return (beanId.equals(((EJSLocalWrapper) obj).beanId));

        } else if (hi.isStatelessSessionHome()) {
            // Session 6.9.2 - Stateless Session Bean
            //  all session objects of the same stateless session bean within the
            //  same home have the same object identity.
            BeanId thisHomeId = hi.getId();
            BeanId cmpHomeId = ((EJSLocalWrapper) obj).beanId.getHome().getId();
            return (thisHomeId.equals(cmpHomeId));

        } else {
            // d114925 end
            // Session 9.8 - Entity Bean
            return this.getPrimaryKey().equals(obj.getPrimaryKey());

        } // d114925
    } // isIdentical

    /**
     * Remove bean associated with this wrapper instance. <p>
     * This method is part of the EJBLocalObject interface.
     */
    public void remove() throws RemoveException, javax.ejb.EJBException
    {
        try {
            container.removeBean(this);
        } catch (java.rmi.RemoteException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".remove", "132", this);
            throw new EJBException(e);
        }
    } // remove

} // EJSLocalWrapper
