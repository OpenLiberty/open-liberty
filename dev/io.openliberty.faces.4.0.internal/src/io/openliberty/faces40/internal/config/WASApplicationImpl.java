/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package io.openliberty.faces40.internal.config;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.util.ExternalSpecifications;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.faces40.internal.extprocessor.JSFExtensionFactory;
import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationWrapper;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

/**
 * WAS custom application implementation that initializes CDIJSFViewHandler per application
 * if CDI feature is enabled.
 */
public class WASApplicationImpl extends ApplicationWrapper
{
    private static final String CLASS_NAME = WASApplicationImpl.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);

    private Application application;

    public WASApplicationImpl(Application delegate)
    {
        this.application = delegate;
        if (log.isLoggable(Level.FINE))
        {
            log.fine("New WASApplication instance created");
        }
    }

    @Override
    @Trivial
    public Application getWrapped()
    {
        return application;
    }

    @Override
    public void setViewHandler(ViewHandler viewHandler)
    {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        // Check to see if CDI feature is enabled AND the app has an active BeanManager
        if (ExternalSpecifications.isCDIAvailable(externalContext) && CDIUtils.getBeanManager(externalContext) != null)
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("CDI feature is enabled. Set ViewHandler = " + viewHandler);
            }
            application.setViewHandler(viewHandler);
            JSFExtensionFactory.initializeCDIJSFViewHandler(application);
        }
        else
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine("CDI feature is not enabled. Set ViewHandler = " + viewHandler);
            }
            application.setViewHandler(viewHandler);
        }
    }
}
