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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ClientImpl.IllegalEmptyResponseException;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.interceptors.MessageModeInInterceptor;
import org.apache.cxf.jaxws.interceptors.MessageModeOutInterceptor;
import org.apache.cxf.jaxws.support.JaxWsClientEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

public class DispatchImpl<T> implements Dispatch<T>, BindingProvider {  // Liberty change: Closeable implementation is removed
    private static final Logger LOG = LogUtils.getL7dLogger(DispatchImpl.class);
    private static final String DISPATCH_NS = "http://cxf.apache.org/jaxws/dispatch";
    private static final String INVOKE_NAME = "Invoke";
    private static final String INVOKE_ONEWAY_NAME = "InvokeOneWay";
    private static final QName INVOKE_QNAME = new QName(DISPATCH_NS, INVOKE_NAME);
    private static final QName INVOKE_ONEWAY_QNAME = new QName(DISPATCH_NS, INVOKE_ONEWAY_NAME);

    private final Binding binding;
    private final EndpointReferenceBuilder builder;

    private final Client client;
    private final Class<T> cl;
    private final JAXBContext context;
    private Message error;
    private Service.Mode mode;

    DispatchImpl(Client client, Service.Mode m, JAXBContext ctx, Class<T> clazz) {
        this.binding = ((JaxWsEndpointImpl)client.getEndpoint()).getJaxwsBinding();
        this.builder = new EndpointReferenceBuilder((JaxWsEndpointImpl)client.getEndpoint());

        this.client = client;
        this.mode = m;
        context = ctx;
        cl = clazz;
        setupEndpointAddressContext(client.getEndpoint());
        addInvokeOperation(false);
        addInvokeOperation(true);
        if (m == Service.Mode.MESSAGE && binding instanceof SOAPBinding) {
            if (DataSource.class.isAssignableFrom(clazz)) {
                error = new Message("DISPATCH_OBJECT_NOT_SUPPORTED", LOG,
                                    "DataSource",
                                    m,
                                    "SOAP/HTTP");
            } else if (m == Service.Mode.MESSAGE) {
                SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
                client.getOutInterceptors().add(saajOut);
                client.getOutInterceptors().
                    add(new MessageModeOutInterceptor(saajOut,
                                                      client.getEndpoint()
                                                          .getBinding().getBindingInfo().getName()));
                client.getInInterceptors().add(new SAAJInInterceptor());
                client.getInInterceptors()
                    .add(new MessageModeInInterceptor(clazz,
                                                      client.getEndpoint()
                                                          .getBinding().getBindingInfo().getName()));
            }
        } else if (m == Service.Mode.PAYLOAD
            && binding instanceof SOAPBinding
            && SOAPMessage.class.isAssignableFrom(clazz)) {
            error = new Message("DISPATCH_OBJECT_NOT_SUPPORTED", LOG,
                                "SOAPMessage",
                                m,
                                "SOAP/HTTP");
        }
    }

    DispatchImpl(Client cl, Service.Mode m, Class<T> clazz) {
        this(cl, m, null, clazz);
    }

    private void addInvokeOperation(boolean oneWay) {
        String name = oneWay ? INVOKE_ONEWAY_NAME : INVOKE_NAME;

        ServiceInfo info = client.getEndpoint().getEndpointInfo().getService();
        OperationInfo opInfo = info.getInterface()
            .addOperation(oneWay ? INVOKE_ONEWAY_QNAME : INVOKE_QNAME);
        MessageInfo mInfo = opInfo.createMessage(new QName(DISPATCH_NS, name + "Request"), Type.INPUT);
        opInfo.setInput(name + "Request", mInfo);
        MessagePartInfo mpi = mInfo.addMessagePart("parameters");
        if (context == null) {
            mpi.setTypeClass(cl);
        }
        mpi.setElement(true);

        if (!oneWay) {
            mInfo = opInfo.createMessage(new QName(DISPATCH_NS, name + "Response"), Type.OUTPUT);
            opInfo.setOutput(name + "Response", mInfo);
            mpi = mInfo.addMessagePart("parameters");
            mpi.setElement(true);
            if (context == null) {
                mpi.setTypeClass(cl);
            }
        }

        for (BindingInfo bind : client.getEndpoint().getEndpointInfo().getService().getBindings()) {
            BindingOperationInfo bo = new BindingOperationInfo(bind, opInfo);
            bind.addOperation(bo);
        }
    }

