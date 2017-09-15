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

import javax.xml.namespace.QName;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.AddressingFeature.Responses;
import javax.xml.ws.soap.SOAPBinding;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.RespectBinding;
import com.ibm.ws.javaee.dd.ws.PortComponent;
import com.ibm.ws.javaee.dd.ws.WebserviceDescription;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.AddressingFeatureInfo;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.MTOMFeatureInfo;
import com.ibm.ws.jaxws.metadata.RespectBindingFeatureInfo;
import com.ibm.ws.jaxws.metadata.builder.AbstractEndpointInfoConfigurator;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoConfigurator;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Process the websevices.xml to configure EndpointInfo
 */
@Component(service = { EndpointInfoConfigurator.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = false, property = { "service.vendor=IBM" })
public class WebservicesXMLEndpointInfoConfigurator extends AbstractEndpointInfoConfigurator {
    private static final TraceComponent tc = Tr.register(WebservicesXMLEndpointInfoConfigurator.class);

    private static final String SERVICE_LINK = "webservicesXMLEndpointInfoConfigurator.merge.service.link";

    private static final String PORT_COMPONENT = "webservicesXMLEndpointInfoConfigurator.merge.port.component";

    public WebservicesXMLEndpointInfoConfigurator() {
        super(EndpointInfoConfigurator.Phase.PROCESS_DESCRIPTOR);
    }

    @Override
    public void prepare(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {
        if (!JaxWsDDHelper.isWebServicesXMLExisting(context.getContainer())) {
            return;
        }

        PortComponent portCmpt = null;
        String beanName = endpointInfo.getBeanName();

        // Try use the servlet name and qualified className
        String serviceLink = endpointInfo.isConfiguredInWebXml() ? endpointInfo.getServletName() : endpointInfo.getImplBeanClassName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Try to get the Port Component by servlet-link=" + serviceLink);
        }
        portCmpt = JaxWsDDHelper.getPortComponentByServletLink(serviceLink, context.getContainer());
        if (null == portCmpt) {
            if (null == beanName) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not get PortComponent either by servlet name or bean name.");
                }
                return;
            }
            // try to get the portComponent by EJB bean name
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Try to get the Port Component by ejb-link=" + beanName);
            }
            portCmpt = JaxWsDDHelper.getPortComponentByEJBLink(beanName, context.getContainer());
            if (null == portCmpt) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not get PortComponent either by servlet name or bean name.");
                }
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully get the Port Component by ejb-link=" + beanName);
            }
            serviceLink = beanName;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Successfully get the Port Component by servlet-link=" + serviceLink);
            }
        }

        context.addContextEnv(SERVICE_LINK, serviceLink);
        context.addContextEnv(PORT_COMPONENT, portCmpt);

        String seiClassName = portCmpt.getServiceEndpointInterface();
        if (!StringUtils.isEmpty(seiClassName)) {
            /*
             * According to JSR109 5.3.2.1
             * 
             * The <service-endpoint-interface> element in the deployment descriptor for an implementation bean must
             * match @WebService.endpointInterface member attribute if it is specified for the bean. Any other value is
             * ignored.
             */
            if (!StringUtils.isEmpty(endpointInfo.getServiceEndpointInterface())) {
                if (!seiClassName.equals(endpointInfo.getServiceEndpointInterface())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The <service-endpoint-interface> value: " + seiClassName +
                                     " is not equal to @WebService.endpointInterface: " + endpointInfo.getServiceEndpointInterface());
                    }
                }
            } else {
                // if the Service Endpoint Interface is not specified in @WebService annotation but specified via webservices.xml,
                // set it here.
                if (!JaxWsUtils.isProvider(context.getInfoStore().getDelayableClassInfo(endpointInfo.getImplBeanClassName()))) {
                    endpointInfo.setServiceEndpointInterface(seiClassName);
                }
            }
        }

    }

    @Override
    public void config(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {
        if (!JaxWsDDHelper.isWebServicesXMLExisting(context.getContainer())) {
            return;
        }

        String serviceLink = (String) context.getContextEnv(SERVICE_LINK);
        if (serviceLink == null) {
            return;
        }

        PortComponent portCmpt = (PortComponent) context.getContextEnv(PORT_COMPONENT);

        // port-component-name
        String pcname = portCmpt.getPortComponentName();
        if (pcname != null) {
            endpointInfo.setPortComponentName(pcname);
        } else {
            Tr.warning(tc, "warn.dd.invalid.portcomponentname", serviceLink);
        }

        // wsdl-file
        String wsdlfile;
        if (serviceLink.equals(endpointInfo.getBeanName())) {
            wsdlfile = getWsdlFile(JaxWsDDHelper.getWebserviceDescriptionByEJBLink(serviceLink, context.getContainer()));
        } else {
            wsdlfile = getWsdlFile(JaxWsDDHelper.getWebserviceDescriptionByServletLink(serviceLink, context.getContainer()));
        }
        if (wsdlfile != null) {
            endpointInfo.setWsdlLocation(wsdlfile);
        }

        // wsdl-service
        QName wsdlservice = portCmpt.getWSDLService();
        if (wsdlservice != null) {
            endpointInfo.setWsdlService(wsdlservice);
        }
        // set targetNamespace
        /*
         * According to JSR109 5.3.2.1
         * If <wsdl-service> element is provided in the deployment descriptor, then the namespace used in this element
         * overrides the targetNamespace member attribute in this annotation.
         */
        endpointInfo.setTargetNamespaceURL(endpointInfo.getWsdlService().getNamespaceURI());
        // if we use the targetNamespace form description xml, we should check the interface targetNamespace should be replaced by it as well
        String seiClassName = endpointInfo.getServiceEndpointInterface();
        if (seiClassName == null || StringUtils.isEmpty(seiClassName)) {
            endpointInfo.setInterfaceTragetNameSpaceURL(endpointInfo.getWsdlService().getNamespaceURI());
        }
        // wsdl-port
        QName wsdlport = portCmpt.getWSDLPort();
        if (wsdlport != null) {
            endpointInfo.setWsdlPort(wsdlport);
        }
        // validate the namespace in wsdl-port matches the one specified for wsdl-service
        /*
         * According to JSR109 5.3.2.1
         * If the namespace in <wsdl-port> element is specified, must match the effective target namespace.
         */
        QName portQName = endpointInfo.getWsdlPort();
        if (!portQName.getNamespaceURI().equals(endpointInfo.getTargetNamespaceURL())) {
            Tr.warning(tc, "warn.dd.invalid.namespace.of.wsdlport", portQName.getNamespaceURI(), portQName, endpointInfo.getPortComponentName());
            endpointInfo.setWsdlPort(new QName(endpointInfo.getTargetNamespaceURL(), portQName.getLocalPart()));
        }

        // enable-mtom, mtom-threshold
        configMTOM(endpointInfo, portCmpt);
        // respect-binding
        configRespectBinding(endpointInfo, portCmpt);
        // addressing
        configAddressing(endpointInfo, portCmpt);

        // protocol-binding
        String protocolBinding_wsXml = portCmpt.getProtocolBinding();
        if (protocolBinding_wsXml != null) {
            if (validateProtocolBindingInWebservicesXml(protocolBinding_wsXml)) {
                endpointInfo.setProtocolBinding(JaxWsUtils.getProtocolByToken(protocolBinding_wsXml));
            } else {
                Tr.warning(tc, "warn.dd.invalid.protocolbinding", protocolBinding_wsXml, endpointInfo.getPortComponentName());
            }
        }

        // handlerchains
        JaxWsHandlerInfoMergingHelper.mergeHandlerChains(portCmpt, endpointInfo);

        /*
         * As the serviceQName may changed, need to reset the address of endpointInfo if the endpoint is not configured in web.xml
         */
        if (!endpointInfo.isConfiguredInWebXml()) {
            endpointInfo.clearAddresses();
            endpointInfo.addAddress("/" + endpointInfo.getWsdlService().getLocalPart());
        }
    }

    private String getWsdlFile(WebserviceDescription wsDes) {
        if (null == wsDes) {
            return null;
        }
        return wsDes.getWSDLFile();
    }

    private boolean validateProtocolBindingInWebservicesXml(String protocolBinding_wsXml) {

        if (protocolBinding_wsXml.equals(SOAPBinding.SOAP11HTTP_BINDING) ||
            protocolBinding_wsXml.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING) ||
            protocolBinding_wsXml.equals(SOAPBinding.SOAP12HTTP_BINDING) ||
            protocolBinding_wsXml.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING) ||
            protocolBinding_wsXml.equals(HTTPBinding.HTTP_BINDING) ||
            protocolBinding_wsXml.equals(JaxWsConstants.SOAP11_HTTP_TOKEN) ||
            protocolBinding_wsXml.equals(JaxWsConstants.SOAP12_HTTP_TOKEN) ||
            protocolBinding_wsXml.equals(JaxWsConstants.SOAP11_HTTP_MTOM_TOKEN) ||
            protocolBinding_wsXml.equals(JaxWsConstants.SOAP12_HTTP_MTOM_TOKEN) ||
            protocolBinding_wsXml.equals(JaxWsConstants.XML_HTTP_TOKEN)) {
            return true;
        }

        return false;

    }

    protected void configMTOM(EndpointInfo epi, PortComponent portCmpt) {

        MTOMFeatureInfo mtom_epi = epi.getMTOMFeatureInfo();
        if (null == mtom_epi) {
            mtom_epi = new MTOMFeatureInfo();
        }

        if (portCmpt.isSetEnableMTOM()) {
            mtom_epi.setEnabled(portCmpt.isEnableMTOM());
        }

        if (portCmpt.isSetMTOMThreshold()) {
            mtom_epi.setThreshold(portCmpt.getMTOMThreshold());
        }

        if (portCmpt.isSetEnableMTOM() || portCmpt.isSetMTOMThreshold()) {
            epi.setMTOMFeatureInfo(mtom_epi);
        }

    }

    protected void configRespectBinding(EndpointInfo epi, PortComponent portCmpt) {
        RespectBindingFeatureInfo respectBinding_epi = epi.getRespectBindingFeatureInfo();
        RespectBinding respectBinding_wsXml = portCmpt.getRespectBinding();

        if (respectBinding_wsXml == null) {
            return;
        }

        // <enabled>
        if (respectBinding_wsXml.isSetEnabled()) {

            if (null == respectBinding_epi) {
                respectBinding_epi = new RespectBindingFeatureInfo();
            }
            respectBinding_epi.setEnabled(respectBinding_wsXml.isEnabled());

            epi.setRespectBindingFeatureInfo(respectBinding_epi);
        }
    }

    protected void configAddressing(EndpointInfo epi, PortComponent portCmpt) {

        /**
         * In order to match the behavior of tWAS, the addressing element is considered as a whole.
         * if <addressing> is set in webservices.xml, the addressing feature will follow its setting, no matter what are set in annotations.
         * if its sub-element <enabled> is not set,the default value (true) will be used.
         * if its sub-element <required> is not set,the default value (false) will be used.
         * if its sub-element <responses> is not set,the default value (ALL) will be used.
         */

        Addressing addressing_wsXml = portCmpt.getAddressing();

        if (addressing_wsXml == null) {
            return;
        }

        AddressingFeatureInfo addressing_epi = new AddressingFeatureInfo();

        // <enabled>
        if (addressing_wsXml.isSetEnabled()) {
            addressing_epi.setEnabled(addressing_wsXml.isEnabled());
        }

        // <required>
        if (addressing_wsXml.isSetRequired()) {
            addressing_epi.setRequired(addressing_wsXml.isRequired());
        }

        // <responses>
        int responses = addressing_wsXml.getAddressingResponsesTypeValue();

        if (responses == com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_ALL
            || responses == com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_ANONYMOUS
            || responses == com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_NON_ANONYMOUS) {

            switch (responses) {
                case com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_ALL:
                    addressing_epi.setResponses(Responses.ALL);
                    break;
                case com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_ANONYMOUS:
                    addressing_epi.setResponses(Responses.ANONYMOUS);
                    break;
                case com.ibm.ws.javaee.dd.common.wsclient.Addressing.ADDRESSING_RESPONSES_NON_ANONYMOUS:
                    addressing_epi.setResponses(Responses.NON_ANONYMOUS);
                    break;
                default:
                    // Do nothing
                    break;
            }
        }

        // set AddressingFeatureInfo in endpointInfo
        epi.setAddressingFeatureInfo(addressing_epi);
    }
}
