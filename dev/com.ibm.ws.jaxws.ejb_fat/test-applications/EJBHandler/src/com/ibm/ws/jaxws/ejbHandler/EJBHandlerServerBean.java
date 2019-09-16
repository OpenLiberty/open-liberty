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
package com.ibm.ws.jaxws.ejbHandler;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
@Stateless(name = "EJBHandlerBean")
@WebService
@HandlerChain(file = "handlers.xml")
public class EJBHandlerServerBean {

    @EJB(name = "EchoBean")
    Echo echo;

    public String echoServer(String value) {

        try {
            InitialContext jndiContext = new InitialContext();
            LogicalHandlerSayHi logicalHandlerSayHiBean = (LogicalHandlerSayHi) jndiContext.lookup("java:comp/env/LogicalHandlerSayHiBean");
            if (logicalHandlerSayHiBean == null) {
                System.out.println(getClass().getName() + "Unable to lookup LogicalHandlerSayHiBean from handler");
            } else {
                System.out.println(getClass().getName() + ":LogicalHandlerSayHiBean.sayHi:" + logicalHandlerSayHiBean.sayHi("ivan"));
            }
        } catch (NamingException e) {
            System.out.println(getClass().getName() + "Unable to lookup LogicalHandlerSayHiBean from handler " + e.getMessage());
        }

        try {
            InitialContext jndiContext = new InitialContext();
            SOAPHandlerSayHi soapHandlerSayHi = (SOAPHandlerSayHi) jndiContext.lookup("java:comp/env/SOAPHandlerSayHiBean");
            if (soapHandlerSayHi == null) {
                System.out.println(getClass().getName() + "Unable to lookup SOAPHandlerSayHi from handler");
            } else {
                System.out.println(getClass().getName() + ":SOAPHandlerSayHiBean.sayHi:" + soapHandlerSayHi.sayHi("ivan"));
            }
        } catch (NamingException e) {
            System.out.println(getClass().getName() + "Unable to lookup SOAPHandlerSayHi from handler " + e.getMessage());
        }

        System.out.println(getClass().getName() + ":EchoBean.echo:" + echo.echo("ivan"));
        return value;
    }
}
