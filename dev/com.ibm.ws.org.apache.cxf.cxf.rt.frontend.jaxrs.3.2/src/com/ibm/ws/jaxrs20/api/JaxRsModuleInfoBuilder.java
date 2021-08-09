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
package com.ibm.ws.jaxrs20.api;

import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * The implementations of this interface will analysis the current module and add EndpointInfo in the JaxWsModuleInfo instance
 */
public interface JaxRsModuleInfoBuilder {

    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, Container containerToAdapt, JaxRsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException;

    public JaxRsModuleType getSupportType();

}
