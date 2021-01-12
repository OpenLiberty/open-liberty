/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxws.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;
import javax.xml.ws.RespectBindingFeature;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.xml.XMLBinding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.binding.DefaultBindingImpl;
import org.apache.cxf.jaxws.binding.http.HTTPBindingImpl;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerFaultInInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerFaultOutInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerInInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerOutInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultInInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultOutInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.jaxws.interceptors.HolderInInterceptor;
import org.apache.cxf.jaxws.interceptors.HolderOutInterceptor;
import org.apache.cxf.jaxws.interceptors.MessageModeInInterceptor;
import org.apache.cxf.jaxws.interceptors.MessageModeOutInterceptor;
import org.apache.cxf.jaxws.interceptors.SwAInInterceptor;
import org.apache.cxf.jaxws.interceptors.SwAOutInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassInInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassOutInterceptor;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.wsdl.WSDLManager;


/**
 * A JAX-WS specific implementation of the CXF {@link org.apache.cxf.endpoint.Endpoint} interface.
 * Extends the interceptor provider functionality of its base class by adding
 * interceptors in which to execute the JAX-WS handlers.
 * Creates and owns an implementation of {@link Binding} in addition to the
 * CXF {@link org.apache.cxf.binding.Binding}.
 *
 */
public class JaxWsEndpointImpl extends EndpointImpl {

