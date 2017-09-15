/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.event;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;


public class FilterListenerImpl
 implements FilterInvocationListener, FilterListener, FilterErrorListener
{

protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.websphere.servlet.event");
	private static final String CLASS_NAME="com.ibm.websphere.servlet.event.FilterListenerImpl";

	 public FilterListenerImpl()
	 {
	 }
	
	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.event.FilterErrorListener#onFilterDestroyError(com.ibm.websphere.servlet.event.FilterErrorEvent)
	 */
	public void onFilterDestroyError(FilterErrorEvent evt) {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterDestroyError", "onFilterStartDoFilter -->" + evt.getFilterName() + " error -->" + evt.getError());
	    }

	}
	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.event.FilterErrorListener#onFilterDoFilterError(com.ibm.websphere.servlet.event.FilterErrorEvent)
	 */
	public void onFilterDoFilterError(FilterErrorEvent evt) {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterDoFilterError", "onFilterDoFilterError -->" + evt.getFilterName()+ " error -->" + evt.getError());
	    }

	}
	/* (non-Javadoc)
	 * @see com.ibm.websphere.servlet.event.FilterErrorListener#onServletInitError(com.ibm.websphere.servlet.event.FilterErrorEvent)
	 */
	public void onFilterInitError(FilterErrorEvent evt) {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterInitError", "onFilterInitError -->" + evt.getFilterName()+ " error -->" + evt.getError());
	    }

	}
	
	 public void onFilterStartDoFilter(FilterInvocationEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterStartDoFilter", "onFilterStartDoFilter -->" + filterinvocationevent.getFilterName() +" request -->" + filterinvocationevent.getServletRequest());
	    }
	 }
	
	 public void onFilterFinishDoFilter(FilterInvocationEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterFinishDoFilter", "onFilterFinishDoFilter -->" + filterinvocationevent.getFilterName() +" request -->" + filterinvocationevent.getServletRequest());
	    }
	 }
	
	 public void onFilterStartInit(FilterEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterStartInit", "onFilterStartInit -->" + filterinvocationevent.getFilterName() );
	    }
	 }
	
	 public void onFilterFinishInit(FilterEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterFinishInit", "onFilterFinishInit -->" + filterinvocationevent.getFilterName() );
	    }
	 }
	
	 public void onFilterStartDestroy(FilterEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterStartDestroy", "onFilterStartDestroy -->" + filterinvocationevent.getFilterName() );
	    }
	 }
	
	 public void onFilterFinishDestroy(FilterEvent filterinvocationevent)
	 {
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
	    	logger.logp(Level.FINE, CLASS_NAME,"onFilterFinishDestroy", "onFilterFinishDestroy -->" + filterinvocationevent.getFilterName() );
	    }
	 }
}
