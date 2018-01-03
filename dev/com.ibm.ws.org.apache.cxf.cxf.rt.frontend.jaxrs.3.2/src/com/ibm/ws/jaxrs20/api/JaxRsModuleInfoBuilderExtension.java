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

import java.util.Set;

import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleType;
import com.ibm.ws.jaxrs20.metadata.builder.JaxRsModuleInfoBuilderContext;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * The extension of a JaxWsModuleInfoBuilder to help build JaxWsModuleInfo.
 */
public interface JaxRsModuleInfoBuilderExtension {

    public void preBuild(JaxRsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxRsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException;

    public void postBuild(JaxRsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxRsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException;

    public Set<JaxRsModuleType> getSupportTypes();

}
