/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.web;

import org.apache.cxf.jaxws.JAXWSMethodInvoker;

/**
 *
 */
public class POJOJAXWSMethodInvoker extends JAXWSMethodInvoker {

    private final Object serviceObject;

    /**
     * @return the serviceObject
     */
    public Object getServiceObject() {
        return serviceObject;
    }

    /**
     * @param bean
     */
    public POJOJAXWSMethodInvoker(Object bean) {
        super(bean);
        this.serviceObject = bean;
    }
}
