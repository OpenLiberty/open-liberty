/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.webxml;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.ServletConfiguratorHelperFactory;

public class JspConfiguratorHelperFactory implements ServletConfiguratorHelperFactory {

    @Override
    public ServletConfiguratorHelper createConfiguratorHelper(ServletConfigurator configurator) {
        return new JspConfiguratorHelper(configurator);
    }

    protected void activate(ComponentContext context) {
        
    }
    protected void deactivate(ComponentContext context) {
        
    }
}
