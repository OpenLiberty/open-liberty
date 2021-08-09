/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.webapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHost;
import com.ibm.ws.webcontainer.osgi.metadata.WebCollaboratorComponentMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.metadata.WebComponentMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.metadata.WebModuleMetaDataImpl;
import com.ibm.wsspi.webcontainer.metadata.WebCollaboratorComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * Liberty level of configuration for the Webapp.
 */
public class WebAppConfiguration extends com.ibm.ws.webcontainer.webapp.WebAppConfiguration
{
  private final WebModuleMetaDataImpl webModuleMetaData;

  /**
   * The "default" component metadata that is used for requests that don't
   * otherwise have a metadata (e.g., plain files rather than servlets).
   */
  private final WebComponentMetaData webComponentMetaData;
  
  private DynamicVirtualHost dVirtualHost = null;

  private List<String> orderedLibPaths;
  
  private java.util.Dictionary<java.lang.String,java.lang.String> bundleHeaders = null;
  
  boolean cdiEnabled = false;
  /**
   * Basic constructor with actual config set later.
   * 
   * @param id
   */
  public WebAppConfiguration(ApplicationMetaData amd, String id)
  {
    super(id);
    this.webModuleMetaData = new WebModuleMetaDataImpl(amd);
    webModuleMetaData.setWebAppConfiguration(this);
    WebCollaboratorComponentMetaData wccmd = new WebCollaboratorComponentMetaDataImpl(webModuleMetaData);
    this.webModuleMetaData.setCollaboratorComponentMetaData(wccmd);
    this.webComponentMetaData = new WebComponentMetaDataImpl(this.webModuleMetaData);
  }

  // Override to ensure all WebApp are osgi.WebApp.
  @Override
  public void setWebApp(com.ibm.ws.webcontainer.webapp.WebApp webApp)
  {
      super.setWebApp((WebApp)webApp);
  }

  // Convenience method to upcast the result to osgi.WebApp.
  public WebApp getWebApp()
  {
      return (WebApp)super.getWebApp();
  }

  public WebModuleMetaData getMetaData()
  {
      return this.webModuleMetaData;
  }

  public WebComponentMetaData getDefaultComponentMetaData()
  {
    return webComponentMetaData;
  }

  @Override
  public boolean isJCDIEnabled()
  {
      return cdiEnabled;
  }

  @Override
  public void setJCDIEnabled(boolean b)
  {
    cdiEnabled = b;
  }

  public void setVirtualHost(DynamicVirtualHost host) {
    setVirtualHostName(host.getName());
    this.dVirtualHost = host;
  }
  
  /** {@inheritDoc} */
  @Override
  public List<String> getVirtualHostList() {
    return dVirtualHost.getAliases();
  }
  
  /** {@inheritDoc} */
  @Override
  public String getMimeType(String extension) {
      // look for app-configured mime type
      String mimeType = super.getMimeType(extension);
      if(mimeType == null) {
          mimeType = dVirtualHost.getMimeType(extension);
      }
      return mimeType;
  }

  /**
   * @return the orderedLibPaths
   */
  public List<String> getOrderedLibPaths() {
      return orderedLibPaths;
  }

  /**
   * @param orderedLibPaths the orderedLibPaths to set
   */
  public void setOrderedLibPaths(List<String> orderedLibPaths) {
      this.orderedLibPaths = Collections.unmodifiableList(new ArrayList<String>(orderedLibPaths));
  }

/**
 * @param bundleHeaders the bundleHeaders to set
 */
public void setBundleHeaders(java.util.Dictionary<java.lang.String,java.lang.String> bundleHeaders) {
    this.bundleHeaders = bundleHeaders;
}

/**
 * @return the bundleHeaders
 */
public java.util.Dictionary<java.lang.String,java.lang.String> getBundleHeaders() {
    return bundleHeaders;
}
}
