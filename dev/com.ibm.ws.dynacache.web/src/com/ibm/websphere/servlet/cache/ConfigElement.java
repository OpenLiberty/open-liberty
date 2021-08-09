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
package com.ibm.websphere.servlet.cache;

import java.util.HashSet;

/**
 * @deprecated 
 * This Class represents a variable specified in the cachespec.xml
 * document.  If an application uses its own Id or MetaDataGenerator, 
 * this class can be used to read the cache policies defined in the Application 
 * Assembly Tool (WAS 4.x and higher), or in the cachespec.xml file 
 * (WAS 5.0 and higher).
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
 * For example, a request parameter defined so in cachespec.xml<br><br>
 * &lt;request&gt;  <br>
 *   &lt;parameter id="cityname" data_id="city" required="true" /&gt;<br>
 * &lt;/request&gt; <br><br>
 * would generate a ConfigElement object where<ul>
 * <li>id returns "cityname",
 * <li> method returns null, <li>dataId returns "city", <li>invalidate
 * returns null, <li>required 
 * returns TRUE, <li>and type returns ConfigElement.RequestParameter. </ul>
 * @ibm-api 
 */
public abstract class ConfigElement {

  /**
   * This specifies the component type of the variable. For Servlets/ JSPs it may 
   * be set to ConfigElement.RequestParameter, ConfigElement.RequestAttribute, 
   * ConfigElement.SessionParameter or ConfigElement.Cookie 
   * @ibm-api 
   */
   public int type = -1;

   /**
    * This specifies that the type of component is a parameter from the request object.
    * @ibm-api 
    */
   public static final int RequestParameter = 0;

   /**
    * This specifies that the type of component is an attribute of the request object.
    * @ibm-api 
    */
   public static final int RequestAttribute = 1;

   /**
    * This specifies that the type of component is an parameter from the HTTPSession object.
    * @ibm-api 
    */
   public static final int SessionParameter = 2;

   /**
    * This specifies that the type of component is a cookie object.
    * @ibm-api 
    */
   public static final int Cookie           = 3;
                                       
   /**
    * This is the identifier of the cache entry.
    * It must be unique within the scope of the cache.
    * which is the WebSphere server group for the application.
    * @ibm-api 
    */
   public String  id = null;

   /**
    * This is the method to be called on the command or object.
    * @ibm-api 
    */
   public String  method = null;

   /**
    * This is the data id that makes this entry
    * invalid when it becomes invalid.
    * It must be unique within the same scope as the CacheEntry id.
    * These data ids identify the underlying dynamic content
    * (i.e., the raw data).
    * When a piece of data is used in only one CacheEntry,
    * the data id of the data can be the same as the CacheEntry id.
    * When a piece of data is used in multiple fragments,
    * its data id would be different from any of the CacheEntry ids.
    * @ibm-api 
    */
   public String  dataId = null;

   /** 
    * This is the invalidation ID for this entry.
    * @ibm-api 
    */
   public String  invalidate = null;

   /**
   * This is a hashset of strings whose keys, if equal to the value of this variable on the request,
   * will exclude this entry from being cached.
   * @ibm-api 
   */
   public HashSet exclude = null;   //do not cache if this variable's value is equal to any of these strings

   /**
    * This flag indicates whether or not this variable is required to generate the cache ID for this entry.
    * @ibm-api 
    */
   public boolean required = false;

   /**
   * This flag indicates that, if this variable is present on the request, the servlet/JSP should not be cached.
   * @ibm-api 
   */
   public boolean excludeAll = false;

   /**
   * This flag indicates that, if this variable is present on the request, its value will not be used to generate
   * the cache id.
   * @ibm-api 
   */
   public boolean ignoreValue = false; 

   /**
    * This gets the id variable.
    *
    * @return The cache id.
    * @ibm-api 
    */
   public abstract String  getId();

   /**
    * This gets the value of the method variable.
    *
    * @return The method name.
    * @ibm-api 
    */
   public abstract String  getMethod();

   /**
    * This gets the data id variable.
    *
    * @return The data id.
    * @ibm-api 
    */
   public abstract String  getDataId();

   /**
    * This gets value of the inavalidate variable.
    *
    * @return The invalidation id.
    * @ibm-api 
    */
   public abstract String  getInvalidate();

   /**
   * This gets the set of strings that, if equal to the value of this variable on the request,
   * will exclude this entry from being cached.
   *
   * @return Set of variables values.
   * @ibm-api 
   */
   public abstract HashSet getExclude();

   /**
   * This gets the value of the excludeAll flag.
   *
   * @return True, if a servlet/ JSP should not be cached.
   * @ibm-api 
   */
   public abstract boolean getExcludeAll();

   /**
   * This gets the value of the required flag.
   *
   * @return True, if the variable is required to create the cache id.
   * @ibm-api 
   */
   public abstract boolean getRequired();

   /**
   * This gets the value of the ignoreValue flag.
   *
   * @return True if the value of this variable can be ignored while building the cache id.
   * @ibm-api 
   */
   public abstract boolean getIgnoreValue();
   
}
