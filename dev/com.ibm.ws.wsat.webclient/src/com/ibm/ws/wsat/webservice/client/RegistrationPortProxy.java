/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.webservice.client;

import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterResponseType;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterType;

public class RegistrationPortProxy {

    private static final TraceComponent tc = Tr.register(
                                                         RegistrationPortProxy.class, "WSATWEBCLIENT", null);

    protected Descriptor _descriptor;

    public class Descriptor {
        private RegistrationService _service = null;
        private RegistrationPortType _proxy = null;
        private Dispatch<Source> _dispatch = null;
        private boolean _useJNDIOnly = false;

        public Descriptor() {
            init();
        }

        public Descriptor(URL wsdlLocation, QName serviceName) {
            if (serviceName != null)
                _service = new RegistrationService(
                                wsdlLocation, serviceName);
            else
                _service = new RegistrationService(wsdlLocation);
            initCommon();
        }

        public void init() {
            _service = null;
            _proxy = null;
            _dispatch = null;
            try {
                InitialContext ctx = new InitialContext();
                _service = (RegistrationService) ctx
                                .lookup("java:comp/env/service/RegistrationService");
            } catch (NamingException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI lookup failure: javax.naming.NamingException: "
                             , e.getMessage());
                }
            }

            if (_service == null && !_useJNDIOnly)
                _service = new RegistrationService();
            initCommon();
        }

        private void initCommon() {
            _proxy = _service.getRegistrationPort();
        }

        public RegistrationPortType getProxy() {
            return _proxy;
        }

        public void useJNDIOnly(boolean useJNDIOnly) {
            _useJNDIOnly = useJNDIOnly;
            init();
        }

        public Dispatch<Source> getDispatch() {
            if (_dispatch == null) {
                QName portQName = new QName("", "RegistrationPort");
                _dispatch = _service.createDispatch(portQName, Source.class,
                                                    Service.Mode.MESSAGE);

                String proxyEndpointUrl = getEndpoint();
                BindingProvider bp = _dispatch;
                String dispatchEndpointUrl = (String) bp.getRequestContext()
                                .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                if (!dispatchEndpointUrl.equals(proxyEndpointUrl))
                    bp.getRequestContext().put(
                                               BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                               proxyEndpointUrl);
            }
            return _dispatch;
        }

        public String getEndpoint() {
            BindingProvider bp = (BindingProvider) _proxy;
            return (String) bp.getRequestContext().get(
                                                       BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        }

        public void setEndpoint(String endpointUrl) {
            BindingProvider bp = (BindingProvider) _proxy;
            bp.getRequestContext().put(
                                       BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

            if (_dispatch != null) {
                bp = _dispatch;
                bp.getRequestContext().put(
                                           BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            }
        }

        public void setMTOMEnabled(boolean enable) {
            SOAPBinding binding = (SOAPBinding) ((BindingProvider) _proxy)
                            .getBinding();
            binding.setMTOMEnabled(enable);
        }
    }

    public RegistrationPortProxy() {
        _descriptor = new Descriptor();
        _descriptor.setMTOMEnabled(false);
    }

    public RegistrationPortProxy(URL wsdlLocation, QName serviceName) {
        _descriptor = new Descriptor(wsdlLocation, serviceName);
        _descriptor.setMTOMEnabled(false);
    }

    public Descriptor _getDescriptor() {
        return _descriptor;
    }

    public RegisterResponseType registerOperation(RegisterType parameters) {
        return _getDescriptor().getProxy().registerOperation(parameters);
    }

}
