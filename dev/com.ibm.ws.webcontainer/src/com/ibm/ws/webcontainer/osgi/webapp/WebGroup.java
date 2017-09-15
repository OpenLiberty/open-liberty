/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 1, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ws.webcontainer.osgi.webapp;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WebGroup extends com.ibm.ws.webcontainer.webapp.WebGroup
{
  
  private static final TraceComponent tc = Tr.register(WebGroup.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);


  /**
   * @param name
   * @param parent
   */
  public WebGroup(String name, Container parent)
  {
    super(name, parent);
    // TODO Auto-generated constructor stub
  }
  
  
  /**
   * Method addWebApplication.
   * 
   * @param deployedModule
   * @param extensionFactories
   */
  // BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component
  // Specific)
  @SuppressWarnings("unchecked")
  public void addWebApplication(DeployedModule deployedModule, List extensionFactories) throws Throwable
  {
    WebAppConfiguration wConfig = deployedModule.getWebAppConfig();
    String displayName = wConfig.getDisplayName();
    if (tc.isInfoEnabled())
      Tr.info(tc, "loading.web.module", displayName);
    this.webApp = deployedModule.getWebApp();
    // LIBERTY: don't initialize the web app yet - be lazy
    this.webApp.setParent(this);
    this.addSubContainer(webApp); // calling setParent does not add to the list of subContainers

    webApp.setModuleConfig(deployedModule);
    webApp.setExtensionFactories(extensionFactories);
  }

  // END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component
  // Specific)

}
