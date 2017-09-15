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

package com.ibm.ws.jaxws.client.injection;

import java.lang.annotation.Annotation;

import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;

/**
 * This class will implement the @WebServiceRef interace, and it
 * will be used to store metadata for a JAX-WS service-ref that
 * was found in a client deployment descriptor.
 * 
 */
public class WebServiceRefSimulator implements javax.xml.ws.WebServiceRef {

    private final String mappedName;

    private final String name;

    private final Class<?> type;

    private final Class<?> value;

    private final String wsdlLocation;

    private final String lookup;

    public WebServiceRefSimulator(String mappedName, String name, Class<?> type,
                                  Class<?> value, String wsdlLocation, String lookup) {
        this.mappedName = mappedName == null ? "" : mappedName;
        this.name = name == null ? "" : name;
        this.type = type == null ? Object.class : type;
        this.value = value == null ? Service.class : value;
        this.wsdlLocation = wsdlLocation == null ? "" : wsdlLocation;
        this.lookup = lookup == null ? "" : lookup;
    }

    @Override
    public String mappedName() {
        return mappedName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class type() {
        return type;
    }

    @Override
    public Class value() {
        return value;
    }

    @Override
    public String wsdlLocation() {
        return wsdlLocation;
    }

    @Override
    public String lookup() {
        return lookup;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return WebServiceRef.class;
    }

    @Override
    public String toString() {
        String result = "webServiceRef = " + this.getClass().getName() + " { " +
                        "name= " + name + " type= " + type + " value= " + value + " mappedName= " + mappedName +
                        " lookup=" + lookup + " wsdlLocation= " + wsdlLocation + " }";
        return result;
    }
}