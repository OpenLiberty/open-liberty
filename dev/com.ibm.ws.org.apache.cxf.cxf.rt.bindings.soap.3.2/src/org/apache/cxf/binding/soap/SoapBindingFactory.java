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

package org.apache.cxf.binding.soap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger; // Liberty Change

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCInInterceptor;
import org.apache.cxf.binding.soap.interceptor.RPCOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapHeaderOutFilterInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSInInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBinding;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBody;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapFault;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapHeader;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapOperation;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultipleEndpointObserver;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl.binding.AbstractWSDLBindingFactory;
import org.apache.cxf.wsdl.interceptors.BareOutInterceptor;
import org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor;
import org.apache.cxf.wsdl.interceptors.WrappedOutInterceptor;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;

import static org.apache.cxf.helpers.CastUtils.cast;
import com.ibm.websphere.ras.annotation.Sensitive; // Liberty Change


// Liberty Change: When a <wsdl:header message="ns2:header"> is defined in a <wsdl:operation>
// and the tns is not the same namespace as "ns2", CXF cannot deserialize the header value from schema.
// Our changes to the class correct this behavior by preventing CXF from setting the namespace of the header
// to the namespace of the message. 
@NoJSR250Annotations(unlessNull = { "bus" })
public class SoapBindingFactory extends AbstractWSDLBindingFactory {
    // Liberty Change: Add Logger for debugging
    private static final Logger LOG = Logger.getLogger(SoapBindingFactory.class.getName());
    public static final Collection<String> DEFAULT_NAMESPACES = Collections.unmodifiableList(Arrays.asList(
        "http://schemas.xmlsoap.org/soap/",
        "http://schemas.xmlsoap.org/wsdl/soap/",
        "http://schemas.xmlsoap.org/wsdl/soap12/",
        "http://schemas.xmlsoap.org/wsdl/soap/http",
        "http://www.w3.org/2003/05/soap/bindings/HTTP/",
        "http://www.w3.org/2010/soapjms/"
    ));


    public static final String SOAP_11_BINDING = "http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String SOAP_12_BINDING = "http://schemas.xmlsoap.org/wsdl/soap12/";

    public static final String HEADER = "messagepart.isheader";
    public static final String OUT_OF_BAND_HEADER = "messagepart.is_out_of_band_header";

    public SoapBindingFactory() {
    }

    public SoapBindingFactory(Bus b) {
        super(b, DEFAULT_NAMESPACES);
    }

    public BindingInfo createBindingInfo(ServiceInfo si, String bindingid, Object conf) {
        SoapBindingConfiguration config;
        if (conf instanceof SoapBindingConfiguration) {
            config = (SoapBindingConfiguration)conf;
        } else {
            config = new SoapBindingConfiguration();
        }
        if (WSDLConstants.NS_SOAP12.equals(bindingid)
            || WSDLConstants.NS_SOAP12_HTTP_BINDING.equals(bindingid)) {
            config.setVersion(Soap12.getInstance());
            config.setTransportURI(WSDLConstants.NS_SOAP_HTTP_TRANSPORT);
        }
        SoapBindingInfo info = new SoapBindingInfo(si,
                                                   bindingid,
                                                   config.getVersion());

        info.setName(config.getBindingName(si));
        info.setStyle(config.getStyle());

        info.setTransportURI(config.getTransportURI());

        if (config.isMtomEnabled()) {
            info.setProperty(Message.MTOM_ENABLED, Boolean.TRUE);
        }

        for (OperationInfo op : si.getInterface().getOperations()) {
            SoapOperationInfo sop = new SoapOperationInfo();
            sop.setAction(config.getSoapAction(op));
            sop.setStyle(config.getStyle(op));

            BindingOperationInfo bop =
                info.buildOperation(op.getName(), op.getInputName(), op.getOutputName());

            bop.addExtensor(sop);

            info.addOperation(bop);


            BindingMessageInfo bInput = bop.getInput();
            if (bInput != null) {
                final MessageInfo input;
                BindingMessageInfo unwrappedMsg = bInput;
                if (bop.isUnwrappedCapable()) {
                    input = bop.getOperationInfo().getUnwrappedOperation().getInput();
                    unwrappedMsg = bop.getUnwrappedOperation().getInput();
                } else {
                    input = bop.getOperationInfo().getInput();
                }
                setupHeaders(bop, bInput, unwrappedMsg, input, config);
            }

            BindingMessageInfo bOutput = bop.getOutput();
            if (bOutput != null) {
                final MessageInfo output;
                BindingMessageInfo unwrappedMsg = bOutput;
                if (bop.isUnwrappedCapable()) {
                    output = bop.getOperationInfo().getUnwrappedOperation().getOutput();
                    unwrappedMsg = bop.getUnwrappedOperation().getOutput();
                } else {
                    output = bop.getOperationInfo().getOutput();
                }
                setupHeaders(bop, bOutput, unwrappedMsg, output, config);
            }
        }

        try {
            createSoapBinding(info);
        } catch (WSDLException e) {
            e.printStackTrace();
        }

        return info;
    }


