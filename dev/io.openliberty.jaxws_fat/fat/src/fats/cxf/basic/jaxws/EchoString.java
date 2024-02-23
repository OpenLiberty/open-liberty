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

//import javax.xml.ws.*;
import javax.jws.*;
import javax.jws.soap.SOAPBinding;

/**
 * Use Doc/Lit BARE service to test jaxws 2.2 (spec 3.6 Conformance "Overriding JAXB types empty namespace: JAX-WS tools and runtimes MUST override
 * the default empty namespace for JAXB types and elements to SEI's targetNamespace.")
 *
 */
public interface EchoString {

    @WebResult(name = "echoResponse", targetNamespace = "http://jaxws.basic.cxf.fats/", partName = "echoResponse")

    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL,
                 parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public String echo(String parm);

}
