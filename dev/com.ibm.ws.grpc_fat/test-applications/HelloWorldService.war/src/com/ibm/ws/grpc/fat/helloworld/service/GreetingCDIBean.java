/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.grpc.fat.helloworld.service;

import java.io.Serializable;

import javax.enterprise.context.Dependent;

@Dependent
public class GreetingCDIBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private String greeting;
    private String interceptorMessage;

    public GreetingCDIBean() {
        greeting = "howdy from GreetingCDIBean";
        interceptorMessage = "server CDI interceptor invoked";
    }

    public String getGreeting() {
        return greeting;
    }

    public String getInterceptorMessage() {
        return interceptorMessage;
    }
}