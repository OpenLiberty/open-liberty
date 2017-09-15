/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;

public class ExtensionFactoryServiceListener implements ServiceListener
{

  private BundleContext context;
  private WebContainer webcontainer;

  public ExtensionFactoryServiceListener(BundleContext context, WebContainer webcontainer)
  {
    this.context = context;
    this.webcontainer = webcontainer;
  }

  public void serviceChanged(ServiceEvent ev)
  {
    ServiceReference sr = ev.getServiceReference();
    switch (ev.getType())
    {
      case ServiceEvent.REGISTERED:
      {
        registerExtensionFactory(sr);
      }
        break;
      case ServiceEvent.UNREGISTERING:
      {
        unregisterExtensionFactory(sr);
      }
        break;
      default:
        break;
    }
  }

  private void registerExtensionFactory(ServiceReference sr)
  {
    ExtensionFactory factory = (ExtensionFactory) context.getService(sr);
    webcontainer.addExtensionFactory(factory);
  }

  private void unregisterExtensionFactory(ServiceReference sr)
  {
    this.context.ungetService(sr);
    // TODO LIBERTY not sure what we need to cleanup.
  }
}
