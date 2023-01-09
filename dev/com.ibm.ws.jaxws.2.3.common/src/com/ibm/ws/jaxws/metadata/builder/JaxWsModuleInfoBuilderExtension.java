/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.metadata.builder;

import java.util.Set;

import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * The extension of a JaxWsModuleInfoBuilder to help build JaxWsModuleInfo.
 */
public interface JaxWsModuleInfoBuilderExtension {

    public void preBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException;

    public void postBuild(JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException;

    public Set<JaxWsModuleType> getSupportTypes();

}
