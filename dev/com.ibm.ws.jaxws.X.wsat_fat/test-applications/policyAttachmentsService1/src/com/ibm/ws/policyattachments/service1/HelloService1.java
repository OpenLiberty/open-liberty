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
package com.ibm.ws.policyattachments.service1;


import javax.jws.WebService;

@WebService(serviceName="HelloService1")
public class HelloService1 {
	public String helloWithoutPolicy(){
		return "helloWithoutPolicy invoked";
	}
	
	public String helloWithPolicy(){
		return "helloWithPolicy invoked";
	}
	
	public String helloWithOptionalPolicy(){
		return "helloWithOptionalPolicy invoked";
	}
	
	public String helloWithYouWant(){
		return "helloWithYouWant invoked";
	}
}
