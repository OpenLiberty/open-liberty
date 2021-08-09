/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi.service.impl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jws.HandlerChain;
import javax.jws.WebService;

import com.ibm.ws.jaxws.cdi.beans.Student;
import com.ibm.ws.jaxws.cdi.service.Simple;

@WebService
@HandlerChain(file = "../../../../../../../../wsdl/handler-chain.xml")
public class SimpleImpl implements Simple {

//	private Student student = new Student();

    @Inject
    private Student student;

    @PostConstruct
    public void postCostructor() {
        System.out.println("SimpleImpl service's post constructor called");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("SimpleImpl service's pre destroy called");
    }

    @Override
    public String echo(String arg0) {
        return arg0 + "," + student.talk();
    }

}
