/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ComponentIDMap;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.wsbnd.HttpPublishing;
import com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceDescription;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpoint;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.StringUtils;

/*
 <xsd:complexType name="webservicesBndType">
 <xsd:sequence>
 <xsd:element name="webservice-description" type="ws:webserviceDescriptionType" minOccurs="0" maxOccurs="unbounded" />
 <xsd:element name="webservice-endpoint-properties" type="ws:webserviceEndpointPropertiesType" minOccurs="0" maxOccurs="1" />
 <xsd:element name="webservice-endpoint" type="ws:webserviceEndpointType" minOccurs="0" maxOccurs="unbounded" />
 <xsd:element name="http-publishing" type="ws:httpPublishingType" minOccurs="0" maxOccurs="1" />
 <xsd:element name="service-ref" type="ws:serviceRefType" minOccurs="0" maxOccurs="unbounded" />
 </xsd:sequence>
 <xsd:attribute name="version" type="xsd:string" use="required" fixed="1.0" />
 <xsd:attribute name="id" type="xsd:ID" />
 </xsd:complexType>
 */
public class WebservicesBndType extends DDParser.ElementContentParsable implements WebservicesBnd, DDParser.RootParsable {
    private final String path;

    private StringType version;

    private DDParser.ComponentIDMap idMap;

    private final Map<String, WebserviceDescriptionType> webserviceDescriptionTypeMap = new HashMap<String, WebserviceDescriptionType>();

    private WebserviceEndpointPropertiesType webserviceEndpointProperties;

    private final Map<String, WebserviceEndpointType> webserviceEndpointTypeMap = new HashMap<String, WebserviceEndpointType>();

    private HttpPublishingType httpPublishingType;

    //service ref map without component-name
    private Map<String, ServiceRefType> serviceRefTypeMap = new HashMap<String, ServiceRefType>();
    //service ref map with component-name
    private final Map<String, ServiceRefType> ejbServiceRefTypeMap = new HashMap<String, ServiceRefType>();

