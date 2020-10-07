/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.webcontainer.VirtualHost;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.webapp.WebGroup;
import com.ibm.ws.webcontainer.webapp.WebGroupConfiguration;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Class encapsulating a single WAR application image.
 */
public class DeployedModule extends com.ibm.ws.container.DeployedModule
{
  @SuppressWarnings("unused")
  private static final TraceComponent tc = Tr.register(DeployedModule.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
  /**
   * A ClassLoader or LazyClassLoader to use as the thread context class loader
   * while components in this module are running.  This should either be
   * {@link #moduleClassLoader} or delegate to it.
   */
  private ClassLoader loader;
  /** Webcontainer app created for this module */
  private WebApp webApp;
  private WebAppConfiguration webAppConfig;
  private Future<Boolean> contextRootAdded;
  private String properContextRoot;
  private String mappingContextRoot;
  private AtomicInteger initTaskCount = new AtomicInteger(2);
  private CompletionListener<Boolean> initTasksDoneListener;
  
  public DeployedModule(Container webAppContainer,
                        WebAppConfiguration webAppConfig,
                        ClassLoader loader)
  {
    this.loader = loader;
    this.webAppConfig = webAppConfig;
    this.webApp = webAppConfig.getWebApp();
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getWebAppConfig()
   */
  public WebAppConfiguration getWebAppConfig()
  {
    return this.webAppConfig;
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getWebApp()
   */
  public WebApp getWebApp()
  {
    return this.webApp;
  }

  /**
   * The class loader to use for the thread context class loader. This could be
   * the same as the actual module class loader, but this implementation returns
   * the result of passing that class loader to 
   * ClassLoadingService.createThreadContextClassLoader().
   *
   * @see com.ibm.ws.container.DeployedModule#getClassLoader()
   */
  public ClassLoader getClassLoader()
  {
    return loader;
  }

    /**
     * @param contextRootAdded
     * @param listener
     */
    public void addStartupListener(Future<Boolean> contextRootAdded, CompletionListener<Boolean> listener) {
        this.contextRootAdded = contextRootAdded;
        this.initTasksDoneListener = listener;
    }

    /**
     * This will complete the future returned to appmanager
     * with successful application initialization
     */
    public void initTaskComplete() {
        if (initTaskCount.decrementAndGet() == 0 && initTasksDoneListener != null) {
            initTasksDoneListener.successfulCompletion(contextRootAdded, true);
        }
    }

    /**
     * This will complete the future returned to appmanager
     * with fail application initialization
     */
    public void initTaskFailed() {
        if (initTasksDoneListener != null) {
            initTasksDoneListener.successfulCompletion(contextRootAdded, false);
        }
    }

  /*
   * @see com.ibm.ws.container.DeployedModule#getContextRoot()
   */
  public String getContextRoot()
  {
    return this.getWebAppConfig().getContextRoot();
  }

  public void setContextRootAdded(Future<Boolean> contextRootAdded)
  {
    this.contextRootAdded = contextRootAdded;
  }

  public Future<Boolean> getContextRootAdded()
  {
    return this.contextRootAdded;
  }

  public String getProperContextRoot()
  {
    String properContextRoot = this.properContextRoot;
    if (properContextRoot == null)
    {
      properContextRoot = VirtualHost.makeProperContextRoot(getContextRoot());
      this.properContextRoot = properContextRoot;
    }
    return properContextRoot;
  }

  public String getMappingContextRoot()
  {
    String mappingContextRoot = this.mappingContextRoot;
    if (mappingContextRoot == null)
    {
      mappingContextRoot = VirtualHost.makeMappingContextRoot(getProperContextRoot());
      this.mappingContextRoot = mappingContextRoot;
    }
    return mappingContextRoot;
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getDisplayName()
   */
  public String getDisplayName()
  {
    return this.getWebAppConfig().getDisplayName();
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getName()
   */
  public String getName()
  {
    return this.webApp.getName();
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getWebGroup()
   */
  public WebGroup getWebGroup()
  {
    return null;
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getWebGroupConfig()
   */
  public WebGroupConfiguration getWebGroupConfig()
  {
    return null;
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getVirtualHostName()
   */
  public String getVirtualHostName()
  {
    return getWebAppConfig().getVirtualHostName();
  }

  /*
   * @see com.ibm.ws.container.DeployedModule#getVirtualHosts()
   */
  @SuppressWarnings("deprecation")
  public com.ibm.ws.http.VirtualHost[] getVirtualHosts()
  {
    return null;
  }
}
