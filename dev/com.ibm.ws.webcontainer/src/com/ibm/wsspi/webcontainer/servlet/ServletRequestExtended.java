/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

/**
 *  SPI used to extended ServletRequest to provide added method.
 */
public interface ServletRequestExtended {
    
    /*
     * Get the exception which caused the failure of the current response or null
     * if there is no failure.
     */
    public Throwable getCurrentException();
    
}
