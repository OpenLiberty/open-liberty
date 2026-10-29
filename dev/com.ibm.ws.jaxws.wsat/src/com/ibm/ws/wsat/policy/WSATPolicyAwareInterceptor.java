/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyInInterceptor;
import org.apache.cxf.ws.policy.PolicyOutInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.components.WSATFeatureService;
import com.ibm.ws.jaxws.wsat.components.WSATInterceptorService;

/**
 *
 */
public class WSATPolicyAwareInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(WSATPolicyAwareInterceptor.class, Constants.TRACE_GROUP, null);

    private final boolean isOut;

    public WSATPolicyAwareInterceptor(String phase, boolean isOut) {
        super(phase);
        this.isOut = isOut;
        // HANDLE feature not exist scenario, the soap header can only be checked at this phase
        if (phase.equals(Phase.PRE_PROTOCOL))
            getBefore().add(MustUnderstandInterceptor.class.getName());
        else if (phase.equals(Phase.WRITE)) {
            getBefore().add(SoapOutInterceptor.class.getName());
            // HANDLE feature exist scenario, if feature exists, we need insert interceptors in this early stage.
        } else if (phase.equals(Phase.SETUP)) {
            getAfter().add(PolicyOutInterceptor.class.getName());
        } else {
            getAfter().add(PolicyInInterceptor.class.getName());
        }
    }

    @Trivial
    private WSATInterceptorService getService() {
        BundleContext context = FrameworkUtil.getBundle(WSATInterceptorService.class).getBundleContext();
        WSATInterceptorService ret;
        if (context != null) {
            ServiceReference<WSATInterceptorService> serviceRef = context.getServiceReference(WSATInterceptorService.class);
            if (serviceRef != null)
                ret = context.getService(serviceRef);
            else
                ret = null;
        } else {
            ret = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "getService {0}",
                     ret);
        return ret;
    }

    private boolean assertAssertion(Message message) {
        boolean assertion_exist = false;
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "handleMessage",
                     "Checking if there's any ATAssertion present in the AssertionInfoMap",
                     aim);
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(Constants.AT_ASSERTION_QNAME);
            if (ais != null) {
                for (AssertionInfo a : ais) {
                    a.setAsserted(true);
                    assertion_exist = true;
                }
            }
        }
        return assertion_exist;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        // JAX-WS application requests, inbound and outbound, only care about the public
        // wsAtomicTransaction-1.2 feature. With only the private internal features (used for WS-AT
        // protocol traffic, e.g. by T-Rex) application requests must behave exactly as if WS-AT were
        // not installed, i.e. take the feature_no_exist path.
        boolean feature_exist = WSATFeatureService.isPublicFeaturePresent();
        //We need assert the assertion anyway...
        boolean assertionExist = assertAssertion(message);
        boolean internal_only = !feature_exist && WSATFeatureService.isWSATPresent();
        if (internal_only && isWSATProtocolMessage(message)) {
            handleMessage_internal_only_feature_exist(message);
        } else if (!feature_exist && ((getPhase().equals(Phase.PRE_PROTOCOL) && !isOut) || (getPhase().equals(Phase.WRITE)) && isOut)) {
            handleMessage_feature_no_exist(message, assertionExist);
        } else if (feature_exist && ((getPhase().equals(Phase.RECEIVE) && !isOut) || (getPhase().equals(Phase.SETUP)) && isOut)) {
            //Due to priority is transaction first, we don't need really check assertion here if WS-AT feature presents..
            handleMessage_feature_exist(message);
        }

    }

    /**
     * @return true if the message is part of WS-AT protocol traffic (a request to, or a response from,
     *         the WS-AT protocol services under {@link Constants#WSAT_APPLICATION_NAME}, or a request sent to them).
     */
    private boolean isWSATProtocolMessage(Message message) {
        String path = (String) message.get(Message.PATH_INFO);
        Exchange ex = message.getExchange();
        if (path == null && ex != null && ex.getInMessage() != null) {
            // Server response: the request path is only on the inbound message
            path = (String) ex.getInMessage().get(Message.PATH_INFO);
        }
        if (path == null) {
            // Client request
            path = (String) message.get(Message.ENDPOINT_ADDRESS);
        }
        return path != null && path.contains(Constants.WSAT_APPLICATION_NAME);
    }

    /**
     * Only the internal WS-AT features are installed, and this is WS-AT protocol traffic: let it through.
     * Inbound protocol requests are still subject to the SSL client certificate check.
     */
    private void handleMessage_internal_only_feature_exist(Message message) {
        if (isOut || !getPhase().equals(Phase.RECEIVE)) {
            return;
        }

        WSATInterceptorService service = getService();
        if (service == null)
            throw new Fault(new RuntimeException("WS-AT Feature is present, however there is no interceptor service available"));

        Exchange ex = message.getExchange();
        if (ex == null) {
            throw new Fault(new RuntimeException("Not able to get exchange from message"));
        }

        LibertyApplicationBus.Type busType = ex.getBus().getExtension(LibertyApplicationBus.Type.class);
        if (busType != null && busType.equals(LibertyApplicationBus.Type.SERVER)) {
            message.getInterceptorChain().add(service.getSSLServerInterceptor());
        }
    }

    /**
     * @param message
     */
    private void handleMessage_feature_exist(Message message) {
        WSATInterceptorService service = getService();

        if (service == null)
            throw new Fault(new RuntimeException("WS-AT Feature is present, however there is no interceptor service available"));

        Exchange ex = message.getExchange();
        if (ex == null) {
            throw new Fault(new RuntimeException("Not able to get exchange from message"));
        }

        LibertyApplicationBus.Type busType = ex.getBus().getExtension(LibertyApplicationBus.Type.class);
        if (busType == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WSATPolicyAwareInterceptor", "busType is NULL");
            }
            return;
        }
        if (isOut && busType.equals(LibertyApplicationBus.Type.CLIENT)) {
            //NOTE: message.getInterceptorChain() is the current interceptor list.
            //If we are in the middle of the interceptor process, the list will NOT be refreshed again from ex.getEndpoint().getOutInterceptors()
            //So message.getInterceptorChain() is the correct and only way to modify it.

            message.getInterceptorChain().add(service.getCoorContextOutInterceptor());
        } else if (!isOut && busType.equals(LibertyApplicationBus.Type.SERVER)) {
            String requrl = (String) message.get(Message.PATH_INFO);
            if (requrl != null && requrl.contains(Constants.WSAT_APPLICATION_NAME))
                message.getInterceptorChain().add(service.getSSLServerInterceptor());
            else
                message.getInterceptorChain().add(service.getCoorContextInInterceptor());
        }
    }

    /**
     * @param message
     */
    private void handleMessage_feature_no_exist(Message message, boolean assertion_exist) {

        if (isOut) {
            if (assertion_exist) {
                throw new Fault(new RuntimeException("WS-AT Feature is not installed"));
            } else {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null
                    && xtw.toString().contains(Constants.NAMESPACE_WSAT)) {
                    throw new Fault(new RuntimeException("WS-AT Feature is not installed"));
                }
            }
        }

        //Fix defect 178773, if the message is not instanceof SoapMessage
        // java.lang.ClassCastException: org.apache.cxf.message.XMLMessage incompatible with org.apache.cxf.binding.soap.SoapMessage
        @SuppressWarnings("unchecked")
        ArrayList<Header> headers = (ArrayList<Header>) message.get(Constants.SOAP_HEADER_KEY);
        if (headers != null) {
            for (Header h : headers) {
                if (h.getObject() instanceof Element) {
                    Element headerElement = (Element) h.getObject();
                    String headerURI = headerElement.getNamespaceURI();
                    if (headerURI != null && headerURI.contains(Constants.NAMESPACE_WSCOOR)) {
                        if (headerElement.getTextContent().contains(
                                                                    Constants.NAMESPACE_WSAT))
                            throw new Fault(new RuntimeException("WS-AT Feature is not installed"));
                    }
                }
            }
        }

    }

}
