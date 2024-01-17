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

package org.apache.cxf.jaxws;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.saaj.SAAJFactoryResolver;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;

public class JaxWsClientProxy extends org.apache.cxf.frontend.ClientProxy implements
    BindingProvider {

    public static final String THREAD_LOCAL_REQUEST_CONTEXT = "thread.local.request.context";

    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsClientProxy.class);

    private Binding binding;
    private EndpointReferenceBuilder builder;

    public JaxWsClientProxy(Client c, Binding b) {
        super(c);
        this.binding = b;
        setupEndpointAddressContext(getClient().getEndpoint());
        this.builder = new EndpointReferenceBuilder((JaxWsEndpointImpl)getClient().getEndpoint());
    }
    public void close() throws IOException {
        super.close();
        binding = null;
        builder = null;
    }

    private void setupEndpointAddressContext(Endpoint endpoint) {
        // NOTE for jms transport the address would be null
        if (null != endpoint && null != endpoint.getEndpointInfo().getAddress()) {
            getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                      endpoint.getEndpointInfo().getAddress());
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }

        Endpoint endpoint = getClient().getEndpoint();
        String address = endpoint.getEndpointInfo().getAddress();
        MethodDispatcher dispatcher = (MethodDispatcher)endpoint.getService().get(
                                                                                  MethodDispatcher.class
                                                                                      .getName());
        Object[] params = args;
        if (null == params) {
            params = new Object[0];
        }


        try {
            if (method.getDeclaringClass().equals(BindingProvider.class)
                || method.getDeclaringClass().equals(Object.class)
                || method.getDeclaringClass().equals(Closeable.class)
                || method.getDeclaringClass().equals(AutoCloseable.class)) {
                return method.invoke(this, params);
            } else if (method.getDeclaringClass().isInstance(client)) {
                return method.invoke(client, params);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        BindingOperationInfo oi = dispatcher.getBindingOperation(method, endpoint);
        if (oi == null) {
            Message msg = new Message("NO_BINDING_OPERATION_INFO", LOG, method.getName());
            throw new WebServiceException(msg.toString());
        }

        client.getRequestContext().put(Method.class.getName(), method);
        boolean isAsync = isAsync(method);

        Object result = null;
        try {
            if (isAsync) {
                result = invokeAsync(method, oi, params);
            } else {
                result = invokeSync(method, oi, params);
            }
        } catch (WebServiceException wex) {
            throw wex;
        } catch (Exception ex) {
            throw mapException(method, oi, ex);
        } finally {
            if (addressChanged(address)) {
                setupEndpointAddressContext(getClient().getEndpoint());
            }
        }

        Map<String, Object> respContext = client.getResponseContext();
        Map<String, Scope> scopes = CastUtils.cast((Map<?, ?>)respContext.get(WrappedMessageContext.SCOPES));
        if (scopes != null) {
            for (Map.Entry<String, Scope> scope : scopes.entrySet()) {
                if (scope.getValue() == Scope.HANDLER) {
                    respContext.remove(scope.getKey());
                }
            }
        }
        return adjustObject(result);
    }
    Exception mapException(Method method, BindingOperationInfo boi, Exception ex) {
        if (method != null) {
            for (Class<?> excls : method.getExceptionTypes()) {
                if (excls.isInstance(ex)) {
                    return ex;
                }
            }
        } else if (boi != null) {
            for (BindingFaultInfo fi : boi.getFaults()) {
                Class<?> c = fi.getFaultInfo().getProperty(Class.class.getName(), Class.class);
                if (c != null && c.isInstance(ex)) {
                    return ex;
                }
            }
            if (ex instanceof IOException) {
                return ex;
            }
        }

        if (ex instanceof Fault && ex.getCause() instanceof IOException) {
            return new WebServiceException(ex.getMessage(), ex.getCause());
        }
        if (getBinding() instanceof HTTPBinding) {
            HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
            exception.initCause(ex);
            return exception;
        } else if (getBinding() instanceof SOAPBinding) {
            try {
                SOAPFault soapFault = createSoapFault((SOAPBinding)getBinding(), ex);
                if (soapFault == null) {
                    throw new WebServiceException(ex);
                }
                SOAPFaultException exception = new SOAPFaultException(soapFault);
                if (ex instanceof Fault && ex.getCause() != null) {
                    exception.initCause(ex.getCause());
                } else {
                    exception.initCause(ex);
                }
                return exception;
            } catch (SOAPException e) {
                return new WebServiceException(ex);
            }
        }
        return new WebServiceException(ex);
    }

    boolean isAsync(Method m) {
        return m.getName().endsWith("Async")
            && (Future.class.equals(m.getReturnType())
                || Response.class.equals(m.getReturnType()));
    }

    static SOAPFault createSoapFault(SOAPBinding binding, Exception ex) throws SOAPException {
        SOAPFault soapFault;
        try {
            soapFault = binding.getSOAPFactory().createFault();
        } catch (Throwable t) {
            //probably an old version of saaj or something that is not allowing createFault
            //method to work.  Try the saaj 1.2 method of doing this.
            try {
                soapFault = binding.getMessageFactory().createMessage()
                    .getSOAPPart().getEnvelope().getBody().addFault();
            } catch (Throwable t2) {
                //still didn't work, we'll just throw what we have
                return null;
            }
        }

        if (ex instanceof SoapFault) {
            if (!soapFault.getNamespaceURI().equals(((SoapFault)ex).getFaultCode().getNamespaceURI())
                && SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE
                    .equals(((SoapFault)ex).getFaultCode().getNamespaceURI())) {
                //change to 1.1
                try {
                    soapFault = SAAJFactoryResolver.createSOAPFactory(null).createFault();
                } catch (Throwable t) {
                    //ignore
                }
            }
            final boolean isSoap11 = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(soapFault.getNamespaceURI());

            if (StringUtils.isEmpty(((SoapFault)ex).getLang())) {
                soapFault.setFaultString(((SoapFault)ex).getReason());
            } else {
                soapFault.setFaultString(((SoapFault)ex).getReason(), stringToLocale(((SoapFault)ex).getLang()));
            }

            SAAJUtils.setFaultCode(soapFault, ((SoapFault)ex).getFaultCode());
            String role = ((SoapFault)ex).getRole();
            if (role != null) {
                soapFault.setFaultActor(role);
            }
            if (((SoapFault)ex).getSubCodes() != null && !isSoap11) {
                // set the subcode only if it is supported (e.g, 1.2)
                for (QName fsc : ((SoapFault)ex).getSubCodes()) {
                    soapFault.appendFaultSubcode(fsc);
                }
            }

            if (((SoapFault)ex).hasDetails()) {
                Node nd = soapFault.getOwnerDocument().importNode(((SoapFault)ex).getDetail(),
                                                                  true);
                nd = nd.getFirstChild();
                soapFault.addDetail();
                while (nd != null) {
                    Node next = nd.getNextSibling();
                    soapFault.getDetail().appendChild(nd);
                    nd = next;
                }
            }
        } else {
            String msg = ex.getMessage();
            if (msg != null) {
                soapFault.setFaultString(msg);
            }
        }
        return soapFault;
    }

    private static Locale stringToLocale(String locale) {
        // use the IETF BCP 47 delimiter but accept the toString delimiter for cxf 2.7.x
        String[] parts = locale.split("-", 0);
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }

    private boolean addressChanged(String address) {
        return !(address == null
                 || getClient().getEndpoint().getEndpointInfo() == null
                 || address.equals(getClient().getEndpoint().getEndpointInfo().getAddress()));
    }

    @SuppressWarnings("unchecked")
    private Object invokeAsync(Method method, final BindingOperationInfo oi, Object[] params) throws Exception {

        client.setExecutor(getClient().getEndpoint().getExecutor());

        AsyncHandler<Object> handler;
        if (params.length > 0 && params[params.length - 1] instanceof AsyncHandler) {
            handler = (AsyncHandler<Object>)params[params.length - 1];
            Object[] newParams = new Object[params.length - 1];
            System.arraycopy(params, 0, newParams, 0, newParams.length);
            params = newParams;
        } else {
            handler = null;
        }
        ClientCallback callback = new JaxwsClientCallback<Object>(handler, this) {
            @Override
            protected Throwable mapThrowable(Throwable t) {
                if (t instanceof Exception) {
                    t = mapException(null, oi, (Exception)t);
                }
                return t;
            }
        };

        Response<Object> ret = new JaxwsResponseCallback<>(callback);
        client.invoke(callback, oi, params);
        return ret;
    }

    public Map<String, Object> getRequestContext() {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }
        return new WrappedMessageContext(this.getClient().getRequestContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Map<String, Object> getResponseContext() {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }
        return new WrappedMessageContext(this.getClient().getResponseContext(),
                                                          null,
                                                          Scope.APPLICATION);
    }

    public Binding getBinding() {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }
        return binding;
    }

    public EndpointReference getEndpointReference() {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }
        return builder.getEndpointReference();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        if (client == null) {
            throw new IllegalStateException("The client has been closed.");
        }
        return builder.getEndpointReference(clazz);
    }
}
