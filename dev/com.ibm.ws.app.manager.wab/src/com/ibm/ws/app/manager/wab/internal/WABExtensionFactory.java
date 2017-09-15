/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.internal;

import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { WABExtensionFactory.class, ExtensionFactory.class },
           immediate = true,
           property = "service.vendor=IBM")
public class WABExtensionFactory implements ExtensionFactory {

    private volatile ExtensionFactory delegate;

    /**
     * @param delegate
     */
    void setDelegate(ExtensionFactory delegate) {
        this.delegate = delegate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory#createExtensionProcessor(com.ibm.wsspi.webcontainer.servlet.IServletContext)
     */
    @Override
    public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception {
        ExtensionFactory delegate = this.delegate;
        if (delegate != null)
            return delegate.createExtensionProcessor(webapp);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.webcontainer.extension.ExtensionFactory#getPatternList()
     */
    @Override
    public List getPatternList() {
        ExtensionFactory delegate = this.delegate;
        if (delegate != null)
            return delegate.getPatternList();
        return Collections.emptyList();
    }

}
