/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata.builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature.Responses;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.AddressingFeatureInfo;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.MTOMFeatureInfo;
import com.ibm.ws.jaxws.metadata.RespectBindingFeatureInfo;
import com.ibm.ws.jaxws.metadata.ServiceModeInfo;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;

/**
 * Scan the annotations and configure the EndpointInfo
 */
@Component(service = { EndpointInfoConfigurator.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = false, property = { "service.vendor=IBM" })
public class AnnotationEndpointInfoConfigurator extends AbstractEndpointInfoConfigurator {

    private static final TraceComponent tc = Tr.register(AnnotationEndpointInfoConfigurator.class);

    public AnnotationEndpointInfoConfigurator() {
        super(EndpointInfoConfigurator.Phase.PROCESS_ANNOTATION);
    }

    @Override
    public void prepare(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) {
        InfoStore infoStore = context.getInfoStore();
        ClassInfo implBeanClassInfo = infoStore.getDelayableClassInfo(endpointInfo.getImplBeanClassName());

        // just get the serviceEndpointInterface from annotation
        endpointInfo.setServiceEndpointInterface(JaxWsUtils.getSEIClassNameFromAnnotation(implBeanClassInfo));
    }

    @Override
    public void config(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) {
        InfoStore infoStore = context.getInfoStore();
        ClassInfo implBeanClassInfo = infoStore.getDelayableClassInfo(endpointInfo.getImplBeanClassName());

        String seiClassName = endpointInfo.getServiceEndpointInterface();

        //first set targetNamespace and interface targetNamespace
        endpointInfo.setTargetNamespaceURL(JaxWsUtils.getImplementedTargetNamespace(implBeanClassInfo));
        endpointInfo.setInterfaceTragetNameSpaceURL(JaxWsUtils.getInterfaceTargetNamespace(implBeanClassInfo, seiClassName, endpointInfo.getTargetNamespaceURL(), infoStore));

        endpointInfo.setWsdlService(JaxWsUtils.getServiceQName(implBeanClassInfo, seiClassName, endpointInfo.getTargetNamespaceURL()));
        endpointInfo.setWsdlPort(JaxWsUtils.getPortQName(implBeanClassInfo, seiClassName, endpointInfo.getTargetNamespaceURL()));
        endpointInfo.setPortComponentName(JaxWsUtils.getPortComponentName(implBeanClassInfo, seiClassName, infoStore));
        if (endpointInfo.getAddresses().length == 0) {
            endpointInfo.addAddress("/" + JaxWsUtils.getServiceName(implBeanClassInfo));
        }
        endpointInfo.setWsdlLocation(JaxWsUtils.getWSDLLocation(implBeanClassInfo, seiClassName, infoStore));

        configMTOMFeatureInfo(implBeanClassInfo, seiClassName, infoStore, endpointInfo);
        configAddressingFeatureInfo(implBeanClassInfo, seiClassName, infoStore, endpointInfo, context);
        configRespectBindingFeatureInfo(implBeanClassInfo, seiClassName, infoStore, endpointInfo);
        configProtocolBinding(implBeanClassInfo, seiClassName, infoStore, endpointInfo);
        configServiceMode(implBeanClassInfo, seiClassName, infoStore, endpointInfo);

        HandlerChainInfoBuilder handlerChainBuilder = new HandlerChainInfoBuilder(JaxWsUtils.getModuleInfo(context.getContainer()).getClassLoader());
        endpointInfo.setHandlerChainsInfo(handlerChainBuilder.buildHandlerChainsInfoFromAnnotation(implBeanClassInfo, seiClassName, infoStore, endpointInfo.getWsdlPort(),
                                                                                                   endpointInfo.getWsdlService(), endpointInfo.getProtocolBinding()));
    }

    private void configMTOMFeatureInfo(ClassInfo implClassInfo, String seiClassName, InfoStore infoStore, EndpointInfo endpointInfo) {
        AnnotationInfo anno = implClassInfo.getAnnotation(JaxWsConstants.MTOM_ANNOTATION_NAME);
        if (null == anno) {
            if (!StringUtils.isEmpty(seiClassName)) {
                anno = infoStore.getDelayableClassInfo(seiClassName).getAnnotation(JaxWsConstants.MTOM_ANNOTATION_NAME);
            }
            if (null == anno) {
                return;
            }
        }
        endpointInfo.setMTOMFeatureInfo(new MTOMFeatureInfo(anno.getBoolean("enabled"), anno.getValue("threshold").getInteger()));
    }

    private void configAddressingFeatureInfo(ClassInfo implClassInfo, String seiClassName, InfoStore infoStore, EndpointInfo endpointInfo, EndpointInfoBuilderContext ctx) {
        AnnotationInfo anno = implClassInfo.getAnnotation(JaxWsConstants.ADDRESSING_ANNOTATION_NAME);
        if (null == anno) {
            if (!StringUtils.isEmpty(seiClassName)) {
                anno = infoStore.getDelayableClassInfo(seiClassName).getAnnotation(JaxWsConstants.ADDRESSING_ANNOTATION_NAME);
            }
            if (null == anno) {
                // Addressing annotation takes precedence over WSDL's Addressing config
                // If no annotation, see if there's something in WSDL
                configAddressingFeatureInfoFromWsdl(implClassInfo, ctx, endpointInfo);
                return;
            }
        }
        endpointInfo.setAddressingFeatureInfo(new AddressingFeatureInfo(anno.getBoolean("enabled"), anno.getBoolean("required"),
                        anno.getValue("responses") == null ? "ALL" : anno.getValue("responses").getEnumValue()));
    }

    private void configAddressingFeatureInfoFromWsdl(ClassInfo implClassInfo, EndpointInfoBuilderContext ctx, EndpointInfo endpointInfo) {

        endpointInfo.getWsdlPort();
        String wsdlLocation = endpointInfo.getWsdlLocation();
        URL wsdlUrl = JaxWsUtils.resolve(wsdlLocation, ctx.getContainer());
        if (wsdlUrl != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WSDL URL = " + wsdlUrl);
            }
            InputStream wsdlInputStream = null;

            try {
                wsdlInputStream = wsdlUrl.openStream();

                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setFeature("http://xml.org/sax/features/namespaces", true);
                factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

                SAXParser parser = factory.newSAXParser();
                WsdlAddressingEnabledHandler handler = new WsdlAddressingEnabledHandler(endpointInfo, ctx);
                parser.parse(wsdlInputStream, handler);

                QName addrAttachPoint = (QName) ctx.getContextEnv("addressing.wsdl.enabled");
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Addressing policy attached to " + addrAttachPoint);
                    Tr.debug(tc, "EndpointInfo binding = " + endpointInfo.getWsdlBinding());
                    Tr.debug(tc, "EndpointInfo port = " + endpointInfo.getWsdlPort());
                }

                if (addrAttachPoint != null) {
                    if ((endpointInfo.getWsdlBinding() != null && endpointInfo.getWsdlBinding().equals(addrAttachPoint))
                        || (endpointInfo.getWsdlPort() != null && endpointInfo.getWsdlPort().equals(addrAttachPoint))) {
                        AddressingFeatureInfo addrFromWsdlInfo = new AddressingFeatureInfo();
                        addrFromWsdlInfo.setEnabled(true);
                        String optional = (String) ctx.getContextEnv("addressing.wsdl.optional");
                        if (optional != null && optional.equals("false")) {
                            // Optional="false" means pass true into AddressingFeatureInfo.setRequired()
                            // Default is false
                            addrFromWsdlInfo.setRequired(true);
                        }
                        Boolean anonResp = (Boolean) ctx.getContextEnv("addressing.wsdl.responses.anonymous");
                        if (anonResp != null) {
                            if (anonResp.booleanValue()) {
                                addrFromWsdlInfo.setResponses(Responses.ANONYMOUS);
                            } else {
                                addrFromWsdlInfo.setResponses(Responses.NON_ANONYMOUS);
                            }
                        } // default is Responses.ALL
                        endpointInfo.setAddressingFeatureInfo(addrFromWsdlInfo);
                    }
                }
            } catch (IOException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to read WSDL due to IOException: {0}", e);
                }
            } catch (ParserConfigurationException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to read WSDL due to ParserConfigurationException: {0}", e);
                }
            } catch (SAXException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to read WSDL due to SAXException: {0}", e);
                }
            } finally {
                if (wsdlInputStream != null) {
                    try {
                        wsdlInputStream.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No WSDL URL specified");
            }
        }
    }

    private void configRespectBindingFeatureInfo(ClassInfo implClassInfo, String seiClassName, InfoStore infoStore, EndpointInfo endpointInfo) {
        AnnotationInfo anno = implClassInfo.getAnnotation(JaxWsConstants.RESPECT_BINDING_ANNOTATION_NAME);
        if (null == anno) {
            if (!StringUtils.isEmpty(seiClassName)) {
                anno = infoStore.getDelayableClassInfo(seiClassName).getAnnotation(JaxWsConstants.RESPECT_BINDING_ANNOTATION_NAME);
            }
            if (null == anno) {
                return;
            }
        }
        endpointInfo.setRespectBindingFeatureInfo(new RespectBindingFeatureInfo(anno.getBoolean("enabled")));
    }

    private void configProtocolBinding(ClassInfo implClassInfo, String seiClassName, InfoStore infoStore, EndpointInfo endpointInfo) {
        AnnotationInfo anno = implClassInfo.getAnnotation(JaxWsConstants.BINDING_TYPE_ANNOTATION_NAME);
        if (null == anno) {
            if (!StringUtils.isEmpty(seiClassName)) {
                anno = infoStore.getDelayableClassInfo(seiClassName).getAnnotation(JaxWsConstants.BINDING_TYPE_ANNOTATION_NAME);
            }
            if (null == anno) {
                return;
            }
        }
        endpointInfo.setProtocolBinding(JaxWsUtils.getProtocolByToken(anno.getValue("value").getStringValue(), true));
    }

    private void configServiceMode(ClassInfo implClassInfo, String seiClassName, InfoStore infoStore, EndpointInfo endpointInfo) {
        AnnotationInfo anno = implClassInfo.getAnnotation(JaxWsConstants.SERVICE_MODE_ANNOTATION_NAME);
        if (null == anno) {
            if (!StringUtils.isEmpty(seiClassName)) {
                anno = infoStore.getDelayableClassInfo(seiClassName).getAnnotation(JaxWsConstants.SERVICE_MODE_ANNOTATION_NAME);
            }
            if (null == anno) {
                // If no ServiceMode annotation, by default to Service.Mode.PAYLOAD
                endpointInfo.setServiceModeInfo(new ServiceModeInfo(Service.Mode.PAYLOAD));
                return;
            }
        }
        endpointInfo.setServiceModeInfo(new ServiceModeInfo(anno.getValue("value").getEnumValue()));
    }
}

