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
package com.ibm.ws.wsat.assertion.server;

import javax.jws.WebService;

@WebService(wsdlLocation="WEB-INF/wsdl/assertionOptional.wsdl")
public class HelloImplAssertionOptional{
	public String sayHello(){
		return "Hello World!";
	}
	
	public String sayHelloToOther(String username){
		return "Hello " + username;
	}
}