    private void createSoapBinding(final SoapBindingInfo bi) throws WSDLException {
        boolean isSoap12 = bi.getSoapVersion() instanceof Soap12;
        Bus bs = getBus();
        WSDLManager m = bs.getExtension(WSDLManager.class);
        ExtensionRegistry extensionRegistry = m.getExtensionRegistry();

        SoapBinding soapBinding = SOAPBindingUtil.createSoapBinding(extensionRegistry, isSoap12);
        soapBinding.setStyle(bi.getStyle());
        soapBinding.setTransportURI(bi.getTransportURI());
        bi.addExtensor(soapBinding);

        for (BindingOperationInfo b : bi.getOperations()) {
            for (BindingFaultInfo faultInfo : b.getFaults()) {
                SoapFault soapFault = SOAPBindingUtil.createSoapFault(extensionRegistry, isSoap12);
                soapFault.setUse("literal");
                soapFault.setName(faultInfo.getFaultInfo().getFaultName().getLocalPart());
                faultInfo.addExtensor(soapFault);
            }
            SoapOperationInfo soi = b.getExtensor(SoapOperationInfo.class);

            SoapOperation soapOperation = SOAPBindingUtil.createSoapOperation(extensionRegistry,
                                                                              isSoap12);
            soapOperation.setSoapActionURI(soi.getAction());
            soapOperation.setStyle(soi.getStyle());
            boolean isRpc = "rpc".equals(soapOperation.getStyle());

            b.addExtensor(soapOperation);

            if (b.getInput() != null) {
                List<String> bodyParts = null;
                List<SoapHeaderInfo> headerInfos = b.getInput().getExtensors(SoapHeaderInfo.class);
                if (headerInfos != null && !headerInfos.isEmpty()) {
                    bodyParts = new ArrayList<>();
                    for (MessagePartInfo part : b.getInput().getMessageParts()) {
                        bodyParts.add(part.getName().getLocalPart());
                    }

                    for (SoapHeaderInfo headerInfo : headerInfos) {
                        SoapHeader soapHeader = SOAPBindingUtil.createSoapHeader(extensionRegistry,
                                                                                 BindingInput.class,
                                                                                 isSoap12);
                        soapHeader.setMessage(b.getInput().getMessageInfo().getName());
                        soapHeader.setPart(headerInfo.getPart().getName().getLocalPart());
                        soapHeader.setUse("literal");
                        bodyParts.remove(headerInfo.getPart().getName().getLocalPart());
                        headerInfo.getPart().setProperty(HEADER, true);
                        b.getInput().addExtensor(soapHeader);
                    }
                }
                SoapBody body = SOAPBindingUtil.createSoapBody(extensionRegistry,
                                                               BindingInput.class,
                                                               isSoap12);
                body.setUse("literal");
                if (isRpc) {
                    body.setNamespaceURI(b.getName().getNamespaceURI());
                }

                if (bodyParts != null) {
                    body.setParts(bodyParts);
                }

                b.getInput().addExtensor(body);
            }

            if (b.getOutput() != null) {
                List<String> bodyParts = null;
                List<SoapHeaderInfo> headerInfos = b.getOutput().getExtensors(SoapHeaderInfo.class);
                if (headerInfos != null && !headerInfos.isEmpty()) {
                    bodyParts = new ArrayList<>();
                    for (MessagePartInfo part : b.getOutput().getMessageParts()) {
                        bodyParts.add(part.getName().getLocalPart());
                    }
                    for (SoapHeaderInfo headerInfo : headerInfos) {
                        SoapHeader soapHeader = SOAPBindingUtil.createSoapHeader(extensionRegistry,
                                                                             BindingOutput.class,
                                                                             isSoap12);
                        soapHeader.setMessage(b.getOutput().getMessageInfo().getName());
                        soapHeader.setPart(headerInfo.getPart().getName().getLocalPart());
                        soapHeader.setUse("literal");
                        bodyParts.remove(headerInfo.getPart().getName().getLocalPart());
                        b.getOutput().addExtensor(soapHeader);
                    }
                }
                SoapBody body = SOAPBindingUtil.createSoapBody(extensionRegistry,
                                                               BindingOutput.class,
                                                               isSoap12);
                body.setUse("literal");
                if (isRpc) {
                    body.setNamespaceURI(b.getName().getNamespaceURI());
                }

                if (bodyParts != null) {
                    body.setParts(bodyParts);
                }

                b.getOutput().addExtensor(body);
            }
        }
    }


