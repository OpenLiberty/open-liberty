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
package com.ibm.ws.jaxws.metadata.builder;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.jaxws.javaee.CString;
import org.apache.cxf.jaxws.javaee.ParamValueType;
import org.apache.cxf.jaxws.javaee.PortComponentHandlerType;
import org.apache.cxf.jaxws.javaee.XsdQNameType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.HandlerChainAnnotationSer;
import com.ibm.ws.jaxws.metadata.HandlerChainInfo;
import com.ibm.ws.jaxws.metadata.HandlerChainsInfo;
import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.ParamValueInfo;
import com.ibm.ws.jaxws.metadata.XsdQNameInfo;
import com.ibm.ws.jaxws.utils.JAXBUtils;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;

/**
 * Extends the cxf AnnotationHandlerChainBuilder
 */
public class HandlerChainInfoBuilder {

    private static final TraceComponent tc = Tr.register(HandlerChainInfoBuilder.class);

    private static JAXBContext context;

    static {
        try {
            context = JAXBUtils.newInstance(PortComponentHandlerType.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final ClassLoader classLoader;

    public HandlerChainInfoBuilder(ClassLoader classLoader) {

        this.classLoader = classLoader;
    }

    /**
     * Build the handlerChain info from web.xml
     * 
     * @param hChain
     * @return
     */
    public static HandlerChainInfo buildHandlerChainInfoFromXML(HandlerChain hChain) {
        HandlerChainInfo hcInfo = new HandlerChainInfo();
        // set Service QName
        if (hChain.getServiceNamePattern() != null) {
            hcInfo.setServiceNamePattern(new QName(hChain.getServiceNamePattern().getNamespaceURI(),
                            hChain.getServiceNamePattern().getLocalPart()));
        } else {
            hcInfo.setServiceNamePattern(new QName("*"));
        }
        // set Port QName
        if (hChain.getPortNamePattern() != null) {

            hcInfo.setPortNamePattern(new QName(hChain.getPortNamePattern().getNamespaceURI(),
                            hChain.getPortNamePattern().getLocalPart()));
        } else {
            hcInfo.setPortNamePattern(new QName("*"));
        }
        // add protocol bindings
        hcInfo.addProtocolBindings(hChain.getProtocolBindings());

        for (com.ibm.ws.javaee.dd.common.wsclient.Handler handler : hChain.getHandlers()) {
            hcInfo.addHandlerInfo(buildHandlerInfoFromXML(handler));
        }
        return hcInfo;
    }

    /**
     * Build the handler info from web.xml
     * 
     * @param handler
     * @return
     */
    public static HandlerInfo buildHandlerInfoFromXML(com.ibm.ws.javaee.dd.common.wsclient.Handler handler) {
        HandlerInfo hInfo = new HandlerInfo();

        hInfo.setHandlerClass(handler.getHandlerClassName());
        hInfo.setHandlerName(handler.getHandlerName());

        for (ParamValue pv : handler.getInitParams()) {
            hInfo.addInitParam(new ParamValueInfo(pv.getName(), pv.getValue()));
        }
        for (String soapRole : handler.getSoapRoles()) {
            hInfo.addSoapRole(soapRole);
        }
        for (com.ibm.ws.javaee.dd.common.QName header : handler.getSoapHeaders()) {
            hInfo.addSoapHeader(new XsdQNameInfo(new QName(header.getNamespaceURI(), header.getLocalPart()), ""));
        }

        return hInfo;
    }

    /**
     * Get the handlerChainsInfo from @HandlerChain
     * 
     * @param serviceClazzName
     * @param hcSer
     * @param portQName
     * @param serviceQName
     * @param bindingID
     * @return
     */
    public HandlerChainsInfo buildHandlerChainsInfoFromAnnotation(String serviceClazzName, HandlerChainAnnotationSer hcSer,
                                                                  QName portQName, QName serviceQName, String bindingID) {
        HandlerChainsInfo chainsInfo = new HandlerChainsInfo();

        validateAnnotation(hcSer.getFile(), serviceClazzName);
        processHandlerChainAnnotation(chainsInfo, serviceClazzName, hcSer.getFile(), portQName, serviceQName, bindingID);

        return chainsInfo;
    }

    /**
     * Get the handlerChainsInfo from @HandlerChain
     * 
     * @param clz
     * @param portQName
     * @param serviceQName
     * @param bindingID
     * @return
     */
    public HandlerChainsInfo buildHandlerChainsInfoFromAnnotation(ClassInfo clzInfo, String seiClassName, InfoStore infoStore, QName portQName, QName serviceQName, String bindingID) {
        HandlerChainsInfo chainsInfo = new HandlerChainsInfo();

        HandlerChainAnnotation hcAnn = findHandlerChainAnnotation(clzInfo, seiClassName, infoStore, true);

        if (hcAnn != null) {
            hcAnn.validate();
            processHandlerChainAnnotation(chainsInfo, hcAnn.getDeclaringClass().getName(), hcAnn.getFileName(), portQName, serviceQName, bindingID);
        }

        return chainsInfo;
    }

    protected void processHandlerChainAnnotation(HandlerChainsInfo chainsInfo, String serviceClazzName, String fileName, QName portQName,
                                                 QName serviceQName, String bindingID) {
        try {

            URL handlerFileURL = resolveHandlerChainFileName(serviceClazzName, fileName);
            if (handlerFileURL == null) {
                throw new WebServiceException(Tr.formatMessage(tc, "error.no.handlerChainFile.found", fileName));
            }

            Document doc = XMLUtils.parse(handlerFileURL.openStream());
            Element el = doc.getDocumentElement();
            if (!"http://java.sun.com/xml/ns/javaee".equals(el.getNamespaceURI())
                || !"handler-chains".equals(el.getLocalName())) {

                String xml = XMLUtils.toString(el);
                throw new WebServiceException(Tr.formatMessage(tc, "error.invalid.handlerChainFile.content", xml));
            }

            Node node = el.getFirstChild();
            while (node != null) {
                if (node instanceof Element) {
                    el = (Element) node;
                    if (!el.getNamespaceURI().equals("http://java.sun.com/xml/ns/javaee")
                        || !el.getLocalName().equals("handler-chain")) {

                        String xml = XMLUtils.toString(el);
                        throw new WebServiceException(Tr.formatMessage(tc, "error.invalid.handlerChainFile.content", xml));
                    }
                    chainsInfo.addHandlerChainInfo(processHandlerChainElement(el, portQName, serviceQName, bindingID));
                }
                node = node.getNextSibling();
            }
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new WebServiceException(Tr.formatMessage(tc, "error.unknown.exception", e.getMessage()));
        }
    }

    protected HandlerChainInfo processHandlerChainElement(Element el, QName portQName, QName serviceQName, String bindingID) {
        HandlerChainInfo chainInfo = new HandlerChainInfo();

        Node node = el.getFirstChild();
        while (node != null) {
            Node cur = node;
            node = node.getNextSibling();
            QName elQName = null;
            if (cur instanceof Element) {
                el = (Element) cur;
                if (!el.getNamespaceURI().equals("http://java.sun.com/xml/ns/javaee")) {
                    String xml = XMLUtils.toString(el);
                    throw new WebServiceException(Tr.formatMessage(tc, "error.invalid.handlerChainFile.content", xml));
                }
                String name = el.getLocalName();
                if ("port-name-pattern".equals(name)) {
                    elQName = this.getQNameFromElement(el);
                    if (!JaxWsUtils.matchesQName(elQName, portQName, portQName.getPrefix().isEmpty())) {
                        return chainInfo;
                    }
                    chainInfo.setPortNamePattern(this.getQNameFromElement(el));
                } else if ("service-name-pattern".equals(name)) {
                    elQName = this.getQNameFromElement(el);
                    if (!JaxWsUtils.matchesQName(elQName, serviceQName, serviceQName.getPrefix().isEmpty())) {
                        return chainInfo;
                    }
                    chainInfo.setServiceNamePattern(this.getQNameFromElement(el));
                } else if ("protocol-bindings".equals(name)) {
                    // get the protocol-bindings content
                    String elementText = el.getTextContent().trim();
                    String[] bindings = elementText.split("\\s");

                    boolean include = false;
                    for (String binding : bindings) {
                        if (JaxWsUtils.singleProtocolMatches(binding, bindingID)) {
                            include |= true;
                        }
                        chainInfo.addProtocolBinding(JaxWsUtils.getProtocolByToken(binding));
                    }
                    if (!include && bindings.length != 0) {
                        return new HandlerChainInfo();
                    }
                } else if ("handler".equals(name)) {
                    HandlerInfo handlerInfo = processHandlerElement(el);
                    if (handlerInfo != null) {
                        chainInfo.addHandlerInfo(handlerInfo);
                    }
                }
            }
        }
        return chainInfo;
    }

    protected HandlerInfo processHandlerElement(Element el) {
        try {
            PortComponentHandlerType pt = context.createUnmarshaller()
                            .unmarshal(el, PortComponentHandlerType.class).getValue();
            return adaptToHandlerInfo(pt);
        } catch (JAXBException e) {
            // log the error info
        }
        return null;
    }

    private HandlerInfo adaptToHandlerInfo(PortComponentHandlerType pt) {
        HandlerInfo handler = new HandlerInfo();

        handler.setId(pt.getId());
        handler.setHandlerClass(pt.getHandlerClass().getValue());
        handler.setHandlerName(pt.getHandlerName().getValue());

        for (CString sRole : pt.getSoapRole()) {
            handler.addSoapRole(sRole.getValue());
        }
        for (XsdQNameType sHead : pt.getSoapHeader()) {
            handler.addSoapHeader(new XsdQNameInfo(sHead.getValue(), sHead.getId()));
        }
        for (ParamValueType param : pt.getInitParam()) {
            handler.addInitParam(new ParamValueInfo(param.getParamName().getValue(), param.getParamValue().getValue()));
        }
        return handler;
    }

    protected HandlerChainAnnotation findHandlerChainAnnotation(ClassInfo clzInfo, String seiClassName, InfoStore infoStore, boolean searchSEI) {
        if (clzInfo == null) {
            return null;
        }

        HandlerChainAnnotation hcAnn = null;
        AnnotationInfo ann = clzInfo.getAnnotation(JaxWsConstants.HANDLER_CHAIN_ANNOTATION_NAME);
        if (ann == null) {
            if (searchSEI) {
                /*
                 * HandlerChain annotation can be specified on the SEI
                 * but the implementation bean might not implement the SEI.
                 */
                AnnotationInfo ws = clzInfo.getAnnotation(JaxWsConstants.WEB_SERVICE_ANNOTATION_NAME);
                boolean noSEI = false;
                if (seiClassName == null || seiClassName.isEmpty()) {
                    if (ws == null || StringUtils.isEmpty(ws.getValue("endpointInterface").getStringValue())) {
                        noSEI = true;
                    } else {
                        seiClassName = ws.getValue("endpointInterface").getStringValue().trim();
                    }
                }
                if (!noSEI) {
                    ClassInfo seiClass = infoStore.getDelayableClassInfo(seiClassName);

                    // check SEI class and its interfaces for HandlerChain annotation
                    hcAnn = findHandlerChainAnnotation(seiClass, null, infoStore, false);
                }
            }
            if (hcAnn == null) {
                // check interfaces for HandlerChain annotation
                for (ClassInfo iface : clzInfo.getInterfaces()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Checking for HandlerChain annotation on {0}", iface.getName());
                    }
                    ann = iface.getAnnotation(JaxWsConstants.HANDLER_CHAIN_ANNOTATION_NAME);
                    if (ann != null) {
                        hcAnn = new HandlerChainAnnotation(ann, iface);
                        break;
                    }
                }
                if (hcAnn == null) {
                    hcAnn = findHandlerChainAnnotation(clzInfo.getSuperclass(), null, infoStore, false);
                }
            }
        } else {
            hcAnn = new HandlerChainAnnotation(ann, clzInfo);
        }

        return hcAnn;
    }

    /**
     * Resolve handler chain configuration file associated with the given class
     * 
     * @param clzName
     * @param fileName
     * @return A URL object or null if no resource with this name is found
     */
    protected URL resolveHandlerChainFileName(String clzName, String fileName) {
        URL handlerFile = null;
        InputStream in = null;

        String handlerChainFileName = fileName;
        URL baseUrl = classLoader.getResource(getClassResourceName(clzName));

        try {
            //if the filename start with '/', then find and return the resource under the web application home directory directory.
            if (handlerChainFileName.charAt(0) == '/') {
                return classLoader.getResource(handlerChainFileName.substring(1));
            }

            //otherwise, create a new url instance according to the baseurl and the fileName
            handlerFile = new URL(baseUrl, handlerChainFileName);
            in = handlerFile.openStream();
        } catch (Exception e) {
            // log the error msg
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return handlerFile;
    }

    private String getClassResourceName(String clzName) {
        int index = clzName.lastIndexOf('.');
        String classResourceName = ".";
        if (index != -1) {
            // APAR PI08401:
            //
            // Previously the logic was to get java package name from class name,
            // and then search the package name as resource. It has been found that
            // the same java package could exist in more than one jar achieves, in 
            // such case it could found the wrong path. 
            // The new logic is to search where the java class file is. Since we 
            // just want to locate the handler chain configuration file from there. 
            //
            //classResourceName = clzName.substring(0, index + 1).replace('.', '/');
            classResourceName = clzName.replace('.', '/');
            classResourceName = classResourceName + ".class";
        }

        return classResourceName;
    }

    protected QName getQNameFromElement(Element el) {
        if (el == null) {
            return null;
        }

        String elementText = el.getTextContent().trim();

        int colonIndex = elementText.indexOf(':');
        String pfx = "";

        if (colonIndex == -1) {
            return new QName("", elementText, pfx);
        }

        String localPart = elementText.substring(colonIndex + 1);
        pfx = elementText.substring(0, colonIndex);
        String ns = el.lookupNamespaceURI(pfx);
        if (ns == null) {
            ns = el.lookupNamespaceURI(null);
        }

        return new QName(ns, localPart, pfx);

    }

    /**
     * sorts the handlers into correct order. All of the logical handlers first
     * followed by the protocol handlers
     * 
     * @param handlers
     * @return sorted list of handlers
     */
    @SuppressWarnings("rawtypes")
    public static List<Handler> sortHandlers(List<Handler> handlers) {

        List<LogicalHandler<?>> logicalHandlers = new ArrayList<LogicalHandler<?>>();
        List<Handler<?>> protocolHandlers = new ArrayList<Handler<?>>();

        for (Handler<?> handler : handlers) {
            if (handler instanceof LogicalHandler) {
                logicalHandlers.add((LogicalHandler<?>) handler);
            } else {
                protocolHandlers.add(handler);
            }
        }

        List<Handler> sortedHandlers = new ArrayList<Handler>(logicalHandlers.size() + protocolHandlers.size());
        sortedHandlers.addAll(logicalHandlers);
        sortedHandlers.addAll(protocolHandlers);
        return sortedHandlers;
    }

    static class HandlerChainAnnotation {
        private final ClassInfo declaringClzInfo;
        private final AnnotationInfo ann;
        private final String fileName;

        HandlerChainAnnotation(AnnotationInfo hc, ClassInfo clzInfo) {
            ann = hc;
            declaringClzInfo = clzInfo;
            fileName = ann.getValue("file") == null ? null : ann.getValue("file").getStringValue();
        }

        public ClassInfo getDeclaringClass() {
            return declaringClzInfo;
        }

        public String getFileName() {
            return fileName;
        }

        public void validate() {
            validateAnnotation(fileName, declaringClzInfo.getName());
        }

        @Override
        public String toString() {
            return "[" + declaringClzInfo + "," + ann + "]";
        }
    }

    private static void validateAnnotation(String fileName, String className) {
        if (null == fileName || "".equals(fileName)) {
            throw new WebServiceException(Tr.formatMessage(tc, "error.handlerChain.annotation.without.file", className));
        }
    }
}