class WsdlAddressingEnabledHandler extends DefaultHandler {
    private static final TraceComponent tc = Tr.register(WsdlAddressingEnabledHandler.class);

    private final EndpointInfo endptInfo;
    private final EndpointInfoBuilderContext context;
    private String bindingName;
    private String portName;
    private final HashMap<String, String> preNsMapping = new HashMap<String, String>();

    public WsdlAddressingEnabledHandler(EndpointInfo endptInfo, EndpointInfoBuilderContext context) {
        this.endptInfo = endptInfo;
        this.context = context;
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "prefix = " + prefix + " uri = " + uri);
        }
        // should be synchronized?
        preNsMapping.put(prefix, uri);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        // This isn't getting the fully qualified name, only the local part
        if (uri.equals("http://schemas.xmlsoap.org/wsdl/") && localName.equals("binding")) {
            bindingName = attributes.getValue("name");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "startElement for binding " + bindingName);
            }
        }

        if (uri.equals("http://schemas.xmlsoap.org/wsdl/") && localName.equals("port")) {
            portName = attributes.getValue("name");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "startElement for port " + portName);
            }
            // We need to look for the binding that corresponds to the port set on 
            // EndpointInfo. There could be multiple bindings and ports in one WSDL
            // that have nothing to do with the EndpointInfo passed into this class's ctor
            if (this.endptInfo.getWsdlPort().getLocalPart().equals(portName)) {
                String bindingForPortName = attributes.getValue("binding");
                // If the name has a prefix, find the namespace URI for the prefix
                // that was--hopefully!--declared earlier in WSDL
                String bindingNs = "";
                if (bindingForPortName.indexOf(":") > 0) {
                    String bindingPrefix = bindingForPortName.substring(0, bindingForPortName.indexOf(":"));
                    bindingNs = preNsMapping.get(bindingPrefix);
                    bindingForPortName = bindingForPortName.substring(bindingForPortName.indexOf(":") + 1, bindingForPortName.length());
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Namespace URI for prefix " + bindingPrefix + " = " + bindingNs);
                        Tr.debug(tc, "Local name = " + bindingForPortName);
                    }
                }
                QName bQName = new QName(bindingNs, bindingForPortName);
                this.endptInfo.setWsdlBinding(bQName);
            }
        }

        // When the element ends, the name should've been null'ed out (see endElement()).
        // If it's non-null still and the Addressing element is there, then associate policy
        // with binding.
        if (bindingName != null) {
            if (uri.equals(JAXWSAConstants.NS_WSAM) && localName.equals("Addressing")) {
                // Binding name found from the wsdl:binding name attribute is only the local part
                // We have to assume that EndpointInfo.getTargetNamespaceURL() is the correct
                // namespace for the various WSDL elements (but don't know if this a safe assumption!)
                this.context.addContextEnv("addressing.wsdl.enabled", new QName(this.endptInfo.getTargetNamespaceURL(), bindingName));
                String optional = attributes.getValue("http://www.w3.org/ns/ws-policy", "Optional");
                this.context.addContextEnv("addressing.wsdl.optional", optional);
            }

            if (uri.equals(JAXWSAConstants.NS_WSAM) && localName.indexOf("AnonymousResponses") > -1) {
                if (localName.equals("AnonymousResponses")) {
                    this.context.addContextEnv("addressing.wsdl.responses.anonymous", Boolean.TRUE);
                } else if (localName.equals("NonAnonymousResponses")) {
                    this.context.addContextEnv("addressing.wsdl.responses.anonymous", Boolean.FALSE);
                }
            }
        }

        // Do the same with port
        if (portName != null) {
            if (uri.equals(JAXWSAConstants.NS_WSAM) && localName.equals("Addressing")) {
                // Port name found from the wsdl:port name attribute is only the local part
                // We have to assume that EndpointInfo.getTargetNamespaceURL() is the correct
                // namespace for the various WSDL elements
                this.context.addContextEnv("addressing.wsdl.enabled", new QName(this.endptInfo.getTargetNamespaceURL(), portName));
                String optional = attributes.getValue("http://www.w3.org/ns/ws-policy", "Optional");
                this.context.addContextEnv("addressing.wsdl.optional", optional);
            }

            if (uri.equals(JAXWSAConstants.NS_WSAM) && localName.indexOf("AnonymousResponses") > -1) {
                if (localName.equals("AnonymousResponses")) {
                    this.context.addContextEnv("addressing.wsdl.responses.anonymous", Boolean.TRUE);
                } else if (localName.equals("NonAnonymousResponses")) {
                    this.context.addContextEnv("addressing.wsdl.responses.anonymous", Boolean.FALSE);
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (uri.equals("http://schemas.xmlsoap.org/wsdl/") && localName.equals("binding")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "endElement for binding " + bindingName);
            }
            bindingName = null;
        }

        if (uri.equals("http://schemas.xmlsoap.org/wsdl/") && localName.equals("port")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "endElement for port " + portName);
            }
            portName = null;
        }
    }

    @Override
    public void endDocument() {}
}
