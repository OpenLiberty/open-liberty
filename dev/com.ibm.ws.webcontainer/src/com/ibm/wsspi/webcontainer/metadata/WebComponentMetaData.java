/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Map;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;


/**
 * The metadata for a component in a Web Module (ie., a Servlet, or a JSP or any other
 * entity compiled as a Servlet), which gets shared accross components. Components
 * can push their optimized config data associated with the servlet onto this
 * metaData and they can retrieve them upon invocation wither from the invocation
 * call or from the ThreadContext
 * 
 * @ibm-private-in-use
 * 
*/
public interface WebComponentMetaData extends ComponentMetaData

 {
	   public static final int SERVLET = 1;
	   public static final int JSP = 2;

	   /**
	    * Returns the type of component associated with this metadata object.
	    * The current types are SERVLET and JSP
	    *  
	    * @return
	    */
	   public int getWebComponentType();
	   
	   /**
	    * Returns the version of the servlet specification this servlet is
	    * compliant with.
	    * @return
	    */
	   public String getWebComponentVersion();
	   
	   /**
	    * Returns the compiled classname of the associated component
	    * @return
	    */
	   public String getImplementationClass();
	   
	   /**
	    * Returns whether or not the component associated with this metaData object
	    * is a JSP.
	    * @return
	    */
	   public boolean isTypeJSP();
	   
	   /**
	    * Returns the description of the component associated with this metaData
	    * object
	    * @return
	    */
	   public String getWebComponentDescription();
	   
	   /**
	    * Returns the initialization parameters that have been configured for the component
	    * associated with tihs metaData Object.
	    * @return
	    */
	   @SuppressWarnings("unchecked")
	   public Map getWebComponentInitParameters();
	   
	   /**
	    * Returns the list of PageList metaDatas associated with the component
	    * to which this metaData object is attached.
	    * @return
	    */
	   @SuppressWarnings("unchecked")
	   public ArrayList getPageListMetaData();
	   
	   /**
	    * Returns the IServletConfig interface associated with the component to wehich 
	    * this metaData object is attached.
	    * @return
	    */
	   public IServletConfig getServletConfig();
	   
	   /**
	    * Returns the securityMetaData object attached to the component associated
	    * with this metaData object
	    * 
	    * NOTE: This method must only be invoked by security providers who had
	    * earlier pushed the securityMetaData objects into this metaData during metadata
	    * creation.
	    * @return
	    */
	   public Object getSecurityMetaData();
	   
	   /**
	    * Call to push the securityMetaData into this metaData object at metaData
	    * creation time. This is typically done by security providers, who push 
	    * security constraint representations for the component associated with 
	    * this metaData object, only to retrieve it again at the time when the 
	    * component is invoked at runtime.
	    * @param metaData
	    */
	   public void setSecurityMetaData(Object metaData);
	   
	   public void handleCallbacks();

           //PK54805
           public int setCallbacksID();
           //PK54805
           public int getCallbacksId();
           //PK54805
           public void handleCallbacks(int callbacksIdInt);
}


