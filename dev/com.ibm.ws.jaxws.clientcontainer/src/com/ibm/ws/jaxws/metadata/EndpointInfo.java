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
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.ws.jaxws.utils.StringUtils;

public class EndpointInfo implements Serializable {

    private static final long serialVersionUID = 97168420190792502L;

    private final List<String> addresses = new ArrayList<String>(2);

    private final String implBeanClassName;

    private String portComponentName;

    private String wsdlLocation;

    private String protocolBinding;

    //The implementation targetNameSpaceURL
    private String targetNamespaceURL;

    //The SEI targetNameSpace, it may be different with the current implementation class
    private String interfaceTragetNameSpaceURL;

    private String portLink;

    private QName wsdlPort;

    private QName wsdlService;

    private HandlerChainsInfo handlerChainsInfo;

    private boolean configuredInWebXml = false;

    private String servletName;

    private String beanName;

    private String serviceEndpointInterface;

    private MTOMFeatureInfo mtomFeatureInfo;

    private AddressingFeatureInfo addressingFeatureInfo;

    private RespectBindingFeatureInfo respectBindingFeatureInfo;

    private ServiceModeInfo serviceModeInfo;

    private Map<String, String> endpointProperties;

    private EndpointType endpointType;

    public EndpointInfo(String implBeanClassName, EndpointType endpointType) {
        this.implBeanClassName = implBeanClassName;
        this.endpointType = endpointType;
    }

    /**
     * @return the endpointProperties
     */
    public Map<String, String> getEndpointProperties() {
        return endpointProperties;
    }

    /**
     * @param endpointProperties the endpointProperties to set
     */
    public void setEndpointProperties(Map<String, String> endpointProperties) {
        this.endpointProperties = endpointProperties;
    }

    /**
     * @return the endpointType
     */
    public EndpointType getEndpointType() {
        return endpointType;
    }

    /**
     * @param endpointType the endpointType to set
     */
    public void setEndpointType(EndpointType endpointType) {
        this.endpointType = endpointType;
    }

    /**
     * @return the wsdlPort
     */
    public QName getWsdlPort() {
        return wsdlPort;
    }

    /**
     * @return the mtom
     */
    public MTOMFeatureInfo getMTOMFeatureInfo() {
        return mtomFeatureInfo;
    }

    /**
     * @param mtom the mtom to set
     */
    public void setMTOMFeatureInfo(MTOMFeatureInfo mtomFeatureInfo) {
        this.mtomFeatureInfo = mtomFeatureInfo;
    }

    /**
     * @return the addressing
     */
    public AddressingFeatureInfo getAddressingFeatureInfo() {
        return addressingFeatureInfo;
    }

    /**
     * @param addressing the addressing to set
     */
    public void setAddressingFeatureInfo(AddressingFeatureInfo addressingFeatureInfo) {
        this.addressingFeatureInfo = addressingFeatureInfo;
    }

    /**
     * @return the respectBinding
     */
    public RespectBindingFeatureInfo getRespectBindingFeatureInfo() {
        return respectBindingFeatureInfo;
    }

    /**
     * @param respectBinding the respectBinding to set
     */
    public void setRespectBindingFeatureInfo(RespectBindingFeatureInfo respectBindingFeatureInfo) {
        this.respectBindingFeatureInfo = respectBindingFeatureInfo;
    }

    /**
     * @return the serviceEndpointInterface
     */
    public String getServiceEndpointInterface() {
        return serviceEndpointInterface;
    }

    /**
     * @param serviceEndpointInterface the serviceEndpointInterface to set
     */
    public void setServiceEndpointInterface(String serviceEndpointInterface) {
        this.serviceEndpointInterface = serviceEndpointInterface;
    }

    /**
     * @return the configuredInWebXml
     */
    public boolean isConfiguredInWebXml() {
        return configuredInWebXml;
    }

    /**
     * @param configuredInWebXml the configuredInWebXml to set
     */
    public void setConfiguredInWebXml(boolean configuredInWebXml) {
        this.configuredInWebXml = configuredInWebXml;
    }

