/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.samples.jaxws.catalog.stubs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Response wrapper for the add operation
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
                                  "_return"
})
@XmlRootElement(name = "addResponse")
public class AddResponse {

    @XmlElement(name = "return", required = true)
    protected int _return;

    public AddResponse() {

    }

    public AddResponse(int ret) {
        _return = ret;
    }

    /**
     * Gets the return value.
     *
     * @return the result of the add operation
     */
    public int getReturn() {
        return _return;
    }

    /**
     * Sets the return value.
     *
     * @param value the result of the add operation
     */
    public void setReturn(int value) {
        this._return = value;
    }

}