    private void setupHeaders(BindingOperationInfo op,
                              BindingMessageInfo bMsg,
                              BindingMessageInfo unwrappedBMsg,
                              MessageInfo msg,
                              SoapBindingConfiguration config) {
        List<MessagePartInfo> parts = new ArrayList<>();
        for (MessagePartInfo part : msg.getMessageParts()) {
            if (config.isHeader(op, part)) {
                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setPart(part);
                headerInfo.setUse(config.getUse());
                
                bMsg.addExtensor(headerInfo);
            } else {
                parts.add(part);
            }
        }
        unwrappedBMsg.setMessageParts(parts);
    }

    public Binding createBinding(BindingInfo binding) {
        // The default style should be doc-lit wrapped.
        String parameterStyle = SoapBindingConstants.PARAMETER_STYLE_WRAPPED;
        String bindingStyle = SoapBindingConstants.BINDING_STYLE_DOC;

        boolean hasWrapped = false;

        final org.apache.cxf.binding.soap.SoapBinding sb;
        final SoapVersion version;
        if (binding instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo) binding;
            version = sbi.getSoapVersion();
            sb = new org.apache.cxf.binding.soap.SoapBinding(binding, version);
            // Service wide style
            if (!StringUtils.isEmpty(sbi.getStyle())) {
                bindingStyle = sbi.getStyle();
            }

            boolean hasRPC = false;
            boolean hasDoc = false;

            // Operation wide style, what to do with the mixed style/use?
            for (BindingOperationInfo boi : sbi.getOperations()) {
                String st = sbi.getStyle(boi.getOperationInfo());
                if (st != null) {
                    bindingStyle = st;
                    if (SoapBindingConstants.BINDING_STYLE_RPC.equalsIgnoreCase(st)) {
                        hasRPC = true;
                    } else {
                        hasDoc = true;
                    }
                }
                if (boi.getUnwrappedOperation() == null) {
                    parameterStyle = SoapBindingConstants.PARAMETER_STYLE_BARE;
                } else {
                    hasWrapped = true;
                }
            }

            if (Boolean.TRUE.equals(binding.getService().getProperty("soap.force.doclit.bare"))) {
                hasDoc = true;
                hasRPC = false;
                parameterStyle = SoapBindingConstants.PARAMETER_STYLE_BARE;
                bindingStyle = SoapBindingConstants.BINDING_STYLE_DOC;
            }
            if (hasRPC && hasDoc) {
                throw new RuntimeException("WSI-BP prohibits RPC and Document style "
                                           + "operations in same service.");
            }

            //jms
            if (sbi.getTransportURI().equals(SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID)) {
                sb.getInInterceptors().add(new SoapJMSInInterceptor());
            }
        } else {
            throw new RuntimeException("Can not initialize SoapBinding, BindingInfo is not SoapBindingInfo");
        }

        sb.getOutFaultInterceptors().add(new StaxOutInterceptor());
        sb.getOutFaultInterceptors().add(new SoapOutInterceptor(getBus()));
        sb.getOutFaultInterceptors().add(new AttachmentOutInterceptor());

        sb.getInInterceptors().add(new AttachmentInInterceptor());
        sb.getInInterceptors().add(new StaxInInterceptor());
        sb.getInInterceptors().add(new SoapActionInInterceptor());

        sb.getOutInterceptors().add(new AttachmentOutInterceptor());
        sb.getOutInterceptors().add(new StaxOutInterceptor());
        sb.getOutInterceptors().add(SoapHeaderOutFilterInterceptor.INSTANCE);

