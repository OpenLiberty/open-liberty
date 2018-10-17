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
package com.ibm.ws.springboot.support.version15.test.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

	@RequestMapping("/")
	public String hello() {
		return "HELLO SPRING BOOT!!";
	}

	
	@RequestMapping(value="/exception", produces="text/html")
	public void throwIllegalArgumentException() {
		throw new IllegalArgumentException("Thrown on purpose for FAT test. Exception error page.");
	}

	@RequestMapping(value="/other-exception", produces="text/html")
    public void throwFileSystemNotFoundException() {
        throw new java.nio.file.FileSystemNotFoundException("Thrown on purpose for FAT test. Default error page.");
    }   

}
