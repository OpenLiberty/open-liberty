/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.cache.config.Component;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.xml.ParserFactory;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.ServerCache;

public class WebServicesCacheProcessor extends FragmentCacheProcessor {
   protected static TraceComponent tc = Tr.register(WebServicesCacheProcessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   public static final String SOAP_ACTION = "SOAPAction";

   // id constants for type=BODY
   public static final String HASH = "Hash";
   public static final String LITERAL = "Literal";

   SAXParserFactory factory = null;
   SAXParser parser = null;
   SOAPRequestHandler soapHandler = null;
   MessageDigest md = null;

   private void newSAXParser() {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "WSCP.newSAXParser()");
      try {
         factory = ParserFactory.newSAXParserFactory();
         factory.setFeature("http://xml.org/sax/features/namespaces", true);
         parser = factory.newSAXParser();
         soapHandler = new SOAPRequestHandler();
      } catch (ParserConfigurationException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.WebServicesCacheProcessor.newSAXParser", "44", this);
         if (tc.isDebugEnabled())
            Tr.debug(tc, "WSCP: ParserConfigurationException in newSAXParser()");
      } catch (SAXNotRecognizedException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.WebServicesCacheProcessor.newSAXParser", "47", this);
         if (tc.isDebugEnabled())
            Tr.debug(tc, "WSCP: SAXNotRecognizedException in newSAXParser()");
      } catch (SAXException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.WebServicesCacheProcessor.newSAXParser", "50", this);
         if (tc.isDebugEnabled())
            Tr.debug(tc, "WSCP: SAXException in newSAXParser()");
      }
   }

   // Please call reset() before you call execute().
   public void reset(com.ibm.ws.cache.config.ConfigEntry ce) {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "WSCP.reset()");
      if (soapHandler != null)
         soapHandler.reset();
      super.reset(ce);
   }

   protected Object getComponentValue(Component c) {
      if (tc.isDebugEnabled())
         Tr.debug(tc, "WSCP.getComponentValue(Component) for " + configEntry.name);
      if (c == null) {
         if (tc.isDebugEnabled())
            Tr.debug(tc, "WSCP.getComponentValue(): null component passed in, returning null.");
         return null;
      }
      /* 
       * PM98732 - default behavior is to set Component.required to true.  This overrides what the customer has set in the cachespec.xml.   I have added a custom property if set to false then 
       * we won't override the value set in the cachespec.xml.   
       */
      boolean setRequiredToTrue = true;
      DCache cache = ServerCache.getCache(configEntry.instanceName);
      if(cache != null) {
    	  setRequiredToTrue = cache.getCacheConfig().isWebservicesSetRequiredTrue();
    	  if (tc.isDebugEnabled())
    		  Tr.debug(tc, "WSCP.getComponentValue(): webservicesSetRequireTrue: " + setRequiredToTrue);
      }
      switch (c.iType) {
         case Component.SERVICE_OPERATION :
            // If serviceOperation type component returns null, it is uncacheable
         	if (setRequiredToTrue)
                c.required = true;
            if (!request.getMethod().equals("POST"))
               return null;
            // "serviceOperation" requires value or not-value
            if (c.values == null && c.notValues == null) {
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP.getComponentValue(): serviceOperation type component has neither value nor not-value, returning null.");
               return null;
            }
            String serviceOperation = null;
            try {
               //Should not create parser twice for one WSCP!
               if (parser == null)
                  newSAXParser();
               //Should not parse one request twice!
               if (!soapHandler.isParsed()) {
                  request.setGeneratingId(true);
                  parser.parse(request.getInputStream(), soapHandler);
                  request.setGeneratingId(false);
               }
               serviceOperation = soapHandler.getServiceOperation();
            } catch (SAXException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "90", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: SAXException in getComponentValue(), returning null.");
               return null;
            } catch (IOException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "94", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: IOException in getComponentValue(), returning null.");
               return null;
            }
            return serviceOperation;

         case Component.SERVICE_OPERATION_PARAMETER :
            // If serviceOperationParameter type component returns null, it is uncacheable
         	if (setRequiredToTrue)
                c.required = true;
            if (!request.getMethod().equals("POST"))
               return null;
            // "serviceOperation" requires value or not-value
            if (c.values == null && c.notValues == null) {
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP.getComponentValue(): serviceOperationParameter type component has neither value nor not-value, returning null.");
               return null;
            }
            String serviceParameter = null;
            try {
               //Should not create parser twice for one WSCP!
               if (parser == null)
                  newSAXParser();
               //Should not parse one request twice!
               if (!soapHandler.isParsed()) {
                  request.setGeneratingId(true);
                  parser.parse(request.getInputStream(), soapHandler);
                  request.setGeneratingId(false);
               }
               serviceParameter = (String) soapHandler.getServiceOperationParameter().get(c.id);
            } catch (SAXException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "121", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: SAXException in getComponentValue(), returning null.");
               return null;
            } catch (IOException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "125", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: IOException in getComponentValue(), returning null.");
               return null;
            }
            return serviceParameter;

         case Component.SOAP_ACTION :
            // If SOAPAction type component returns null, it is uncacheable
         	if (setRequiredToTrue)
                c.required = true;
            // "SOAPAction" requires value or not-value
            if (c.values == null && c.notValues == null) {
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP.getComponentValue(): serviceOperation type component has neither value nor not-value, returning null.");
               return null;
            }
            String soapAction = request.getHeader(SOAP_ACTION);
            if (soapAction != null) {
               soapAction = soapAction.trim();
               if (soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
                  soapAction = soapAction.substring(1, soapAction.length() - 1);
               }
            }
            return soapAction;

         case Component.SOAP_ENVELOPE :
            // If body type component returns null, it is uncacheable
         	if (setRequiredToTrue)
                c.required = true;
            if (!request.getMethod().equals("POST"))
               return null;
            request.setGeneratingId(true);
            try {
               byte[] buffer = new byte[1024];
               InputStream in = request.getInputStream();
               ByteArrayOutputStream reqOS = new ByteArrayOutputStream();
               int length;
               while ((length = in.read(buffer)) != -1) {
                  reqOS.write(buffer, 0, length);
               }
               byte[] reqContent = reqOS.toByteArray();

               // id is Literal expression of request
               if (c.id.equalsIgnoreCase(LITERAL)) {
                  return new String(reqContent);
               }
               // id is Hash value of request
               // else if(c.id.equalsIgnoreCase(HASH)){
               else { //default is hash value.
                  if (md == null)
                     md = MessageDigest.getInstance("SHA");
                  byte[] reqHash = md.digest(reqContent);
                  return Base64Coder.encode(reqHash);
               }
            } catch (NoSuchAlgorithmException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "175", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: NoSuchAlgorithmException in getComponentValue(), returning null.");
               return null;
            } catch (IOException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.config.WebServicesCacheProcessor.getComponentValue", "179", this);
               if (tc.isDebugEnabled())
                  Tr.debug(tc, "WSCP: IOException in getComponentValue(), returning null.");
               return null;
            } finally {
               request.setGeneratingId(false);
            }
            //break;
         default :
            return super.getComponentValue(c);
      }
      //return null;
   }
}
