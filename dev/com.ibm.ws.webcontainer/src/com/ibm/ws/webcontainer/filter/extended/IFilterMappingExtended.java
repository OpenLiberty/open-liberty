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
package com.ibm.ws.webcontainer.filter.extended;

import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

public interface IFilterMappingExtended extends IFilterMapping{


    /**
     * Gets the servlet-filter mapping name.
     * @return String
     */
    public abstract String getServletFilterMappingName();

    /**
     * Sets the servlet-filter mapping name.
     */
    public abstract void saveServletFilterMappingName(String servletName);

    /**
     * Sets the servlet config
     */
    public abstract void setServletConfig(IServletConfig sconfig);

}
