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
 * Created on May 25, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.ibm.ws.webcontainer.osgi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.BundleContext;

import com.ibm.ws.webcontainer.osgi.extension.LinkExtensionProcessor;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@SuppressWarnings("unchecked")
public class LinkExtensionFactory implements ExtensionFactory
{
  private BundleContext bundleContext = null;
  private List extList = null;
  private String serviceName = null;
  private String supportName = null;

  public LinkExtensionFactory(BundleContext bundleContext, String extensions, String serviceName, String supportName)
  {
    this.bundleContext = bundleContext;
    extList = new ArrayList();
    StringTokenizer st = new StringTokenizer(extensions, ",");
    while (st.hasMoreTokens())
    {
      String ext = st.nextToken();
      if (ext.trim().length() > 0)
      {
        extList.add(ext);
      }
    }
    this.serviceName = serviceName;
    this.supportName = supportName;
  }

  public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception
  {
    ExtensionProcessor ep = null;
    Manifest manifest = new Manifest(webapp.getResourceAsStream("/META-INF/MANIFEST.MF"));
    Attributes attributes = manifest.getMainAttributes();
    Attributes.Name supportKey = new Attributes.Name(supportName);
    boolean createExtensionProcessor = false;
    for (Iterator itr = attributes.keySet().iterator(); itr.hasNext();)
    {
      Attributes.Name name = (Attributes.Name) itr.next();
      if (name.equals(supportKey))
      {
        createExtensionProcessor = true;
        break;
      }
    }
    if (createExtensionProcessor)
      ep = new LinkExtensionProcessor(webapp, bundleContext, serviceName);
    return ep;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory#getPatternList()
   */
  public List getPatternList()
  {
    return extList;
  }
}
