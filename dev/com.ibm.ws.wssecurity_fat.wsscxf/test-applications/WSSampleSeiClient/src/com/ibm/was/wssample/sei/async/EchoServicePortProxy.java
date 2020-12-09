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

package com.ibm.was.wssample.sei.async;

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

import com.ibm.was.wssample.sei.echo.EchoStringInput;
import com.ibm.was.wssample.sei.echo.EchoStringResponse;

public class EchoServicePortProxy {

    protected Descriptor _descriptor;

    public class Descriptor {
        private com.ibm.was.wssample.sei.async.EchoService _service = null;
        private com.ibm.was.wssample.sei.async.EchoServicePortType _proxy = null;
        private Dispatch<Source> _dispatch = null;

        public Descriptor() {
            try {
                InitialContext ctx = new InitialContext();
                _service = (com.ibm.was.wssample.sei.async.EchoService) ctx.lookup("java:comp/env/service/EchoService");
            } catch (NamingException e) {
                if ("true".equalsIgnoreCase(System.getProperty("DEBUG_PROXY"))) {
                    System.out.println("NamingException: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            if (_service == null)
                _service = new com.ibm.was.wssample.sei.async.EchoService();
            initCommon();
        }

        public Descriptor(URL wsdlLocation, QName serviceName) {
            _service = new com.ibm.was.wssample.sei.async.EchoService(wsdlLocation, serviceName);
            initCommon();
        }

        private void initCommon() {
            _proxy = _service.getEchoServicePort();
        }

        public com.ibm.was.wssample.sei.async.EchoServicePortType getProxy() {
            return _proxy;
        }

        public Dispatch<Source> getDispatch() {
            if (_dispatch == null) {
                QName portQName = new QName("http://com/ibm/was/wssample/sei/echo/", "EchoServicePort");
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
            BindingProvider bp = (BindingProvider) _proxy;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

            if (_dispatch != null) {
                bp = _dispatch;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            }
        }
    }

    public EchoServicePortProxy() {
        _descriptor = new Descriptor();
    }

    public EchoServicePortProxy(URL wsdlLocation, QName serviceName) {
        _descriptor = new Descriptor(wsdlLocation, serviceName);
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

}