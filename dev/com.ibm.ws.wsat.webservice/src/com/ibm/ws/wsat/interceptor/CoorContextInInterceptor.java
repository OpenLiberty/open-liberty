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
package com.ibm.ws.wsat.interceptor;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.policy.PolicyVerificationInInterceptor;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.globalhandler.GlobalHandlerInterceptor;
import com.ibm.ws.jaxws.wsat.Constants.AssertionStatus;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.utils.WSATOSGIService;
import com.ibm.ws.wsat.utils.WSCoorConstants;
import com.ibm.ws.wsat.utils.WSCoorUtil;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContext;

/**
 *
 */
public class CoorContextInInterceptor extends AbstractPhaseInterceptor<SoapMessage> implements SoapInterceptor {

    final TraceComponent tc = Tr.register(
                                          CoorContextInInterceptor.class, WSCoorConstants.TRACE_GROUP, null);

    private String ctxId = null;

    private static final Set<QName> HEADERS = new HashSet<QName>();

    static {
        HEADERS.add(new QName(WSCoorConstants.NAMESPACE_WSCOOR, WSCoorConstants.COORDINATION_CONTEXT_ELEMENT_STRING));
    }

    private AssertionStatus isOptional;

    /**
     * @param phase
     */
    public CoorContextInInterceptor(String phase) {
        super(phase);
        getAfter().add(PolicyVerificationInInterceptor.class.getName());
        getBefore().add(GlobalHandlerInterceptor.class.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        WSCoorUtil.assertAT(message);
        CoordinationContext cc = null;
        isOptional = WSCoorUtil.isOptional(message, true);
        List<Header> headers = message.getHeaders();

        if (headers != null && headers.size() > 0) {
            for (int i = 0; i < headers.size(); i++) {
                Header soapHeader = headers.get(i);

                if (soapHeader.getName().getLocalPart().equals(WSCoorConstants.COORDINATION_CONTEXT_ELEMENT_STRING)) {
                    try {
                        JAXBContext jc = JAXBContext.newInstance(CoordinationContext.class);
                        Unmarshaller unmarshaller = jc.createUnmarshaller();
                        // Extract SOAP Header
                        Element element = (Element) soapHeader.getObject();
                        // XMLUtils.printDOM(element);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(
                                     tc,
                                     "handleMessage",
                                     "Extract the CoordinationContext from soap header",
                                     XMLUtils.toString(element));
                        }

                        cc = (CoordinationContext) unmarshaller.unmarshal(element);
                        break;
                    } catch (JAXBException e) {
                        FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.CoorContextInInterceptor", "120");
                        throw new Fault(e);
                    }
                }
            }
        }

        if (cc == null) {
            if (isOptional == AssertionStatus.FALSE) {
                throw new Fault(new WSATException("CoordinationContext is NULL"));
            }
            return;
        } else {
            WSCoorUtil.checkHandlerServiceReady();
            if (message.getExchange().isOneWay()) {
                throw new Fault(new WSATException("WS-AT can not work on ONE-WAY webservice method"));
            }
            ctxId = cc.getIdentifier().getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "handleMessage",
                         "Get cxtId from Extracted CoordinationContext",
                         ctxId);
            }
            EndpointReferenceType epr = cc.getRegistrationService();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "handleMessage",
                         "Get Coordinator endpointRef from Extracted CoordinationContext",
                         epr);
            }

            try {
                //handleServerRequest will take care of calling reg service
                WSATOSGIService.getInstance().getHandlerService().handleServerRequest(ctxId, epr, cc.getExpires().getValue());
            } catch (WSATException e) {
                FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.CoorContextInInterceptor", "146");
                throw new Fault(e);
            }

            WSATCompleteTransInterceptor _comTransOut = new WSATCompleteTransInterceptor(Phase.POST_PROTOCOL_ENDING, LibertyApplicationBus.Type.SERVER);
            Exchange ex = message.getExchange();
            Endpoint ep = ex.get(Endpoint.class);
            ep.getOutInterceptors().add(_comTransOut);
        }

    }

    @Override
    public void handleFault(SoapMessage message) {
        WSCoorUtil.checkHandlerServiceReady();

        // TODO TJB: ctxId no longer needed here (and probably not available anyway)

        if (ctxId != null && !ctxId.equals("")) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(
                             tc,
                             "handleFault",
                             "Execute handleServerFault for transaction",
                             ctxId);
                }
                WSATOSGIService.getInstance().getHandlerService().handleServerFault();
            } catch (WSATException e) {
                FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.CoorContextInInterceptor", "185");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(
                         tc,
                         "handleFault",
                         "Cannot get transId, won't execute handleServerFault");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.binding.soap.interceptor.SoapInterceptor#getRoles()
     */
    @Override
    public Set<URI> getRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.binding.soap.interceptor.SoapInterceptor#getUnderstoodHeaders()
     */
    @Override
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

}
