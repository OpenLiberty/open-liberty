/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import javax.servlet.ServletContext;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.webcontainer.servlet.IServletContextExtended;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class WASContextEstablisher implements ContextEstablisher {
	
 	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(WASContextEstablisher.class);

	
	private IServletContextExtended _webapp;
	private boolean _isJava2SecurityEnabled = false; 
	
	/**
	 * Ctor
	 * @param webapp
	 */
	public WASContextEstablisher( IServletContext webapp){
		_webapp = (IServletContextExtended) webapp;
		if(System.getSecurityManager()!=null){
			_isJava2SecurityEnabled = true;
		}
	}
	
	/**
	 * Getter for servlet context
	 */
	public ServletContext getServletContext(){
		return _webapp;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.events.ContextEstablisher#establishContext()
	 */
	public void establishContext(){
		establishContext(_webapp.getClassLoader());
	}
	
	/**
	 * @see com.ibm.ws.sip.container.events.ContextEstablisher#establishContext(java.lang.ClassLoader)
	 */
	public void establishContext(ClassLoader cl) {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry( this, "establishContext", new Object[]{cl});
		}
		if(!_isJava2SecurityEnabled){
			Thread.currentThread().setContextClassLoader( cl);
		}else{
			final ClassLoader finalCl = cl;
			java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
			     public Object run() {
			       Thread.currentThread().setContextClassLoader( finalCl);
			       return null;
			     }
			   });
		}
		//		moti: trying to find NPE at : defect  444757.1
		ICollaboratorHelper icollabHlper =  _webapp.getCollaboratorHelper();
		IWebAppNameSpaceCollaborator spcCollab=  icollabHlper.getWebAppNameSpaceCollaborator();
		WebAppConfigExtended appcfg = (WebAppConfigExtended) _webapp.getWebAppConfig();
		
		// Moti: 29/Mar/2009: fix for PK83467: a NPE is thrown when
		// App server is stopped (due to stopping web container before sip container)
		if (appcfg == null) {
			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"establishContext(1)",
						"Web container is probably down, request ignored");
			}
			return;
		}

		WebModuleMetaData metaData = appcfg.getMetaData();
		spcCollab.preInvoke(metaData.getCollaboratorComponentMetaData());
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit( this, "establishContext", new Object[]{icollabHlper,spcCollab,appcfg,metaData});
		}

	}

	/**
	 * @see com.ibm.ws.sip.container.events.ContextEstablisher#removeContext(java.lang.ClassLoader)
	 */
	public void removeContext( ClassLoader cl) {
		if(!_isJava2SecurityEnabled){
			Thread.currentThread().setContextClassLoader( cl);
		}else{
			final ClassLoader finalCl = cl;
			java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
			     public Object run() {
			       Thread.currentThread().setContextClassLoader( finalCl);
			       return null;
			     }
			   });
		}
		_webapp.getCollaboratorHelper().getWebAppNameSpaceCollaborator().postInvoke();

	}
	
	/**
	 * 
	 * @return
	 */
	public ClassLoader getApplicationClassLoader(){
		return _webapp.getClassLoader();
	}

	/**
	 * @see com.ibm.ws.sip.container.events.ContextEstablisher#getThreadCurrentClassLoader()
	 */
	public ClassLoader getThreadCurrentClassLoader(){
		if(!_isJava2SecurityEnabled){
			return Thread.currentThread().getContextClassLoader();
		}else{
			return (ClassLoader) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
			     public Object run() {
			    	 return Thread.currentThread().getContextClassLoader();
			     }
			   });
		}
		
	}
}
