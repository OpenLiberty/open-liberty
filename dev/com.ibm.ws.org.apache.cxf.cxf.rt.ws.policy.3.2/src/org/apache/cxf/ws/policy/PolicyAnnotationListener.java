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

package org.apache.cxf.ws.policy;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.neethi.Constants;

/**
 *
 */
public class PolicyAnnotationListener implements FactoryBeanListener {
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyAnnotationListener.class);
    private static final String EXTRA_POLICIES = PolicyAnnotationListener.class.getName() + ".EXTRA_POLICIES";

    private Bus bus;

    public PolicyAnnotationListener(Bus bus) {
        this.bus = bus;
    }

    public PolicyAnnotationListener() {
    }

    public void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object... args) {
        switch (ev) {
        case INTERFACE_CREATED: {
            InterfaceInfo ii = (InterfaceInfo)args[0];
            Class<?> cls = (Class<?>)args[1];
            addPolicies(factory, ii, cls);
            break;
        }

        case ENDPOINT_SELECTED: {
            Class<?> cls = (Class<?>)args[2];
            Class<?> implCls = (Class<?>)args[3];
            Endpoint ep = (Endpoint)args[1];
            if (ep.getEndpointInfo().getInterface() != null) {
                addPolicies(factory, ep, cls);

                // this will allow us to support annotations in Implementations.
                // The call has been moved here so that the ServiceInfo
                // policy stuff is loaded before jaxws factory calls the PolicyEngineImpl
                addEndpointImplPolicies(factory, ep, implCls);
            }
            break;
        }

        case INTERFACE_OPERATION_BOUND: {
            OperationInfo inf = (OperationInfo)args[0];
            Method m = (Method)args[1];
            addPolicies(factory, inf, m);
            break;
        }

        case BINDING_OPERATION_CREATED:
            BindingOperationInfo boi = (BindingOperationInfo) args[1];
            Method m = (Method)args[2];
            addPolicies(factory, boi.getOperationInfo(), m);
            break;

        default:
            //ignore
        }
    }

    private void addPolicies(AbstractServiceFactoryBean factory, OperationInfo inf, Method m) {
        if (m == null) {
            return;
        }

        Policy p = m.getAnnotation(Policy.class);
        Policies ps = m.getAnnotation(Policies.class);
        if (p != null || ps != null) {
            List<Policy> list = new ArrayList<>();
            if (p != null) {
                list.add(p);
            }
            if (ps != null) {
                Collections.addAll(list, ps.value());
            }
            ListIterator<Policy> it = list.listIterator();
            while (it.hasNext()) {
                p = it.next();
                Policy.Placement place = p.placement();
                if (place == Policy.Placement.DEFAULT) {
                    place = Policy.Placement.BINDING_OPERATION;
                }
                ServiceInfo service = inf.getInterface().getService();
                Class<?> cls = m.getDeclaringClass();
                switch (place) {
                case PORT_TYPE_OPERATION:
                    addPolicy(inf, service, p, cls,
                              inf.getName().getLocalPart() + "PortTypeOpPolicy");
                    it.remove();
                    break;
                case PORT_TYPE_OPERATION_INPUT:
                    addPolicy(inf.getInput(), service, p, cls,
                              inf.getName().getLocalPart() + "PortTypeOpInputPolicy");
                    it.remove();
                    break;
                case PORT_TYPE_OPERATION_OUTPUT:
                    addPolicy(inf.getOutput(), service, p, cls,
                              inf.getName().getLocalPart() + "PortTypeOpOutputPolicy");
                    it.remove();
                    break;
                case PORT_TYPE_OPERATION_FAULT: {
                    for (FaultInfo f : inf.getFaults()) {
                        if (p.faultClass().equals(f.getProperty(Class.class.getName()))) {
                            addPolicy(f, service, p, cls,
                                      f.getName().getLocalPart() + "PortTypeOpFaultPolicy");
                            it.remove();
                        }
                    }
                    break;
                }
                default:
                    //nothing
                }
            }

            if (!list.isEmpty()) {
                List<Policy> stuff = CastUtils.cast((List<?>)inf.getProperty(EXTRA_POLICIES));
                if (stuff != null) {
                    for (Policy p2 : list) {
                        if (!stuff.contains(p2)) {
                            stuff.add(p2);
                        }
                    }
                } else {
                    inf.setProperty(EXTRA_POLICIES, list);
                }
            }
        }
    }

    private void addPolicies(AbstractServiceFactoryBean factory, Endpoint ep, Class<?> cls) {
        List<Policy> list = CastUtils.cast((List<?>)ep.getEndpointInfo()
                                           .getInterface().removeProperty(EXTRA_POLICIES));
        if (list != null) {
            addPolicies(factory, ep, cls, list, Policy.Placement.BINDING);
        }

        ServiceInfo service = ep.getService().getServiceInfos().get(0);
        for (BindingOperationInfo binfo : ep.getBinding().getBindingInfo().getOperations()) {
            List<Policy> later = CastUtils.cast((List<?>)binfo.getOperationInfo()
                                                           .removeProperty(EXTRA_POLICIES));
            if (later != null) {
                for (Policy p : later) {
                    switch (p.placement()) {
                    case DEFAULT:
                    case BINDING_OPERATION:
                        addPolicy(binfo, service, p, cls,
                                  binfo.getName().getLocalPart() + "BindingOpPolicy");
                        break;
                    case BINDING_OPERATION_INPUT:
                        addPolicy(binfo.getInput(), service, p, cls,
                                  binfo.getName().getLocalPart() + "BindingOpInputPolicy");
                        break;
                    case BINDING_OPERATION_OUTPUT:
                        addPolicy(binfo.getOutput(), service, p, cls,
                                  binfo.getName().getLocalPart() + "BindingOpOutputPolicy");
                        break;
                    case BINDING_OPERATION_FAULT: {
                        for (BindingFaultInfo f : binfo.getFaults()) {
                            if (p.faultClass().equals(f.getFaultInfo()
                                                        .getProperty(Class.class.getName()))) {
                                addPolicy(f, service, p, cls,
                                          f.getFaultInfo().getName().getLocalPart() + "BindingOpFaultPolicy");
                            }
                        }
                        break;
                    }
                    default:
                        //nothing
                    }
                }
            }
        }
    }

    private void addEndpointImplPolicies(AbstractServiceFactoryBean factory, Endpoint endpoint, Class<?> cls) {
        List<Policy> list = CastUtils.cast((List<?>)endpoint.getEndpointInfo()
                                           .getInterface().removeProperty(EXTRA_POLICIES));
        if (list != null) {
            addPolicies(factory, endpoint, cls, list, Policy.Placement.BINDING);
        }
        if (cls == null) {
            return;
        }

        Policy p = cls.getAnnotation(Policy.class);
        Policies ps = cls.getAnnotation(Policies.class);
        if (p != null || ps != null) {
            list = new ArrayList<>();
            if (p != null) {
                list.add(p);
            }
            if (ps != null) {
                Collections.addAll(list, ps.value());
            }
            addPolicies(factory, endpoint, cls, list, Policy.Placement.SERVICE);
        }
    }

    private void addPolicies(AbstractServiceFactoryBean factory, Endpoint endpoint, Class<?> cls,
                             List<Policy> list, Policy.Placement defaultPlace) {
        ListIterator<Policy> it = list.listIterator();
        InterfaceInfo inf = endpoint.getEndpointInfo().getInterface();
        BindingInfo binf = endpoint.getBinding().getBindingInfo();
        ServiceInfo si = endpoint.getService().getServiceInfos().get(0);
        while (it.hasNext()) {
            Policy p = it.next();
            Policy.Placement place = p.placement();
            if (place == Policy.Placement.DEFAULT) {
                place = defaultPlace;
            }
            switch (place) {
            case PORT_TYPE: {
                addPolicy(inf, si, p, cls,
                          inf.getName().getLocalPart() + "PortTypePolicy");
                it.remove();
                break;
            }
            case BINDING: {
                addPolicy(binf, si, p, cls,
                          binf.getName().getLocalPart() + "BindingPolicy");
                it.remove();
                break;
            }
            case SERVICE: {
                addPolicy(si, si, p, cls,
                          si.getName().getLocalPart() + "ServicePolicy");
                it.remove();
                break;
            }
            case SERVICE_PORT: {
                addPolicy(endpoint.getEndpointInfo(), si, p, cls,
                          endpoint.getEndpointInfo().getName().getLocalPart() + "PortPolicy");
                it.remove();
                break;
            }
            default:
            }

        }
    }


    private void addPolicies(AbstractServiceFactoryBean factory, InterfaceInfo ii, Class<?> cls) {
        if (cls == null) {
            return;
        }
        Policy p = cls.getAnnotation(Policy.class);
        Policies ps = cls.getAnnotation(Policies.class);
        if (p != null || ps != null) {
            List<Policy> list = new ArrayList<>();
            if (p != null) {
                list.add(p);
            }
            if (ps != null) {
                Collections.addAll(list, ps.value());
            }
            ListIterator<Policy> it = list.listIterator();
            while (it.hasNext()) {
                p = it.next();
                Policy.Placement place = p.placement();
                if (place == Policy.Placement.DEFAULT) {
                    place = Policy.Placement.BINDING;
                }
                switch (place) {
                case PORT_TYPE: {
                    addPolicy(ii, ii.getService(), p, cls,
                              ii.getName().getLocalPart() + "PortTypePolicy");
                    it.remove();
                    break;
                }
                case SERVICE: {
                    addPolicy(ii.getService(),
                              ii.getService(),
                              p, cls,
                              ii.getService().getName().getLocalPart() + "ServicePolicy");
                    it.remove();
                    break;
                }
                default:
                }
            }

            if (!list.isEmpty()) {
                List<Policy> stuff = CastUtils.cast((List<?>)ii.getProperty(EXTRA_POLICIES));
                if (stuff != null) {
                    for (Policy p2 : list) {
                        if (!stuff.contains(p2)) {
                            stuff.add(p2);
                        }
                    }
                } else {
                    ii.setProperty(EXTRA_POLICIES, list);
                }
            }
        }
    }

    private void addPolicy(AbstractPropertiesHolder place,
                           ServiceInfo service,
                           Policy p,
                           Class<?> cls,
                           String defName) {
        Element el = addPolicy(service, p, cls, defName);
        if (el != null && !isExistsPolicyReference(place.getExtensors().get(), getPolicyRefURI(el))) {
            UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
            uee.setElement(el);
            uee.setRequired(true);
            uee.setElementType(DOMUtils.getElementQName(el));
            place.addExtensor(uee);
        }
    }
    private Element addPolicy(ServiceInfo service, Policy p, Class<?> cls, String defName) {
        String uri = p.uri();
        String ns = Constants.URI_POLICY_NS;

        if (p.includeInWSDL()) {
            Element element = loadPolicy(uri, defName);
            if (element == null) {
                return null;
            }

            // might have been updated on load policy
            uri = getPolicyId(element);
            ns = element.getNamespaceURI();

            if (service.getDescription() == null && cls != null) {
                service.setDescription(new DescriptionInfo());
                URL u = cls.getResource("/");
                if (u != null) {
                    service.getDescription().setBaseURI(u.toString());
                }
            }

            // if not already added to service add it, otherwise ignore
            // and just create the policy reference.
            if (!isExistsPolicy(service.getDescription().getExtensors().get(), uri)) {
                UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
                uee.setElement(element);
                uee.setRequired(true);
                uee.setElementType(DOMUtils.getElementQName(element));
                service.getDescription().addExtensor(uee);
            }

            uri = "#" + uri;
        }

        Document doc = DOMUtils.getEmptyDocument();
        Element el = doc.createElementNS(ns, "wsp:" + Constants.ELEM_POLICY_REF);
        Attr att = doc.createAttributeNS(null, "URI");
        att.setValue(uri);
        el.setAttributeNodeNS(att);
        return el;
    }

    private String getPolicyId(Element element) {
        return element.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                     PolicyConstants.WSU_ID_ATTR_NAME);
    }
    private String getPolicyRefURI(Element element) {
        return element.getAttributeNS(null, "URI");
    }
    private boolean isExistsPolicy(Object[] exts, String uri) {
        exts = exts == null ? new Object[0] : exts;
        for (Object o : exts) {
            if (o instanceof UnknownExtensibilityElement) {
                UnknownExtensibilityElement uee = (UnknownExtensibilityElement)o;
                String uri2 = getPolicyId(uee.getElement());
                if (uri.equals(uri2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isExistsPolicyReference(Object[] exts, String uri) {
        exts = exts == null ? new Object[0] : exts;
        for (Object o : exts) {
            if (o instanceof UnknownExtensibilityElement) {
                UnknownExtensibilityElement uee = (UnknownExtensibilityElement)o;
                String uri2 = getPolicyRefURI(uee.getElement());
                if (uri.equals(uri2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Element loadPolicy(String uri, String defName) {
        if (!uri.startsWith("#")) {
            return loadRemotePolicy(uri, defName);
        }
        return loadLocalPolicy(uri);
    }

    private Element loadRemotePolicy(String uri, String defName) {
        ExtendedURIResolver resolver = new ExtendedURIResolver();
        // InputSource src = resolver.resolve(uri, "classpath:"); Liberty change: line removed
        // Liberty change: try/catch block below added
        InputSource src = null;
        try {
            src = resolver.resolve(uri, "classpath:");
        } catch(Exception e1) {
            LOG.log(Level.WARNING, e1.getMessage());
        }  //Liberty change: end

        if (null == src) {
            return null;
        }
        XMLStreamReader reader = null;
        try {
            reader = StaxUtils.createXMLStreamReader(src);
            Document doc = StaxUtils.read(reader);
            uri = getPolicyId(doc.getDocumentElement());
            if (StringUtils.isEmpty(uri)) {
                Attr att = doc.createAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                                 "wsu:" + PolicyConstants.WSU_ID_ATTR_NAME);
                att.setNodeValue(defName);
                doc.getDocumentElement().setAttributeNodeNS(att);
            }

            return doc.getDocumentElement();
        } catch (XMLStreamException e) {
            LOG.log(Level.WARNING, e.getMessage());
            return null;
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e) {
                LOG.log(Level.WARNING, e.getMessage());
            }
        }
    }

    private Element loadLocalPolicy(String uri) {
        PolicyBean pb = bus.getExtension(ConfiguredBeanLocator.class)
            .getBeanOfType(uri.substring(1), PolicyBean.class);
        if (null != pb) {
            return pb.getElement();
        }
        return null;
    }
}