    private static final long serialVersionUID = 4718088821386100282L;
    private static final String URI_POLICY_NS = "http://www.w3.org/ns/ws-policy";
    private static final String URI_WSU_NS
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsEndpointImpl.class);

    private Binding jaxwsBinding;
    private JaxWsImplementorInfo implInfo;
    private List<WebServiceFeature> wsFeatures;
    private List<Feature> features;

    //interceptors added/removed to chains as needed
    private SOAPHandlerInterceptor soapHandlerInterceptor;
    private LogicalHandlerInInterceptor logicalInInterceptor;
    private LogicalHandlerOutInterceptor logicalOutInterceptor;
    private LogicalHandlerFaultOutInterceptor logicalFaultOutInterceptor;
    private SOAPHandlerFaultOutInterceptor soapFaultOutInterceptor;
    private LogicalHandlerFaultInInterceptor logicalFaultInInterceptor;
    private SOAPHandlerFaultInInterceptor soapFaultInInterceptor;
    private boolean handlerInterceptorsAdded;

    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei) throws EndpointException {
        this(bus, s, ei, null, null, null, true);
    }

    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei,
                             List<WebServiceFeature> wf) throws EndpointException {
        this(bus, s, ei, null, wf, new ArrayList<>(), true);
    }

    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei, JaxWsImplementorInfo implementorInfo,
                             List<WebServiceFeature> wf, List<? extends Feature> af, boolean isFromWsdl)
        throws EndpointException {
        super(bus, s, ei);
        this.implInfo = implementorInfo;
        this.wsFeatures = new ArrayList<>();
        if (af != null) {
            features = CastUtils.cast(af);
        } else {
            features = new ArrayList<>();
        }
        if (wf != null) {
            for (WebServiceFeature f : wf) {
                if (f instanceof Feature) {
                    features.add((Feature)f);
                } else {
                    wsFeatures.add(f);
                }
            }
        }
        createJaxwsBinding();

        List<Interceptor<? extends Message>> in = super.getInInterceptors();
        List<Interceptor<? extends Message>> out = super.getOutInterceptors();

        boolean isProvider = implInfo != null && implInfo.isWebServiceProvider();
        Class<?> clazz = implInfo != null && isProvider ? implInfo.getProviderParameterType() : null;
        Mode mode = implInfo != null && isProvider ? implInfo.getServiceMode() : null;

        if (isProvider) {
            s.put(AbstractInDatabindingInterceptor.NO_VALIDATE_PARTS, Boolean.TRUE);
        }

        // Inbound chain
        logicalInInterceptor = new LogicalHandlerInInterceptor(jaxwsBinding);
        if (!isProvider) {
            in.add(new WrapperClassInInterceptor());
            in.add(new HolderInInterceptor());
        }
        if (getBinding() instanceof SoapBinding) {
            soapHandlerInterceptor = new SOAPHandlerInterceptor(jaxwsBinding);
            in.add(new SwAInInterceptor());
            getOutInterceptors().add(new SwAOutInterceptor());
            if (isProvider && mode == Mode.MESSAGE) {
                in.add(new SAAJInInterceptor());
            }
        }
        if (isProvider && mode == Mode.MESSAGE) {
            in.add(new MessageModeInInterceptor(clazz, getBinding().getBindingInfo().getName()));
        }

        // Outbound chain
        logicalOutInterceptor = new LogicalHandlerOutInterceptor(jaxwsBinding);
        if (!isProvider) {
            out.add(new WrapperClassOutInterceptor());
            out.add(new HolderOutInterceptor());
        }
        if (getBinding() instanceof SoapBinding && mode == Mode.MESSAGE) {
            SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
            out.add(saajOut);
            out.add(new MessageModeOutInterceptor(saajOut,
                                                  getBinding().getBindingInfo().getName()));
        } else if (isProvider) {
            out.add(new MessageModeOutInterceptor(clazz, getBinding().getBindingInfo().getName()));
        }

        logicalFaultOutInterceptor = new LogicalHandlerFaultOutInterceptor(jaxwsBinding);
        logicalFaultInInterceptor = new LogicalHandlerFaultInInterceptor(jaxwsBinding);

        if (getBinding() instanceof SoapBinding) {
            soapFaultOutInterceptor = new SOAPHandlerFaultOutInterceptor(jaxwsBinding);
            soapFaultInInterceptor = new SOAPHandlerFaultInInterceptor(jaxwsBinding);
        }

        if (ei != null) {
            if (!isFromWsdl) {
                buildWsdlExtensibilities(ei.getBinding());
            }
            extractWsdlExtensibilities(ei);
        }
        resolveFeatures();
    }

    private void extractWsdlExtensibilities(EndpointInfo endpoint) {
        List<ExtensibilityElement> portExtensors = getExtensors(endpoint);
        List<ExtensibilityElement> bindingExtensors = getExtensors(endpoint.getBinding());

        //check the extensions under <wsdl:binding>
        checkRespectBindingFeature(bindingExtensors);

        Collection<BindingOperationInfo> bindingOperations = endpoint.getBinding().getOperations();
        if (null != bindingOperations) {
            Iterator<BindingOperationInfo> iterator = bindingOperations.iterator();
            while (iterator.hasNext()) {
                BindingOperationInfo operationInfo = iterator.next();
                BindingMessageInfo inputInfo = operationInfo.getInput();
                BindingMessageInfo outputnfo = operationInfo.getOutput();
                Collection<BindingFaultInfo> faults = operationInfo.getFaults();

                //check the extensions under <wsdl:operation>
                checkRespectBindingFeature(getExtensors(operationInfo));
                //check the extensions under <wsdl:input>
                checkRespectBindingFeature(getExtensors(inputInfo));
                //check the extensions under <wsdl:output>
                checkRespectBindingFeature(getExtensors(outputnfo));
                if (null != faults) {
                    Iterator<BindingFaultInfo> faultIterator = faults.iterator();
                    while (faultIterator.hasNext()) {
                        BindingFaultInfo faultInfo = faultIterator.next();

                        //check the extensions under <wsdl:fault>
                        checkRespectBindingFeature(getExtensors(faultInfo));
                    }
                }

            }
        }


        if (hasUsingAddressing(bindingExtensors) || hasUsingAddressing(portExtensors)) {
            WSAddressingFeature feature = new WSAddressingFeature();
            if (addressingRequired(bindingExtensors)
                || addressingRequired(portExtensors)) {
                feature.setAddressingRequired(true);
            }
            addAddressingFeature(feature);
        }
        extractWsdlEprs(endpoint);
    }

    private List<ExtensibilityElement> getExtensors(Extensible extensibleInfo) {
        return (null != extensibleInfo) ? extensibleInfo.getExtensors(ExtensibilityElement.class) : null;
    }

    private void checkRespectBindingFeature(List<ExtensibilityElement> bindingExtensors) {
        if (bindingExtensors != null) {
            Iterator<ExtensibilityElement> extensionElements = bindingExtensors.iterator();
            while (extensionElements.hasNext()) {
                ExtensibilityElement ext = extensionElements.next();
                if (ext instanceof UnknownExtensibilityElement && Boolean.TRUE.equals(ext.getRequired())) { // Liberty change: ""&& this.wsFeatures != null" statement is removed from if clause
                    for (WebServiceFeature feature : this.wsFeatures) {
                        if (feature instanceof RespectBindingFeature && feature.isEnabled()) {

                            org.apache.cxf.common.i18n.Message message =
                                new org.apache.cxf.common.i18n.Message("UNKONWN_REQUIRED_WSDL_BINDING", LOG);
                            LOG.severe(message.toString());
                            throw new WebServiceException(message.toString());
                        }
                    }
                }
            }
        }

    }

    private void extractWsdlEprs(EndpointInfo endpoint) {
        //parse the EPR in wsdl
        List<ExtensibilityElement> portExtensors = endpoint.getExtensors(ExtensibilityElement.class);
        if (portExtensors != null) {
            Iterator<ExtensibilityElement> extensionElements = portExtensors.iterator();
            QName wsaEpr = new QName(Names.WSA_NAMESPACE_NAME, "EndpointReference");
            while (extensionElements.hasNext()) {
                ExtensibilityElement ext = extensionElements.next();
                if (ext instanceof UnknownExtensibilityElement && wsaEpr.equals(ext.getElementType())) {
                    DOMSource domSource = new DOMSource(((UnknownExtensibilityElement)ext).getElement());
                    W3CEndpointReference w3cEPR = new W3CEndpointReference(domSource);
                    EndpointReferenceType ref = ProviderImpl.convertToInternal(w3cEPR);
                    endpoint.getTarget().setMetadata(ref.getMetadata());
                    endpoint.getTarget().setReferenceParameters(ref.getReferenceParameters());
                    endpoint.getTarget().getOtherAttributes().putAll(ref.getOtherAttributes());
                }

            }
        }
    }

    private boolean hasUsingAddressing(List<ExtensibilityElement> exts) {
        boolean found = false;
        if (exts != null) {
            Iterator<ExtensibilityElement> extensionElements = exts.iterator();
            while (extensionElements.hasNext() && !found) {
                ExtensibilityElement ext =
                    extensionElements.next();
                found = JAXWSAConstants.WSAW_USINGADDRESSING_QNAME.equals(ext.getElementType());
            }
        }
        return found;
    }
    private boolean addressingRequired(List<ExtensibilityElement> exts) {
        boolean found = false;
        if (exts != null) {
            Iterator<ExtensibilityElement> extensionElements = exts.iterator();
            while (extensionElements.hasNext() && !found) {
                ExtensibilityElement ext =
                    extensionElements.next();
                if (JAXWSAConstants.WSAW_USINGADDRESSING_QNAME.equals(ext.getElementType())
                    && ext.getRequired() != null) {
                    return ext.getRequired();
                }
            }
        }
        return false;
    }

    private void buildWsdlExtensibilities(BindingInfo bindingInfo) {
        Addressing addressing = getAddressing();
        if (addressing != null) {
            ExtensionRegistry extensionRegistry
                = getBus().getExtension(WSDLManager.class).getExtensionRegistry();
            try {
                ExtensibilityElement el = extensionRegistry.createExtension(javax.wsdl.Binding.class,
                                                                            JAXWSAConstants.
                                                                            WSAW_USINGADDRESSING_QNAME);
                el.setRequired(addressing.required());
                bindingInfo.addExtensor(el);

                StringBuilder polRefId = new StringBuilder(bindingInfo.getName().getLocalPart());
                polRefId.append("_WSAM_Addressing_Policy");
                UnknownExtensibilityElement uel = new UnknownExtensibilityElement();

                W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                writer.writeStartElement("wsp", "PolicyReference", URI_POLICY_NS);
                writer.writeAttribute("URI", "#" + polRefId.toString());
                writer.writeEndElement();
                Element pr = writer.getDocument().getDocumentElement();
                uel.setElement(pr);
                uel.setElementType(DOMUtils.getElementQName(pr));
                bindingInfo.addExtensor(uel);

                writer = new W3CDOMStreamWriter();
                writer.writeStartElement("wsp", "Policy", URI_POLICY_NS);
                writer.writeAttribute("wsu", URI_WSU_NS,
                                      "Id", polRefId.toString());
                writer.writeStartElement("wsam", "Addressing", JAXWSAConstants.NS_WSAM);
                if (!addressing.required()) {
                    writer.writeAttribute("wsp", URI_POLICY_NS,
                                          "Optional", "true");
                }
                writer.writeStartElement("wsp", "Policy", URI_POLICY_NS);

                String s = getAddressingRequirement(addressing);
                if (s != null) {
                    writer.writeEmptyElement("wsam", s, JAXWSAConstants.NS_WSAM);
                }

                writer.writeEndElement();
                writer.writeEndElement();
                writer.writeEndElement();

                pr = writer.getDocument().getDocumentElement();

                uel = new UnknownExtensibilityElement();
                uel.setElement(pr);
                uel.setElementType(DOMUtils.getElementQName(pr));
                if (bindingInfo.getService().getDescription() == null) {
                    DescriptionInfo description = new DescriptionInfo();
                    description.setName(bindingInfo.getService().getName());
                    bindingInfo.getService().setDescription(description);
                }
                bindingInfo.getService().getDescription().addExtensor(uel);

            } catch (Exception e) {
                //ignore
                e.printStackTrace();
            }
        }
    }

    private String getAddressingRequirement(Addressing addressing) {
        try {
            Object o = Addressing.class.getMethod("responses").invoke(addressing);
            if (o != null) {
                String s = o.toString();
                if ("ANONYMOUS".equals(s)) {
                    return "AnonymousResponses";
                } else if ("NON_ANONYMOUS".equals(s)) {
                    return "NonAnonymousResponses";
                }
            }
        } catch (Throwable ex) {
            //ignore - probably JAX-WS 2.1
        }
        return null;
    }

    private Addressing getAddressing() {
        Class<?> serviceClass = implInfo.getImplementorClass();
        if (serviceClass != null) {
            Addressing ad = serviceClass.getAnnotation(Addressing.class);
            if (ad != null) {
                return ad;
            }
        }

        serviceClass = implInfo.getSEIClass();
        if (serviceClass != null) {
            Addressing ad = serviceClass.getAnnotation(Addressing.class);
            if (ad != null) {
                return ad;
            }
        }
        return null;
    }

    public Binding getJaxwsBinding() {
        return jaxwsBinding;
    }

    private AddressingFeature getAddressingFeature() {
        if (wsFeatures == null) {
            return null;
        }
        for (WebServiceFeature feature : wsFeatures) {
            if (feature instanceof AddressingFeature) {
                return (AddressingFeature)feature;
            }
        }
        return null;
    }

    public final void resolveFeatures() {
        AddressingFeature addressing = getAddressingFeature();
        if (addressing == null) {
            return;
        }
        if (addressing.isEnabled()) {
            WSAddressingFeature feature = getWSAddressingFeature();
            if (feature == null) {
                feature = new WSAddressingFeature();
                addAddressingFeature(feature);
            }
            feature.setAddressingRequired(addressing.isRequired());
            feature.setResponses(addressing.getResponses().toString());
        } else {
            removeAddressingFeature();
            getEndpointInfo().setProperty("org.apache.cxf.ws.addressing.MAPAggregator.addressingDisabled",
                                          Boolean.TRUE);
        }
    }

    public List<Feature> getFeatures() {
        return features;
    }

    private WSAddressingFeature getWSAddressingFeature() {
        if (features == null) {
            return null;
        }
        for (Feature f : features) {
            if (f instanceof WSAddressingFeature) {
                return (WSAddressingFeature)f;
            }
        }
        return null;
    }

    private void addAddressingFeature(WSAddressingFeature a) {
        Feature f = getWSAddressingFeature();
        if (f == null) {
            features.add(a);
        }
    }

    private void removeAddressingFeature() {
        Feature f = getWSAddressingFeature();
        if (f != null) {
            features.remove(f);
        }
    }

    private MTOMFeature getMTOMFeature() {
        if (wsFeatures == null) {
            return null;
        }
        for (WebServiceFeature feature : wsFeatures) {
            if (feature instanceof MTOMFeature) {
                return (MTOMFeature)feature;
            }
        }
        return null;
    }

    final void createJaxwsBinding() {
        if (getBinding() instanceof SoapBinding) {
            jaxwsBinding = new SOAPBindingImpl(getEndpointInfo().getBinding(), this);
            MTOMFeature mtomFeature = getMTOMFeature();
            if (mtomFeature != null && mtomFeature.isEnabled()) {
                ((SOAPBinding)jaxwsBinding).setMTOMEnabled(true);
            }
        } else if (getBinding() instanceof XMLBinding) {
            jaxwsBinding = new HTTPBindingImpl(getEndpointInfo().getBinding(), this);
        } else {
            //REVISIT: Should not get here, though some bindings like JBI
            //did not implement their own Binding type.
            jaxwsBinding = new DefaultBindingImpl(this);
        }
    }

    public void addHandlerInterceptors() {
        if (handlerInterceptorsAdded) {
            return;
        }

        handlerInterceptorsAdded = true;

        List<Interceptor<? extends Message>> in = super.getInInterceptors();
        List<Interceptor<? extends Message>> out = super.getOutInterceptors();
        List<Interceptor<? extends Message>> outFault = super.getOutFaultInterceptors();
        List<Interceptor<? extends Message>> inFault = super.getInFaultInterceptors();

        in.add(logicalInInterceptor);
        out.add(logicalOutInterceptor);
        inFault.add(logicalFaultInInterceptor);
        outFault.add(logicalFaultOutInterceptor);
        if (soapHandlerInterceptor != null) {
            in.add(soapHandlerInterceptor);
            out.add(soapHandlerInterceptor);
        }
        if (soapFaultInInterceptor != null) {
            inFault.add(soapFaultInInterceptor);
        }
        if (soapFaultOutInterceptor != null) {
            outFault.add(soapFaultOutInterceptor);
        }
    }
    public void removeHandlerInterceptors() {
        if (!handlerInterceptorsAdded) {
            return;
        }

        handlerInterceptorsAdded = false;

        List<Interceptor<? extends Message>> in = super.getInInterceptors();
        List<Interceptor<? extends Message>> out = super.getOutInterceptors();
        List<Interceptor<? extends Message>> outFault = super.getOutFaultInterceptors();
        List<Interceptor<? extends Message>> inFault = super.getInFaultInterceptors();

        in.remove(logicalInInterceptor);
        out.remove(logicalOutInterceptor);
        inFault.remove(logicalFaultInInterceptor);
        outFault.remove(logicalFaultOutInterceptor);
        if (soapHandlerInterceptor != null) {
            in.remove(soapHandlerInterceptor);
            out.remove(soapHandlerInterceptor);
        }
        if (soapFaultInInterceptor != null) {
            inFault.remove(soapFaultInInterceptor);
        }
        if (soapFaultOutInterceptor != null) {
            outFault.remove(soapFaultOutInterceptor);
        }
    }
}
