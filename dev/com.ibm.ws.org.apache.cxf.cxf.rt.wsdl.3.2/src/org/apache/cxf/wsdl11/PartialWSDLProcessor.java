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

package org.apache.cxf.wsdl11;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;



public final class PartialWSDLProcessor  {
    private static String bindingName = "SoapBinding";
    private static String style = "document";
    private static String useLiteral = "literal";
    private static String serviceName = "Service";
    private static String portName = "Port";


    private PartialWSDLProcessor() {
    }


    public static boolean isPortTypeExisted(Definition wsdlDefinition, QName name) {
        Map<QName, PortType>  portTypes = CastUtils.cast(wsdlDefinition.getPortTypes());  // Liberty change: getAllPortTypes() replaced by getPortTypes()
        if (portTypes == null || portTypes.isEmpty()) {
            return false;
        }
        String existPortTypeName = null;
        PortType portType = null;
        try {
            for (Entry<QName, PortType> entry : portTypes.entrySet()) {
                existPortTypeName = entry.getKey().getLocalPart();
                if (name.getLocalPart().contains(existPortTypeName)) {
                    portType = entry.getValue();
                    break;
                }
            }
        } catch (Exception e) {
            portType = null;
        }
        return portType != null;
    }

    public static boolean isBindingExisted(Definition wsdlDefinition, QName name) {
        Map<QName, Binding> bindings = CastUtils.cast(wsdlDefinition.getAllBindings());
        Binding binding = null;
        if (bindings == null || bindings.isEmpty()) {
            return false;
        }
        try {
            for (Entry<QName, Binding> entry : bindings.entrySet()) {
                if (entry.getKey().getLocalPart().contains(name.getLocalPart())) {
                    binding = entry.getValue();
                    break;
                }
            }
        } catch (Exception e) {
            binding = null;
        }
        return binding != null;
    }
    public static boolean isServiceExisted(Definition wsdlDefinition, QName name) {
        return wsdlDefinition.getService(name) != null;
    }

    public static Binding doAppendBinding(Definition wsdlDefinition, String name, PortType portType,
                                             ExtensionRegistry extReg) throws Exception {
        Binding binding = wsdlDefinition.createBinding();
        binding.setQName(new QName(wsdlDefinition.getTargetNamespace(), name + bindingName));
        binding.setUndefined(false);
        binding.setPortType(portType);
        setSoapBindingExtElement(wsdlDefinition, binding, extReg);
        addBindingOperation(wsdlDefinition, portType, binding, extReg);
        return binding;


    }

    private static void setSoapBindingExtElement(Definition wsdlDefinition, Binding binding,
                                                 ExtensionRegistry extReg) throws Exception {
        SOAPBindingUtil.addSOAPNamespace(wsdlDefinition, false);
        SOAPBinding
            soapBinding = SOAPBindingUtil.createSoapBinding(extReg, false);
        soapBinding.setStyle(style);
        binding.addExtensibilityElement(soapBinding);
    }

    @SuppressWarnings("unchecked")
    private static void addBindingOperation(Definition wsdlDefinition, PortType portType, Binding binding,
                                            ExtensionRegistry extReg) throws Exception {
        List<Operation> ops = portType.getOperations();
        for (Operation op : ops) {
            BindingOperation bindingOperation = wsdlDefinition.createBindingOperation();
            setSoapOperationExtElement(bindingOperation, extReg);
            bindingOperation.setName(op.getName());
            if (op.getInput() != null) {
                bindingOperation.setBindingInput(getBindingInput(op.getInput(), wsdlDefinition, extReg));
            }
            if (op.getOutput() != null) {
                bindingOperation.setBindingOutput(getBindingOutput(op.getOutput(), wsdlDefinition, extReg));
            }
            if (op.getFaults() != null && op.getFaults().size() > 0) {
                addSoapFaults(op, bindingOperation, wsdlDefinition, extReg);
            }
            bindingOperation.setOperation(op);
            binding.addBindingOperation(bindingOperation);
        }
    }




    private static void setSoapOperationExtElement(BindingOperation bo, ExtensionRegistry extReg) throws Exception {
        SOAPOperation soapOperation = SOAPBindingUtil.createSoapOperation(extReg, false);
        soapOperation.setStyle(style);
        soapOperation.setSoapActionURI("");
        bo.addExtensibilityElement(soapOperation);
    }



    private static BindingInput getBindingInput(Input input, Definition wsdlDefinition,
                                                ExtensionRegistry extReg) throws Exception {
        BindingInput bi = wsdlDefinition.createBindingInput();
        bi.setName(input.getName());
        bi.addExtensibilityElement(getSoapBody(BindingInput.class, extReg));
        return bi;
    }


    private static BindingOutput getBindingOutput(Output output, Definition wsdlDefinition,
                                                  ExtensionRegistry extReg) throws Exception {
        BindingOutput bo = wsdlDefinition.createBindingOutput();
        bo.setName(output.getName());
        bo.addExtensibilityElement(getSoapBody(BindingOutput.class, extReg));
        return bo;
    }

    private static SOAPBody getSoapBody(Class<?> parent, ExtensionRegistry extReg) throws Exception {
        SOAPBody soapBody = SOAPBindingUtil.createSoapBody(extReg, parent, false);
        soapBody.setUse(useLiteral);
        return soapBody;
    }

    private static void addSoapFaults(Operation op, BindingOperation bindingOperation, Definition wsdlDefinition,
                                      ExtensionRegistry extReg) throws Exception {
        Map<String, Fault> faults = CastUtils.cast(op.getFaults());
        for (Fault fault : faults.values()) {
            BindingFault bf = wsdlDefinition.createBindingFault();
            bf.setName(fault.getName());
            setSoapFaultExtElement(bf, extReg);
            bindingOperation.addBindingFault(bf);
        }
    }

    private static void setSoapFaultExtElement(BindingFault bf, ExtensionRegistry extReg) throws Exception {
        SOAPFault soapFault = SOAPBindingUtil.createSoapFault(extReg, false);
        soapFault.setName(bf.getName());
        soapFault.setUse(useLiteral);
        bf.addExtensibilityElement(soapFault);
    }

    public static SOAPAddress setAddrElement(Definition wsdlDefinition, Port port,
                                             ExtensionRegistry extReg) throws Exception {
        SOAPAddress address = SOAPBindingUtil.createSoapAddress(extReg, false);
        address.setLocationURI("dummy");
        return address;
    }
    public static javax.wsdl.Service doAppendService(Definition wsdlDefinition,
                                                     String existPortName, ExtensionRegistry
                                                     extReg, Binding binding) throws Exception {
        javax.wsdl.Service wsdlService = wsdlDefinition.createService();
        wsdlService.setQName(new QName(wsdlDefinition.getTargetNamespace(), existPortName + serviceName));
        Port port = wsdlDefinition.createPort();
        port.setName(existPortName + portName);
        port.setBinding(binding);
        SOAPAddress address = PartialWSDLProcessor.setAddrElement(wsdlDefinition, port, extReg);
        port.addExtensibilityElement(address);
        wsdlService.addPort(port);
        return wsdlService;
    }


}
