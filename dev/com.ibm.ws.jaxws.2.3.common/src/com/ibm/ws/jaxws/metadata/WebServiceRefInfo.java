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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceRef;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.utils.StringUtils;

/**
 * This class is used to hold the metadata associated with a service-ref. An instance of this class will be stored in
 * the naming Reference object when the service-ref info is bound into the namespace. Because it is stored along with
 * the naming Reference object, it must be serializable since Reference objects are passed between server instances,
 * etc.
 */
public class WebServiceRefInfo implements Serializable {

    private static final TraceComponent tc = Tr.register(WebServiceRefInfo.class);

    private static final long serialVersionUID = -6008015521793819719L;

    private String jndiName;
    private String mappedName;
    private String lookupName;
    private String serviceInterfaceClassName;
    private String serviceRefTypeClassName;
    private String wsdlLocation;
    private QName serviceQName;
    private QName portQName;

    //the default overridden port address in a service
    private String defaultPortAddress;

    private Map<String, String> properties;

    private String handlerChainDeclaringClassName;
    private HandlerChainAnnotationSer handlerChainAnnotation;
    private boolean handlersFromXML;

    private String componenetName;

    public String getComponenetName() {
        return componenetName;
    }

    public void setComponenetName(String componenetName) {
        this.componenetName = componenetName;
    }

    /**
     * Use 2 for the initializtion size for the array list, as mostly, users should not configure a long HandlerChainInfo for the service reference
     */
    private final List<HandlerChainInfo> handlerChains = new ArrayList<HandlerChainInfo>(2);

    /**
     * This Map holds lists of annotations keyed by SEI classname. For a given service-ref, multiple port-component-refs
     * could exist, each with its own unique SEI classname, and each port-component-ref could be configured with the
     * MTOM, Addressing, and/or RespectBinding features.
     */
    private final Map<String, PortComponentRefInfo> seiNamePortComponentRefInfoMap = new HashMap<String, PortComponentRefInfo>();

    /**
     * The map hold the PortComponentRefInfo defined in the liberty-webservices-bnd, keyed by the port qname.
     */
    private final Map<QName, PortComponentRefInfo> bindingPortComponentRefInfoMap = new HashMap<QName, PortComponentRefInfo>();

    private transient final WebServiceRef webServiceRefAnnotation;

    private WebServiceRefPartialInfo partialInfo;

    private transient JaxWsClientMetaData clientMetaData;

    public WebServiceRefInfo(WebServiceRef wsrAnnot) {
        jndiName = wsrAnnot.name();
        mappedName = wsrAnnot.mappedName();
        lookupName = wsrAnnot.lookup();
        wsdlLocation = wsrAnnot.wsdlLocation();

        setServiceInterfaceClassName((wsrAnnot.value() != null ? wsrAnnot.value().getName() : null));
        setServiceRefTypeClassName((wsrAnnot.type() != null ? wsrAnnot.type().getName() : null));

        webServiceRefAnnotation = wsrAnnot;
    }

    public WebServiceRef getAnnotationValue() {
        return webServiceRefAnnotation;
    }

    public Map<String, PortComponentRefInfo> getPortComponentRefInfoMap() {
        return seiNamePortComponentRefInfoMap;
    }

    public PortComponentRefInfo getPortComponentRefInfo(String seiClassName) {
        return seiNamePortComponentRefInfoMap.get(seiClassName);
    }

    public PortComponentRefInfo getPortComponentRefInfo(QName portQName) {
        if (null == portQName) {
            return null;
        }

        PortComponentRefInfo portInfo = bindingPortComponentRefInfoMap.get(portQName);
        if (null == portInfo) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No PortComponentRefInfo object was found with the given qname '" + portQName + "', try again with its local part.");
            }

