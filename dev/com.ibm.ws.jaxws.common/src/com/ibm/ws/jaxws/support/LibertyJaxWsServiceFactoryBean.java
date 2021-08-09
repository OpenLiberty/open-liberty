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
package com.ibm.ws.jaxws.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.JAXWSMethodDispatcher;
import org.apache.cxf.jaxws.JAXWSProviderMethodDispatcher;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceConfiguration;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.jaxws.support.WebServiceProviderConfiguration;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.wsdl.WSDLManager;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.wsspi.anno.info.InfoStoreException;

/**
 * Extend the org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean
 * so that we can leverage our LibertyJaxWsImplementorInfo
 */
public class LibertyJaxWsServiceFactoryBean extends JaxWsServiceFactoryBean {
    private static final TraceComponent tc = Tr.register(LibertyJaxWsServiceFactoryBean.class);

    private static final String URI_POLICY_NS = "http://www.w3.org/ns/ws-policy";
    private static final String URI_WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    private AbstractServiceConfiguration jaxWsConfiguration;
    private final JaxWsPublisherContext publisherContext;
    private LibertyJaxWsImplementorInfo implInfo;

    private List<WebServiceFeature> setWsFeatures;
    private final List<WebServiceFeature> wsFeatures = new ArrayList<WebServiceFeature>();

    public LibertyJaxWsServiceFactoryBean(LibertyJaxWsImplementorInfo implInfo, JaxWsPublisherContext publisherContext) {
        super(implInfo);
        this.publisherContext = publisherContext;
        this.implInfo = implInfo;
        initializeConfig(this.implInfo);
        this.serviceClass = implInfo.getEndpointClass();
        this.serviceType = implInfo.getSEIType();
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (serviceClass == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The service class is null");
            }
            return;
        }

        EndpointInfoBuilder builder = (EndpointInfoBuilder) publisherContext.getAttribute(JaxWsConstants.ENDPOINT_INFO_BUILDER);
        EndpointInfoBuilderContext context = (EndpointInfoBuilderContext) publisherContext.getAttribute(JaxWsConstants.ENDPOINT_INFO_BUILDER_CONTEXT);
        if (null != builder && null != context) {
            try {
                context.getInfoStore().open();
                setJaxWsImplementorInfo(new LibertyJaxWsImplementorInfo(serviceClass, builder.build(context, serviceClass.getName(), EndpointType.SERVLET), publisherContext));
            } catch (InfoStoreException e) {
                throw new WebServiceException(e);
            } catch (ClassNotFoundException e) {
                throw new WebServiceException(Tr.formatMessage(tc, "err.unable.set.serviceclass", serviceClass), e);
            } catch (Exception e) {
                throw new WebServiceException(e);
            } finally {
                try {
                    context.getInfoStore().close();
                } catch (InfoStoreException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The exception occurs when close the infoStore.", e);
                    }
                }
            }

