/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.util.Map;

/**
 *
 */
public interface ITransferContextService {

    /**
     * store data to be transferred to another thread in order to preserve service provider context.
     * 
     * @param m Map object to be populated with thread context data that will need to be transferred to
     * another thread to preserve context for the transfer producer services.
     */
    public void storeState(Map<String,Object> m);


    /**
     * Set/restore data on this thread in order to preserve provider context during thread switching
     *
     * @param m populated data that will be made available to a new thread that will need to access 
     * context for a transfer producer service.
     */
    public void restoreState(Map<String,Object> m);
    
    /**
     * Reset data on this thread in order to revert context back on this thread once the async task is done.
     *
     */
    public void resetState();
}
