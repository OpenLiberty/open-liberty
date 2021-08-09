/*******************************************************************************
 * Copyright (c) 2001, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;

/**
 * An <code>EJSWrapperBase</code> wraps an EJB and interposes
 * calls to the container before and after every method call on the
 * EJB. <p>
 *
 * The <code>EJSWrapperBase</code> is designed to contain a minimum amount
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
 */
@Sensitive
public abstract class EJSWrapperBase
{ // d111687
    /**
     * Container this wrapper lives in
     */
    public EJSContainer container;

    /**
     * The identity of this wrapper object.
     */
    public BeanId beanId; // d110126 d163197

    /**
     * The bean meta data for this wrapper object.
     **/
    public BeanMetaData bmd; // LIDB2775-23.8

    /**
     * Table of transaction attributes for the methods on this wrapper.
     * Indexed by method id defined in deployed code. <p>
     */
    protected EJBMethodInfoImpl methodInfos[];
    protected int isolationAttrs[];
    protected String methodNames[];

    /**
     * The servant manager associated with this wrapper. Used when
     * processing request from ORB to generate this wrapper's key
     * (it's portion of an IOR).
     */
    protected WrapperManager wrapperManager;

    /**
     * Performance data object for this wrapper.
     */
    // d174057.2 F743-22763
    public EJBPMICollaborator ivPmiBean = null;

    /**
     * Reference to the EJSWrapperCommon that holds this Remote/Local Wrapper.
     **/
    // d140003.9
    protected EJSWrapperCommon ivCommon;

    /**
     * True if this wrapper is fully managed by the WrapperManager. <p>
     *
     * False is not an indication that the wrapper is not in the EJSWrapperCommon
     * object (and thus in the Wrapper Cache). False just means that the
     * wrapper does not need to go through WrapperManager pre/postInvoke, where
     * it is pinned in the cache and LRU data is updated. <p>
     **/
    // d174057.2
    protected boolean isManagedWrapper = false;

    /**
     * The interface type for which the wrapper is an implementation.
     **/
    public WrapperInterface ivInterface; // d452386

    /**
     * Business interface index special value for aggregate local wrappers. <p>
     *
     * Aggregate local wrappers implement all of the business local interfaces
     * and therefore do not have a specific index, however some indication is
     * required to differentiate them from component local wrappers. <p>
     *
     * The special index values of -2 is used to avoid conflicting with any
     * possible valid index value, and to stand out from the default of -1. <p>
     */
    // F743-34304
    public static final int AGGREGATE_LOCAL_INDEX = -2;

    /**
     * Eye catcher for aggregate wrappers, stored in WrapperId in place of
     * the interface name.
     */
    // d677413
    public static final String AGGREGATE_EYE_CATCHER = "AGGREGATE";

    /**
     * Business interface index if ivInterface indicates it is either
     * a local or remote business interface. Will be a special value for
     * aggregate local wrappers (all interfaces).
     */
    public int ivBusinessInterfaceIndex; // d452386

    /**
     * Return string representation of this wrapper instance. <p>
     */
    @Override
    public String toString()
    {
        return super.toString() + "(" + beanId.toString() + ")";
    } // toString

    /**
     * Indicates whether some other object is "equal to" this one. The EJB
     * specification requires that client views implement hashCode and equals.
     * For remote views, javax.rmi.CORBA.Stub will use the IOR. This method
     * works for all bean types including entities, which is not required by
     * the EJB specification but is required by PM.
     */
    // d583041
    @Override
    public boolean equals(Object other)
    {
        if (this == other) // PM27840
        {
            return true;
        }

        if (other instanceof EJSWrapperBase)
        {
            EJSWrapperBase otherWrapper = (EJSWrapperBase) other;

            return ivInterface == otherWrapper.ivInterface &&
                   ivBusinessInterfaceIndex == otherWrapper.ivBusinessInterfaceIndex &&
                   beanId.equals(otherWrapper.beanId);
        }

        return false;
    }

    /**
     * Returns a hash code value for the object. The EJB specification
     * requires that client views implement hashCode and equals. For remote
     * views, javax.rmi.CORBA.Stub will use the IOR. This method works for all
     * bean types including entities, which is not required by the EJB
     * specification but is required by PM.
     */
    // d583041
    @Override
    public int hashCode()
    {
        return beanId.hashCode() ^
               ivInterface.ordinal() ^
               ivBusinessInterfaceIndex;
    }

} // EJSWrapperBase
