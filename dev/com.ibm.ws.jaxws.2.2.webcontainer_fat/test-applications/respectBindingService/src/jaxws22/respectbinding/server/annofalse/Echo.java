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

package jaxws22.respectbinding.server.annofalse; // don't change this package

import javax.jws.WebService;
import javax.xml.ws.RespectBinding;

import jaxws22.respectbinding.server.Exception_Exception;

// The initial test run is from the validRequiredNoFeature test taken from tWAS
@RespectBinding(enabled = false)
@WebService(targetNamespace = "http://server.respectbinding.jaxws22/", wsdlLocation = "WEB-INF/wsdl/EchoService.wsdl")
public class Echo {
    public String echo(String in) throws Exception_Exception {
        System.out.println("EchoPort's RespectBinding is disabled and called with arg:" + in);
        return in;
    }
}
