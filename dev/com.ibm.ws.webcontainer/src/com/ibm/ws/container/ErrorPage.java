/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container;

/**
 * An error page.  Prior to Servlet 3.0, two cases were valid:
 * 
 * An error-code error page:
 * 
 * &lt;error-page&gt;
 *   &lt;error-code&gt;404&lt;/error-code&gt;
 *   &lg;location&gt;/MyDefaultErrorPage.html&lt;/location&gt;
 * &lt;/error-page&gt;
 * 
 * An exception-type error page:
 * 
 * &lt;error-page&gt;
 *   &lt;exception-type&gt;404&lt;/exception-type&gt; 
 *   &lg;location&gt;/MyDefaultErrorPage.html&lt;/location&gt;
 * &lt;/error-page&gt;
 *  
 * Starting with Servlet 3.0, both error-code and exception-type could
 * be omitted, creating a default error page:
 * 
 * &lt;error-page&gt
 *   &lg;location&gt;/MyDefaultErrorPage.html&lt;/location&gt;
 * &lt;/error-page&gt;
 * 
 * The Liberty Profile initial release (which has servlet container feature version 3.0) could
 * have supported default error pages, but did not.  The Liberty Profile servlet container feature
 * version 3.1 adds support for default error pages.
 * 
 * Default error pages are ignored if the servlet container feature version is 3.0.
 * 
 */
public class ErrorPage {
    
  private String location;
  private String errorParam; // error-code value or exception-type

  public ErrorPage(String location, String error) {
    this.location = location;
    this.errorParam = error;
  }

  // Liberty 3386 - support incremental field assignment during xml parsing
  public ErrorPage() {
    // do nothing
  }

  public ErrorPage(String location) {
    this.location = location;
  }
  
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getErrorParam() {
    return errorParam;
  }

  public void setErrorParam(String errorParam) {
    this.errorParam = errorParam;
  }

  // The exception should probably never be obtained using the current context class loader.
  // Use of the WAR class loader is correct.
  @SuppressWarnings("rawtypes")
  public Class getException() {
    try {
      return Class.forName(errorParam, true, Thread.currentThread().getContextClassLoader()).newInstance().getClass();
    } catch (Exception e) {
      return null;
    }
  }

  // PK52168 - STARTS
  @SuppressWarnings("rawtypes")  
  public Class getException(ClassLoader warClassLoader) {
    try {
      return Class.forName(errorParam, true, warClassLoader);
    } catch (Exception e) {
      return null;
    }
  }
  // PK52168 - ENDS
}
