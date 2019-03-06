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
package com.ibm.ws.jaxws.ejbinwar.ejb;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jws.WebService;

@WebService(serviceName = "SayHelloStatelessService",
            portName = "SayHelloStalelessPort")
@Stateless(name = "SayHelloBean")
public class SayHelloStatelessBean implements SayHelloLocal {
    @Resource
    SessionContext context;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloLocal#invokeOther()
     */
    @Override
    public String invokeOther() {
        return context.getBusinessObject(SayHelloLocal.class).sayHello("Anonym");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.ejbinwar.ejb.SayHelloLocal#sayHello(java.lang.String)
     */
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + getClass().getSimpleName();
    }

}