        if (SoapBindingConstants.BINDING_STYLE_RPC.equalsIgnoreCase(bindingStyle)) {
            sb.getInInterceptors().add(new RPCInInterceptor());
            sb.getOutInterceptors().add(new RPCOutInterceptor());
        } else if (SoapBindingConstants.BINDING_STYLE_DOC.equalsIgnoreCase(bindingStyle)
                        && SoapBindingConstants.PARAMETER_STYLE_BARE.equalsIgnoreCase(parameterStyle)) {
            //sb.getInInterceptors().add(new BareInInterceptor());
            sb.getInInterceptors().add(new DocLiteralInInterceptor());
            if (hasWrapped) {
                sb.getOutInterceptors().add(new WrappedOutInterceptor());
            }
            sb.getOutInterceptors().add(new BareOutInterceptor());
        } else {
            //sb.getInInterceptors().add(new WrappedInInterceptor());
            sb.getInInterceptors().add(new DocLiteralInInterceptor());
            sb.getOutInterceptors().add(new WrappedOutInterceptor());
            sb.getOutInterceptors().add(new BareOutInterceptor());
        }
        sb.getInInterceptors().add(new SoapHeaderInterceptor());

        sb.getInInterceptors().add(new ReadHeadersInterceptor(getBus(), version));
        sb.getInInterceptors().add(new StartBodyInterceptor());
        sb.getInInterceptors().add(new CheckFaultInterceptor());
        sb.getInInterceptors().add(new MustUnderstandInterceptor());
        sb.getOutInterceptors().add(new SoapPreProtocolOutInterceptor());
        sb.getOutInterceptors().add(new SoapOutInterceptor(getBus()));
        sb.getOutFaultInterceptors().add(new SoapOutInterceptor(getBus()));
        sb.getOutFaultInterceptors().add(SoapHeaderOutFilterInterceptor.INSTANCE);

        if (version.getVersion() == 1.1) {
            sb.getInFaultInterceptors().add(new Soap11FaultInInterceptor());
            sb.getOutFaultInterceptors().add(new Soap11FaultOutInterceptor());
        } else if (version.getVersion() == 1.2) {
            sb.getInFaultInterceptors().add(new Soap12FaultInInterceptor());
            sb.getOutFaultInterceptors().add(new Soap12FaultOutInterceptor());
        }

        if (binding.getService() != null) {
            for (EndpointInfo ei : binding.getService().getEndpoints()) {
                if (ei.getAddress() != null && ei.getAddress().startsWith("soap.udp")) {
                    setupUDP(sb, ei);
                }
            }
        }

