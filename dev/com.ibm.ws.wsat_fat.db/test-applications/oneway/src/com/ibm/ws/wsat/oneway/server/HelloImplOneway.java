/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.oneway.server;

import javax.jws.Oneway;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

@Addressing(enabled=true, required=true)
@WebService(wsdlLocation="WEB-INF/wsdl/HelloImplOnewayService.wsdl")
public class HelloImplOneway{
	
	@Oneway
	public String sayHello(){
		return "Hello World!";
	}
}
