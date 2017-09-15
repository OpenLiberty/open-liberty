/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.collaborator;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppNameSpaceCollaborator;

/*
 * In WAS this class manages the association of the ComponentMetaData object with the thread 
 * when other components (or application code) are invoked. It uses the deprecated getThreadContext
 * method and operates directly on the thread context.  
 */

public class WebAppNameSpaceCollaboratorImpl implements IWebAppNameSpaceCollaborator
{
    private final ComponentMetaDataAccessorImpl cmdai;
    
    public WebAppNameSpaceCollaboratorImpl(){
        cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
    }

    /*
     * Argument type is 'Object' to fit with common webcontainer code.
     */
    public void preInvoke(Object compMetaData)
    {
        cmdai.beginContext((ComponentMetaData)compMetaData);   
    }
 
    public void postInvoke()
    {
        cmdai.endContext();
    }
}