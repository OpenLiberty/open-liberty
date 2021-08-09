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
package com.ibm.ws.jaxws.ejbwscontext;

import java.security.Principal;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * EchoInfo
 */
@Stateless
@WebService(serviceName = "EchoInfoService", portName = "EchoInfoPort", endpointInterface = "com.ibm.ws.jaxws.ejbwscontext.EchoInfoInterface",
            targetNamespace = "http://ejbwscontext.jaxws.ws.ibm.com")
public class EchoInfo implements EchoInfoInterface {

    @Resource
    WebServiceContext wsc;

    @Override
    public String getInfo(String action) {

        StringBuffer sb = new StringBuffer("[" + action + "]:");
        if ("MCKEYSIZE".equals(action)) {
            MessageContext mc = wsc.getMessageContext();

            if (mc != null) {
                sb.append("@OK: MessageContext key size=" + mc.keySet().size());
            } else {
                sb.append("@ERR: MessageContext is NULL");
            }

        } else if ("MCFIELDS".equals(action)) {
            MessageContext mc = wsc.getMessageContext();

            if (mc != null) {
                sb.append("HTTP_REQUEST_HEADERS:" + mc.get(MessageContext.HTTP_REQUEST_HEADERS) + "<br/>");
                sb.append("HTTP_REQUEST_METHOD:" + mc.get(MessageContext.HTTP_REQUEST_METHOD) + "<br/>");
                sb.append("HTTP_RESPONSE_CODE:" + mc.get(MessageContext.HTTP_RESPONSE_CODE) + "<br/>");
                sb.append("HTTP_RESPONSE_HEADERS:" + mc.get(MessageContext.HTTP_RESPONSE_HEADERS) + "<br/>");
                sb.append("INBOUND_MESSAGE_ATTACHMENTS:" + mc.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS) + "<br/>");
                sb.append("MESSAGE_OUTBOUND_PROPERTY:" + mc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) + "<br/>");
                sb.append("PATH_INFO:" + mc.get(MessageContext.PATH_INFO) + "<br/>");
                sb.append("QUERY_STRING:" + mc.get(MessageContext.QUERY_STRING) + "<br/>");
                sb.append("REFERENCE_PARAMETERS:" + mc.get(MessageContext.REFERENCE_PARAMETERS) + "<br/>");
                sb.append("SERVLET_CONTEXT:" + mc.get(MessageContext.SERVLET_CONTEXT) + "<br/>");
                sb.append("SERVLET_REQUEST:" + mc.get(MessageContext.SERVLET_REQUEST) + "<br/>");
                sb.append("SERVLET_RESPONSE:" + mc.get(MessageContext.SERVLET_RESPONSE) + "<br/>");
                sb.append("WSDL_DESCRIPTION:" + mc.get(MessageContext.WSDL_DESCRIPTION) + "<br/>");
                sb.append("WSDL_INTERFACE:" + mc.get(MessageContext.WSDL_INTERFACE) + "<br/>");
                sb.append("WSDL_OPERATION:" + mc.get(MessageContext.WSDL_OPERATION) + "<br/>");
                sb.append("WSDL_PORT:" + mc.get(MessageContext.WSDL_PORT) + "<br/>");
                sb.append("WSDL_SERVICE:" + mc.get(MessageContext.WSDL_SERVICE) + "<br/>");
                sb.append("@OK: Common Fields Loaded");

            } else {
                sb.append("@ERR: MessageContext is NULL");
            }
        } else if ("PRIN".equals(action)) {
            Principal prn = wsc.getUserPrincipal();

            if (prn != null) {
                sb.append("@OK: Principal user name=" + prn.getName());
            } else {
                sb.append("@ERR: Principal is NULL");
            }

        } else if ("ROLE".equals(action)) {
            if (wsc.isUserInRole("role_1")) {
                sb.append("@OK: role_1 allowed");
            } else {
                sb.append("@ERR: other roles");
            }
        }

        return sb.toString();
    }
}
