/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fats.cxf.basic.jaxws;



import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the jaxws.defaultpackage.wsfvt.server package.
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

    private final static QName _Echo_QNAME = new QName("http://jaxws.basic.cxf.fats/", "echo");
    private final static QName _EchoResponse_QNAME = new QName("http://jaxws.basic.cxf.fats/", "echoResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: jaxws.defaultpackage.wsfvt.server
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://jaxws.basic.cxf.fats/", name = "echo")
    public JAXBElement<String> createEcho(String value) {
        return new JAXBElement<String>(_Echo_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://jaxws.basic.cxf.fats/", name = "echoResponse")
    public JAXBElement<String> createEchoResponse(String value) {
        return new JAXBElement<String>(_EchoResponse_QNAME, String.class, null, value);
    }

}
