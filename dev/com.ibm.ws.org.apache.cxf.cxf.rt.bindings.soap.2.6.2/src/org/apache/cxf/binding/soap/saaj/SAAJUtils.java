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

package org.apache.cxf.binding.soap.saaj;

import java.lang.reflect.Method;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;

import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cxf.common.logging.LogUtils;

/**
 *
 */
public final class SAAJUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(SAAJUtils.class); 

    private SAAJUtils() {
        //not constructed
    }

    public static SOAPHeader getHeader(SOAPMessage m) throws SOAPException {
        try {
            return m.getSOAPHeader();
        } catch (UnsupportedOperationException ex) {
            return m.getSOAPPart().getEnvelope().getHeader();
        }
    }
    public static SOAPBody getBody(SOAPMessage m) throws SOAPException {
        try {
            return m.getSOAPBody();
        } catch (UnsupportedOperationException ex) {
            return m.getSOAPPart().getEnvelope().getBody();
        } catch (IllegalArgumentException ex) {
            //java9
            return null;
        }
    }
    public static void setFaultCode(SOAPFault f, QName code) throws SOAPException {
        try {
            f.setFaultCode(code);
        } catch (Throwable t) {
            int count = 1;
            String pfx = "fc1";
            while (!StringUtils.isEmpty(f.getNamespaceURI(pfx))) {
                count++;
                pfx = "fc" + count;
            }

	    // if no namespace URI is specified, don't call addNamespaceDeclaration(), it will fail
	    // with IllegalArgumentException: WSWS3382E
	    String xmlNS = code.getNamespaceURI();
	    if (xmlNS == null || xmlNS.length() == 0) {
	       // Log message and ignore
	       LOG.log(Level.FINE, "The prefix is: " + pfx + "  but there is no namespace.");
            }
	    else {
                f.addNamespaceDeclaration(pfx, xmlNS);
	    }
            f.setFaultCode(pfx + ":" + code.getLocalPart());
        }
    }

    public static Element adjustPrefix(Element e, String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        try {
            String s = e.getPrefix();
            if (!prefix.equals(s)) {
                e.setPrefix(prefix);
                if (e instanceof SOAPElement) {
                    ((SOAPElement)e).removeNamespaceDeclaration(s);
                } else if (e.getClass().getName().equals(
                       "com.sun.org.apache.xerces.internal.dom.ElementNSImpl")) {
                    //since java9 159 SOAPPart1_1Impl.getDocumentElement not return SOAPElement
                    try {
                        Method method = e.getClass().getMethod("removeAttribute", String.class);
                        method.invoke(e, "xmlns:" + s);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        } catch (Throwable t) {
            //likely old old version of SAAJ, we'll just try our best
        }
        return e;
    }
}