            this.serviceClass = getJaxWsImplementorInfo().getEndpointClass();
            this.checkServiceClassAnnotations(this.serviceClass);
            this.serviceType = implInfo.getSEIType();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invoke the setServiceClass method from " + super.getClass().getName());
            }
            super.setServiceClass(serviceClass);
        }
    }

    @Override
    public Service create() {
        org.apache.cxf.service.Service s = super.create();

        s.put(ENDPOINT_CLASS, implInfo.getEndpointClass());

        if (s.getDataBinding() != null) {
            setMTOMFeatures(s.getDataBinding());
        }
        return s;
    }

    private void setMTOMFeatures(DataBinding databinding) {
        if (this.wsFeatures != null) {
            for (WebServiceFeature wsf : this.wsFeatures) {
                if (wsf instanceof MTOMFeature) {
                    databinding.setMtomEnabled(true);
                    MTOMFeature f = (MTOMFeature) wsf;
                    if (f.getThreshold() > 0) {
                        databinding.setMtomThreshold(((MTOMFeature) wsf).getThreshold());
                    }
                }
            }
        }
    }

    @Override
    public Endpoint createEndpoint(EndpointInfo ei) throws EndpointException {
        Endpoint ep = new JaxWsEndpointImpl(getBus(), getService(), ei, implInfo, wsFeatures,
                        this.getFeatures(), true);// set isFromWsdl = true to avoid process @Addressing, will process the AddressingFeature when get the Endpoint.
        if (null != ei && !isFromWsdl()) {
            // build the wsdl Extensibilities
            JaxWsEndpointImplHelper helper = new JaxWsEndpointImplHelper((JaxWsEndpointImpl) ep, ei);

            helper.buildWsdlExtensibilities();
            helper.extractWsdlExtensibilities();
        }
        sendEvent(FactoryBeanListener.Event.ENDPOINT_CREATED, ei, ep);
        return ep;
    }

    @Override
    public void setWsFeatures(List<WebServiceFeature> swsFeatures) {
        this.setWsFeatures = swsFeatures;
        if (null != swsFeatures) {
            wsFeatures.addAll(swsFeatures);
        }
    }

    @Override
    public void setJaxWsImplementorInfo(JaxWsImplementorInfo jaxWsImplementorInfo) {
        if (jaxWsImplementorInfo instanceof LibertyJaxWsImplementorInfo) {
            this.implInfo = (LibertyJaxWsImplementorInfo) jaxWsImplementorInfo;
            initializeConfig(implInfo);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invoke the setJaxWsImplementorInfo from " + super.getClass().getName());
            }
            super.setJaxWsImplementorInfo(jaxWsImplementorInfo);
        }
    }

    protected void initializeConfig(JaxWsImplementorInfo jaxWsImplementorInfo) {
        if (jaxWsImplementorInfo.isWebServiceProvider()) {
            jaxWsConfiguration = new WebServiceProviderConfiguration();
            jaxWsConfiguration.setServiceFactory(this);
            getServiceConfigurations().add(0, jaxWsConfiguration);
            setWrapped(false);
            setDataBinding(new SourceDataBinding());
            setMethodDispatcher(new JAXWSProviderMethodDispatcher(jaxWsImplementorInfo));
        } else {
            jaxWsConfiguration = new JaxWsServiceConfiguration();
            jaxWsConfiguration.setServiceFactory(this);
            getServiceConfigurations().add(0, jaxWsConfiguration);

            Class<?> seiClass = jaxWsImplementorInfo.getEndpointClass();
            if (seiClass != null && seiClass.getPackage() != null) {
                XmlSchema schema = seiClass.getPackage().getAnnotation(XmlSchema.class);
                if (schema != null && XmlNsForm.QUALIFIED.equals(schema.elementFormDefault())) {
                    setQualifyWrapperSchema(true);
                }
            }
            setMethodDispatcher(new JAXWSMethodDispatcher(jaxWsImplementorInfo));
        }
        loadWSFeatureAnnotation();
    }

    private void loadWSFeatureAnnotation() {
        List<WebServiceFeature> features = new ArrayList<WebServiceFeature>();

        features.add(implInfo.getMTOMFeature());
        features.add(implInfo.getAddressingFeature());
        features.add(implInfo.getRespectBindingFeature());

        for (int i = 0; i < features.size(); ++i) {
            if (null != features.get(i)) {
                wsFeatures.add(features.get(i));
            }
        }
        if (setWsFeatures != null) {
            wsFeatures.addAll(setWsFeatures);
        }
    }

    private class JaxWsEndpointImplHelper {
        private final JaxWsEndpointImpl endpoint;
        private final EndpointInfo endpointInfo;
        private final BindingInfo bindingInfo;

        JaxWsEndpointImplHelper(JaxWsEndpointImpl endpoint, EndpointInfo endpointInfo) {
            this.endpoint = endpoint;
            this.endpointInfo = endpointInfo;
            bindingInfo = endpointInfo.getBinding();
        }

        /**
         * Build the Addressing feature extensibility.
         */
        public void buildWsdlExtensibilities() {
            AddressingFeature addressing = getAddressFeature();
            if (addressing != null) {
                ExtensionRegistry extensionRegistry = getBus().getExtension(WSDLManager.class)
                                .getExtensionRegistry();
                try {
                    ExtensibilityElement el = extensionRegistry.createExtension(javax.wsdl.Binding.class,
                                                                                JAXWSAConstants.
                                                                                WSAW_USINGADDRESSING_QNAME);
                    el.setRequired(addressing.isRequired());
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
                    if (!addressing.isRequired()) {
                        writer.writeAttribute("wsp", URI_POLICY_NS,
                                              "Optional", "true");
                    }
                    writer.writeStartElement("wsp", "Policy", URI_POLICY_NS);

                    String s = getAddressingFeatureRequirement(addressing);
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
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "The exception is encountered when buildWsdlExtensibilities.", e);
                    }
                }
            }
        }

        private AddressingFeature getAddressFeature() {
            for (WebServiceFeature wsFeature : wsFeatures) {
                if (wsFeature instanceof AddressingFeature) {
                    return (AddressingFeature) wsFeature;
                }
            }
            return null;
        }

        private String getAddressingFeatureRequirement(AddressingFeature addressingFeature) {
            try {
                Object o = addressingFeature.getResponses();
                if (o != null) {
                    String s = o.toString();
                    if ("ANONYMOUS".equals(s)) {
                        return "AnonymousResponses";
                    } else if ("NON_ANONYMOUS".equals(s)) {
                        return "NonAnonymousResponses";
                    }
                }
            } catch (Throwable ex) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The error is encountered when get the Responses of the AddressingFeature, probably using JAX-WS 2.1", ex);
                }
            }
            return null;
        }

        /**
         * Validate the Addressing feature and set the endpointReference to the EndpointInfo
         * 
         */
        public void extractWsdlExtensibilities() {
            List<ExtensibilityElement> bindingExtensors = endpointInfo.getBinding().getExtensors(ExtensibilityElement.class);
            List<ExtensibilityElement> portExtensors = endpointInfo.getExtensors(ExtensibilityElement.class);
            if (hasUsingAddressing(bindingExtensors) || hasUsingAddressing(portExtensors)) {
                WSAddressingFeature feature = new WSAddressingFeature();
                if (addressingRequired(bindingExtensors)
                    || addressingRequired(portExtensors)) {
                    feature.setAddressingRequired(true);
                }
                addAddressingFeature(feature);
            }
            extractWsdlEprs();
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

        private void extractWsdlEprs() {
            //parse the EPR in wsdl
            List<ExtensibilityElement> portExtensors = endpointInfo.getExtensors(ExtensibilityElement.class);
            if (portExtensors != null) {
                Iterator<ExtensibilityElement> extensionElements = portExtensors.iterator();
                QName wsaEpr = new QName(Names.WSA_NAMESPACE_NAME, "EndpointReference");
                while (extensionElements.hasNext()) {
                    ExtensibilityElement ext = extensionElements.next();
                    if (ext instanceof UnknownExtensibilityElement && wsaEpr.equals(ext.getElementType())) {
                        DOMSource domSource = new DOMSource(((UnknownExtensibilityElement) ext).getElement());
                        W3CEndpointReference w3cEPR = new W3CEndpointReference(domSource);
                        EndpointReferenceType ref = ProviderImpl.convertToInternal(w3cEPR);
                        endpointInfo.getTarget().setMetadata(ref.getMetadata());
                        endpointInfo.getTarget().setReferenceParameters(ref.getReferenceParameters());
                        endpointInfo.getTarget().getOtherAttributes().putAll(ref.getOtherAttributes());
                    }

                }
            }
        }

        private void addAddressingFeature(AbstractFeature feature) {
            if (endpoint.getFeatures() == null || endpoint.getFeatures().isEmpty()) {
                endpoint.getFeatures().add(feature);
            } else {
                for (AbstractFeature f : endpoint.getFeatures()) {
                    if (f instanceof WSAddressingFeature) {
                        return;
                    }
                }
                endpoint.getFeatures().add(feature);
            }
        }
    }
}
