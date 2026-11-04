/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.version40.test.http.war.app;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/testController")
public class TestController {

	// In-memory storage for TestData objects
	private final ConcurrentHashMap<String, TestData> dataStore = new ConcurrentHashMap<>();

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
	
	@RequestMapping(value = "postJson", method = RequestMethod.POST,
	                consumes = "application/json")
	public void postJson(@RequestBody TestData data) throws Exception{
		// Store the data using message as key
		dataStore.put(data.getMessage(), data);
	}
	
	@RequestMapping(value = "getJson/{key}", method = RequestMethod.GET,
	                produces = "application/json")
	public ResponseEntity<TestData> getJson(@PathVariable("key") String key) throws Exception{
		TestData data = dataStore.get(key);
		if (data != null) {
			return ResponseEntity.ok(data);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
	}
	
	@RequestMapping(value = "put", method = RequestMethod.PUT)
	public String normalPut() throws Exception{
		return "Greetings from Spring Boot!. I am here to PUT things!";
	}
	
	@RequestMapping(value = "putJson/{key}", method = RequestMethod.PUT,
	                consumes = "application/json", produces = "application/json")
	public ResponseEntity<TestData> putJson(@PathVariable("key") String key, @RequestBody TestData data) throws Exception{
		if (dataStore.containsKey(key)) {
			// Update existing data
			dataStore.put(key, data);
			return ResponseEntity.ok(data);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
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
