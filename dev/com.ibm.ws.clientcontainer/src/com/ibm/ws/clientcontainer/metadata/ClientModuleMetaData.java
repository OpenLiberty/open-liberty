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
package com.ibm.ws.clientcontainer.metadata;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public interface ClientModuleMetaData extends ModuleMetaData {
    public ModuleInfo getModuleInfo();

    public ApplicationClient getAppClient();

}
