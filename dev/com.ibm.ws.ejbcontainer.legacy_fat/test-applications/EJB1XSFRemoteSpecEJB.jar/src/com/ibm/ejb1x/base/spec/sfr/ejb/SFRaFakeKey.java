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

package com.ibm.ejb1x.base.spec.sfr.ejb;

import java.io.Serializable;

//
//
//  SFRaFake PrimaryKey to execute remove(PrimaryKey) method of Home interface.
//
//
@SuppressWarnings("serial")
public class SFRaFakeKey implements Serializable {
    public String keyid;

    /**
     * SFRaFakeKey constructor comment.
     */
    public SFRaFakeKey() {
        super();
    }

    /**
     *
     * @param id java.lang.String
     */
    public SFRaFakeKey(String id) {

        this.keyid = id;

    }

    /**
     *
     * @return java.lang.String
     */
    @Override
    public String toString() {
        return keyid;
    }
}
