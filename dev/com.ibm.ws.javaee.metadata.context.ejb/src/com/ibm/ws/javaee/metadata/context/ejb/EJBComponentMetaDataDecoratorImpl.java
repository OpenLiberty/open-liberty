/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.metadata.context.ejb;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.javaee.metadata.context.ComponentMetaDataDecorator;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

/**
 * This component must only be active if the EJBContainer is.
 */
@Component(service = ComponentMetaDataDecorator.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class EJBComponentMetaDataDecoratorImpl implements ComponentMetaDataDecorator {
    @Activate
    protected void activate(ComponentContext context) {}

    @Deactivate
    protected void deactivate(ComponentContext context) {}

    /**
     * @see com.ibm.ws.javaee.metadata.context.ComponentMetaDataDecorator#decorate(com.ibm.ws.runtime.metadata.ComponentMetaData)
     */
    @Override
    public ComponentMetaData decorate(ComponentMetaData metadata) {
        if (metadata instanceof EJBComponentMetaData)
            metadata = new EJBComponentMetaDataWrapper((EJBComponentMetaData) metadata);

        return metadata;
    }
}
