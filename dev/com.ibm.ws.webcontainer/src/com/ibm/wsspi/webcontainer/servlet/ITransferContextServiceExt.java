/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
 * ITransferContextService extension. 
 */
public interface ITransferContextServiceExt extends ITransferContextService{
    
    /**
     * Informs service implementers that contextual work has been initiated prior to being queued for
     * execution.
     * 
     * @param m The map holding default and implementer specific context information.
     */
    public void preProcessWorkState(Map<String, Object> m);
    
    /**
     * Informs service implementers that the asynchronous request has completed.
     * 
     * @param m The map holding default and implementer specific context information.
     */
    public void completeState(Map<String, Object> m);
}
