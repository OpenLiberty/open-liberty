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

package com.ibm.ws.jaxws.policy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.policy.attachment.external.DomainExpression;

public class URIDomainExpression implements DomainExpression {

    private final Wsdl11XPointer wsdl11XPointer;

    public URIDomainExpression(final String uriContext) {
        wsdl11XPointer = new Wsdl11XPointer(uriContext);
    }

    @Override
    public boolean appliesTo(ServiceInfo si) {
        if (si == null) {
            return false;
        }
        if (wsdl11XPointer.matchesWsdl(si.getTargetNamespace())) {
            return true;
        }
        if ((si.getName() != null)
            && wsdl11XPointer.matchesService(si.getTargetNamespace(), si.getName().getLocalPart())) {
            return true;
        }
        if ((si.getInterface() != null) && (si.getInterface().getName() != null)
            && wsdl11XPointer.matchesPortType(si.getTargetNamespace(),
                                              si.getInterface().getName().getLocalPart())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliesTo(EndpointInfo ei) {
        if (ei == null) {
            return false;
        }
        if ((ei.getService() != null) && (ei.getService().getName() != null) && (ei.getName() != null)
            && wsdl11XPointer.matchesPort(ei.getService().getTargetNamespace(),
                                          ei.getService().getName().getLocalPart(),
                                          ei.getName().getLocalPart())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliesTo(BindingOperationInfo boi) {
        if (boi == null) {
            return false;
        }
        if ((boi.getBinding() != null) && (boi.getBinding().getName() != null)
            && wsdl11XPointer.matchesBinding(
                                             boi.getBinding().getName().getNamespaceURI(),
                                             boi.getBinding().getName().getLocalPart())) {
            return true;
        }
        if ((boi.getName() != null) && (boi.getBinding() != null) && (boi.getBinding().getName() != null)
            && wsdl11XPointer.matchesBindingOperation(
                                                      boi.getBinding().getName().getNamespaceURI(),
                                                      boi.getBinding().getName().getLocalPart(),
                                                      boi.getName().getLocalPart())) {
            return true;
        }
        if ((boi.getOperationInfo() != null) && (boi.getOperationInfo().getInterface() != null)
            && (boi.getOperationInfo().getInterface().getName() != null) && (boi.getOperationInfo().getName() != null)
            && wsdl11XPointer.matchesPortTypeOperation(
                                                       boi.getOperationInfo().getInterface().getName().getNamespaceURI(),
                                                       boi.getOperationInfo().getInterface().getName().getLocalPart(),
                                                       boi.getOperationInfo().getName().getLocalPart())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliesTo(BindingMessageInfo bmi) {
        if (bmi == null) {
            return false;
        }
        if ((bmi.getMessageInfo() != null) && (bmi.getMessageInfo().getName() != null)
            && wsdl11XPointer.matchesMessage(
                                             bmi.getMessageInfo().getName().getNamespaceURI(),
                                             bmi.getMessageInfo().getName().getLocalPart())) {
            return true;
        }

        if (checkBindingOperationInOut(bmi)) {
            return true;
        }

        if (checkPortTypeOperationInOut(bmi)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean appliesTo(BindingFaultInfo bfi) {
        if ((bfi == null) || (bfi.getFaultInfo() == null) || (bfi.getBindingOperation() == null)) {
            return false;
        }

        if (checkBindingOperationFault(bfi)) {
            return true;
        }

        if (checkPortTypeOperationFault(bfi)) {
            return true;
        }
        return false;
    }

    private boolean checkBindingOperationInOut(BindingMessageInfo bmi) {
        if ((bmi.getMessageInfo() != null) && (bmi.getMessageInfo().getName() != null)
            && (bmi.getBindingOperation() != null) && (bmi.getBindingOperation().getName() != null)
            && (bmi.getBindingOperation().getBinding() != null)
            && (bmi.getBindingOperation().getBinding().getName() != null)) {
            if ((Type.INPUT == bmi.getMessageInfo().getType())
                && wsdl11XPointer.matchesBindingOperationInput(
                                                               bmi.getMessageInfo().getName().getNamespaceURI(),
                                                               bmi.getBindingOperation().getBinding().getName().getLocalPart(),
                                                               bmi.getBindingOperation().getName().getLocalPart())) {
                return true;
            }
            if ((Type.OUTPUT == bmi.getMessageInfo().getType())
                && wsdl11XPointer.matchesBindingOperationOutput(
                                                                bmi.getMessageInfo().getName().getNamespaceURI(),
                                                                bmi.getBindingOperation().getBinding().getName().getLocalPart(),
                                                                bmi.getBindingOperation().getName().getLocalPart())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPortTypeOperationInOut(BindingMessageInfo bmi) {
        InterfaceInfo ini = null;
        if ((bmi.getBindingOperation() != null) && (bmi.getBindingOperation().getOperationInfo() != null)) {
            ini = bmi.getBindingOperation().getOperationInfo().getInterface();
        }

        if ((ini != null) && (ini.getName() != null)
            && (bmi.getMessageInfo() != null)
            && (bmi.getBindingOperation() != null) && (bmi.getBindingOperation().getName() != null)) {
            if ((Type.INPUT == bmi.getMessageInfo().getType())
                && wsdl11XPointer.matchesPortTypeOperationInput(
                                                                ini.getName().getNamespaceURI(),
                                                                ini.getName().getLocalPart(),
                                                                bmi.getBindingOperation().getName().getLocalPart())) {
                return true;
            }
            if ((Type.OUTPUT == bmi.getMessageInfo().getType())
                && wsdl11XPointer.matchesPortTypeOperationOutput(
                                                                 ini.getName().getNamespaceURI(),
                                                                 ini.getName().getLocalPart(),
                                                                 bmi.getBindingOperation().getName().getLocalPart())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPortTypeOperationFault(BindingFaultInfo bfi) {
        InterfaceInfo ini = null;
        if ((bfi.getBindingOperation() != null) && (bfi.getBindingOperation().getOperationInfo() != null)) {
            ini = bfi.getBindingOperation().getOperationInfo().getInterface();
        }

        if ((ini != null) && (ini.getName() != null)
            && (bfi.getBindingOperation().getName() != null)
            && (bfi.getFaultInfo().getFaultName() != null)
            && wsdl11XPointer.matchesPortTypeOperationFault(
                                                            ini.getName().getNamespaceURI(),
                                                            ini.getName().getLocalPart(),
                                                            bfi.getBindingOperation().getName().getLocalPart(),
                                                            bfi.getFaultInfo().getFaultName().getLocalPart())) {
            return true;
        }
        return false;
    }

    private boolean checkBindingOperationFault(BindingFaultInfo bfi) {
        if ((bfi.getFaultInfo() != null) && (bfi.getFaultInfo().getFaultName() != null)
            && (bfi.getBindingOperation().getName() != null)
            && (bfi.getBindingOperation().getBinding() != null)
            && (bfi.getBindingOperation().getBinding().getName() != null)
            && wsdl11XPointer.matchesBindingOperationFault(
                                                           bfi.getFaultInfo().getFaultName().getNamespaceURI(),
                                                           bfi.getBindingOperation().getBinding().getName().getLocalPart(),
                                                           bfi.getBindingOperation().getName().getLocalPart(),
                                                           bfi.getFaultInfo().getFaultName().getLocalPart())) {
            return true;
        }
        return false;
    }

    public static class Wsdl11XPointer {
        private static final String SINGLE_PATH = "\\(([^\\)]*)\\)$";
        private static final String DOUBLE_PATH = "\\(([^/]*)/([^\\)]*)\\)$";
        private static final String TRIPPLE_PATH = "\\(([^/]*)/([^/]*)/([^\\)]*)\\)$";

        private static final Pattern PATTERN_DEFINITIONS = Pattern.compile("^wsdl11.definitions\\(\\)$");

        private static final Pattern PATTERN_PORT_TYPE = Pattern.compile("^wsdl11.portType" + SINGLE_PATH);

        private static final Pattern PATTERN_PORT_TYPE_OPERATION = Pattern.compile("^wsdl11.portTypeOperation" + DOUBLE_PATH);

        private static final Pattern PATTERN_PORT_TYPE_OPERATION_INPUT = Pattern.compile("^wsdl11.portTypeOperation.input" + DOUBLE_PATH);

        private static final Pattern PATTERN_PORT_TYPE_OPERATION_OUTPUT = Pattern.compile("^wsdl11.portTypeOperation.output" + DOUBLE_PATH);

        private static final Pattern PATTERN_PORT_TYPE_OPERATION_FAULT = Pattern.compile("^wsdl11.portTypeOperation.fault" + TRIPPLE_PATH);

        private static final Pattern PATTERN_MESSAGE = Pattern.compile("^wsdl11.message" + SINGLE_PATH);

        private static final Pattern PATTERN_MESSAGE_PART = Pattern.compile("^wsdl11.messagePart" + DOUBLE_PATH);

        private static final Pattern PATTERN_SERVICE = Pattern.compile("^wsdl11.service" + SINGLE_PATH);

        private static final Pattern PATTERN_PORT = Pattern.compile("^wsdl11.port" + DOUBLE_PATH);

        private static final Pattern PATTERN_BINDING = Pattern.compile("^wsdl11.binding" + SINGLE_PATH);

        private static final Pattern PATTERN_BINDING_OPERATION =
                        Pattern.compile("^wsdl11.bindingOperation" + DOUBLE_PATH);

        private static final Pattern PATTERN_BINDING_OPERATION_INPUT =
                        Pattern.compile("^wsdl11.bindingOperation.input" + DOUBLE_PATH);

        private static final Pattern PATTERN_BINDING_OPERATION_OUTPUT =
                        Pattern.compile("^wsdl11.bindingOperation.output" + DOUBLE_PATH);

        private static final Pattern PATTERN_BINDING_OPERATION_FAULT =
                        Pattern.compile("^wsdl11.bindingOperation.fault" + TRIPPLE_PATH);

        private final String targetNamespace;
        private final String wsdl11Pointer;

        public Wsdl11XPointer(final String uriContext) {
            if ((uriContext == null) || uriContext.isEmpty()) {
                throw new IllegalArgumentException(
                                "External policy attachment URI element cannot be empty");
            }
            int pos = uriContext.indexOf('#');
            if (pos != -1) {
                targetNamespace = uriContext.substring(0, pos);
                wsdl11Pointer = uriContext.substring(pos + 1);
            } else {
                targetNamespace = "";
                wsdl11Pointer = uriContext;
            }
        }

        boolean matchesWsdl(String wsdlTargetNamespace) {
            Matcher matcher = PATTERN_DEFINITIONS.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher);
        }

        boolean matchesPortType(String wsdlTargetNamespace, String portType) {
            Matcher matcher = PATTERN_PORT_TYPE.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, portType);
        }

        boolean matchesPortTypeOperation(String wsdlTargetNamespace, String portType, String operation) {
            Matcher matcher = PATTERN_PORT_TYPE_OPERATION.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, portType, operation);
        }

        boolean matchesPortTypeOperationInput(String wsdlTargetNamespace, String portType, String operation) {
            Matcher matcher = PATTERN_PORT_TYPE_OPERATION_INPUT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, portType, operation);
        }

        boolean matchesPortTypeOperationOutput(String wsdlTargetNamespace, String portType, String operation) {
            Matcher matcher = PATTERN_PORT_TYPE_OPERATION_OUTPUT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, portType, operation);
        }

        boolean matchesPortTypeOperationFault(String wsdlTargetNamespace, String portType,
                                              String operation, String fault) {
            Matcher matcher = PATTERN_PORT_TYPE_OPERATION_FAULT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, portType, operation, fault);
        }

        boolean matchesMessage(String wsdlTargetNamespace, String message) {
            Matcher matcher = PATTERN_MESSAGE.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, message);
        }

        boolean matchesMessagePart(String wsdlTargetNamespace, String message, String part) {
            Matcher matcher = PATTERN_MESSAGE_PART.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, message, part);
        }

        boolean matchesService(String wsdlTargetNamespace, String service) {
            Matcher matcher = PATTERN_SERVICE.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, service);
        }

        boolean matchesPort(String wsdlTargetNamespace, String service, String port) {
            Matcher matcher = PATTERN_PORT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, service, port);
        }

        boolean matchesBinding(String wsdlTargetNamespace, String binding) {
            Matcher matcher = PATTERN_BINDING.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, binding);
        }

        boolean matchesBindingOperation(String wsdlTargetNamespace, String binding, String operation) {
            Matcher matcher = PATTERN_BINDING_OPERATION.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, binding, operation);
        }

        boolean matchesBindingOperationInput(String wsdlTargetNamespace, String binding, String operation) {
            Matcher matcher = PATTERN_BINDING_OPERATION_INPUT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, binding, operation);
        }

        boolean matchesBindingOperationOutput(String wsdlTargetNamespace, String binding, String operation) {
            Matcher matcher = PATTERN_BINDING_OPERATION_OUTPUT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, binding, operation);
        }

        boolean matchesBindingOperationFault(String wsdlTargetNamespace, String binding,
                                             String operation, String fault) {
            Matcher matcher = PATTERN_BINDING_OPERATION_FAULT.matcher(wsdl11Pointer);
            return matches(wsdlTargetNamespace, matcher, binding, operation, fault);
        }

        private boolean matches(String wsdlTargetNamespace, Matcher matcher, String... args) {
            if (!targetNamespace.equals(wsdlTargetNamespace) || !matcher.matches()) {
                return false;
            }
            for (int i = 0; i < args.length; i++) {
                String matcherGroup = matcher.group(i + 1);
                if ((matcherGroup == null) || (!matcherGroup.equals(args[i]))) {
                    return false;
                }
            }
            return true;
        }
    }
}
