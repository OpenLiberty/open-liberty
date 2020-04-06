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
package com.ibm.ws.wsat.utils;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.PolicyComponent;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.Constants.AssertionStatus;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContext;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContextType.Identifier;
import com.ibm.ws.wsat.webservice.client.wscoor.Expires;

/**
 *
 */
@Component(name = "com.ibm.ws.wsat.utils.CommonService", immediate = true, property = { "service.vendor=IBM" })
public class WSCoorUtil {
    private static final TraceComponent tc = Tr.register(WSCoorUtil.class, WSCoorConstants.TRACE_GROUP);

    public static void checkHandlerServiceReady() {
        if (WSATOSGIService.getInstance().getHandlerService() == null) {
            throw new RuntimeException("com.ibm.ws.wsat.common.Handler is null");
        }
    }

    public static String resolveHost() throws WSATException {
        String host = "";
        boolean isWSATSSLEnabled = WSATOSGIService.getInstance().getConfigService().isSSLEnabled();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "resolveHost",
                     "Checking if enable SSL for WS-AT",
                     isWSATSSLEnabled);
        host = WSATOSGIService.getInstance().getConfigService().getWSATUrl();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "resolveHost",
                     "Checking which url is using for WS-AT",
                     host);
        return host;
    }

    public static CoordinationContext createCoordinationContext(WSATContext ctx, EndpointReferenceType epr) {
        CoordinationContext cc = new CoordinationContext();
        Expires expires = new Expires();
        expires.setValue(ctx.getExpires());
        cc.setExpires(expires);
        cc.setCoordinationType(WSCoorConstants.NAMESPACE_WSAT);

        //Add transaction id to cc
        Identifier id = new Identifier();
        id.setValue(ctx.getId());
        cc.setIdentifier(id);
        cc.setRegistrationService(epr);

        return cc;
    }

    public static EndpointReferenceType createEpr(String hostname) throws SOAPException {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue(hostname);
        epr.setAddress(uri);
        ReferenceParametersType para = new ReferenceParametersType();
        epr.setReferenceParameters(para);
        return epr;
    }

    public static boolean assertAT(Message message) {
        boolean needFurtherCheck = true;
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = null;
        if (aim != null) {
            ais = aim.get(Constants.AT_ASSERTION_QNAME);
            if (null != ais && ais.size() != 0) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                    needFurtherCheck = false;
                }
            }
        }
        return needFurtherCheck;
    }

    /*
     * This assumes that the policy component is defined in its 'normalized form':
     * <wsp:ExactlyOne>
     * ( <wsp:All ( <Assertion...> ... </Assertion> )* </wsp:All> )*
     * </wsp:ExactlyOne>
     * 
     * We are only interested in checking for <wsat:ATTransaction> assertions
     * within the <wsp:All> wrapper. 'optional' is indicated by an empty
     * <wsp:All></wsp:All> element, in addition to the WSAT assertion.
     * We do not care any form other than /wsp:Policy/wsp:ExactlyOne/wsp:All here
     */

    private static AssertionStatus isOptionalAssertion(PolicyComponent pc) {
        AssertionStatus result = AssertionStatus.NULL;
        if (pc instanceof ExactlyOne) {
            List<PolicyComponent> components = ((ExactlyOne) pc).getPolicyComponents();
            if (components != null && !components.isEmpty()) {
                boolean wsatPolicy = false;
                boolean isOptional = false;
                for (PolicyComponent p : components) {
                    AssertionStatus result2 = isOptionalAssertion(p);
                    if (result2 == AssertionStatus.FALSE) {
                        wsatPolicy = true;
                    } else if (result2 == AssertionStatus.TRUE) {
                        isOptional = true;
                    }
                }
                //wsatPolicy TRUE, isOptional TRUE <=> OPTIONAL ASSERTION TRUE
                //wsatPolicy TRUE, isOptional FALSE <=> OPTIONAL ASSERTION FALSE
                //wsatPolicy FALSE, isOptional TRUE <=> OPTIONAL ASSERTION NULL
                //wsatPolicy FALSE, isOptional FALSE <=> OPTIONAL ASSERTION NULL
                if (wsatPolicy) {
                    result = (isOptional) ? AssertionStatus.TRUE : AssertionStatus.FALSE;
                }
            }
        } else if (pc instanceof All) {
            //we only expect
            List<PolicyComponent> assertions = ((All) pc).getAssertions();
            if (assertions == null || assertions.isEmpty()) {
                //<All></All> expression means AT_Assertion not existing can be accepted, return optional TRUE
                result = AssertionStatus.TRUE;
            } else {
                for (PolicyComponent p : assertions) {
                    if (p instanceof Assertion) {
                        //<All>AT_ASSERTION</All> expression means AT_Assertion existence can be accepted, return optional FALSE
                        if (((Assertion) p).getName().equals(Constants.AT_ASSERTION_QNAME)) {
                            result = AssertionStatus.FALSE;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    /*
     * Returns the status of the wsat:ATTransaction policy for the message being processed:
     * AssertionStatus.NULL - no policy present
     * AssertionStatus.TRUE - policy present but marked optional
     * AssertionStatus.FALSE - policy present and required
     */
    public static AssertionStatus isOptional(Message msg, boolean isServer) {
        AssertionStatus result = AssertionStatus.NULL;
        Exchange ex = msg.getExchange();
        Bus bus = ex.get(Bus.class);
        Endpoint e = ex.get(Endpoint.class);
        if (null == e) {
            return result;
        }
        EndpointInfo ei = e.getEndpointInfo();
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        EffectivePolicy ep = null;
        if (isServer) {
            setupBindingOperationInfo((SoapMessage) msg);
            BindingOperationInfo boi = ex.getBindingOperationInfo();
            ep = pe.getEffectiveServerRequestPolicy(ei, boi);
        } else {
            Conduit conduit = ex.getConduit(msg);
            BindingOperationInfo boi = ex.getBindingOperationInfo();
            ep = pe.getEffectiveClientRequestPolicy(ei, boi, conduit);
        }
        if (ep != null) {
            List<PolicyComponent> pList = ep.getPolicy().getPolicyComponents();
            //From here, only the normalized formed policy expression should be got.
            if (pList != null && !pList.isEmpty()) {
                for (PolicyComponent pc : pList) {
                    result = isOptionalAssertion(pc);
                    if (result != AssertionStatus.NULL)
                        //If we've found one certain value, return the result directly.
                        break;
                }

            }
        }

        return result;
    }

    public static AssertionInfo checkWSATAssertion(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(
                     tc,
                     "checkWSATAssertion",
                     "Checking if there's any ATAssertion present in the AssertionInfoMap",
                     aim);
        if (aim != null) {
            Collection<AssertionInfo> ais = aim
                            .get(WSCoorConstants.AT_ASSERTION_QNAME);
            if (ais != null) {
                for (AssertionInfo a : ais) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(
                                 tc,
                                 "checkWSATAssertion",
                                 "Get one ATAssertion in the AssertionInfoMap",
                                 a);
//                    a.setAsserted(true);
                    return a;
                }
            }
        }
        return null;
    }

    private static void setupBindingOperationInfo(SoapMessage message) {
        Exchange exch = message.getExchange();
        if (exch.get(BindingOperationInfo.class) == null) {
            //need to know the operation to determine if oneway
            QName opName = getOpQName(message);
            if (opName == null) {
                return;
            }
            BindingOperationInfo bop = ServiceModelUtil.getOperationForWrapperElement(exch, opName, false);
            if (bop == null) {
                bop = ServiceModelUtil.getOperation(exch, opName);
            }
            if (bop != null) {
                exch.put(BindingOperationInfo.class, bop);
                exch.put(OperationInfo.class, bop.getOperationInfo());
                if (bop.getOutput() == null) {
                    exch.setOneWay(true);
                }
            }
        }
    }

    private static QName getOpQName(SoapMessage message) {
        SOAPMessageContextImpl sm = new SOAPMessageContextImpl(message);
        try {
            SOAPMessage msg = sm.getMessage();
            if (msg == null) {
                return null;
            }
            SOAPBody body = SAAJUtils.getBody(msg);
            if (body == null) {
                return null;
            }
            org.w3c.dom.Node nd = body.getFirstChild();
            while (nd != null && !(nd instanceof org.w3c.dom.Element)) {
                nd = nd.getNextSibling();
            }
            if (nd != null) {
                return new QName(nd.getNamespaceURI(), nd.getLocalName());
            }
            //Fix for CTS Defect 174209
            Collection<BindingOperationInfo> boi = message.getExchange().getEndpoint().getEndpointInfo().getBinding().getOperations();
            if (boi.size() > 0) {
                return boi.iterator().next().getName();
            }
        } catch (SOAPException e) {
            //ignore, nothing we can do
        }
        return null;
    }
}
