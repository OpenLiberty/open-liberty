/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.sample.bean;

import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

import com.ibm.sample.jaxws.hello.client.HelloService;
import com.ibm.sample.util.PropertiesUtil;

/**
 *
 */
@Stateless
public class HelloBean {
    @WebServiceRef(name = "service/TestService")
    private HelloService helloService;

    public String getTestProperties() {
        return PropertiesUtil.getTestProperties(helloService.getHelloPort());
    }
}
