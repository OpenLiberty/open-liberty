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
package com.ibm.ws.cache.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


public class Component {
   private static TraceComponent tc = Tr.register(Component.class,"WebSphere Dynamic Cache","com.ibm.ws.cache.resources.dynacache");
   public String  type;
   public String  id;
   public boolean ignoreValue;
   public boolean multipleIds;
   public Method  method;
   public Field   field;
   public int index = -1;
   public boolean required;
   public HashMap values;
   public HashMap notValues;
   public ArrayList valueRanges;
   public ArrayList notValueRanges;

   // Implementation fields
   public int     iType;
   public Method  idMethod;
   public Field   idField;

   // valid component types
   public static final int METHOD = 0;
   public static final int FIELD  = 1;
   public static final int SESSION = 2;
   public static final int PARAMETER = 3;
   public static final int COOKIE = 4;
   public static final int HEADER = 5;
   public static final int LOCALE = 6;
   public static final int SOAP_ACTION = 7;
   public static final int SERVICE_OPERATION = 8;
   public static final int SERVICE_OPERATION_PARAMETER=9;
   public static final int SOAP_ENVELOPE=10;
   public static final int ATTRIBUTE=11;
   public static final int PATH_INFO=12;
   public static final int SERVLET_PATH=13;
   public static final int PARAMETER_LIST = 14;
   public static final int OPERATION = 15;
   public static final int PART = 16;
   public static final int WSDL_SERVICE = 17;
   public static final int WSDL_PORT = 18;
   public static final int SOAP_HEADER_ENTRY = 19;
   public static final int REQUEST_TYPE = 20;
   public static final int TILES_ATTRIBUTE = 21;
   public static final int PORTLET_SESSION = 22;
   public static final int PORTLET_WINDOW_ID = 23;
   public static final int PORTLET_MODE = 24;
   public static final int PORTLET_WINDOW_STATE = 25;
   public static final int SESSION_ID = 26;

   public void validate() {
      if (type.equalsIgnoreCase("method"))
         iType=METHOD;
      else if (type.equalsIgnoreCase("field"))
         iType=FIELD;
      else if (type.equalsIgnoreCase("session"))
         iType=SESSION;
      else if (type.equalsIgnoreCase("parameter"))
         iType=PARAMETER;
      else if (type.equalsIgnoreCase("cookie"))
         iType=COOKIE;
      else if (type.equalsIgnoreCase("header"))
         iType=HEADER;
      else if (type.equalsIgnoreCase("locale"))
         iType=LOCALE;
      else if (type.equalsIgnoreCase("SOAPAction"))
         iType=SOAP_ACTION;
      else if (type.equalsIgnoreCase("serviceOperation"))
         iType=SERVICE_OPERATION;
      else if (type.equalsIgnoreCase("serviceOperationParameter"))
         iType=SERVICE_OPERATION_PARAMETER;
      else if (type.equalsIgnoreCase("SOAPEnvelope"))
         iType=SOAP_ENVELOPE;
      else if (type.equalsIgnoreCase("attribute"))
         iType=ATTRIBUTE;
      else if (type.equalsIgnoreCase("pathInfo"))
         iType=PATH_INFO;
      else if (type.equalsIgnoreCase("servletpath"))
         iType=SERVLET_PATH;
      else if (type.equalsIgnoreCase("parameter-list"))
          iType=PARAMETER_LIST;
      else if (type.equalsIgnoreCase("operation"))
         iType=OPERATION;
      else if (type.equalsIgnoreCase("part"))
         iType=PART;
      else if (type.equalsIgnoreCase("WSDLServiceName"))
         iType=WSDL_SERVICE;
      else if (type.equalsIgnoreCase("WSDLPortName"))
         iType=WSDL_PORT;
      else if (type.equalsIgnoreCase("SOAPHeaderEntry")) {
          iType=SOAP_HEADER_ENTRY;
      }
      else if (type.equalsIgnoreCase("requestType"))   
	  iType=REQUEST_TYPE;  
      else if (type.equalsIgnoreCase("tiles_attribute"))
	iType=TILES_ATTRIBUTE;
      else if (type.equalsIgnoreCase("portletSession"))
    	iType=PORTLET_SESSION;
      else if (type.equalsIgnoreCase("portletWindowId"))
    	iType=PORTLET_WINDOW_ID;
      else if (type.equalsIgnoreCase("portletMode"))
    	iType=PORTLET_MODE;
      else if (type.equalsIgnoreCase("portletWindowState"))
    	iType=PORTLET_WINDOW_STATE;
      else if (type.equalsIgnoreCase("sessionId"))
    	iType=SESSION_ID;
      else
        Tr.error(tc,"DYNA0049E",new Object[] {type});
   }



