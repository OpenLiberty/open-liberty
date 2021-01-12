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

package org.apache.cxf.wsdl.interceptors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.constants.Constants;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

public class DocLiteralInInterceptor extends AbstractInDatabindingInterceptor {
    public static final String KEEP_PARAMETERS_WRAPPER = DocLiteralInInterceptor.class.getName()
        + ".DocLiteralInInterceptor.keep-parameters-wrapper";
    public static final String IGNORE_PARAMETER = "com.ibm.jaxws.cxf.IgnoreValidSoap"; // Liberty change: added line

    private static final Logger LOG = LogUtils.getL7dLogger(DocLiteralInInterceptor.class);

    public DocLiteralInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(@Sensitive Message message) { // Liberty change: @Sensitive is added as parameter
        LOG.entering("DocLiteralInInterceptor", "handleMessage"); // Liberty change: added line
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("DocLiteralInInterceptor skipped in HTTP GET method");
            LOG.exiting("DocLiteralInInterceptor", "handleMessage"); // Liberty change: added line
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        MessageContentsList parameters = new MessageContentsList();

        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.getBindingOperationInfo();

        boolean client = isRequestor(message);

        //if body is empty and we have BindingOperationInfo, we do not need to match
        //operation anymore, just return
        if (bop != null && !StaxUtils.toNextElement(xmlReader)) {
            // body may be empty for partial response to decoupled request
            LOG.exiting("DocLiteralInInterceptor", "handleMessage"); // Liberty change: added line
            return;
        }

        Service service = ServiceModelUtil.getService(message.getExchange());
        bop = getBindingOperationInfo(xmlReader, exchange, bop, client);
        boolean forceDocLitBare = false;
        if (bop != null && bop.getBinding() != null) {
            forceDocLitBare = Boolean.TRUE.equals(bop.getBinding().getService().getProperty("soap.force.doclit.bare"));
        }
        DataReader<XMLStreamReader> dr = getDataReader(message);

        try {
            if (!forceDocLitBare && bop != null && bop.isUnwrappedCapable()) {
                ServiceInfo si = bop.getBinding().getService();
                // Wrapped case
                MessageInfo msgInfo = setMessage(message, bop, client, si);
                setDataReaderValidation(service, message, dr);

                // Determine if we should keep the parameters wrapper
                if (shouldWrapParameters(msgInfo, message)) {
                    QName startQName = xmlReader.getName();
                    MessagePartInfo mpi = msgInfo.getFirstMessagePart();
                    if (!mpi.getConcreteName().equals(startQName)) {
                        throw new Fault("UNEXPECTED_WRAPPER_ELEMENT", LOG, null, startQName,
                                        mpi.getConcreteName());
                    }
                    Object wrappedObject = dr.read(mpi, xmlReader);
                    parameters.put(mpi, wrappedObject);
                } else {
                    // Unwrap each part individually if we don't have a wrapper

                    bop = bop.getUnwrappedOperation();

                    msgInfo = setMessage(message, bop, client, si);
                    List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
                    Iterator<MessagePartInfo> itr = messageParts.iterator();

                    // advance just past the wrapped element so we don't get
                    // stuck
                    if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                        StaxUtils.nextEvent(xmlReader);
                    }

                    // loop through each child element
                    getPara(xmlReader, dr, parameters, itr, message);
                }

            } else {
                //Bare style
                BindingMessageInfo msgInfo = null;


                Endpoint ep = exchange.getEndpoint();
                ServiceInfo si = ep.getEndpointInfo().getService();
                if (bop != null) { //for xml binding or client side
                    if (client) {
                        msgInfo = bop.getOutput();
                    } else {
                        msgInfo = bop.getInput();
                        if (bop.getOutput() == null) {
                            exchange.setOneWay(true);
                        }
                    }
                    if (msgInfo == null) {
                        return;
                    }
                    setMessage(message, bop, client, si, msgInfo.getMessageInfo());
                }

                Collection<OperationInfo> operations = null;
                operations = new ArrayList<>();
                operations.addAll(si.getInterface().getOperations());

                if (xmlReader == null || !StaxUtils.toNextElement(xmlReader)) {
                    // empty input
                    // getBindingOperationForEmptyBody(operations, ep, exchange); Liberty change: removed line
                    // Liberty change: 11 lines added below (for loop)
                    // TO DO : check duplicate operation with no input
                    for (OperationInfo op : operations) {
                        MessageInfo bmsg = op.getInput();
                        if (bmsg.getMessageParts().size() == 0) {
                            BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                            exchange.put(BindingOperationInfo.class, boi);
                            exchange.put(OperationInfo.class, op);
                            exchange.setOneWay(op.isOneWay());
                        }
                    }
                    LOG.exiting("DocLiteralInInterceptor", "handleMessage"); // Liberty change: end
                    return;
                }

                setDataReaderValidation(service, message, dr);

                int paramNum = 0;

                do {
                    QName elName = xmlReader.getName();
                    Object o = null;

                    MessagePartInfo p;
                    if (!client && msgInfo != null && msgInfo.getMessageParts() != null
                        && msgInfo.getMessageParts().isEmpty()) {
                        //no input messagePartInfo
                        LOG.exiting("DocLiteralInInterceptor", "handleMessage"); // Liberty change: added line
                        return;
                    }

                    if (msgInfo != null && msgInfo.getMessageParts() != null
                        && msgInfo.getMessageParts().size() > 0) {
                        if (msgInfo.getMessageParts().size() > paramNum) {
                            p = msgInfo.getMessageParts().get(paramNum);
                        } else {
                            p = null;
                        }
                    } else {
                        p = findMessagePart(exchange, operations, elName, client, paramNum, message);
                    }

                    if (!forceDocLitBare) {
                        //Make sure the elName found on the wire is actually OK for
                        //the purpose we need it
                        validatePart(p, elName, message);
                    }

                    o = dr.read(p, xmlReader);
                    if (forceDocLitBare && parameters.isEmpty()) {
                        // webservice provider does not need to ensure size
                        parameters.add(o);
                    } else {
                        // parameters.put(p, o); Liberty change: removed line
                        parameters = this.checkValidSoap(parameters, p, o); // Liberty change: added line
                    }

                    paramNum++;
                    if (message.getContent(XMLStreamReader.class) == null || o == xmlReader) {
                        xmlReader = null;
                    }
                } while (xmlReader != null && StaxUtils.toNextElement(xmlReader));

            }

            message.setContent(List.class, parameters);
        } catch (Fault f) {
            if (!isRequestor(message)) {
                f.setFaultCode(Fault.FAULT_CODE_CLIENT);
            }
            throw f;
        }
    }

    // Liberty change: checkValidSoap method below is added(overrided)
    @Trivial
    private MessageContentsList checkValidSoap(MessageContentsList parameters, MessagePartInfo p, Object o) {
        String temp = System.getProperty(IGNORE_PARAMETER);
        boolean ignoreValidSoap = Boolean.parseBoolean(temp);
        if (ignoreValidSoap && (p != null)) {
            parameters.put(p, o);
        } else if (!ignoreValidSoap) {
            parameters.put(p, o);
        }
        return parameters;
    } // Liberty change: end

    /* Liberty change: getBindingOperationForEmptyBody method below is removed
    private void getBindingOperationForEmptyBody(Collection<OperationInfo> operations, Endpoint ep, Exchange exchange) {
        // TO DO : check duplicate operation with no input and also check if the action matches
        for (OperationInfo op : operations) {
            MessageInfo bmsg = op.getInput();
            int bPartsNum = bmsg.getMessagePartsNumber();
            if (bPartsNum == 0
                || (bPartsNum == 1
                    && Constants.XSD_ANYTYPE.equals(bmsg.getFirstMessagePart().getTypeQName()))) {
                BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                exchange.put(BindingOperationInfo.class, boi);
                exchange.setOneWay(op.isOneWay());
            }
        }
    }  Liberty change: end */

    private BindingOperationInfo getBindingOperationInfo(DepthXMLStreamReader xmlReader, Exchange exchange,
                                                         BindingOperationInfo bop, boolean client) {
        //bop might be a unwrapped, wrap it back so that we can get correct info
        if (bop != null && bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }

        if (bop == null) {
            QName startQName = xmlReader == null
                ? new QName("http://cxf.apache.org/jaxws/provider", "invoke")
                : xmlReader.getName();
            bop = getBindingOperationInfo(exchange, startQName, client);
        }
        return bop;
    }

    private void validatePart(MessagePartInfo p, QName elName, Message m) {
        if (p == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", LOG, elName),
                            Fault.FAULT_CODE_CLIENT);

        }

        boolean synth = false;
        if (p.getMessageInfo() != null && p.getMessageInfo().getOperation() != null) {
            OperationInfo op = p.getMessageInfo().getOperation();
            Boolean b = (Boolean)op.getProperty("operation.is.synthetic");
            if (b != null) {
                synth = b;
            }
        }

        if (MessageUtils.getContextualBoolean(m, "soap.no.validate.parts", false)) {
            // something like a Provider service or similar that is forcing a
            // doc/lit/bare on an endpoint that may not really be doc/lit/bare.
            // we need to just let these through per spec so the endpoint
            // can process it
            synth = true;
        }
        if (synth) {
            return;
        }
        if (p.isElement()) {
            if (p.getConcreteName() != null
                && !elName.equals(p.getConcreteName())
                && !synth) {
                throw new Fault("UNEXPECTED_ELEMENT", LOG, null, elName,
                                p.getConcreteName());
            }
        } else {
            if (!(elName.equals(p.getName()) || elName.equals(p.getConcreteName()))
                && !synth) {
                throw new Fault("UNEXPECTED_ELEMENT", LOG, null, elName,
                                p.getConcreteName());
            }
        }
    }

    @Trivial // Liberty change: added line
    private void getPara(DepthXMLStreamReader xmlReader,
                         DataReader<XMLStreamReader> dr,
                         MessageContentsList parameters,
                         Iterator<MessagePartInfo> itr,
                         Message message) {

        boolean hasNext = true;
        while (itr.hasNext()) {
            MessagePartInfo part = itr.next();
            if (hasNext) {
                hasNext = StaxUtils.toNextElement(xmlReader);
            }
            Object obj = null;
            if (hasNext) {
                QName rname = xmlReader.getName();
                while (part != null
                    && !rname.equals(part.getConcreteName())) {
                    if (part.getXmlSchema() instanceof XmlSchemaElement) {
                        //TODO - should check minOccurs=0 and throw validation exception
                        //thing if the part needs to be here
                        parameters.put(part, null);
                    }

                    if (itr.hasNext()) {
                        part = itr.next();
                    } else {
                        part = null;
                    }
                }
                if (part == null) {
                    return;
                }
                if (rname.equals(part.getConcreteName())) {
                    obj = dr.read(part, xmlReader);
                }
            }
            parameters.put(part, obj);
        }
    }

    @Trivial // Liberty change: added line
    private MessageInfo setMessage(Message message, BindingOperationInfo operation,
                                   boolean requestor, ServiceInfo si) {
        MessageInfo msgInfo = getMessageInfo(message, operation, requestor);
        return setMessage(message, operation, requestor, si, msgInfo);
    }

    @Trivial // Liberty change: added line
    protected BindingOperationInfo getBindingOperationInfo(Exchange exchange, QName name,
                                                           boolean client) {
        BindingOperationInfo bop = ServiceModelUtil.getOperationForWrapperElement(exchange, name, client);
        if (bop == null) {
            bop = super.getBindingOperationInfo(exchange, name, client);
        }

        if (bop != null) {
            exchange.put(BindingOperationInfo.class, bop);
        }
        return bop;
    }

    @Trivial // Liberty change: added line
    protected boolean shouldWrapParameters(MessageInfo msgInfo, Message message) {
        Object keepParametersWrapperFlag = message.get(KEEP_PARAMETERS_WRAPPER);
        if (keepParametersWrapperFlag == null) {
            return msgInfo.getFirstMessagePart().getTypeClass() != null;
        }
        return Boolean.parseBoolean(keepParametersWrapperFlag.toString());
    }
}
