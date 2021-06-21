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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Fault;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;

import org.apache.cxf.common.util.ExtensionInvocationHandler;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.wsdl.WSDLConstants;


public final class SOAPBindingUtil {
    private static Map<String, String> bindingMap = new HashMap<>();

    static {
        bindingMap.put("RPC", "SOAPBinding.Style.RPC");
        bindingMap.put("DOCUMENT", "SOAPBinding.Style.DOCUMENT");
        bindingMap.put("LITERAL", "SOAPBinding.Use.LITERAL");
        bindingMap.put("ENCODED", "SOAPBinding.Use.ENCODED");
        bindingMap.put("BARE", "SOAPBinding.ParameterStyle.BARE");
        bindingMap.put("WRAPPED", "SOAPBinding.ParameterStyle.WRAPPED");
    }

    private SOAPBindingUtil() {
    }

    public static String getBindingAnnotation(String key) {
        return bindingMap.get(key.toUpperCase());
    }

    public static <T> T getProxy(Class<T> cls, Object obj) {
        InvocationHandler ih = new ExtensionInvocationHandler(obj);
        /*
         * If we put proxies into the loader of the proxied class, they'll just pile up.
         */
        Object proxy = null;
        try {
            proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                              new Class[] {cls}, ih);
        } catch (Throwable ex) {
            // Using cls classloader as a fallback to make it work within OSGi
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != cls.getClassLoader()) {
                proxy = Proxy.newProxyInstance(cls.getClassLoader(),
                                              new Class[] {cls}, ih);
            } else {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException)ex;
                }
                throw new RuntimeException(ex);
            }
        }
        return cls.cast(proxy);
    }

    public static boolean isSOAPBinding(Binding binding) {
        for (Object obj : binding.getExtensibilityElements()) {
            if (isSOAPBinding(obj)) {
                return true;
            }
        }
        return false;
    }

    public static String getBindingStyle(Binding binding) {
        for (Object obj : binding.getExtensibilityElements()) {
            if (isSOAPBinding(obj)) {
                return getSoapBinding(obj).getStyle();
            }
        }
        return "";
    }

    public static SOAPOperation getSoapOperation(List<ExtensibilityElement> exts) {
        if (exts != null) {
            for (ExtensibilityElement ext : exts) {
                if (isSOAPOperation(ext)) {
                    return getSoapOperation(ext);
                }
            }
        }
        return null;
    }

    public static SOAPOperation getSoapOperation(Object obj) {
        if (isSOAPOperation(obj)) {
            return getProxy(SOAPOperation.class, obj);
        }
        return null;
    }

    public static String getSOAPOperationStyle(BindingOperation bop) {
        String style = "";
        if (bop != null) {
            for (Object obj : bop.getExtensibilityElements()) {
                if (isSOAPOperation(obj)) {
                    style = getSoapOperation(obj).getStyle();
                    break;
                }
            }
        }
        return style;
    }

    public static SOAPBody getBindingInputSOAPBody(BindingOperation bop) {
        BindingInput bindingInput = bop.getBindingInput();
        if (bindingInput != null) {
            for (Object obj : bindingInput.getExtensibilityElements()) {
                if (isSOAPBody(obj)) {
                    return getSoapBody(obj);
                }
            }
        }

        return null;
    }

    public static SOAPBody getBindingOutputSOAPBody(BindingOperation bop) {
        BindingOutput bindingOutput = bop.getBindingOutput();
        if (bindingOutput != null) {
            for (Object obj : bindingOutput.getExtensibilityElements()) {
                if (isSOAPBody(obj)) {
                    return getSoapBody(obj);
                }
            }
        }

        return null;
    }

    public static SOAPBody getSoapBody(List<ExtensibilityElement> exts) {
        if (exts != null) {
            for (ExtensibilityElement ext : exts) {
                if (isSOAPBody(ext)) {
                    return getSoapBody(ext);
                }
            }
        }
        return null;
    }

    public static SOAPBody getSoapBody(Object obj) {
        if (isSOAPBody(obj)) {
            return getProxy(SOAPBody.class, obj);
        }
        return null;
    }

    public static boolean isSOAPBody(Object obj) {
        return obj instanceof SOAPBody || obj instanceof SOAP12Body;
    }

    public static boolean isSOAPHeader(Object obj) {
        return obj instanceof SOAPHeader || obj instanceof SOAP12Header;
    }

    public static List<SOAPHeader> getSoapHeaders(List<ExtensibilityElement> exts) {
        List<SOAPHeader> headers = new ArrayList<>();
        if (exts != null) {
            for (ExtensibilityElement ext : exts) {
                if (isSOAPHeader(ext)) {
                    headers.add(getSoapHeader(ext));
                }
            }
        }
        return headers;
    }

    public static SOAPHeader getSoapHeader(Object obj) {
        if (isSOAPHeader(obj)) {
            return getProxy(SOAPHeader.class, obj);
        }
        return null;
    }

    public static SOAPAddress getSoapAddress(Object obj) {
        if (isSOAPAddress(obj)) {
            return getProxy(SOAPAddress.class, obj);
        }
        return null;
    }

    public static boolean isSOAPAddress(Object obj) {
        return obj instanceof SOAPAddress || obj instanceof SOAP12Address;
    }

    public static SOAPHeader getBindingInputSOAPHeader(BindingOperation bop) {
        BindingInput bindingInput = bop.getBindingInput();
        if (bindingInput != null) {
            for (Object obj : bindingInput.getExtensibilityElements()) {
                if (isSOAPHeader(obj)) {
                    return getProxy(SOAPHeader.class, obj);
                }
            }
        }

        return null;
    }

    public static SOAPHeader getBindingOutputSOAPHeader(BindingOperation bop) {
        BindingOutput bindingOutput = bop.getBindingOutput();
        if (bindingOutput != null) {
            for (Object obj : bindingOutput.getExtensibilityElements()) {
                if (isSOAPHeader(obj)) {
                    return getProxy(SOAPHeader.class, obj);
                }
            }
        }

        return null;
    }

    public static SOAPBinding getSoapBinding(List<ExtensibilityElement> exts) {
        for (ExtensibilityElement ext : exts) {
            if (isSOAPBinding(ext)) {
                return getSoapBinding(ext);
            }
        }
        return null;
    }

    public static SOAPBinding getSoapBinding(Object obj) {
        if (isSOAPBinding(obj)) {
            return getProxy(SOAPBinding.class, obj);
        }
        return null;
    }

    public static boolean isSOAPBinding(Object obj) {
        return obj instanceof SOAPBinding || obj instanceof SOAP12Binding;
    }

    public static List<SOAPFault> getBindingOperationSoapFaults(BindingOperation bop) {
        List<SOAPFault> faults = new ArrayList<>();
        for (Object obj : bop.getBindingFaults().values()) {
            if (!(obj instanceof BindingFault)) {
                continue;
            }
            BindingFault faultElement = (BindingFault) obj;
            for (Object flt : faultElement.getExtensibilityElements()) {
                SOAPFault fault = getSoapFault(flt);
                if (fault != null) {
                    faults.add(fault);
                }
            }
        }
        return faults;
    }

    public static SOAPFault getSoapFault(Object obj) {
        if (isSOAPFault(obj)) {
            return getProxy(SOAPFault.class, obj);
        }
        return null;
    }

    public static boolean isMixedStyle(Binding binding) {
        String bindingStyle = "";
        String previousOpStyle = "";
        String style = "";

        for (Object obj : binding.getExtensibilityElements()) {
            if (isSOAPBinding(obj)) {
                SOAPBinding soapBinding = getSoapBinding(obj);
                bindingStyle = soapBinding.getStyle();
                if (bindingStyle == null) {
                    bindingStyle = "";
                }
            }
        }
        for (Object bobj : binding.getBindingOperations()) {
            BindingOperation bop = (BindingOperation)bobj;
            for (Object obj : bop.getExtensibilityElements()) {
                if (isSOAPOperation(obj)) {
                    SOAPOperation soapOperation = getSoapOperation(obj);
                    style = soapOperation.getStyle();
                    if (style == null) {
                        style = "";
                    }

                    if ("".equals(bindingStyle) && "".equals(previousOpStyle) || "".equals(bindingStyle)
                        && previousOpStyle.equalsIgnoreCase(style)) {
                        previousOpStyle = style;

                    } else if (!"".equals(bindingStyle) && "".equals(previousOpStyle)
                               && bindingStyle.equalsIgnoreCase(style)
                               || bindingStyle.equalsIgnoreCase(previousOpStyle)
                               && bindingStyle.equalsIgnoreCase(style)) {
                        previousOpStyle = style;
                    } else if (!"".equals(bindingStyle) && "".equals(style) && "".equals(previousOpStyle)) {
                        continue;
                    } else {
                        return true;
                    }

                }

            }
        }
        return false;
    }

    public static String getCanonicalBindingStyle(Binding binding) {
        String bindingStyle = getBindingStyle(binding);
        if (!StringUtils.isEmpty(bindingStyle)) {
            return bindingStyle;
        }
        for (Object bobj : binding.getBindingOperations()) {
            BindingOperation bindingOp = (BindingOperation)bobj;
            String bopStyle = getSOAPOperationStyle(bindingOp);
            if (!StringUtils.isEmpty(bopStyle)) {
                return bopStyle;
            }
        }
        return "";

    }

    public static boolean isSOAPOperation(Object obj) {
        return obj instanceof SOAPOperation || obj instanceof SOAP12Operation;
    }

    public static boolean isSOAPFault(Object obj) {
        return obj instanceof SOAPFault || obj instanceof SOAP12Fault;
    }

    public static SOAPAddress createSoapAddress(ExtensionRegistry extReg, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(Port.class,
                                                               WSDLConstants.QNAME_SOAP12_BINDING_ADDRESS);
        } else {
            extElement = extReg.createExtension(Port.class,
                                                             WSDLConstants.QNAME_SOAP_BINDING_ADDRESS);
        }
        return getSoapAddress(extElement);
    }

    public static SOAPBody createSoapBody(ExtensionRegistry extReg, Class<?> clz, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(clz, new QName(WSDLConstants.NS_SOAP12,
                                                                           "body"));
        } else {
            extElement = extReg.createExtension(clz, new QName(WSDLConstants.NS_SOAP11,
                                                                         "body"));
        }
        return getSoapBody(extElement);
    }

    public static SOAPBinding createSoapBinding(ExtensionRegistry extReg, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(Binding.class,
                                                               new QName(WSDLConstants.NS_SOAP12,
                                                                         "binding"));
            ((SOAP12Binding)extElement).setTransportURI(WSDLConstants.NS_SOAP_HTTP_TRANSPORT);
        } else {
            extElement = extReg.createExtension(Binding.class,
                                                             new QName(WSDLConstants.NS_SOAP11,
                                                                       "binding"));
            ((SOAPBinding)extElement).setTransportURI(WSDLConstants.NS_SOAP_HTTP_TRANSPORT);
        }
        return getSoapBinding(extElement);
    }

    public static SOAPOperation createSoapOperation(ExtensionRegistry extReg, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(BindingOperation.class,
                                                                 new QName(WSDLConstants.NS_SOAP12,
                                                                           "operation"));
        } else {
            extElement = extReg.createExtension(BindingOperation.class,
                                                               new QName(WSDLConstants.NS_SOAP11,
                                                                         "operation"));
        }
        return getSoapOperation(extElement);
    }

    public static SOAPFault createSoapFault(ExtensionRegistry extReg, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(BindingFault.class,
                                                             new QName(WSDLConstants.NS_SOAP12,
                                                                       "fault"));
        } else {
            extElement = extReg.createExtension(BindingFault.class,
                                                           new QName(WSDLConstants.NS_SOAP11,
                                                                     "fault"));
        }
        return getSoapFault(extElement);
    }

    public static SOAPHeader createSoapHeader(ExtensionRegistry extReg, Class<?> clz, boolean isSOAP12)
        throws WSDLException {
        ExtensibilityElement extElement = null;
        if (isSOAP12) {
            extElement = extReg.createExtension(clz,
                                                              new QName(WSDLConstants.NS_SOAP12,
                                                                        "header"));
        } else {
            extElement = extReg.createExtension(clz,
                                                            new QName(WSDLConstants.NS_SOAP11,
                                                                      "header"));
        }
        return getSoapHeader(extElement);
    }


    public static void addSOAPNamespace(Definition definition, boolean isSOAP12) {
        Map<?, ?> namespaces = definition.getNamespaces();
        if (isSOAP12
            && !namespaces.values().contains(WSDLConstants.NS_SOAP12)) {
            definition.addNamespace("soap12", WSDLConstants.NS_SOAP12);
        } else if (!namespaces.values().contains(WSDLConstants.NS_SOAP11)) {
            definition.addNamespace("soap", WSDLConstants.NS_SOAP11);
        }
    }

    // Liberty change: getSoapStyle method below is added
    public static javax.jws.soap.SOAPBinding.Style getSoapStyle(String soapStyle) {
        if ("".equals(soapStyle)) {
            return null;
        } else if ("RPC".equalsIgnoreCase(soapStyle)) {
            return javax.jws.soap.SOAPBinding.Style.RPC;
        } else {
            return javax.jws.soap.SOAPBinding.Style.DOCUMENT;
        }
    } // Liberty change: end
    
    // Liberty change: getSoapUse method below is added
    public static javax.jws.soap.SOAPBinding.Use getSoapUse(String soapUse) {
        if ("".equals(soapUse)) {
            return null;
        } else if ("ENCODED".equalsIgnoreCase(soapUse)) {
            return javax.jws.soap.SOAPBinding.Use.ENCODED;
        } else {
            return javax.jws.soap.SOAPBinding.Use.LITERAL;
        }
    } // Liberty change: end

}
