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

import java.net.URL;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.RespectBindingFeature;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.metadata.AddressingFeatureInfo;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.MTOMFeatureInfo;
import com.ibm.ws.jaxws.metadata.RespectBindingFeatureInfo;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;

/**
 * Extend the org.apache.cxf.jaxws.support.JaxWsImplementorInfo
 * so that we can leverage our EndpointInfos.
 */
public class LibertyJaxWsImplementorInfo extends JaxWsImplementorInfo {
    private static final TraceComponent tc = Tr.register(LibertyJaxWsImplementorInfo.class);

    private final JaxWsPublisherContext publisherContext;
    private final Class<?> implementorClass;
    private final Class<?> seiClass;
    private final EndpointInfo endpointInfo;

    private String wsdlLocation;
    private QName interfaceName;

    public LibertyJaxWsImplementorInfo(Class<?> implClass, EndpointInfo edpInfo, JaxWsPublisherContext publisherContext) throws ClassNotFoundException {
        super(implClass);
        endpointInfo = edpInfo;
        this.publisherContext = publisherContext;
        implementorClass = implClass;
        String seiClassName = endpointInfo.getServiceEndpointInterface();
        if (null == seiClassName || seiClassName.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "the service enpoint interface class of " + implementorClass + " is undefined.");
            }
            seiClass = null;
        } else {
            seiClass = this.publisherContext.getPublisherModuleInfo().getClassLoader().loadClass(seiClassName);
        }

    }

    @Override
    public String getBindingType() {
        String bindingType = endpointInfo.getProtocolBinding();
        if (StringUtils.isEmpty(bindingType)) {
            return SOAPBinding.SOAP11HTTP_BINDING;
        }

        // MTOM setting should override the one set in protocol binding
        MTOMFeatureInfo mtomFeatureInfo = endpointInfo.getMTOMFeatureInfo();
        if (mtomFeatureInfo != null && !mtomFeatureInfo.isEnabled()) {
            if (bindingType.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)) {
                return SOAPBinding.SOAP11HTTP_BINDING;
            }
            if (bindingType.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING)) {
                return SOAPBinding.SOAP12HTTP_BINDING;
            }
        }

        return bindingType;
    }

    @Override
    public Class<?> getEndpointClass() {
        return seiClass == null ? implementorClass : seiClass;
    }

    @Override
    public QName getEndpointName() {
        return endpointInfo.getWsdlPort();
    }

    @Override
    public Class<?> getImplementorClass() {
        return implementorClass;
    }

    @Override
    public QName getInterfaceName() {
        if (null != interfaceName) {
            return interfaceName;
        }

        String name = null;

        if (seiClass != null) {
            WebService service = seiClass.getAnnotation(WebService.class);
            if (null != service && !StringUtils.isEmpty(service.name())) {
                name = service.name();
            }
        } else {
            WebService service = implementorClass.getAnnotation(WebService.class);
            if (null != service && !StringUtils.isEmpty(service.name())) {
                name = service.name();
            }
        }

        if (name == null) {
            if (seiClass != null) {
                name = seiClass.getSimpleName();
            } else if (implementorClass != null) {
                name = implementorClass.getSimpleName();
            }
        }

        //Note that the targetNameSpace used here should be interface TargetNameSpace which 
        //may be different from implementation TargetNameSpace.
        return interfaceName = new QName(endpointInfo.getInterfaceTragetNameSpaceURL(), name);
    }

    @Override
    public Class<?> getSEIClass() {
        return seiClass;
    }

    @Override
    public Mode getServiceMode() {
        return endpointInfo.getServiceModeInfo().getValue();
    }

    @Override
    public QName getServiceName() {
        return endpointInfo.getWsdlService();
    }

    @Override
    public String getWsdlLocation() {
        if (null != wsdlLocation) {
            return wsdlLocation;
        }

        URL wsdlUrl = JaxWsUtils.resolve(endpointInfo.getWsdlLocation(), publisherContext.getModuleMetaData().getModuleContainer());
        // return wsdlLocation = (null == wsdlUrl ? "" : wsdlUrl.toString());
        return wsdlLocation = (null == wsdlUrl ? endpointInfo.getWsdlLocation() : wsdlUrl.toString());
    }

    public MTOMFeature getMTOMFeature() {
        MTOMFeatureInfo featureInfo = endpointInfo.getMTOMFeatureInfo();
        if (featureInfo != null) {
            return (MTOMFeature) featureInfo.getWebServiceFeature();
        } else {
            if (SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(endpointInfo.getProtocolBinding()) ||
                SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(endpointInfo.getProtocolBinding())) {
                return new MTOMFeature(true);
            }
        }

        return null;
    }

    public AddressingFeature getAddressingFeature() {
        AddressingFeatureInfo featureInfo = endpointInfo.getAddressingFeatureInfo();
        if (null != featureInfo) {
            if (ProviderImpl.isJaxWs22()) {
                try {
                    Object responses = featureInfo.getResponses();
                    java.lang.reflect.Constructor<?> constructor =
                                    AddressingFeature.class.getConstructor(new Class[] {
                                                                                        boolean.class, boolean.class, responses.getClass()
                                    });
                    return (AddressingFeature) constructor.newInstance(featureInfo.isEnabled(), featureInfo.isRequired(), responses);
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "could not use AddressingFeature(boolean, boolean, Responses) construction", e);
                    }
                    return new AddressingFeature(featureInfo.isEnabled(), featureInfo.isRequired());
                }
            } else {
                return new AddressingFeature(featureInfo.isEnabled(), featureInfo.isRequired());
            }
        }
        return null;
    }

    public RespectBindingFeature getRespectBindingFeature() {
        RespectBindingFeatureInfo featureInfo = endpointInfo.getRespectBindingFeatureInfo();

        if (null != featureInfo) {
            return (RespectBindingFeature) featureInfo.getWebServiceFeature();
        }
        return null;
    }

}
