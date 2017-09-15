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
package com.ibm.ws.webcontainer.osgi.extension;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.osgi.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * @author asisin
 */
@SuppressWarnings("unchecked")
public class LinkExtensionProcessor extends WebExtensionProcessor
{

  private BundleContext bundleContext = null;
  private ExtensionProcessor processor = null;
  private String serviceName = null;
  private List patternList = null;
  private boolean changedMappings = false;

  public LinkExtensionProcessor(IServletContext context, BundleContext bundleContext, String serviceName)
  {
    super(context);
    this.bundleContext = bundleContext;
    this.serviceName = serviceName;
  }

  public void handleRequest(ServletRequest req, ServletResponse res) throws Exception
  {
    if (processor == null)
    {
      createProcessor();
    }

    if (changedMappings == false)
    {
      changedMappings = true;
      for (Iterator itr = patternList.iterator(); itr.hasNext();)
      {
        String mapping = (String) itr.next();
        ((WebApp) extensionContext).getRequestMapper().replaceMapping(mapping, processor);
      }
    }
    processor.handleRequest(req, res);

  }

  public List getPatternList()
  {
    if (patternList == null)
    {
      if (processor == null)
      {
        createProcessor();
      }
      patternList = processor.getPatternList();
    }
    return patternList;
  }

  private void createProcessor()
  {
    if (processor == null)
    {
      ServiceReference sr = bundleContext.getServiceReference(serviceName);

      ExtensionFactoryService service = (ExtensionFactoryService) bundleContext.getService(sr);
      try
      {
        processor = service.getExtensionFactory().createExtensionProcessor(extensionContext);
      }
      catch (Exception e)
      {
          FFDCFilter.processException(e, this.getClass().getName() + ".createProcessor", "97");
      }
      bundleContext.ungetService(sr);
    }
  }

  public IServletWrapper getServletWrapper()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public WebComponentMetaData getMetaData()
  {
    // TODO Auto-generated method stub
    return null;
  }

  public IServletWrapper getServletWrapper(ServletRequest req, ServletResponse resp)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
