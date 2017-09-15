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
package com.ibm.ws.jsp.taglib.annotation;

import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.JspTag;

import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public abstract class AnnotationHandler {
     private static HashMap<IServletContext, AnnotationHandler>
          tagAnnotationHandlers = new HashMap<IServletContext,
          AnnotationHandler>();
     
     private IServletContext context;
     
     protected AnnotationHandler () {
     }
     
     public static synchronized AnnotationHandler getInstance
          (ServletContext context) {
          IServletContext servletContext;
          AnnotationHandler tagAnnotationHandler;
          
          // Find the underlying IServletContext for the passed in context
          // first.
          
          if (context instanceof ServletContextFacade) {
               servletContext = ((ServletContextFacade)
                    context).getIServletContext();
          }
          
          else {
               servletContext = (IServletContext) context;
          }
          
          tagAnnotationHandler = AnnotationHandler.tagAnnotationHandlers.get
               (servletContext);
          
          if (tagAnnotationHandler == null) {
               tagAnnotationHandler = AnnotationHandler.createInstance();
               
               tagAnnotationHandler.setServletContext (servletContext);
               
               AnnotationHandler.tagAnnotationHandlers.put (servletContext,
                    tagAnnotationHandler);
          }
          
          return tagAnnotationHandler;
     }
     
     public static synchronized AnnotationHandler removeAnnotationHandler(ServletContext context) {
         AnnotationHandler rc = null;
         if (AnnotationHandler.tagAnnotationHandlers!=null) {
             IServletContext servletContext;
             if (context instanceof ServletContextFacade) {
                 servletContext = ((ServletContextFacade)
                      context).getIServletContext();
             } else {
                 servletContext = (IServletContext) context;
             }
             rc = AnnotationHandler.tagAnnotationHandlers.remove(servletContext);
         }
         return rc;
     }
     
     protected IServletContext getServletContext () {
          return this.context;
     }
     
     protected void setServletContext (IServletContext context) {
          this.context = context;
     }
     
     public abstract void doPostConstructAction (JspTag tag);
     
     public abstract void doPostConstructAction (EventListener listener);
     
     public abstract void doPreDestroyAction (JspTag tag);
     
     private static AnnotationHandler createInstance () {
          String implClassName = System.getProperty
               (AnnotationHandler.class.getName());
          AnnotationHandler instance = null;
          
          try {
               instance = (AnnotationHandler) Class.forName
                    (implClassName).newInstance();
          }
          
          catch (Exception e) {
               instance = new DefaultAnnotationHandler();
          }
          
          return instance;
     }
}