        return sb;
    }

    protected void setupUDP(InterceptorProvider p, EndpointInfo ei) {
        //soap UDP requires ws-addressing turned on
        WSAddressingFeature add = new WSAddressingFeature();
        add.setAddressingRequired(true);
        add.initialize(p, bus);

        // UDP has a strict size limit on messages (<64K) so we'll try to shrink the
        // message a little by putting the WSA namespace into the
        // the soap:env which allows it to not be written on every header
        // element as well as disable the output stream optimizations (doesn't really
        // matter on such small messages anyway) to make sure we pickup those
        // namespaces that are declared there.
        p.getOutInterceptors().add(new AbstractSoapInterceptor(Phase.POST_LOGICAL) {
            public void handleMessage(@Sensitive SoapMessage message) throws Fault { // Liberty Change
                AddressingProperties p = ContextUtils.retrieveMAPs(message, false, true);
                if (p == null) {
                    return;
                }
                String ns = p.getNamespaceURI();
                Map<String, String> nsMap = message.getEnvelopeNs();
                if (nsMap == null) {
                    nsMap = new HashMap<>();
                } else {
                    nsMap = new HashMap<>(nsMap);
                }
                message.put("soap.env.ns.map", nsMap);
                if (!nsMap.containsValue(ns)
                    && !nsMap.containsKey("wsa")) {
                    nsMap.put("wsa", ns);
                }
                message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);
            }
        });
        // don't send the optional ReplyTo headers if we don't need to either
        ei.setProperty("ws-addressing.write.optional.replyto", Boolean.FALSE);
    }

    protected void addMessageFromBinding(ExtensibilityElement ext, BindingOperationInfo bop,
                                         boolean isInput) {
        SoapHeader header = SOAPBindingUtil.getSoapHeader(ext);

        ServiceInfo serviceInfo = bop.getBinding().getService();

        if (header != null && header.getMessage() == null) {
            throw new RuntimeException("Problem with WSDL: soap:header element"
                + " for operation " + bop.getName() + " under binding " + bop.getBinding().getName()
                + " does not contain a valid message attribute.");
        }

        if (header != null && serviceInfo.getMessage(header.getMessage()) == null) {
            Definition def = (Definition)serviceInfo.getProperty(WSDLServiceBuilder.WSDL_DEFINITION);
            SchemaCollection schemas = serviceInfo.getXmlSchemaCollection();

            if (def != null && schemas != null) {
                QName qn = header.getMessage();

                javax.wsdl.Message msg = findMessage(qn, def);
                if (msg != null) {
                    addOutOfBandParts(bop, msg, schemas, isInput, header.getPart());
                    serviceInfo.refresh();
                } else {
                    throw new RuntimeException("Problem with WSDL: soap:header element"
                        + " for operation " + bop.getName()
                        + " is referring to an undefined wsdl:message element: " + qn);
                }
            }
        }
    }
    private javax.wsdl.Message findMessage(QName qn, Definition def) {
        javax.wsdl.Message msg = def.getMessage(qn);
        if (msg == null) {
            msg = findMessage(qn, def, new ArrayList<>());
        }
        return msg;
    }
    private javax.wsdl.Message findMessage(QName qn, Definition def, List<Definition> done) {
        javax.wsdl.Message msg = def.getMessage(qn);
        if (msg == null) {
            if (done.contains(def)) {
                return null;
            }
            done.add(def);
            Collection<List<Import>> ilist = CastUtils.cast(def.getImports().values());
            for (List<Import> list : ilist) {
                for (Import i : list) {
                    if (qn.getNamespaceURI().equals(i.getDefinition().getTargetNamespace())) {
                        return i.getDefinition().getMessage(qn);
                    }
                }
            }
            for (List<Import> list : ilist) {
                for (Import i : list) {
                    msg = findMessage(qn, i.getDefinition(), done);
                    if (msg != null) {
                        return msg;
                    }
                }
            }
        }
        return msg;
    }

    private void addOutOfBandParts(final BindingOperationInfo bop, final javax.wsdl.Message msg,
                                   final SchemaCollection schemas, boolean isInput,
                                   final String partName) {
        MessageInfo.Type type;

        int nextId = 0;
        MessageInfo minfo = bop.getOperationInfo().getInput();
        if (minfo != null) {
            for (MessagePartInfo part : minfo.getMessageParts()) {
                if (part.getIndex() >= nextId) {
                    nextId = part.getIndex() + 1;
                }
            }
        }
        minfo = bop.getOperationInfo().getOutput();
        if (minfo != null) {
            for (MessagePartInfo part : minfo.getMessageParts()) {
                if (part.getIndex() >= nextId) {
                    nextId = part.getIndex() + 1;
                }
            }
        }

        if (isInput) {
            type = MessageInfo.Type.INPUT;
            minfo = bop.getOperationInfo().getInput();
        } else {
            type = MessageInfo.Type.OUTPUT;
            minfo = bop.getOperationInfo().getOutput();
        }

        if (minfo == null) {
            minfo = new MessageInfo(bop.getOperationInfo(), type, msg.getQName());
        }
        buildMessage(minfo, msg, schemas, nextId, partName);

        // for wrapped style
        OperationInfo unwrapped = bop.getOperationInfo().getUnwrappedOperation();
        if (unwrapped == null) {
            return;
        }

        nextId = 0;
        if (isInput) {
            minfo = unwrapped.getInput();
            type = MessageInfo.Type.INPUT;
            if (minfo != null) {
                for (MessagePartInfo part : minfo.getMessageParts()) {
                    if (part.getIndex() >= nextId) {
                        nextId = part.getIndex() + 1;
                    }
                }
            }
        } else {
            minfo = unwrapped.getOutput();
            type = MessageInfo.Type.OUTPUT;
            if (minfo != null) {
                for (MessagePartInfo part : minfo.getMessageParts()) {
                    if (part.getIndex() >= nextId) {
                        nextId = part.getIndex() + 1;
                    }
                }
            }
        }

        if (minfo == null) {
            minfo = new MessageInfo(unwrapped, type, msg.getQName());
        }
        buildMessage(minfo, msg, schemas, nextId, partName);
    }

    private void buildMessage(MessageInfo minfo,
                              javax.wsdl.Message msg,
                              SchemaCollection schemas,
                              int nextId,
                              String partNameFilter) {
        for (Part part : cast(msg.getParts().values(), Part.class)) {

            // Liberty Change Start: Correct OutofBound MessagePartInfos from inheriting the wrong QName from MessageInfo
            QName partTypeQname = part.getTypeName();
            QName partElementQname = part.getElementName();
            if (StringUtils.isEmpty(partNameFilter)
                || part.getName().equals(partNameFilter)) {

                if (StringUtils.isEmpty(part.getName())) {
                    throw new RuntimeException("Problem with WSDL: part element in message "
                                               + msg.getQName().getLocalPart()
                                               + " does not specify a name.");
                }
                QName pqname = new QName(minfo.getName().getNamespaceURI(), part.getName());
                MessagePartInfo pi = minfo.getMessagePart(pqname);
                if (pi != null
                    && pi.getMessageInfo().getName().equals(msg.getQName())) {
                    continue;
                }
                // Fix 26846 begin
                String partElementQnameSpace = partElementQname.getNamespaceURI();
                
                // Correct Namespace while keeping everything else intact. 
                // Use ElementQname to check for correct QName
                // Fix 27103 prevents creation of MessagePartInfo (pi) when it's null in system
                if (partElementQnameSpace != null && pi != null && !partElementQnameSpace.equals(minfo.getName().getNamespaceURI())) { 
                    QName newPartTypeQName = new QName(partElementQnameSpace, pqname.getLocalPart());
                    pi = minfo.addOutOfBandMessagePart(newPartTypeQName);
                 // Fix 26846 end
                    LOG.finest("QName mismatch corrected, PartInfo is now = " + pi);
                } else {
                    pi = minfo.addOutOfBandMessagePart(pqname);
                }

                if (!minfo.getName().equals(msg.getQName())) {
                    pi.setMessageContainer(new MessageInfo(minfo.getOperation(), null, msg.getQName()));        
                }

                if (partTypeQname != null) {
                    pi.setTypeQName(partTypeQname);
                    pi.setElement(false);
                    pi.setXmlSchema(schemas.getTypeByQName(partTypeQname));
                } else {
                    pi.setElementQName(partElementQname);
                    pi.setElement(true);
                    pi.setXmlSchema(schemas.getElementByQName(partElementQname));
                }
                // Liberty Change End:
                pi.setProperty(OUT_OF_BAND_HEADER, Boolean.TRUE);
                pi.setProperty(HEADER, Boolean.TRUE);
                pi.setIndex(nextId);
                nextId++;
            }
        }
    }

    public BindingInfo createBindingInfo(ServiceInfo service, javax.wsdl.Binding binding, String ns) {
        SoapBindingInfo bi = new SoapBindingInfo(service, ns);
        // Copy all the extensors
        initializeBindingInfo(service, binding, bi);

        SoapBinding wSoapBinding
            = SOAPBindingUtil.getSoapBinding(bi.getExtensors(ExtensibilityElement.class));


        bi.setTransportURI(wSoapBinding.getTransportURI());
        bi.setStyle(wSoapBinding.getStyle());

        for (BindingOperationInfo boi : bi.getOperations()) {
            initializeBindingOperation(bi, boi);
        }

        return bi;
    }

    private void initializeBindingOperation(SoapBindingInfo bi, BindingOperationInfo boi) {
        SoapOperationInfo soi = new SoapOperationInfo();

        SoapOperation soapOp =
            SOAPBindingUtil.getSoapOperation(boi.getExtensors(ExtensibilityElement.class));

        if (soapOp != null) {
            String action = soapOp.getSoapActionURI();
            if (action == null) {
                action = "";
            }
            soi.setAction(action);
            soi.setStyle(soapOp.getStyle());
        }

        boi.addExtensor(soi);

        if (boi.getInput() != null) {
            initializeMessage(bi, boi, boi.getInput());
        }

        if (boi.getOutput() != null) {
            initializeMessage(bi, boi, boi.getOutput());
        }
    }

    private void initializeMessage(SoapBindingInfo bi, BindingOperationInfo boi, BindingMessageInfo bmsg) {
        
        MessageInfo msg = bmsg.getMessageInfo();

        List<MessagePartInfo> messageParts = new ArrayList<>();
        messageParts.addAll(msg.getMessageParts());

        List<SoapHeader> headers =
            SOAPBindingUtil.getSoapHeaders(bmsg.getExtensors(ExtensibilityElement.class));
        if (headers != null) {
            for (SoapHeader header : headers) {
                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setUse(header.getUse());
                if (StringUtils.isEmpty(header.getPart())) {
                    throw new RuntimeException("Problem with WSDL: soap:header element in operation "
                                               + boi.getName().getLocalPart()
                                               + " does not specify a part.");
                }
                MessagePartInfo part = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),header.getPart()));
                // Liberty Change Start: CXF will create Parts with a QName inherited from the message
                // we can correct for this by tracking if the QName matches an OutofBoundsPart and then removing
                // the mismatched Part from the MessageContents and then adding the mismatched Part to the header lists
                Boolean isOutofBoundPart = false;
                QName outOfBoundQname = null;
                if (part != null && header.getMessage() != null
                    && !part.getMessageInfo().getName().equals(header.getMessage())) {
                    part = null;
                    //out of band, let's find it
                    for (MessagePartInfo mpi : msg.getOutOfBandParts()) {
                        if (mpi.getName().getLocalPart().equals(header.getPart())
                            && mpi.getMessageInfo().getName().equals(header.getMessage())) {
                            part = mpi;
                            isOutofBoundPart = true;
                            outOfBoundQname = mpi.getName();
                        }
                    }
                }
                if (part != null) {
                    if (isOutofBoundPart) {
                          LOG.finest("This Header's QName is mismatched from the expected OutOfBounds QName, removing mismatched MessagePartInfo, and adding mismatched Header");
                          part = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(), header.getPart()));                                        
                          headerInfo.setPart(part);
                          messageParts.remove(part);
                          bmsg.addExtensor(headerInfo);
                    }
                    else {
                          headerInfo.setPart(part);
                          messageParts.remove(part);
                          bmsg.addExtensor(headerInfo);
                    }   // Liberty Change End
              }
            }

            // Exclude the header parts from the message part list.
            bmsg.setMessageParts(messageParts);
        }

        SoapBodyInfo bodyInfo = new SoapBodyInfo();
        SoapBody soapBody = SOAPBindingUtil.getSoapBody(bmsg.getExtensors(ExtensibilityElement.class));

        List<?> parts = null;
        if (soapBody == null) {
            MIMEMultipartRelated mmr = bmsg.getExtensor(MIMEMultipartRelated.class);
            if (mmr != null) {
                parts = mmr.getMIMEParts();
            }
        } else {
            bmsg.addExtensor(soapBody);
            bodyInfo.setUse(soapBody.getUse());
            parts = soapBody.getParts();
        }

        // Initialize the body parts.
        List<MessagePartInfo> attParts = null;
        if (parts != null) {
            List<MessagePartInfo> bodyParts = new ArrayList<>();
            for (Iterator<?> itr = parts.iterator(); itr.hasNext();) {
                Object part = itr.next();
                if (part instanceof MIMEPart) {
                    MIMEPart mpart = (MIMEPart) part;
                    attParts = handleMimePart(mpart, attParts, msg, bmsg, bodyParts, messageParts);
                } else {
                    addSoapBodyPart(msg, bodyParts, (String)part);
                }
            }
            bodyInfo.setParts(bodyParts);
            bodyInfo.setAttachments(attParts);
        } else {
            bodyInfo.setParts(messageParts);
        }

        bmsg.addExtensor(bodyInfo);
    }

    private List<MessagePartInfo> handleMimePart(MIMEPart mpart,
                                                 List<MessagePartInfo> attParts,
                                                 MessageInfo msg,
                                                 BindingMessageInfo bmsg,
                                                 List<MessagePartInfo> bodyParts,
                                                 List<MessagePartInfo> messageParts) {
        if (mpart.getExtensibilityElements().size() < 1) {
            throw new RuntimeException("MIMEPart should at least contain one element!");
        }
        String partName = null;
        for (Object content : mpart.getExtensibilityElements()) {
            if (content instanceof MIMEContent) {
                MIMEContent mc = (MIMEContent)content;
                partName = mc.getPart();

                if (attParts == null) {
                    attParts = new LinkedList<>();
                }

                if (StringUtils.isEmpty(partName)) {
                    throw new RuntimeException("Problem with WSDL: mime content element in operation "
                                               + bmsg.getBindingOperation().getName().getLocalPart()
                                               + " does not specify a part.");
                }

                MessagePartInfo mpi =
                    msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),
                                                 partName));
                mpi.setProperty(Message.CONTENT_TYPE, mc.getType());
                attParts.add(mpi);
                // Attachments shouldn't be part of the body message
                bmsg.getMessageParts().remove(mpi);
            } else if (SOAPBindingUtil.isSOAPBody(content)) {
                SoapBody sb = SOAPBindingUtil.getSoapBody(content);
                if (sb.getParts() != null && sb.getParts().size() == 1) {
                    partName = (String) sb.getParts().get(0);
                }

                // We can have a list of empty part names here.
                if (partName != null) {
                    addSoapBodyPart(msg, bodyParts, partName);
                }
            } else if (SOAPBindingUtil.isSOAPHeader(content)) {
                SoapHeader header = SOAPBindingUtil.getSoapHeader(content);

                SoapHeaderInfo headerInfo = new SoapHeaderInfo();
                headerInfo.setUse(header.getUse());

                if (StringUtils.isEmpty(header.getPart())) {
                    throw new RuntimeException("Problem with WSDL: soap:header element in operation "
                                               + bmsg.getBindingOperation().getName().getLocalPart()
                                               + " does not specify a part.");
                }

                MessagePartInfo mpi =
                    msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),
                                                 header.getPart()));

                if (mpi != null && header.getMessage() != null
                    && !mpi.getMessageInfo().getName().equals(header.getMessage())) {
                    mpi = null;
                    //out of band, let's find it
                    for (MessagePartInfo mpi2 : msg.getOutOfBandParts()) {
                        if (mpi2.getName().getLocalPart().equals(header.getPart())
                            && mpi2.getMessageInfo().getName().equals(header.getMessage())) {
                            mpi = mpi2;
                        }
                    }
                }

                if (mpi != null) {
                    headerInfo.setPart(mpi);
                    messageParts.remove(mpi);
                    bmsg.getMessageParts().remove(mpi);
                    bmsg.addExtensor(headerInfo);
                }
            }
        }
        return attParts;
    }

    private void addSoapBodyPart(MessageInfo msg, List<MessagePartInfo> bodyParts, String partName) {
        MessagePartInfo mpi = msg.getMessagePart(new QName(msg.getName().getNamespaceURI(),
                                                           partName));
        bodyParts.add(mpi);
    }

    @Override
    public synchronized void addListener(Destination d, Endpoint e) {
        synchronized (d) {
            MessageObserver mo = d.getMessageObserver();
            if (d.getAddress() != null
                && d.getAddress().getAddress() != null
                && d.getAddress().getAddress().getValue() != null
                && d.getAddress().getAddress().getValue().startsWith("soap.udp")) {
                //soap.udp REQUIRES usage of WS-Addressing... we need to turn this on
                setupUDP(e, e.getEndpointInfo());
            }
            if (mo == null) {
                super.addListener(d, e);
                return;
            }

            if (mo instanceof ChainInitiationObserver) {
                ChainInitiationObserver cio = (ChainInitiationObserver) mo;

                Binding b = e.getBinding();
                Binding b2 = cio.getEndpoint().getBinding();
                if (b == b2) {
                    //re-registering the same endpoint?
                    return;
                }
                Object o = cio.getEndpoint().get("allow-multiplex-endpoint");
                if (o instanceof String) {
                    o = Boolean.parseBoolean((String)o);
                } else if (o == null) {
                    o = Boolean.FALSE;
                }
                if (b instanceof org.apache.cxf.binding.soap.SoapBinding
                    && b2 instanceof org.apache.cxf.binding.soap.SoapBinding
                    && ((org.apache.cxf.binding.soap.SoapBinding)b).getSoapVersion()
                        .equals(((org.apache.cxf.binding.soap.SoapBinding)b2).getSoapVersion())
                    && Boolean.FALSE.equals(o)) {

                    throw new RuntimeException("Soap "
                                               + ((org.apache.cxf.binding.soap.SoapBinding)b)
                                                   .getSoapVersion().getVersion()
                                               + " endpoint already registered on address "
                                               + e.getEndpointInfo().getAddress());
                }

                MultipleEndpointObserver newMO = new MultipleEndpointObserver(getBus()) {
                    @Override
                    protected Message createMessage(Message message) {
                        return new SoapMessage(message);
                    }
                };

                newMO.getBindingInterceptors().add(new AttachmentInInterceptor());
                newMO.getBindingInterceptors().add(new StaxInInterceptor());

                // This will not work if one of the endpoints disables message
                // processing. But, if you've disabled message processing, you
                // probably aren't going to use this feature.

                newMO.getBindingInterceptors().add(new ReadHeadersInterceptor(getBus(), (SoapVersion)null));
                newMO.getBindingInterceptors().add(new StartBodyInterceptor());
                newMO.getBindingInterceptors().add(new CheckFaultInterceptor());

                // Add in a default selection interceptor
                newMO.getRoutingInterceptors().add(new EndpointSelectionInterceptor());

                newMO.getEndpoints().add(cio.getEndpoint());

                mo = newMO;
            }

            if (mo instanceof MultipleEndpointObserver) {
                MultipleEndpointObserver meo = (MultipleEndpointObserver) mo;
                meo.getEndpoints().add(e);
            }

            d.setMessageObserver(mo);
        }
    }
}
