/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.javaee.common.internal;

import java.util.List;

import javax.xml.namespace.QName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.ws.PortComponent;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.HandlerChainInfo;
import com.ibm.ws.jaxws.metadata.HandlerChainsInfo;
import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.ParamValueInfo;
import com.ibm.ws.jaxws.metadata.XsdQNameInfo;
import com.ibm.ws.jaxws.utils.JaxWsUtils;

public class JaxWsHandlerInfoMergingHelper {

    private static final TraceComponent tc = Tr.register(JaxWsHandlerInfoMergingHelper.class);

    /**
     * Merge the handler chains from webservices.xml file with @HandChain
     * 
     * @param webServices
     * @param endpointInfo
     * @throws Exception
     */
    public static void mergeHandlerChains(PortComponent portComp,
                                          EndpointInfo endpointInfo) {

        if (endpointInfo == null || portComp == null) {
            return;
        }

        HandlerChainsInfo hChainsInfo = new HandlerChainsInfo();

        List<HandlerChain> handlerChains = portComp.getHandlerChains();

        if (handlerChains == null || handlerChains.size() == 0) {
            return;
        }

        for (HandlerChain hChain : handlerChains) {

            HandlerChainInfo chainInfo = new HandlerChainInfo();

            // set portQName and serviceQName
            QName qName = convertDDQNameToQName(hChain.getPortNamePattern());
            if (qName != null) {
                // If endpointInfo's portQName could match the
                // handlerChain's portName, just ignore the followed
                // handlers
                if (!JaxWsUtils.matchesQName(qName, endpointInfo.getWsdlPort(),
                                             true)) {
                    continue;
                }
                chainInfo.setPortNamePattern(qName);
            }

            qName = convertDDQNameToQName(hChain.getServiceNamePattern());
            if (qName != null) {
                if (!JaxWsUtils.matchesQName(qName, endpointInfo
                                .getWsdlService(), true)) {
                    continue;
                }
                chainInfo.setServiceNamePattern(qName);
            }
            // set protocol bindings
            List<String> bindings = hChain.getProtocolBindings();
            boolean include = false;
            if (bindings != null) {
                for (String binding : bindings) {
                    if (JaxWsUtils.singleProtocolMatches(binding, endpointInfo
                                    .getProtocolBinding())) {
                        include |= true;
                    }
                    chainInfo.addProtocolBinding(JaxWsUtils.getProtocolByToken(binding));
                }
                // if the binding protocol is not support, just ignore the
                // handlers
                if (!include && !bindings.isEmpty()) {
                    continue;
                }
            }
            // set handlers
            for (Handler handler : hChain.getHandlers()) {

                HandlerInfo hInfo = new HandlerInfo();

                // handler-class
                String handlerClass = handler.getHandlerClassName();

                if (handlerClass == null || "".equals(handlerClass)) {
                    Tr.warning(tc, "warn.dd.invalid.handler", "<handler-class>", endpointInfo.getPortComponentName());
                    continue;
                }

                // handler-name
                String handlerName = handler.getHandlerName();

                if (handlerName == null || "".equals(handlerName)) {
                    Tr.warning(tc, "warn.dd.invalid.handler", "<handler-name>", endpointInfo.getPortComponentName());
                    continue;
                }

                hInfo.setHandlerClass(handlerClass);
                hInfo.setHandlerName(handlerName);

                // port-name
                List<String> portNames = handler.getPortNames();

                for (String portName : portNames) {
                    hInfo.addPortName(portName);
                }

                // soap-role
                List<String> soapRoles = handler.getSoapRoles();

                for (String soapRole : soapRoles) {
                    hInfo.addSoapRole(soapRole);
                }

                // soap-header
                List<com.ibm.ws.javaee.dd.common.QName> soapHeaders = handler
                                .getSoapHeaders();

                for (com.ibm.ws.javaee.dd.common.QName soapHeader : soapHeaders) {
                    hInfo.addSoapHeader(new XsdQNameInfo(new QName(
                                                                   soapHeader.getNamespaceURI(), soapHeader
                                                                                   .getLocalPart()), ""));// no id info...
                }

                // init-param(rpc)
                List<ParamValue> initParams = handler.getInitParams();

                for (ParamValue pv : initParams) {
                    hInfo.addInitParam(new ParamValueInfo(pv.getName(), pv
                                    .getValue()));
                }

                chainInfo.addHandlerInfo(hInfo);
            }

            hChainsInfo.addHandlerChainInfo(chainInfo);
        }

        if (!hChainsInfo.getHandlerChainInfos().isEmpty()) {
            /**
             * According to the behavior of tWAS, just replace the handlerChain
             * from @HandlerChain
             * 
             * site here: 582987 per JSR 109 6.2.2.3, handler chains specified
             * in the dd override handler chain annotations. We can not have
             * both HCannotations and HCtypes set in the dbc, so wipe out the
             * annotations here
             */
            endpointInfo.setHandlerChainsInfo(hChainsInfo);
        }
    }

    private static QName convertDDQNameToQName(com.ibm.ws.javaee.dd.common.QName dQName) {
        if (dQName == null) {
            return null;
        }

        String ns = dQName.getNamespaceURI();
        String localPart = dQName.getLocalPart();

        int index = localPart.indexOf(":");

        if (index > -1) {
            String prefix = localPart.substring(0, index);
            String localName = (index < localPart.length() - 1) ? localPart
                            .substring(index + 1) : "";
            return new QName(ns, localName, prefix);
        } else
            return new QName(ns, localPart);
    }
}
