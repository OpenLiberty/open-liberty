/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider
     * 
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link EchoStringResponse }
     * 
     */
    public EchoStringResponse createEchoStringResponse() {
        return new EchoStringResponse();
    }

    /**
     * Create an instance of {@link EchoString }
     * 
     */
    public EchoString createEchoString() {
        return new EchoString();
    }

}
