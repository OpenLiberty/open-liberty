/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.injection;

import java.security.Principal;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Message;
import org.w3c.dom.Element;

public class WebServiceContextWrapper implements WebServiceContext {

    private WebServiceContext context = null;

    public WebServiceContextWrapper() {
        context = new WebServiceContextImpl();
    }

    /** {@inheritDoc} */
    @Override
    public EndpointReference getEndpointReference(Element... referenceParameters) {
        return context.getEndpointReference(referenceParameters);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz,
                                                                Element... referenceParameters) {
        return context.getEndpointReference(clazz, referenceParameters);
    }

    @Override
    public MessageContext getMessageContext() {

        WrappedMessageContext wmc = (WrappedMessageContext) context.getMessageContext();

        if (wmc != null) {
            Message msg = wmc.getWrappedMessage();
            SOAPMessageContextImpl smci = new SOAPMessageContextImpl(msg);
            return smci;
        } else {
            return wmc;
        }

    }

    /** {@inheritDoc} */
    @Override
    public Principal getUserPrincipal() {
        return context.getUserPrincipal();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserInRole(final String role) {
        return context.isUserInRole(role);
    }

}