    private void addInvokeOperation(QName operationName, boolean oneWay) {
        ServiceInfo info = client.getEndpoint().getEndpointInfo().getService();

        OperationInfo invokeOpInfo = info.getInterface()
                       .getOperation(oneWay ? INVOKE_ONEWAY_QNAME : INVOKE_QNAME);

        OperationInfo opInfo = info.getInterface().addOperation(operationName);
        opInfo.setInput(invokeOpInfo.getInputName(), invokeOpInfo.getInput());

        if (!oneWay) {
            opInfo.setOutput(invokeOpInfo.getOutputName(), invokeOpInfo.getOutput());
        }

        for (BindingInfo bind : client.getEndpoint().getEndpointInfo().getService().getBindings()) {
            BindingOperationInfo bo = new BindingOperationInfo(bind, opInfo);
            bind.addOperation(bo);
        }
    }

    public Map<String, Object> getRequestContext() {
        return new WrappedMessageContext(client.getRequestContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Map<String, Object> getResponseContext() {
        return new WrappedMessageContext(client.getResponseContext(),
                                         null,
                                         Scope.APPLICATION);
    }
    public Binding getBinding() {
        return binding;
    }
    public EndpointReference getEndpointReference() {
        return builder.getEndpointReference();
    }
    public <X extends EndpointReference> X getEndpointReference(Class<X> clazz) {
        return builder.getEndpointReference(clazz);
    }

    private void setupEndpointAddressContext(Endpoint endpoint) {
        //NOTE for jms transport the address would be null
        if (null != endpoint
            && null != endpoint.getEndpointInfo().getAddress()) {
            Map<String, Object> requestContext
                = new WrappedMessageContext(client.getRequestContext(),
                                            null,
                                            Scope.APPLICATION);
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           endpoint.getEndpointInfo().getAddress());
        }
    }
    public T invoke(T obj) {
        return invoke(obj, false);
    }

    private void checkError() {
        if (error != null) {
            if (getBinding() instanceof SOAPBinding) {
                SOAPFault soapFault = null;
                try {
                    soapFault = JaxWsClientProxy.createSoapFault((SOAPBinding)getBinding(),
                                                                 new Exception(error.toString()));
                } catch (SOAPException e) {
                    //ignore
                }
                if (soapFault != null) {
                    throw new SOAPFaultException(soapFault);
                }
            } else if (getBinding() instanceof HTTPBinding) {
                HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
                exception.initCause(new Exception(error.toString()));
                throw exception;
            }
            throw new WebServiceException(error.toString());
        }
    }
    private RuntimeException mapException(Exception ex) {
        if (ex instanceof Fault && ex.getCause() instanceof IOException) {
            throw new WebServiceException(ex.getMessage(), ex.getCause());
        }

        if (getBinding() instanceof HTTPBinding) {
            HTTPException exception = new HTTPException(HttpURLConnection.HTTP_INTERNAL_ERROR);
            exception.initCause(ex);
            return exception;
        } else if (getBinding() instanceof SOAPBinding) {
            SOAPFault soapFault = null;
            try {
                soapFault = JaxWsClientProxy.createSoapFault((SOAPBinding)getBinding(), ex);
            } catch (SOAPException e) {
                //ignore
            }
            if (soapFault == null) {
                return new WebServiceException(ex);
            }

            SOAPFaultException exception = new SOAPFaultException(soapFault);
            exception.initCause(ex);
            return exception;
        }
        return new WebServiceException(ex);
    }

    @SuppressWarnings("unchecked")
    public T invoke(T obj, boolean isOneWay) {
        StaxSource createdSource = null;  // Liberty change: added line
        checkError();
        try {
            if (obj instanceof SOAPMessage) {
                SOAPMessage msg = (SOAPMessage)obj;
                if (msg.countAttachments() > 0) {
                    client.getRequestContext().put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
                }
            } else if (context != null) {
                Boolean unwrapProperty = obj instanceof JAXBElement ? Boolean.FALSE : Boolean.TRUE;
                getRequestContext().put("unwrap.jaxb.element", unwrapProperty);
            }
            QName opName = (QName)getRequestContext().get(MessageContext.WSDL_OPERATION);
            boolean findDispatchOp = Boolean.TRUE.equals(getRequestContext().get("find.dispatch.operation")); // Liberty change: added line
            // boolean hasOpName; Liberty change: removed line
            if (opName == null) {
                opName = isOneWay ? INVOKE_ONEWAY_QNAME : INVOKE_QNAME;
            } else {
                BindingOperationInfo bop = client.getEndpoint().getBinding()
                                            .getBindingInfo().getOperation(opName);
                if (bop == null) {
                    addInvokeOperation(opName, isOneWay);
                }
            }
/*          Liberty change: removed 7 lines below
            Holder<T> holder = new Holder<>(obj);
            opName = calculateOpName(holder, opName, hasOpName);
            Object[] ret = client.invokeWrapped(opName, holder.value);
            if (isOneWay || ret == null || ret.length == 0) {
                return null;
            }
            return (T)ret[0]; Liberty change: end */
/*      Liberty change: the 2 catch blocks below are moved to the end of
        calculateOpName method that is merged in to this method
        } catch (IllegalEmptyResponseException ie) {
            return null;
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private QName calculateOpName(Holder<T> holder, QName opName, boolean hasOpName) throws XMLStreamException {
        boolean findDispatchOp = Boolean.TRUE.equals(getRequestContext().get("find.dispatch.operation"));
Liberty change: end */

            //CXF-2836 : find the operation for the dispatched object
            // if findDispatchOp is already true, skip the addressing feature lookup.
            // if the addressing feature is enabled, set findDispatchOp to true
            if (!findDispatchOp) {
            // Source createdSource = null; Liberty change: removed line
            Map<String, QName> payloadOPMap = createPayloadEleOpNameMap(
                    client.getEndpoint().getBinding().getBindingInfo());  // Liberty change: hasOpName parameter is removed
            if (findDispatchOp && !payloadOPMap.isEmpty()) {
                String payloadElementName = null;   // Liberty change: payloadElementName type is changed to String from QName
                if (obj instanceof javax.xml.transform.Source) { // Liberty change: holder.value  is replaced by obj
                    XMLStreamReader reader = null;
                    try {
                        reader = StaxUtils.createXMLStreamReader((javax.xml.transform.Source)obj);// Liberty change: holder.value  is replaced by obj
                        Document document = StaxUtils.read(reader);
                        createdSource = new StaxSource(StaxUtils.createXMLStreamReader(document));
                        payloadElementName = getPayloadElementName(document.getDocumentElement());
                    } catch (Exception e) {
                        // ignore, we are trying to get the operation name
                    } finally {
                        StaxUtils.close(reader);
                    }
                }
                if (obj instanceof SOAPMessage) {  // Liberty change: holder.value  is replaced by obj
                    payloadElementName = getPayloadElementName((SOAPMessage)obj); // Liberty change: holder.value  is replaced by obj
                }

                if (this.context != null) {
                    payloadElementName = getPayloadElementName(obj); // Liberty change: holder.value  is replaced by obj
                }

                if (payloadElementName != null) {
  /*                  if (hasOpName) {  Liberty change: 11 lines below are removed
                        // Verify the payload element against the given operation name.
                        // This allows graceful handling of non-standard WSDL definitions
                        // where different operations have the same payload element.
                    QName expectedElementName = payloadOPMap.get(opName.toString());
                        if (expectedElementName == null || !expectedElementName.toString().equals(
                                payloadElementName.toString())) {
                            // Verification of the provided operation name failed.
                            // Resolve the operation name from the payload element.
                            hasOpName = false;
                            payloadOPMap = createPayloadEleOpNameMap(
                                    client.getEndpoint().getBinding().getBindingInfo(), hasOpName);
                        }
                    } Liberty change: end */
                    QName dispatchedOpName = payloadOPMap.get(payloadElementName);  // Liberty change: null is replaced by payloadOPMap.get(payloadElementName)
/*                    if (!hasOpName) { Liberty change: 3 lines below are removed
                        dispatchedOpName = payloadOPMap.get(payloadElementName.toString());
                    } Liberty change: end */
                    if (null != dispatchedOpName) {
                        BindingOperationInfo bop = client.getEndpoint().getBinding().getBindingInfo().getOperation(opName); // Liberty change: added line
                        BindingOperationInfo dbop = client.getEndpoint().getBinding().getBindingInfo().getOperation(dispatchedOpName);
/*                        if (dbop != null) { Liberty change: if clause removed
                            opName = dispatchedOpName;
                        } Liberty change: end */
                    }
                }
            }
          }
/*            if (createdSource != null) {  Liberty change: if clause removed
                holder.value = (T)createdSource;
            } Liberty change: end */
            // Liberty change: below 5 lines are added
            Object ret[] = client.invokeWrapped(opName, createdSource == null ? obj : createdSource);
            if (isOneWay || ret == null || ret.length == 0) {
                return null;
            }
            return (T)ret[0]; //Liberty change: end
        } catch (IllegalEmptyResponseException ie) {
            return null;
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

    public Future<?> invokeAsync(T obj, AsyncHandler<T> asyncHandler) {
        checkError();
        client.setExecutor(getClient().getEndpoint().getExecutor());

        ClientCallback callback = new JaxwsClientCallback<T>(asyncHandler, this) {
            @Override
            protected Throwable mapThrowable(Throwable t) {
                if (t instanceof IOException) {
                    return t;
                } else if (t instanceof Exception) {
                    t = mapException((Exception)t);
                }
                return t;
            }
        };

        Response<T> ret = new JaxwsResponseCallback<>(callback);
        try {
            // boolean hasOpName; Liberty change: removed variable

            QName opName = (QName)getRequestContext().get(MessageContext.WSDL_OPERATION);
            if (opName == null) {
                // hasOpName = false; Liberty change: removed variable
                opName = INVOKE_QNAME;
            } else {
                // hasOpName = true; Liberty change: removed variable
                BindingOperationInfo bop = client.getEndpoint().getBinding()
                    .getBindingInfo().getOperation(opName);
                if (bop == null) {
                    addInvokeOperation(opName, false);
                }
            }

            // Holder<T> holder = new Holder<>(obj);  Liberty change: removed line
            // opName = calculateOpName(holder, opName, hasOpName);  Liberty change: removed variable

            client.invokeWrapped(callback,
                                 opName,
                                 obj);  // Liberty change: holder.value is replaced by obj

            return ret;
        } catch (Exception ex) {
            throw mapException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public Response<T> invokeAsync(T obj) {
        return (Response<T>)invokeAsync(obj, null);
    }

    public void invokeOneWay(T obj) {
        invoke(obj, true);
    }

    public Client getClient() {
        return client;
    }

    private String getPayloadElementName(Element ele) {  // Liberty change: QName is replaced by String
        XMLStreamReader xmlreader = StaxUtils.createXMLStreamReader(ele);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xmlreader);
        try {
            if (this.mode == Service.Mode.PAYLOAD) {

                StaxUtils.skipToStartOfElement(reader);

                return reader.getName().toString(); // Liberty change: toString() is suffixed
            }
            if (this.mode == Service.Mode.MESSAGE) {
                StaxUtils.skipToStartOfElement(reader);
                StaxUtils.toNextTag(reader,
                                    new QName(ele.getNamespaceURI(), "Body"));
                reader.nextTag();
                return reader.getName().toString(); // Liberty change: toString() is suffixed
            }
        } catch (XMLStreamException e) {
            // ignore
        }
        return null;

    }


    private String getPayloadElementName(SOAPMessage soapMessage) {  // Liberty change: QName is replaced by String
        try {
            // we only care about the first element node, not text nodes
            Element element = DOMUtils.getFirstElement(SAAJUtils.getBody(soapMessage));
            if (element != null) {
                return DOMUtils.getElementQName(element).toString(); // Liberty change: toString() is suffixed
            }
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    private String getPayloadElementName(Object object) {  // Liberty change: QName is replaced by String
        JAXBDataBinding dataBinding = new JAXBDataBinding();
        dataBinding.setContext(context);
        DataWriter<XMLStreamWriter> dbwriter = dataBinding.createWriter(XMLStreamWriter.class);
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter resultWriter = StaxUtils.createXMLStreamWriter(stringWriter);
        DepthXMLStreamReader reader = null;
        try {
            dbwriter.write(object, resultWriter);
            resultWriter.flush();
            if (!StringUtils.isEmpty(stringWriter.toString())) {
                ByteArrayInputStream binput = new ByteArrayInputStream(stringWriter.getBuffer().toString()
                    .getBytes());
                XMLStreamReader xmlreader = StaxUtils.createXMLStreamReader(binput);
                reader = new DepthXMLStreamReader(xmlreader);

                StaxUtils.skipToStartOfElement(reader);

                return reader.getName().toString(); // Liberty change: toString() is suffixed

            }
        } catch (XMLStreamException e) {
            // ignore
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e) {
                // ignore
            }
            StaxUtils.close(resultWriter);
        }
        return null;
    }

    private Map<String, QName> createPayloadEleOpNameMap(BindingInfo bindingInfo) { // Liberty change: boolean reverseMapping parameter is removed
        Map<String, QName> payloadElementMap = new java.util.HashMap<>();
        // assume a document binding style, which is default according to W3C spec on WSDL
        String bindingStyle = "document";
        // if the bindingInfo is a SOAPBindingInfo instance then we can see if it has a style
        if (bindingInfo instanceof SoapBindingInfo) {
            String tempStyle = ((SoapBindingInfo)bindingInfo).getStyle();
            if (tempStyle != null) {
                bindingStyle = tempStyle;
            }
        }
        for (BindingOperationInfo bop : bindingInfo.getOperations()) {
            SoapOperationInfo soi = bop.getExtensor(SoapOperationInfo.class);
            if (soi != null) {
                // operation style overrides binding style, if present
                String operationStyle = soi.getStyle() != null ? soi.getStyle() : bindingStyle;
                if ("document".equals(operationStyle)) {
                    // if doc
                    if (bop.getOperationInfo().getInput() != null
                        && bop.getOperationInfo().getInput().getMessagePartsNumber() > 0) {
                        QName qn = bop.getOperationInfo().getInput().getMessagePartByIndex(0)
                            .getElementQName();
                        /* Liberty change: 6 lines below are removed
                        QName op = bop.getOperationInfo().getName();
                        if (reverseMapping) {
                            payloadElementMap.put(op.toString(), qn);
                        } else {
                            payloadElementMap.put(qn.toString(), op);
                        } Liberty change: end */
                        payloadElementMap.put(qn.toString(), bop.getOperationInfo().getName()); //Liberty change: added line
                    }
                } else if ("rpc".equals(operationStyle)) {
                    // if rpc
                    QName op = bop.getOperationInfo().getName();
                    payloadElementMap.put(op.toString(), op);
                }
            }
        }
        return payloadElementMap;
    }
    /* Liberty change: removed method below
    public void close() throws IOException {
        client.destroy();
    } Liberty change: */
}