    public WebservicesBndType(String path) {
        this.path = path;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return path;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public List<WebserviceDescription> getWebserviceDescriptions() {
        List<WebserviceDescription> webserviceDescriptionList = null;
        if (null != webserviceDescriptionTypeMap) {
            webserviceDescriptionList = new ArrayList<WebserviceDescription>(webserviceDescriptionTypeMap.values());
        }
        return webserviceDescriptionList;
    }

    @Override
    public WebserviceDescription getWebserviceDescription(String WebserviceDescriptionName) {
        return WebserviceDescriptionName != null ? webserviceDescriptionTypeMap.get(WebserviceDescriptionName.trim()) : null;
    }

    @Override
    public Map<String, String> getWebserviceEndpointProperties() {
        return null != webserviceEndpointProperties ? webserviceEndpointProperties.getAttributes() : null;
    }

    @Override
    public List<WebserviceEndpoint> getWebserviceEndpoints() {
        List<WebserviceEndpoint> webserviceEndpointList = null;
        if (null != webserviceEndpointTypeMap) {
            webserviceEndpointList = new ArrayList<WebserviceEndpoint>(webserviceEndpointTypeMap.values());
        }
        return webserviceEndpointList;
    }

    @Override
    public WebserviceEndpoint getWebserviceEndpoint(String portComponentName) {
        return portComponentName != null ? webserviceEndpointTypeMap.get(portComponentName.trim()) : null;
    }

    @Override
    public HttpPublishing getHttpPublishing() {
        return this.httpPublishingType;
    }

    @Override
    public List<ServiceRef> getServiceRefs() {
        List<ServiceRef> serviceRefList = null;
        if (null != serviceRefTypeMap) {
            serviceRefList = new ArrayList<ServiceRef>(serviceRefTypeMap.values());
        }

        if (null != ejbServiceRefTypeMap) {
            Collection<ServiceRefType> ejbServiceRefs = ejbServiceRefTypeMap.values();
            if (null != serviceRefList) {
                serviceRefList.addAll(ejbServiceRefs);
            } else {
                serviceRefList = new ArrayList<ServiceRef>(ejbServiceRefs);
            }
        }
        return serviceRefList == null ? Collections.<ServiceRef> emptyList() : serviceRefList;
    }

    @Override
    public ServiceRef getServiceRef(String serviceRefName, String componentName) {
        if (StringUtils.isEmpty(serviceRefName)) {
            return null;
        }

        ServiceRef serviceRef = null;
        //if component name is not empty, find from ejb service ref map first, and then try again from web service ref map
        if (!StringUtils.isEmpty(componentName)) {
            String serviceRefKey = StringUtils.getEJBServiceRefKey(serviceRefName, componentName);
            serviceRef = ejbServiceRefTypeMap.get(serviceRefKey);
        }

        if (null == serviceRef) {
            serviceRef = serviceRefTypeMap.get(serviceRefName.trim());
        }

        return serviceRef;

    }

    /**
     * Allow to use the id the attribute.
     */
    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {

        if (nsURI == null) {
            if (VERSION_ATTRIBUTE_NAME.equals(localName)) {
                version = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (WEBSERVICE_DESCRIPTION_ELEMENT_NAME.equals(localName)) {
            WebserviceDescriptionType webserviceDescriptionType = new WebserviceDescriptionType();
            parser.parse(webserviceDescriptionType);
            String webserviceDescriptionName = webserviceDescriptionType.getWebserviceDescriptionName();
            if (StringUtils.isEmpty(webserviceDescriptionName)) {
                throw new ParseException(parser.requiredAttributeMissing("webservice-description-name"));
            }
            addWebserviceType(webserviceDescriptionType);
            return true;
        } else if (WEBSERVICE_ENDPOINT_ELEMENT_NAME.equals(localName)) {
            WebserviceEndpointType webserviceEndpointType = new WebserviceEndpointType();
            parser.parse(webserviceEndpointType);
            String portComponentName = webserviceEndpointType.getPortComponentName();
            if (StringUtils.isEmpty(portComponentName)) {
                throw new ParseException(parser.requiredAttributeMissing("port-component-name"));
            }
            addServiceEndpointType(webserviceEndpointType);
            return true;
        } else if (HTTP_PUBLISHING_ELEMENT_NAME.equals(localName)) {
            httpPublishingType = new HttpPublishingType();
            parser.parse(httpPublishingType);
            return true;
        } else if (SERVICE_REF_ELEMENT_NAME.equals(localName)) {
            ServiceRefType serviceRefType = new ServiceRefType();
            parser.parse(serviceRefType);
            String serviceRefName = serviceRefType.getName();
            if (StringUtils.isEmpty(serviceRefName)) {
                throw new ParseException(parser.requiredAttributeMissing("name"));
            }
            addServiceRefType(serviceRefType);
            return true;
        } else if (WEBSERVICE_ENDPOINT_PROPERTIES_ELEMENT_NAME.equals(localName)) {
            webserviceEndpointProperties = new WebserviceEndpointPropertiesType();
            parser.parse(webserviceEndpointProperties);
            return true;
        }

        return false;
    }

    private void addWebserviceType(WebserviceDescriptionType webserviceType) {
        String serviceName = webserviceType.getWebserviceDescriptionName();

        if (!StringUtils.isEmpty(serviceName)) {
            webserviceDescriptionTypeMap.put(serviceName.trim(), webserviceType);
        }
    }

    private void addServiceEndpointType(WebserviceEndpointType serviceEnpointType) {
        String portComponentName = serviceEnpointType.getPortComponentName();

        if (!StringUtils.isEmpty(portComponentName)) {
            webserviceEndpointTypeMap.put(portComponentName.trim(), serviceEnpointType);
        }
    }

    private void addServiceRefType(ServiceRefType serviceRefType) {
        String serviceRefName = serviceRefType.getName();
        if (StringUtils.isEmpty(serviceRefName)) {
            return;
        }

        String componentName = serviceRefType.getComponentName();
        if (StringUtils.isEmpty(componentName)) {
            serviceRefTypeMap.put(serviceRefName.trim(), serviceRefType);
        } else {
            String serviceRefKey = StringUtils.getEJBServiceRefKey(serviceRefName, componentName);
            ejbServiceRefTypeMap.put(serviceRefKey, serviceRefType);
        }

    }

    @Override
    protected String toTracingSafeString() {
        return "webservices-bnd";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describeIfSet(VERSION_ATTRIBUTE_NAME, version);
        diag.describe(WEBSERVICE_ENDPOINT_PROPERTIES_ELEMENT_NAME, webserviceEndpointProperties);

        diag.append("[" + WEBSERVICE_DESCRIPTION_ELEMENT_NAME + "<");
        if (null != webserviceDescriptionTypeMap) {
            String prefix = "";
            for (WebserviceDescriptionType webserviceType : webserviceDescriptionTypeMap.values()) {
                diag.append(prefix);
                webserviceType.describe(diag);
                prefix = ",";
            }
        }
        diag.append(">]");

        diag.append("[" + WEBSERVICE_ENDPOINT_ELEMENT_NAME + "<");
        if (null != webserviceEndpointTypeMap) {
            String prefix = "";
            for (WebserviceEndpointType serviceEndpointType : webserviceEndpointTypeMap.values()) {
                diag.append(prefix);
                serviceEndpointType.describe(diag);
                prefix = ",";
            }
        }
        diag.append(">]");

        diag.append("[" + HTTP_PUBLISHING_ELEMENT_NAME + "<");
        if (null != this.httpPublishingType) {
            this.httpPublishingType.describe(diag);
        }
        diag.append(">]");

        diag.append("[" + SERVICE_REF_ELEMENT_NAME + "<");
        if (null != serviceRefTypeMap) {
            String prefix = "";
            for (ServiceRefType serviceRefType : serviceRefTypeMap.values()) {
                diag.append(prefix);
                serviceRefType.describe(diag);
                prefix = ",";
            }
        }
        diag.append(">]");
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        this.idMap = parser.idMap;
        super.finish(parser);
    }

    //ONLY used by WsClientBinding
    public static WebservicesBndType createWebServicesBndType(String path, StringType version, Map<String, ServiceRefType> serviceRefMap) {
        WebservicesBndType wsbnd = new WebservicesBndType(path);
        wsbnd.version = version;
        wsbnd.serviceRefTypeMap = serviceRefMap;
        wsbnd.idMap = new ComponentIDMap();
        return wsbnd;
    }

}
