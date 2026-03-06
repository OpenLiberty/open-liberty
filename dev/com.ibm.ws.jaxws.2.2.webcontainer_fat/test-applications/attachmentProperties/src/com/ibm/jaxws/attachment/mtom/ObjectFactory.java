/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.jaxws.attachment.mtom;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.jaxws.attachment.mtom package.
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
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.jaxws.attachment.mtom
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SendImage }
     *
     */
    public SendImage createSendImage() {
        return new SendImage();
    }

    /**
     * Create an instance of {@link SendImageResponse }
     *
     */
    public SendImageResponse createSendImageResponse() {
        return new SendImageResponse();
    }

    /**
     * Create an instance of {@link ImageDepot }
     *
     */
    public ImageDepot createImageDepot() {
        return new ImageDepot();
    }

}
