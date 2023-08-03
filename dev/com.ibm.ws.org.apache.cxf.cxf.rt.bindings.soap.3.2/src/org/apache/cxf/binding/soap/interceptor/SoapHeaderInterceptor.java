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

package org.apache.cxf.binding.soap.interceptor;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.wsdl.interceptors.BareInInterceptor;
import org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor;

/**
 * Perform databinding of the SOAP headers.
 */
public class SoapHeaderInterceptor extends AbstractInDatabindingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(SoapHeaderInterceptor.class);

    public SoapHeaderInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(BareInInterceptor.class.getName());
        addAfter(RPCInInterceptor.class.getName());
        addAfter(DocLiteralInInterceptor.class.getName());
    }

    public void handleMessage(Message m) throws Fault {
        SoapMessage message = (SoapMessage) m;
        SoapVersion soapVersion = message.getVersion();
        Exchange exchange = message.getExchange();

        MessageContentsList parameters = MessageContentsList.getContentsList(message);

        if (null == parameters) {
            parameters = new MessageContentsList();
        }

        BindingOperationInfo bop = exchange.getBindingOperationInfo();
        if (null == bop) {
            return;
        }

        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }

        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getOutput() : bop.getInput();
        if (bmi == null) {
            // one way operation.
            return;
        }

        List<SoapHeaderInfo> headers = bmi.getExtensors(SoapHeaderInfo.class);
        if (headers == null || headers.isEmpty()) {
            return;
        }

        boolean supportsNode = this.supportsDataReader(message, Node.class);
        Service service = ServiceModelUtil.getService(message.getExchange());

        Schema schema = null;
        final boolean schemaValidationEnabled
                = ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, message);
        if (schemaValidationEnabled) {
            schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0),
                    message.getExchange().getBus());
        }

        for (SoapHeaderInfo header : headers) {
            MessagePartInfo mpi = header.getPart();
            try {
                if (schemaValidationEnabled && schema != null) {
                    validateHeader(message, mpi, schema);
                }
            } catch (Fault f) {
                if (!isRequestor(message)) {
                    f.setFaultCode(Fault.FAULT_CODE_CLIENT);
                }
                throw f;
            }

            if (mpi.getTypeClass() != null) {

                Header param = findHeader(message, mpi);

                Object object = null;
                if (param != null) {
                    message.getHeaders().remove(param);

                    if (param.getDataBinding() == null) {
                        Node source = (Node)param.getObject();
                        if (source instanceof Element) {
                            //need to remove these attributes as they
                            //would cause validation failures
                            Element el = (Element)source;

                            el.removeAttributeNS(soapVersion.getNamespace(),
                                              soapVersion.getAttrNameMustUnderstand());
                            el.removeAttributeNS(soapVersion.getNamespace(),
                                               soapVersion.getAttrNameRole());
                        }
                        if (supportsNode) {
                            object = getNodeDataReader(message).read(mpi, source);
                        } else {
                            W3CDOMStreamReader reader = new W3CDOMStreamReader((Element)source);
                            try {
                                reader.nextTag(); //advance into the first tag
                            } catch (XMLStreamException e) {
                                //ignore
                            }
                            object = getDataReader(message, XMLStreamReader.class).read(mpi, reader);
                        }
                    } else {
                        object = param.getObject();
                    }

                }
                parameters.put(mpi, object);
            }
        }
        if (!parameters.isEmpty()) {
            message.setContent(List.class, parameters);
        }
    }

    private void validateHeader(final SoapMessage message, MessagePartInfo mpi, Schema schema) {
        Header param = findHeader(message, mpi);
        if (param != null
            && param.getDataBinding() == null) {
            Node source = (Node)param.getObject();
            if (!(source instanceof Element)) {
                return;
            }
            if (schema != null) {
                final Element el = (Element)source;
                DOMSource ds = new DOMSource(el);
                try {
                    Validator v = schema.newValidator();
                    ErrorHandler errorHandler = new ErrorHandler() {
                        public void warning(SAXParseException exception) throws SAXException {
                        }
                        public void error(SAXParseException exception) throws SAXException {
                            String msg = exception.getMessage();
                            if (msg.contains(el.getLocalName())
                                && (msg.contains(":" + message.getVersion().getAttrNameRole())
                                    || msg.contains(":" + message.getVersion().getAttrNameMustUnderstand()))) {
                                return;
                            }
                            throw exception;
                        }
                        public void fatalError(SAXParseException exception) throws SAXException {
                            throw exception;
                        }
                    };
                    v.setErrorHandler(errorHandler);
                    v.validate(ds);
                } catch (SAXException | IOException e) {
                    throw new Fault("COULD_NOT_VALIDATE_SOAP_HEADER_CAUSED_BY", LOG, e, e.getClass()
                        .getCanonicalName(), e.getMessage());
                }
            }
        }
    }

    private Header findHeader(SoapMessage message, MessagePartInfo mpi) {
        return message.getHeader(mpi.getConcreteName());
    }
}
