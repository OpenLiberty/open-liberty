/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.collaborator;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class WebAppNameSpaceCollaborator implements
		IWebAppNameSpaceCollaborator {

	private static final String CLASS_NAME="com.ibm.ws.webcontainer.collaborator.WebAppNameSpaceCollaborator";
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.collaborator");
	
	public void preInvoke(Object compMetaData) {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		logger.entering(CLASS_NAME,"preInvoke",compMetaData);
    		logger.exiting(CLASS_NAME,"preInvoke");
		}

	}

	public void postInvoke() {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		logger.entering(CLASS_NAME,"preInvoke");
    		logger.exiting(CLASS_NAME,"preInvoke");
		}
	}

}
