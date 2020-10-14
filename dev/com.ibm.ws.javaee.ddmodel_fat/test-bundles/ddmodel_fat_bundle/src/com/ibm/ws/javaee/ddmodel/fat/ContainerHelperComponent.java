/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.fat;

import java.util.HashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ContainerHelperComponent
    implements ApplicationMetaDataListener, ModuleMetaDataListener, ContainerHelper {

    private Container container;

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        container = event.getContainer();
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        container = null;
    }

    public Container getContainer() {
        return container;
    }

    //

    private final HashMap<String, Container> modules = new HashMap<String, Container>();

    @Override
    public Container getModuleContainer(String moduleName) {
        return modules.get(moduleName);
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        modules.put( event.getMetaData().getName(), event.getContainer() );
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        modules.remove( event.getMetaData().getName() );
    }
}
