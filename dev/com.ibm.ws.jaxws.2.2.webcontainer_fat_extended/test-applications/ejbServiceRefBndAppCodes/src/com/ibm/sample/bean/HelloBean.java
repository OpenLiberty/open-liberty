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
package com.ibm.sample.bean;

import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

import com.ibm.sample.jaxws.hello.client.HelloService;
import com.ibm.sample.util.PropertiesUtil;

@Stateless
public class HelloBean {
    @WebServiceRef(name = "service/TestService")
    private HelloService helloService;

    public String getTestProperties() {
        return PropertiesUtil.getTestProperties(helloService.getHelloPort());
    }
}
