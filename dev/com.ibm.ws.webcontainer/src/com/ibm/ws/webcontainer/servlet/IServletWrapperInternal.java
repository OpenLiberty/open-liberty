/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 *
 */
public interface IServletWrapperInternal  extends IServletWrapper {
    
    /**
     * Sets a flag if it is not set, and returns if this flag had previously been set.
     * This flag can be used to limit the number of warnings that are printed for the same servlet
     * @return true if the warning status flag was set, or false if it was already set. 
     */
    public boolean hitWarningStatus();
    


}
