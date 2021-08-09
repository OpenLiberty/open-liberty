/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

class SOAPRequestHandler extends DefaultHandler {

   protected static TraceComponent tc = Tr.register(SOAPRequestHandler.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   static final String SOAP_ENV_URI = "http://schemas.xmlsoap.org/soap/envelope/";
   //static final String SOAP12_6_ENV_URI = "http://www.w3.org/2001/06/soap-envelope";
   //static final String SOAP12_9_ENV_URI = "http://www.w3.org/2001/09/soap-envelope";
   //static final String SOAP12_12_ENV_URI = "http://www.w3.org/2001/12/soap-envelope";

   HashMap serviceOperationParameter = new HashMap();
   String parameterName = null;
   StringBuffer parameterValue = new StringBuffer();
   String serviceURI = null;
   String serviceOperation = null;
   boolean inBody = false;
   boolean inOperation = false;
   boolean isParsed = false;
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.startElement(); uri=" + uri + ",localname=" + localName + ",qName=" + qName + ".");
      if (localName.equals("Body") && uri.equals(SOAP_ENV_URI)) {
         inBody = true;
      } else if (inBody) {
         serviceURI = uri;
         serviceOperation = localName;
         inBody = false;
         inOperation = true;
      } else if (inOperation) {
         if (parameterName == null) {
            parameterName = localName;
         } else {
            serviceOperationParameter.put(parameterName, parameterValue.toString().trim());
            parameterValue = new StringBuffer();
            parameterName = localName;
         }
      }
   }

   public void characters(char[] ch, int start, int length) {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.characters(); ch=" + new String(ch, start, length));
      if (parameterName != null) {
         parameterValue.append(ch, start, length);
      }
   }

   public void endElement(String uri, String localName, String qName) throws SAXException {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.endElement(); uri=" + uri + ",localname=" + localName + ",qName=" + qName + ".");
      if (localName.equals("Body") && uri.equals("http://schemas.xmlsoap.org/soap/envelope/")) {
         inBody = false;
      } else if (uri.equals(serviceURI) && localName.equals(serviceOperation)) {
         inOperation = false;
      } else if (parameterName != null) {
         serviceOperationParameter.put(parameterName, parameterValue.toString().trim());
         parameterValue = new StringBuffer();
         parameterName = null;
      }

   }

   public void endDocument() throws SAXException {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.endDocument();");
      isParsed = true;
   }

   public HashMap getServiceOperationParameter() {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.getServiceOperationParamete(); return:" + serviceOperationParameter);
      return serviceOperationParameter;
   }

   public String getServiceOperation() {
      String s = serviceURI + ":" + serviceOperation;
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.getServiceOperation(); return:" + s);
      return s;
   }

   public boolean isParsed() {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandler.isParsed(); return:" + isParsed);
      return isParsed;
   }

   public void reset() {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "SOAPRequestHandlar.reset();");
      serviceOperationParameter.clear();
      parameterName = null;
      serviceURI = null;
      serviceOperation = null;
      inBody = false;
      inOperation = false;
      isParsed = false;
   }

}
