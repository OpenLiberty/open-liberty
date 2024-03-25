/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.wsat.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.PolicyComponent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.Constants.AssertionStatus;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.Protocol;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContext;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContextType.Identifier;
import com.ibm.ws.wsat.webservice.client.wscoor.Expires;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "com.ibm.ws.wsat.utils.CommonService",
           immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class WSCoorUtil {
    private static final TraceComponent tc = Tr.register(WSCoorUtil.class, WSCoorConstants.TRACE_GROUP);

    private static final String PROTOCOLSERVICE_REFERENCE_NAME = "protocol";
    private static final AtomicServiceReference<Protocol> protocolService = new AtomicServiceReference<Protocol>(PROTOCOLSERVICE_REFERENCE_NAME);

    @Reference(name = PROTOCOLSERVICE_REFERENCE_NAME, service = Protocol.class)
    protected void setProtocolService(ServiceReference<Protocol> ref) {
        protocolService.setReference(ref);
    }

    protected void unsetProtocolService(ServiceReference<Protocol> ref) {
        protocolService.unsetReference(ref);
    }

    private static final String WSATCONFIGSERVICE_REFERENCE_NAME = "config";
    private static final AtomicServiceReference<WSATConfigService> configService = new AtomicServiceReference<WSATConfigService>(WSATCONFIGSERVICE_REFERENCE_NAME);

    @Reference(name = WSATCONFIGSERVICE_REFERENCE_NAME, service = WSATConfigService.class)
    protected void setConfigService(ServiceReference<WSATConfigService> ref) {
        configService.setReference(ref);
    }

    protected void unsetConfigService(ServiceReference<WSATConfigService> ref) {
        configService.unsetReference(ref);
    }

    private static final String WSATHANDLERSERVICE_REFERENCE_NAME = "handler";
    private static final AtomicServiceReference<Handler> handlerService = new AtomicServiceReference<Handler>(WSATHANDLERSERVICE_REFERENCE_NAME);

    @Reference(name = WSATHANDLERSERVICE_REFERENCE_NAME, service = Handler.class)
    protected void setHandlerService(ServiceReference<Handler> ref) {
        handlerService.setReference(ref);
    }

    protected void unsetHandlerService(ServiceReference<Handler> ref) {
        handlerService.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        handlerService.activate(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Handler service: ", handlerService.getService());
        configService.activate(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Config service: ", configService.getService());
        protocolService.activate(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Protocol service: ", protocolService.getService());
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        handlerService.deactivate(cc);
        configService.deactivate(cc);
        protocolService.deactivate(cc);
    }

    public static void checkHandlerServiceReady() {
        if (handlerService.getService() == null) {
            final RuntimeException re = new RuntimeException("Handler service is not ready");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Handler service is not ready", re);
            throw re;
        }
    }

    @Trivial
    public static WSATConfigService getConfigService() {
        return configService.getService();
    }

    @Trivial
    public static Protocol getProtocolService() {
        return protocolService.getService();
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
            // This may be null if invoking a SOAP Provider WebService acting as a gateway
            if (boi == null)
                boi = getBindingOperationForInvoke(ex);
            if (boi != null)
                ep = pe.getEffectiveServerRequestPolicy(ei, boi, msg);
            else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isOptional", "Could not determine BindingOperationInfo");
        } else {
            Conduit conduit = ex.getConduit(msg);
            BindingOperationInfo boi = ex.getBindingOperationInfo();
            ep = pe.getEffectiveClientRequestPolicy(ei, boi, conduit, msg);
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

    private static BindingOperationInfo getBindingOperationForInvoke(Exchange ex) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getBindingOperationForInvoke", ex != null);

        BindingOperationInfo boi = ServiceModelUtil.getOperationForWrapperElement(ex, new QName("http://cxf.apache.org/jaxws/provider", "invoke"), false);
        if (boi == null)
            boi = ServiceModelUtil.getOperation(ex, "invoke");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getBindingOperationForInvoke", boi);
        return boi;
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
            Collection<AssertionInfo> ais = aim.get(WSCoorConstants.AT_ASSERTION_QNAME);
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

    @Trivial
    public static Handler getHandlerService() {
        return handlerService.getService();
    }
}
