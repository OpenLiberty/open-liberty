/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim;

import java.util.Map;
import java.util.Set;

import com.ibm.wsspi.security.wim.exception.WIMException;

/**
 * An interface used to wrap {@link Repository} instances.
 */
interface RepositoryWrapper {

    Repository getRepository() throws WIMException;

    void clear();

    Map<String, String> getRepositoryBaseEntries();

    Set<String> getRepositoryGroups();

    int isUniqueNameForRepository(String uniqueName, boolean isDn) throws WIMException;
}
