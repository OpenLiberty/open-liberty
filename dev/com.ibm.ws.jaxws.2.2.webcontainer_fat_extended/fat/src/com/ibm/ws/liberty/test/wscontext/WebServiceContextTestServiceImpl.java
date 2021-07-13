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
package com.ibm.ws.liberty.test.wscontext;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

@WebService(serviceName = "WebServiceContextTestService", portName = "WebServiceContextTestServicePort",
            endpointInterface = "com.ibm.ws.liberty.test.wscontext.WebServiceContextTestService", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com")
@Resources({ @Resource(name = "ws/context", type = WebServiceContext.class) })
public class WebServiceContextTestServiceImpl implements WebServiceContextTestService {
    private final static String TEST_PARAMETER_NAME = "testValue";

    private @Resource WebServiceContext context;

    /** {@inheritDoc} */
    @Override
    public boolean isInjectionInstanceNull() {
        return context == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDefaultJndiLookupInstanceNull() {
        WebServiceContext wsContext = null;
        try {
            Context cxt = new InitialContext();
            wsContext = (WebServiceContext) cxt.lookup("java:comp/env/com.ibm.ws.liberty.test.wscontext.WebServiceContextTestServiceImpl/context");

            System.out.println("Hello wsContext value is: " + wsContext);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return wsContext == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelfDefinedJndiLookupInstanceNull() {
        WebServiceContext wsContext = null;
        try {
            Context cxt = new InitialContext();
            wsContext = (WebServiceContext) cxt.lookup("java:comp/env/ws/context");
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return wsContext == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMessageContextNull() {
        WebServiceContext wsContext = null;
        try {
            Context cxt = new InitialContext();
            wsContext = (WebServiceContext) cxt.lookup("java:comp/env/ws/context");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        MessageContext msgContext = null;
        if (null != wsContext) {
            msgContext = wsContext.getMessageContext();
        }
        return msgContext == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isServletContextNull() {
        WebServiceContext wsContext = null;
        try {
            Context cxt = new InitialContext();
            wsContext = (WebServiceContext) cxt.lookup("java:comp/env/ws/context");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        ServletContext servletContext = null;
        if (null != wsContext) {
            servletContext = (ServletContext) wsContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        }
        return servletContext == null;
    }

    /** {@inheritDoc} */
    @Override
    public String getServletContextParameter() {
        WebServiceContext wsContext = null;
        try {
            Context cxt = new InitialContext();
            wsContext = (WebServiceContext) cxt.lookup("java:comp/env/ws/context");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        String testValue = null;
        if (null != wsContext) {
            ServletContext servletContext = (ServletContext) wsContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
            if (null != servletContext) {
                testValue = servletContext.getInitParameter(TEST_PARAMETER_NAME);
            }
        }
        return testValue;
    }

}
