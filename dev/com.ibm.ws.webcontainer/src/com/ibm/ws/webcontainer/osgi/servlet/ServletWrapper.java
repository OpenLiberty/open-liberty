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
package com.ibm.ws.webcontainer.osgi.servlet;

import javax.servlet.Servlet;
import javax.servlet.SingleThreadModel;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.webcontainer.cache.CacheManager;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ServletWrapper extends com.ibm.ws.webcontainer.servlet.ServletWrapper
{

  private static final long serialVersionUID = 1L;
  private transient ManagedObject _mo = null;
  
  /**
   * @param parent
   */
  public ServletWrapper(IServletContext parent)
  {
    super(parent);
  }

  @Override
  protected void createTarget(Servlet s) throws InjectionException
  {
   if (context.getCdiContexts().containsKey(s)){
       _mo = context.getCdiContexts().remove(s);
   } else {
       _mo = ((WebApp)this.getWebApp()).injectAndPostConstruct(s);
   }
    super.createTarget(s);
  }
  

  protected void createTarget(Class<?> Klass) throws InjectionException
  {
       _mo = ((WebApp)this.getWebApp()).injectAndPostConstruct(Klass);
       super.createTarget((Servlet)_mo.getObject());
  }

    @Override
    public void modifyTarget(Servlet s) {
        boolean isSTM = s instanceof SingleThreadModel;
        if (!isSTM){
            CacheManager cm = WebContainer.getCacheManager();
            if (cm != null) { 
                target = cm.getProxiedServlet(s);
            } else {
                target = s;
            }
        }
    }
    
    public void prepareForReload() {
        super.prepareForReload();
        if (null != _mo) _mo.release();
    }
    
    public void unload() throws Exception{
        super.unload();
        if (null != _mo) _mo.release();
    }
    
    public void destroy(){
        super.destroy();
        if (null != _mo) _mo.release();
    }
}
