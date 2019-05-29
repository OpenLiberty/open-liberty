/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.osgi.service.cm.ManagedServiceFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;

/**
 * Clean up HTTPTransportActivator.sorted & props via calling HTTPTransportActivator. deleted(String)
 * In CXF this is achieved via OSGI services(ManagedServiceFactory)
 * since in Liberty we do not expose it as osgi services
 * we need to add an interceptor in the end of client's OUT Flow
 * to get a chance to clean up via call deleted() method.
 */
public class LibertyCustomizeBindingOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final TraceComponent tc = Tr.register(LibertyCustomizeBindingOutEndingInterceptor.class);

    protected final WebServiceRefInfo wsrInfo;

    public LibertyCustomizeBindingOutEndingInterceptor(WebServiceRefInfo wsrInfo) {
        super(Phase.SEND);
        this.wsrInfo = wsrInfo;
    }

    @Override
    public void handleMessage(@Sensitive Message message) throws Fault {

        //if no wsrinfo, we will not override the port address & client prop
        if (wsrInfo != null) {
            cleanUp(message);
        }

    }

    private void cleanUp(Message message) {
        Conduit conduit = message.getExchange().getConduit(message);
        Bus bus = message.getExchange().getBus();
        if (null == bus) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The bus is null");
            }
            return;
        }
        HTTPConduitConfigurer conduitConfigurer = bus.getExtension(HTTPConduitConfigurer.class);

        if (conduitConfigurer != null && conduit != null && conduit instanceof HTTPConduit) {
            if (conduitConfigurer instanceof ManagedServiceFactory) {
                String portQNameStr = getPortQName(message).toString();
                if (portQNameStr != null) {
                    ((ManagedServiceFactory) conduitConfigurer).deleted(portQNameStr);
                }
            }
        }
    }

    private QName getPortQName(Message message) {
        Object wsdlPort = message.getExchange().get(Message.WSDL_PORT);
        if (null != wsdlPort && wsdlPort instanceof QName) {
            return (QName) wsdlPort;
        }
        return null;
    }
}