   public String toString() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("type       : "+type);
      pw.println("id         : "+id);
      pw.println("ignoreValue: "+ignoreValue);
      pw.println("mutipleIds : "+multipleIds);
      pw.println("method     : "+method);
      pw.println("index  : "+index);
      pw.println("required   : "+required);
      pw.println("values     : "+values);
      pw.println("not-values : "+notValues);
      pw.println("value ranges     : "+valueRanges);
      pw.println("not-value ranges : "+notValueRanges);
      return sw.toString();
   }


   public String fancyFormat(int level) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("id         : "+id);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("type       : "+type);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("ignoreValue: "+ignoreValue);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("multipleIds   : "+multipleIds);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("method     : "+method);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("index  : "+index);
      for (int ii = level;ii >0; ii--) pw.print("\t");
      pw.println("required   : "+required);
      if (values != null && values.size() > 0) {
         for (int ii = level;ii >0; ii--) pw.print("\t");
         pw.println("value        : "+values);
         Iterator i = values.values().iterator();
         while (i.hasNext()) {
            pw.println(((Value) i.next()).fancyFormat(level+1));
         }                                                           }
      if (notValues != null && notValues.size() > 0) {
        for (int ii = level;ii >0; ii--) pw.print("\t");
        pw.println("not-value    : "+notValues);
        Iterator i = notValues.values().iterator();
        while (i.hasNext()) {
           pw.println(((NotValue) i.next()).fancyFormat(level+1));
        }   
      }
      if (valueRanges != null && valueRanges.size() > 0) {
        for (int ii = level;ii >0; ii--) pw.print("\t");
        pw.println("value ranges     : "+ valueRanges);
        Iterator i = valueRanges.iterator();
        while (i.hasNext()) {
           pw.println(((Range) i.next()).fancyFormat(level+1));
        }   
      }
      if (notValueRanges != null && notValueRanges.size() > 0) {
        for (int ii = level;ii >0; ii--) pw.print("\t");
        pw.println("not-value ranges : "+ notValueRanges);
        Iterator i = notValueRanges.iterator();
        while (i.hasNext()) {
           pw.println(((Range) i.next()).fancyFormat(level+1));
        }   
      }
      return sw.toString();
   }

   public void getESIComponent(StringBuffer sb) {
      if (required) {
          if(ignoreValue) {
              sb.append("<").append(id);
              sb.append(">");
              getESIValues(sb);
          }
          else {
              sb.append(id);
              getESIValues(sb);	     
          }
      }
      else if(!required){
          sb.append("[");
          if(ignoreValue) {
              sb.append("<").append(id);
              sb.append(">");
              getESIValues(sb);	    	    
           }
           else {
               sb.append(id);
               getESIValues(sb);	     
           }
           sb.append("]");
      }
   }

   public void getESIValues(StringBuffer sb) {
      if (values != null && values.size() > 0) {
         sb.append("={");
         Iterator i = values.values().iterator();
         while (i.hasNext()) {
            sb.append(((Value)i.next()).value);
            if (i.hasNext()) sb.append(" ");
         }
         sb.append("}");
      } else if (notValues != null && notValues.size() > 0) {
      	sb.append("={");
        Iterator i = notValues.values().iterator();
        while (i.hasNext()) {
           sb.append(((NotValue)i.next()).notValue);
           if (i.hasNext()) sb.append(" ");
        }
        sb.append("}");
      }
   }

   public Object clone() {
      Component c =  new Component();
      c.type = type;
      c.iType = iType;
      c.id = id;
      c.ignoreValue = ignoreValue;
      c.multipleIds = multipleIds;
      c.required = required;
      if (method != null) c.method = (Method) method.clone();
      if (field != null) c.field = (Field) field.clone();
      c.index = index;
      if (values != null)  {
         c.values = new HashMap();
         for (Iterator i = values.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            c.values.put(key, ((Value)values.get(key)).clone());
         }
      }
      if (notValues != null)  {
        c.notValues = new HashMap();
        for (Iterator i = notValues.keySet().iterator(); i.hasNext();) {
           Object key = i.next();
           c.notValues.put(key, ((NotValue)notValues.get(key)).clone());
        }
     }
      
     if (valueRanges != null)  {
        c.valueRanges = new ArrayList();
        if (valueRanges != null){
          	c.valueRanges = new ArrayList();
          	for (Iterator i = valueRanges.iterator(); i.hasNext();) {
          		c.valueRanges.add(((Range)i.next()).clone());
          	}
          }
     }
     
     if (notValueRanges != null)  {
        c.notValueRanges = new ArrayList();
        if (notValueRanges != null){
          	c.notValueRanges = new ArrayList();
          	for (Iterator i = notValueRanges.iterator(); i.hasNext();) {
          		c.notValueRanges.add(((Range)i.next()).clone());
          	}
          }
     }
      

      return c;
   }
}