    /**
     * @param wsdlPort the wsdlPort to set
     */
    public void setWsdlPort(QName wsdlPort) {
        this.wsdlPort = wsdlPort;
    }

    /**
     * @return the wsdlService
     */
    public QName getWsdlService() {
        return wsdlService;
    }

    /**
     * @param wsdlService the wsdlService to set
     */
    public void setWsdlService(QName wsdlService) {
        this.wsdlService = wsdlService;
    }

    /**
     * @return the handlerChainsInfo
     */
    public HandlerChainsInfo getHandlerChainsInfo() {
        return handlerChainsInfo;
    }

    /**
     * @param handlerChainsInfo the handlerChainsInfo to set
     */
    public void setHandlerChainsInfo(HandlerChainsInfo handlerChainsInfo) {
        this.handlerChainsInfo = handlerChainsInfo;
    }

    /**
     * @return the portLink
     */
    public String getPortLink() {
        return portLink;
    }

    /**
     * @param portLink the portLink to set
     */
    public void setPortLink(String portLink) {
        this.portLink = portLink;
    }

    /**
     * @return the implBeanClassName
     */
    public String getImplBeanClassName() {
        return implBeanClassName;
    }

    /**
     * @return the wsdlLocation
     */
    public String getWsdlLocation() {
        return wsdlLocation;
    }

    /**
     * @param wsdlLocation the wsdlLocation to set
     */
    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    /**
     * @return the portComponentName
     */
    public String getPortComponentName() {
        return portComponentName;
    }

    /**
     * @param portComponentName the portComponentName to set
     */
    public void setPortComponentName(String portComponentName) {
        this.portComponentName = portComponentName;
    }

    /**
     * @return the protocolBinding
     */
    public String getProtocolBinding() {
        if (StringUtils.isEmpty(protocolBinding)) {
            return SOAPBinding.SOAP11HTTP_BINDING;
        }
        return protocolBinding;
    }

    /**
     * @param protocolBinding the protocolBinding to set
     */
    public void setProtocolBinding(String protocolBinding) {
        this.protocolBinding = protocolBinding;
    }

    public void addAddress(String add) {
        addresses.add(add);
    }

    public void addAddresses(List<String> adds) {
        addresses.addAll(adds);
    }

    public String getAddress(int index) {
        return (addresses.size() > index) ? addresses.get(index) : null;
    }

    public String[] getAddresses() {
        return addresses.toArray(new String[addresses.size()]);
    }

    public boolean removeAddress(String add) {
        return addresses.remove(add);
    }

    public void clearAddresses() {
        addresses.clear();
    }

    /**
     * @return the servletName
     */
    public String getServletName() {
        return servletName;
    }

    /**
     * @param servletName the servletName to set
     */
    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /**
     * @return the beanName
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * @param beanName the beanName to set
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * @return the serviceModeInfo
     */
    public ServiceModeInfo getServiceModeInfo() {
        return serviceModeInfo;
    }

    /**
     * @param serviceModeInfo the serviceModeInfo to set
     */
    public void setServiceModeInfo(ServiceModeInfo serviceModeInfo) {
        this.serviceModeInfo = serviceModeInfo;
    }

    /**
     * @return the targetNamespaceURL
     */
    public String getTargetNamespaceURL() {
        return targetNamespaceURL;
    }

    /**
     * @param targetNamespaceURL the targetNamespaceURL to set
     */
    public void setTargetNamespaceURL(String targetNamespaceURL) {
        this.targetNamespaceURL = targetNamespaceURL;
    }

    /**
     * @return the interfaceTragetNameSpaceURL
     */
    public String getInterfaceTragetNameSpaceURL() {
        return interfaceTragetNameSpaceURL;
    }

    /**
     * @param interfaceTragetNameSpaceURL the interfaceTragetNameSpaceURL to set
     */
    public void setInterfaceTragetNameSpaceURL(String interfaceTragetNameSpaceURL) {
        this.interfaceTragetNameSpaceURL = interfaceTragetNameSpaceURL;
    }

}
