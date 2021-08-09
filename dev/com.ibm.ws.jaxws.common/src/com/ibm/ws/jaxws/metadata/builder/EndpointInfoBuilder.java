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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoConfigurator.Phase;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.jaxws.utils.StringUtils;
import com.ibm.ws.jaxws.utils.URLUtils;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * The class is responsible for building the EndpointInfo instance for each Web Services endpoints,
 * it will delegate the invocation to all the EndpointInfoConfigurators.
 */
public class EndpointInfoBuilder {
    private static final TraceComponent tc = Tr.register(EndpointInfoBuilder.class);

    private final Map<Phase, Set<EndpointInfoConfigurator>> phaseConfigurators = new HashMap<Phase, Set<EndpointInfoConfigurator>>();

    protected void setEndpointInfoConfigurator(EndpointInfoConfigurator configurator) {
        Phase phase = configurator.getPhase();
        if (!phaseConfigurators.containsKey(phase)) {
            Set<EndpointInfoConfigurator> configurators = new CopyOnWriteArraySet<EndpointInfoConfigurator>();
            configurators.add(configurator);
            phaseConfigurators.put(phase, configurators);
        } else {
            Set<EndpointInfoConfigurator> configurators = phaseConfigurators.get(phase);
            if (configurators == null) {
                configurators = new CopyOnWriteArraySet<EndpointInfoConfigurator>();
                phaseConfigurators.put(phase, configurators);
            }
            configurators.add(configurator);
        }

    }

    protected void unsetEndpointInfoConfigurator(EndpointInfoConfigurator configurator) {
        Phase phase = configurator.getPhase();
        if (phaseConfigurators.containsKey(phase)) {
            Set<EndpointInfoConfigurator> configurators = phaseConfigurators.get(phase);
            if (configurators != null && !configurators.isEmpty()) {
                phaseConfigurators.get(phase).remove(configurator);
            }
        }
    }

    /**
     * build the endpointInfo
     * 
     * @param ctx
     * @return
     * @throws Exception
     */
    public EndpointInfo build(EndpointInfoBuilderContext ctx, String serviceImplBeanClassName, EndpointType endpointType) throws UnableToAdaptException {

        EndpointInfo endpointInfo = new EndpointInfo(serviceImplBeanClassName, endpointType);
        endpointInfo.setBeanName((String) ctx.getContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_BEAN_NAME));
        endpointInfo.setServletName((String) ctx.getContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_SERVLET_NAME));

        invokeConfigurators(ctx, ConfigMethod.PREPARE, endpointInfo);
        invokeConfigurators(ctx, ConfigMethod.CONFIG, endpointInfo);

        validate(ctx, endpointInfo);

        ctx.clearContextEnv();

        return endpointInfo;
    }

    private void invokeConfigurators(EndpointInfoBuilderContext ctx, ConfigMethod configMethod, EndpointInfo endpointInfo) throws UnableToAdaptException {
        for (Phase phase : Phase.values()) {
            Set<EndpointInfoConfigurator> configurators = phaseConfigurators.get(phase);
            if (configurators != null && !configurators.isEmpty()) {
                for (Iterator<EndpointInfoConfigurator> it = configurators.iterator(); it.hasNext();) {
                    EndpointInfoConfigurator configurator = it.next();

                    switch (configMethod) {
                        case PREPARE:
                            configurator.prepare(ctx, endpointInfo);
                            break;
                        case CONFIG:
                            configurator.config(ctx, endpointInfo);
                            break;
                    }
                }
            }
        }
    }

    private enum ConfigMethod {
        PREPARE,
        CONFIG
    }

    protected void validate(EndpointInfoBuilderContext ctx, EndpointInfo endpointInfo) {
        String wsdlLocation = endpointInfo.getWsdlLocation();
        if (!StringUtils.isEmpty(wsdlLocation)) {
            if (URLUtils.isAbsolutePath(wsdlLocation)) {
                //Validate WSDL is not absolute
                Tr.warning(tc, "warn.endpoint.absolute.wsdllocation", endpointInfo.getImplBeanClassName(), wsdlLocation);
            } else {
                //Validate WSDL against bindingType
                String bindingType = endpointInfo.getProtocolBinding();
                String portComponentName = endpointInfo.getPortComponentName();
                validateWsdlAgainstBindingType(ctx, bindingType, wsdlLocation, portComponentName);
            }
        }
    }

    @FFDCIgnore( { IOException.class, ParserConfigurationException.class, SAXException.class })
    private void validateWsdlAgainstBindingType(EndpointInfoBuilderContext ctx, final String bindingType, final String wsdlLocation, final String portComponentName) {

        if (bindingType != null && !bindingType.isEmpty()) {
            URL wsdlUrl = JaxWsUtils.resolve(wsdlLocation, ctx.getContainer());
            if (wsdlUrl != null) {

                InputStream wsdlInputStream = null;
                try {
                    wsdlInputStream = wsdlUrl.openStream();

                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    factory.setFeature("http://xml.org/sax/features/namespaces", true);
                    factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

                    SAXParser parser = factory.newSAXParser();

                    // we only validate soap11http/soap12http mismatch between bindingType and wsdl v1.1 file.
                    // Always bypass for other bindingType(eg: HTTPBinding), or the wsdl file is v2.0.
                    WsdlBindingTypeValidationHandler handler = new WsdlBindingTypeValidationHandler(bindingType, wsdlLocation, portComponentName);

                    parser.parse(wsdlInputStream, handler);

                } catch (IOException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to validate wsdl against BindingType due to IOException: {0}", e);
                    }
                } catch (ParserConfigurationException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to validate wsdl against BindingType due to ParserConfigurationException: {0}", e);
                    }
                } catch (SAXException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to validate wsdl against BindingType due to SAXException: {0}", e);
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
            }
        }
    }
}

