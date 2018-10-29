/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.app;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TestApplication {
	public static final String TEST_ATTR = "test.weblistener.attr";
	@Autowired
	ServletContext context; 
	
	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
		for(String arg: args) {
			if("--throw.application.exception=true".equals(arg)) {
				throw new RuntimeException("APPLICATION EXCEPTION");
			}
		}	
	}

	@RequestMapping("/")
	public String hello() {
		return "HELLO SPRING BOOT!!";
	}
	
	@RequestMapping(value="/buttonClicked", produces="text/html")
	public String click() {
		return "Hello. You clicked a button.";
	}
	
	@RequestMapping(value="/getAppProp")
	public String getAppProperty(@RequestParam("key") String key) {
		return env.getProperty(key);
	}
	
	@RequestMapping("/testWebListenerAttr")
	public String testWebListenerAttr() {
		// should be null
		Object result = context.getAttribute(TEST_ATTR);
		if (result == null) {
			return "PASSED";
		} else {
			return "FAILED";
		}
	}

	@RequestMapping("/testContextParams")
	public String testContextParams() {
		return context.getInitParameter("context_parameter_test_key");
	}

	@RequestMapping(value="/exception", produces="text/html")
	public void throwIllegalArgumentException() {
		throw new IllegalArgumentException("Thrown on purpose for FAT test. Exception error page.");
	}

	@RequestMapping(value="/other-exception", produces="text/html")
	public void throwFileSystemNotFoundException() {
		throw new java.nio.file.FileSystemNotFoundException("Thrown on purpose for FAT test. Default error page.");
	}
	
	static final String 
	IbmApiClazzName = "com.ibm.websphere.application.ApplicationMBean",
	TpClazzName 	= "javax.mail.Message";

	@RequestMapping("/loadIbmApiClass")
	public String loadApiClass() { return loadClazz(IbmApiClazzName); }

	@RequestMapping("/loadTpClass")
	public String loadTpClass() { return loadClazz(TpClazzName); }

	String loadClazz(String clazzName) {
		Class<?> clazz = null;
		try {
			clazz = this.getClass().getClassLoader().loadClass(clazzName);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return "SPRING BOOT, YOU GOT" + ((null==clazz) ? " NO " : " ") + "CLAZZ: " + clazzName;
	}
	
}