            portInfo = bindingPortComponentRefInfoMap.get(new QName(portQName.getLocalPart()));
        }
        return portInfo;
    }

    public void addPortComponentRefInfo(PortComponentRefInfo portComponentRefInfo) {
        String seiClassName = portComponentRefInfo.getServiceEndpointInterface();
        QName portQname = portComponentRefInfo.getPortQName();

        if (null != seiClassName) {
            seiNamePortComponentRefInfoMap.put(seiClassName, portComponentRefInfo);
        } else if (null != portQname) {
            bindingPortComponentRefInfoMap.put(portQname, portComponentRefInfo);
        }
    }

    /**
     * Returns the List<WebServiceFeature> associated with the specified SEI class name.
     */
    public List<WebServiceFeature> getWSFeatureForSEIClass(String seiClassName) {
        PortComponentRefInfo portComponentRefInfo = seiNamePortComponentRefInfoMap.get(seiClassName);

        if (portComponentRefInfo == null) {
            return Collections.emptyList();
        }

        List<WebServiceFeatureInfo> featureInfos = portComponentRefInfo.getWebServiceFeatureInfos();

        if (featureInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WebServiceFeature> wsFeatures = new ArrayList<WebServiceFeature>(featureInfos.size());

        for (WebServiceFeatureInfo featureInfo : featureInfos) {
            wsFeatures.add(featureInfo.getWebServiceFeature());
        }
        return wsFeatures;
    }

    public boolean isWebServiceFeaturePresent(String seiClassName, Class<? extends WebServiceFeatureInfo> featureClass) {

        PortComponentRefInfo portComponentRefInfo = seiNamePortComponentRefInfoMap.get(seiClassName);
        if (portComponentRefInfo == null) {
            return false;
        }

        // Retrieve the list of features for this SEI class.
        List<WebServiceFeatureInfo> seiList = portComponentRefInfo.getWebServiceFeatureInfos();

        if (seiList.isEmpty()) {
            return false;
        }

        // Walk the list and see if the feature is present.
        for (WebServiceFeatureInfo wsFeatureInfo : seiList) {
            if (wsFeatureInfo.getClass() == featureClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a feature info to the PortComponentRefInfo, if the target one does not exist, a new one with
     * that port component interface will be created
     * 
     * @param seiName port component interface name
     * @param featureInfo feature info of the target port
     */
    public void addWebServiceFeatureInfo(String seiName, WebServiceFeatureInfo featureInfo) {
        PortComponentRefInfo portComponentRefInfo = seiNamePortComponentRefInfoMap.get(seiName);
        if (portComponentRefInfo == null) {
            portComponentRefInfo = new PortComponentRefInfo(seiName);
            seiNamePortComponentRefInfoMap.put(seiName, portComponentRefInfo);
        }
        portComponentRefInfo.addWebServiceFeatureInfo(featureInfo);
    }

    /**
     * Getters and setters for all the data members...
     */

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
    }

    public String getMappedName() {
        return mappedName;
    }

    public void setLookupName(String lookupName) {
        this.lookupName = lookupName;
    }

    public String getLookupName() {
        return lookupName;
    }

    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    public String getWsdlLocation() {
        if (!StringUtils.isEmpty(wsdlLocation)) {
            return wsdlLocation;
        }
        return partialInfo == null ? null : partialInfo.getWsdlLocation();
    }

    public String getDefaultPortAddress() {
        return defaultPortAddress;
    }

    public void setDefaultPortAddress(String address) {
        this.defaultPortAddress = address;
    }

    public void setServiceQName(QName serviceQName) {
        this.serviceQName = serviceQName;
    }

    public QName getServiceQName() {
        if (serviceQName != null) {
            return serviceQName;
        }
        return partialInfo == null ? null : partialInfo.getServiceQName();
    }

    public void setPortQName(QName portQName) {
        this.portQName = portQName;
    }

    public QName getPortQName() {
        if (portQName != null) {
            return portQName;
        }
        return partialInfo == null ? null : partialInfo.getPortQName();
    }

    public void setServiceInterfaceClassName(String serviceInterfaceClassName) {
        this.serviceInterfaceClassName = serviceInterfaceClassName;
    }

    public String getServiceInterfaceClassName() {
        return serviceInterfaceClassName;
    }

    public void setServiceRefTypeClassName(String serviceRefTypeClassName) {
        this.serviceRefTypeClassName = serviceRefTypeClassName;
    }

    public String getServiceRefTypeClassName() {
        return serviceRefTypeClassName;
    }

    public void addHandlerChain(HandlerChainInfo handlerChain) {
        this.handlerChains.add(handlerChain);
    }

    public boolean removeHandlerChain(HandlerChainInfo handlerChain) {
        return this.handlerChains.remove(handlerChain);
    }

    public void clearHandlerChain(HandlerChainInfo handlerChain) {
        this.handlerChains.clear();
    }

    /**
     * @return the handlersFromXML
     */
    public boolean getHandlersFromXML() {
        return handlersFromXML;
    }

    /**
     * @param handlersFromXML the handlersFromXML to set
     */
    public void setHandlersFromXML(boolean handlersFromXML) {
        this.handlersFromXML = handlersFromXML;
    }

    /**
     * @return the handlerChainDeclaringClassName
     */
    public String getHandlerChainDeclaringClassName() {
        return handlerChainDeclaringClassName;
    }

    /**
     * @param handlerChainDeclaringClassName the handlerChainDeclaringClassName to set
     */
    public void setHandlerChainDeclaringClassName(String handlerChainDeclaringClassName) {
        this.handlerChainDeclaringClassName = handlerChainDeclaringClassName;
    }

    /**
     * @return the handlerChains
     */
    public List<HandlerChainInfo> getHandlerChains() {
        return Collections.unmodifiableList(handlerChains);
    }

    /**
     * @return the handlerChainAnnotation
     */
    public HandlerChainAnnotationSer getHandlerChainAnnotation() {
        return handlerChainAnnotation;
    }

    /**
     * @param handlerChainAnnotation the handlerChainAnnotation to set
     */
    public void setHandlerChainAnnotation(HandlerChain handlerChainAnnotation) {
        this.handlerChainAnnotation = new HandlerChainAnnotationSer(handlerChainAnnotation);
    }

    /**
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * @return the partialInfo
     */
    public WebServiceRefPartialInfo getPartialInfo() {
        return partialInfo;
    }

    /**
     * @param partialInfo the partialInfo to set
     */
    public void setPartialInfo(WebServiceRefPartialInfo partialInfo) {
        this.partialInfo = partialInfo;
        if (partialInfo != null) {
            if (this.handlerChainAnnotation == null)
                this.handlerChainAnnotation = partialInfo.getHandlerChainAnnotation();
            if (this.handlerChainDeclaringClassName == null)
                this.handlerChainDeclaringClassName = partialInfo.getHandlerChainDeclaringClassName();
        }
    }

    /**
     * @return the clientMetaData
     */
    public JaxWsClientMetaData getClientMetaData() {
        return clientMetaData;
    }

    /**
     * @param clientMetaData the clientMetaData to set
     */
    public void setClientMetaData(JaxWsClientMetaData clientMetaData) {
        this.clientMetaData = clientMetaData;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof WebServiceRefInfo))
            return false;

        WebServiceRefInfo wsrObj = (WebServiceRefInfo) obj;
        if (this.handlersFromXML != wsrObj.handlersFromXML)
            return false;
        if (!Utils.compareInstance(this.handlerChainAnnotation, wsrObj.handlerChainAnnotation))
            return false;
        if (!Utils.compareStrings(this.jndiName, wsrObj.jndiName))
            return false;
        if (!Utils.compareStrings(this.mappedName, wsrObj.mappedName))
            return false;
        if (!Utils.compareStrings(this.lookupName, wsrObj.lookupName))
            return false;
        if (!Utils.compareStrings(this.wsdlLocation, wsrObj.wsdlLocation))
            return false;
        if (!Utils.compareStrings(this.serviceInterfaceClassName, wsrObj.serviceInterfaceClassName))
            return false;
        if (!Utils.compareStrings(this.serviceRefTypeClassName, wsrObj.serviceRefTypeClassName))
            return false;
        if (!Utils.compareStrings(this.handlerChainDeclaringClassName, wsrObj.handlerChainDeclaringClassName))
            return false;
        if (!Utils.compareQNames(this.serviceQName, wsrObj.serviceQName))
            return false;
        if (!Utils.compareQNames(this.portQName, wsrObj.portQName))
            return false;
        if (!Utils.compareLists(this.handlerChains, wsrObj.handlerChains))
            return false;
        if (!Utils.compareInstance(this.partialInfo, wsrObj.partialInfo))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = jndiName == null ? 0 : jndiName.hashCode();
        result += mappedName == null ? 0 : mappedName.hashCode();
        result += lookupName == null ? 0 : lookupName.hashCode();
        result += wsdlLocation == null ? 0 : wsdlLocation.hashCode();
        result += serviceInterfaceClassName == null ? 0 : serviceInterfaceClassName.hashCode();
        result += serviceRefTypeClassName == null ? 0 : serviceRefTypeClassName.hashCode();
        result += serviceQName == null ? 0 : serviceQName.hashCode();
        result += portQName == null ? 0 : portQName.hashCode();
        result += handlersFromXML == false ? 0 : 1;
        result += handlerChainDeclaringClassName == null ? 0 : handlerChainDeclaringClassName.hashCode();
        result += handlerChains == null ? 0 : handlerChains.hashCode();
        result += handlerChainAnnotation == null ? 0 : handlerChainAnnotation.hashCode();
        result += partialInfo == null ? 0 : partialInfo.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("*** Begin WebServiceRefInfo: " + this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)) + " ***\n");
        sb.append("JNDI name                       : " + getJndiName() + "\n");
        sb.append("Mapped name                     : " + getMappedName() + "\n");
        sb.append("Lookup name                     : " + getLookupName() + "\n");
        sb.append("WSDL location                   : " + getWsdlLocation() + "\n");
        sb.append("WSDL service                    : " + (getServiceQName() != null ? getServiceQName().toString() : "<null>") + "\n");
        sb.append("Service-Interface classname     : " + getServiceInterfaceClassName() + "\n");
        sb.append("Service-Ref-Type classname      : " + getServiceRefTypeClassName() + "\n");
        sb.append("Port QName                      : " + (getPortQName() != null ? getPortQName().toString() : "<null>") + "\n");
        sb.append("handlerChain file location: " + (handlerChainAnnotation != null ? handlerChainAnnotation.getFile() : "<null>") + "\n");
        sb.append("handlers is From XML: " + handlersFromXML + "\n");
        sb.append("HandlerChain declaring classname: " + getHandlerChainDeclaringClassName() + "\n");
        sb.append("HandlerChains: " + (handlerChains != null ? handlerChains.toString() : "<null>") + "\n");
        sb.append("PartialInfo: " + (partialInfo != null ? partialInfo.toString() : "<null>") + "\n");
        sb.append("*** End WebServiceRefInfo ***\n");
        return sb.toString();
    }

}