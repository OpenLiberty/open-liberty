/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.common.internal;

import java.io.Serializable;

/**
 * J2EE specification states that in addition to Serializable objects
 * HttpSession has to support J2EE objects(EJB, EJBHOME, Context & UserTransction) in
 * a distributable environment. As these J2EE objects are not serializable,
 * we use the serializable information of those Objects and store it form of
 * J2EEObjectWrapper when serializing to external store and we convert the J2EE wrapper
 * back to J2EE objects when deserializing
 *
 * @version 1.0
 * @author srinivas@planetjava
 */
public class J2EEObjectWrapper implements Serializable {

    private Object serObj = null;
    private short ejbLocalType = 0; //cmd LIDB2282.12 - distinguishes EJBLocalHome and EJBLocalObject
    private static final long serialVersionUID = 2362252033211339042L;

    /**
     * Constructor for EJBWrapper.
     */
    public J2EEObjectWrapper() {
        super();
    }

    /**
     * Argument has to be a Serializable
     */
    public J2EEObjectWrapper(Object obj) {
        this.serObj = obj;
    }

    // cmd LIDB2282.12 new constructor that includes type
    public J2EEObjectWrapper(Object obj, short type) {
        this.serObj = obj;
        this.ejbLocalType = type;
    }

    /**
     * To get at the serializable
     * object
     */
    public Object getSerializableObject() {
        return serObj;
    }

    // cmd LIDB2282.12 new method getType()
    public int getEjbLocalType() {
        return ejbLocalType;
    }
}
