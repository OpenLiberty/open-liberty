/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.version20.test.http.war.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testController")
public class TestController {

	@RequestMapping(value = "parm/{pathVar}", method = RequestMethod.GET)
	public String pathVariableMethod(@PathVariable("pathVar") String pathVar) throws Exception{
		return "Greetings from Spring Boot! you gave me " + pathVar;
	}


	@RequestMapping(value = "query", method = RequestMethod.GET)
	public String queryVariableMethod(@RequestParam("queryVal") String qeuryVal) throws Exception{
		return "Greetings from Spring Boot! you gave me a query param val of  " + qeuryVal;
	}


	@RequestMapping(value = "get", method = RequestMethod.GET)
	public String normalGet() throws Exception{
		return "Greetings from Spring Boot!. I am here to GET you excited!";
	}
	
	@RequestMapping(value = "post", method = RequestMethod.POST)
	public String normalPost() throws Exception{
		return "Greetings from Spring Boot!. I am here to build a POST!";
	}
	
	@RequestMapping(value = "put", method = RequestMethod.PUT)
	public String normalPut() throws Exception{
		return "Greetings from Spring Boot!. I am here to PUT things!";
	}
	
	@RequestMapping(value = "delete", method = RequestMethod.DELETE)
	public String normalDelete() throws Exception{
		return "Greetings from Spring Boot!. I am here to DELETE things!";
	}
	
	@RequestMapping(value = "options", method = RequestMethod.OPTIONS)
	public String normalOptions() throws Exception{
		return "Greetings from Spring Boot!. I am out of OPTIONSs!";
	}
	
	@RequestMapping(value = "head", method = RequestMethod.HEAD)
	public String normalHead() throws Exception{
		return "Greetings from Spring Boot!. I have HEAD!";
	}

}
