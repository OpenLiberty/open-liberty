/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.slr.ejb;

/**
 * Insert the type's description here.
 * Creation date: (08/18/2000 11:27:51 AM)
 * 
 * @author Administrator
 */
//
//
//  SLRaFake PrimaryKey to execute remove(PrimaryKey) method of Home interface.
//
//
@SuppressWarnings("serial")
public class SLRaFakeKey implements java.io.Serializable {
    public String keyid;

    /**
     * SLRaFakeKey constructor comment.
     */
    public SLRaFakeKey() {
        super();
    }

    /**
     * Insert the method's description here.
     * Creation date: (08/18/2000 12:26:44 PM)
     * 
     * @param id java.lang.String
     */
    public SLRaFakeKey(String id) {

        this.keyid = id;

    }

    /**
     * Insert the method's description here.
     * Creation date: (08/18/2000 12:09:13 PM)
     * 
     * @return java.lang.String
     */
    @Override
    public String toString() {
        return keyid;
    }
}