class WsdlBindingTypeValidationHandler extends DefaultHandler {
    private static final TraceComponent tc = Tr.register(WsdlBindingTypeValidationHandler.class);

    private final String bindingType;
    private final String wsdlLocation;
    private final String portComponentName;

    //should be always "tns", but sometime there are more than one prefix for the targetNamespace
    private final Set<String> portTypePrefixes = new HashSet<String>(2);

    private boolean processingTargetPortTypeBinding = false;

    private Result result = Result.NOT_FOUND;

    public WsdlBindingTypeValidationHandler(String bindingType, String wsdlLocation, String portComponentName) {
        this.bindingType = bindingType;
        this.wsdlLocation = wsdlLocation;
        this.portComponentName = portComponentName;
    }

    @Override
    public void startDocument() {}

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (!result.equals(Result.PASS)) {
            if ("definitions".equals(localName) && isWsdl11Uri(uri)) { //find the portType prefix
                String tnsValue = attributes.getValue("targetNamespace");
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (!attributes.getQName(i).equals("targetNamespace") && attributes.getValue(i).equals(tnsValue)) {
                        if (attributes.getQName(i).startsWith("xmlns:")) {
                            portTypePrefixes.add(attributes.getQName(i).substring("xmlns:".length()));
                        }
                    }
                }
            } else if ("binding".equals(localName) && isWsdl11Uri(uri)) { //wsdl binding tag
                String portTypeValue = attributes.getValue("type");
                for (String portTypePrefix : portTypePrefixes) {
                    if (portTypeValue.equals(portTypePrefix + ":" + portComponentName)) {
                        processingTargetPortTypeBinding = true; // we are processing one of the target portType's binding
                        break;
                    }
                }
            } else if ("binding".equals(localName) && (isSoap11Uri(uri) || isSoap12Uri(uri)) && processingTargetPortTypeBinding) { //soap binding tag
                String transportValue = attributes.getValue("transport");
                if ((bindingType.equals(SOAPBinding.SOAP11HTTP_BINDING) || bindingType.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING))
                                && isSoap11Uri(uri) && isHttpTransport(transportValue)) { //SOAP11HTTP binding
                    result = Result.PASS;
                } else if ((bindingType.equals(SOAPBinding.SOAP12HTTP_BINDING) || bindingType.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING))
                                && isSoap12Uri(uri) && isHttpTransport(transportValue)) { //SOAP12FTTP binding
                    result = Result.PASS;
                } else {
                    result = Result.ERROR;
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("binding".equals(localName) && isWsdl11Uri(uri)) { //End of processing the target portType's binding
            processingTargetPortTypeBinding = false;
        }
    }

    @Override
    public void endDocument() {
        if (result.equals(Result.ERROR)) { //we are not that strict, NOT_FOUND also pass
            String msg = Tr.formatMessage(tc, "error.endpointinfo.validation.bindingtype.and.wsdl.mismatch", bindingType, portComponentName, wsdlLocation);
            throw new WebServiceException(msg);
        }
    }

    private boolean isWsdl11Uri(String uri) {
        return "http://schemas.xmlsoap.org/wsdl/".equals(uri);
    }

    private boolean isSoap11Uri(String uri) {
        return "http://schemas.xmlsoap.org/wsdl/soap/".equals(uri);
    }

    private boolean isSoap12Uri(String uri) {
        return "http://schemas.xmlsoap.org/wsdl/soap12/".equals(uri);
    }

    private boolean isHttpTransport(String uri) {
        return "http://schemas.xmlsoap.org/soap/http".equals(uri);
    }

    private enum Result {
        NOT_FOUND,
        PASS,
        ERROR
    }

};
