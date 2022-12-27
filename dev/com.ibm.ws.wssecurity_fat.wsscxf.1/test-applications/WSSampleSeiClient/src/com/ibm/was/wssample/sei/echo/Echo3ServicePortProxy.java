/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.was.wssample.sei.echo;

import java.net.URL;
import java.util.concurrent.Future;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

public class Echo3ServicePortProxy implements EchoProxyInterface {

    protected Descriptor _descriptor;

    public class Descriptor {
        private com.ibm.was.wssample.sei.echo.Echo3Service _service = null;
        private com.ibm.was.wssample.sei.echo.EchoServicePortType _proxy = null;
        private Dispatch<Source> _dispatch = null;
        private boolean _useJNDIOnly = false;

        public Descriptor() {
            init();
        }

        // Newly added to allow the EcoService to be added
        public Descriptor(Echo3Service echo3Service) {
            init(echo3Service);
        }

        public Descriptor(URL wsdlLocation, QName serviceName) {
            _service = new com.ibm.was.wssample.sei.echo.Echo3Service(wsdlLocation, serviceName);
            initCommon();
        }

        public void init() {
            _service = null;
            _proxy = null;
            _dispatch = null;
            try {
                InitialContext ctx = new InitialContext();
                _service = (com.ibm.was.wssample.sei.echo.Echo3Service) ctx.lookup("java:comp/env/service/Echo3Service");
            } catch (NamingException e) {
                if ("true".equalsIgnoreCase(System.getProperty("DEBUG_PROXY"))) {
                    System.out.println("JNDI lookup failure: javax.naming.NamingException: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            if (_service == null && !_useJNDIOnly)
                _service = new com.ibm.was.wssample.sei.echo.Echo3Service();
            initCommon();
        }

        // Newly added to allow the EcoService to be added
        public void init(Echo3Service echo3Service) {
            _service = echo3Service;
            _proxy = null;
            _dispatch = null;
            if (_service == null) {
                try {
                    InitialContext ctx = new InitialContext();
                    _service = (com.ibm.was.wssample.sei.echo.Echo3Service) ctx.lookup("java:comp/env/service/Echo3Service");
                } catch (NamingException e) {
                    if ("true".equalsIgnoreCase(System.getProperty("DEBUG_PROXY"))) {
                        System.out.println("JNDI lookup failure: javax.naming.NamingException: " + e.getMessage());
                        e.printStackTrace(System.out);
                    }
                }
            }

            if (_service == null && !_useJNDIOnly)
                _service = new com.ibm.was.wssample.sei.echo.Echo3Service();
            initCommon();
        }

        private void initCommon() {
            _proxy = _service.getEcho3ServicePort();
        }

        public com.ibm.was.wssample.sei.echo.EchoServicePortType getProxy() {
            return _proxy;
        }

        public void useJNDIOnly(boolean useJNDIOnly) {
            _useJNDIOnly = useJNDIOnly;
            init();
        }

        public Dispatch<Source> getDispatch() {
            if (_dispatch == null) {
                QName portQName = new QName("http://com/ibm/was/wssample/sei/echo/", "Echo3ServicePort");
                _dispatch = _service.createDispatch(portQName, Source.class, Service.Mode.MESSAGE);

                String proxyEndpointUrl = getEndpoint();
                BindingProvider bp = _dispatch;
                String dispatchEndpointUrl = (String) bp.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                if (!dispatchEndpointUrl.equals(proxyEndpointUrl))
                    bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, proxyEndpointUrl);
            }
            return _dispatch;
        }

        public String getEndpoint() {
            BindingProvider bp = (BindingProvider) _proxy;
            return (String) bp.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        }

        public void setEndpoint(String endpointUrl) {
            System.out.println("Echo3Service in Proxy is:" + _service);
            BindingProvider bp = (BindingProvider) _proxy;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

            if (_dispatch != null) {
                bp = _dispatch;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            }
        }

        public void setMTOMEnabled(boolean enable) {
            SOAPBinding binding = (SOAPBinding) ((BindingProvider) _proxy).getBinding();
            binding.setMTOMEnabled(enable);
        }
    }

    public Echo3ServicePortProxy() {
        _descriptor = new Descriptor();
        _descriptor.setMTOMEnabled(false);
    }

    // Newly added to allow the EcoService to be added
    public Echo3ServicePortProxy(Service echo3Service) {
        _descriptor = new Descriptor((Echo3Service) echo3Service);
        _descriptor.setMTOMEnabled(false);
    }

    public Echo3ServicePortProxy(URL wsdlLocation, QName serviceName) {
        _descriptor = new Descriptor(wsdlLocation, serviceName);
        _descriptor.setMTOMEnabled(false);
    }

    public Descriptor _getDescriptor() {
        return _descriptor;
    }

    public Response<EchoStringResponse> echoOperationAsync(EchoStringInput parameter) {
        return _getDescriptor().getProxy().echoOperationAsync(parameter);
    }

    public Future<?> echoOperationAsync(EchoStringInput parameter, AsyncHandler<EchoStringResponse> asyncHandler) {
        return _getDescriptor().getProxy().echoOperationAsync(parameter, asyncHandler);
    }

    public EchoStringResponse echoOperation(EchoStringInput parameter) {
        return _getDescriptor().getProxy().echoOperation(parameter);
    }

    @Override
    public String getEchoResponse(Object object, String endpointURL, String input) throws Exception {
        Echo3ServicePortProxy proxy = (Echo3ServicePortProxy) object;
        proxy._getDescriptor().setEndpoint(endpointURL);

        // Build the input object
        com.ibm.was.wssample.sei.echo.EchoStringInput echoParm = new com.ibm.was.wssample.sei.echo.ObjectFactory().createEchoStringInput();
        echoParm.setEchoInput(input);
        System.out.println(">> CLIENT: SOAP11 Echo to " + endpointURL);

        // Call the service
        String retval = proxy.echoOperation(echoParm).getEchoResponse();
        return retval;
    }
}