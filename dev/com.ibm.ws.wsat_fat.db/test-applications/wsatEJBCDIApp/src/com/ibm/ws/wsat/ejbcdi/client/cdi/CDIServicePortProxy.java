/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.client.cdi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

public class CDIServicePortProxy{

    protected Descriptor _descriptor;

    public class Descriptor {
        private com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServiceService _service = null;
        private com.ibm.ws.wsat.ejbcdi.client.cdi.CDIService _proxy = null;
        private Dispatch<Source> _dispatch = null;
        private boolean _useJNDIOnly = false;

        public Descriptor() {
            init();
        }

        public Descriptor(URL wsdlLocation, QName serviceName) {
            _service = new com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServiceService(wsdlLocation, serviceName);
            initCommon();
        }

        public void init() {
            _service = null;
            _proxy = null;
            _dispatch = null;
            try
            {
                InitialContext ctx = new InitialContext();
                _service = (com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServiceService)ctx.lookup("java:comp/env/service/CDIServiceService");
            }
            catch (NamingException e)
            {
                if ("true".equalsIgnoreCase(System.getProperty("DEBUG_PROXY"))) {
                    System.out.println("JNDI lookup failure: javax.naming.NamingException: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            }

            if (_service == null && !_useJNDIOnly)
                _service = new com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServiceService();
            initCommon();
        }

        private void initCommon() {
            _proxy = _service.getCDIServicePort();
        }

        public com.ibm.ws.wsat.ejbcdi.client.cdi.CDIService getProxy() {
            return _proxy;
        }

        public void useJNDIOnly(boolean useJNDIOnly) {
            _useJNDIOnly = useJNDIOnly;
            init();
        }

        public Dispatch<Source> getDispatch() {
            if (_dispatch == null ) {
                QName portQName = new QName("", "CDIServicePort");
                _dispatch = _service.createDispatch(portQName, Source.class, Service.Mode.MESSAGE);

                String proxyEndpointUrl = getEndpoint();
                BindingProvider bp = (BindingProvider) _dispatch;
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

            if (_dispatch != null ) {
                bp = (BindingProvider) _dispatch;
                bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            }
        }

        public void setMTOMEnabled(boolean enable) {
            SOAPBinding binding = (SOAPBinding) ((BindingProvider) _proxy).getBinding();
            binding.setMTOMEnabled(enable);
        }
    }

    public CDIServicePortProxy() {
        _descriptor = new Descriptor();
        _descriptor.setMTOMEnabled(false);
    }

    public CDIServicePortProxy(URL wsdlLocation, QName serviceName) {
        _descriptor = new Descriptor(wsdlLocation, serviceName);
        _descriptor.setMTOMEnabled(false);
    }

    public Descriptor _getDescriptor() {
        return _descriptor;
    }

    public String testCDISayHelloToOtherWithMandatory(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOtherWithMandatory(arg0,arg1);
    }

    public String testCDISayHelloToOtherWithNever(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOtherWithNever(arg0,arg1);
    }

    public String testCDISayHelloToOtherWithNotSupported(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOtherWithNotSupported(arg0,arg1);
    }

    public String testCDISayHelloToOtherWithSupports(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOtherWithSupports(arg0,arg1);
    }

    public String testCDISayHelloToOther(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOther(arg0,arg1);
    }

    public String testCDISayHelloToOtherWithRequiresNew(String arg0, String arg1) throws NamingException_Exception, SQLException_Exception {
        return _getDescriptor().getProxy().testCDISayHelloToOtherWithRequiresNew(arg0,arg1);
    }

}
