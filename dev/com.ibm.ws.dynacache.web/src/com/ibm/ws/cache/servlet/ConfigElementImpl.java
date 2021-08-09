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

import com.ibm.websphere.servlet.cache.*;
import java.util.*;

/**
 * This Class represents a variable specified in the servletcache.xml
 * document.  If an application uses its own Id or MetaDataGenerator, 
 * this class can be used to take configuration info from the xml file.
 * It stores all the data specified in the xml file for 
 * this variable.  The variable may be: 
 * <ul>
 * <li>a request parameter, (a String defined externally by a client), 
 * <li>a request attribute (java object, attached to a ServletRequest 
 * object previously in a servlet/JSP),
 * <li>a session parameter (java object attached
 * an HttpSession object) 
 * </ul>      
 * 
 * For example, a request parameter defined so in servletcache.xml<br><br>
 * &lt;request&gt;  <br>
 *   &lt;parameter id="cityname" data_id="city" required="true" /&gt;<br>
 * &lt;/request&gt; <br><br>
 * would generate a ConfigElement object where<ul>
 * <li>id returns "cityname",
 * <li> method returns null, <li>dataId returns "city", <li>invalidate
 * returns null, <li>required 
 * returns TRUE, <li>and type returns ConfigElement.RequestParameter. </ul>
 * 
 */
public class ConfigElementImpl extends ConfigElement {
   
   public ConfigElementImpl(int type) {
      this.type = type;
   }

   public ConfigElementImpl(int type, String id, String method, boolean excludeAll, HashSet exclude, String dataId, String invalidate, boolean ignoreValue, boolean required) {
      this.type =       type;
      this.id =         id;   
      this.method =     method; 
      this.dataId =     dataId;
      this.exclude =    exclude;
      this.excludeAll = excludeAll;
      this.invalidate = invalidate;
      this.ignoreValue= ignoreValue;
      this.required =   required;
   }   

   public String toString() {
      StringBuffer out = null;
       
      switch (type) {
         case RequestParameter: {out = new StringBuffer("Req Parm ");}break;
         case RequestAttribute: {out = new StringBuffer("Req Attr ");}break;
         case SessionParameter: {out = new StringBuffer("Ses Parm ");}break;
         case Cookie:           {out = new StringBuffer("Cookie ");}break;
      default: {out = new StringBuffer("no type? ");} break;
      }
      out = out.append(id);
      if (excludeAll) {
         out = out.append(" excludes this fragment from caching ");
      } else {
         if (method != null)
            out = out.append(", ").append(method);
         if (exclude != null) {
            out = out.append(", exclude when equal to ");
            Iterator i = exclude.iterator();
            while (i.hasNext()) {
               String k = (String)i.next();
               out = out.append(k).append(", ");
               /*
               if (((Boolean)exclude.get(k)).booleanValue()) {
                  out = out.append(" (pattern matching), ");
               } else {
                  out = out.append(", ");
               }
               */
            }
         }
         if (dataId != null) 
            out = out.append(", ").append(dataId);
         if (invalidate != null) 
            out = out.append(", ").append(invalidate);
         if (ignoreValue) 
            out = out.append(", ignore value");
         if (required) 
            out = out.append(", Required");
      }

      return out.toString();
   }

   public String  getId()  {return id ;}
public String  getMethod() {return method;}
public String  getDataId() {return dataId;}
public String  getInvalidate() {return invalidate;}
public HashSet getExclude() {return exclude;}
public boolean getExcludeAll() {return excludeAll;}
public boolean getRequired() {return required;}
public boolean getIgnoreValue() {return ignoreValue;}
   
}
