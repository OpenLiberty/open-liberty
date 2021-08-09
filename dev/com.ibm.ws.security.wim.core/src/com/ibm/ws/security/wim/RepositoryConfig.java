/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

/**
 * TODO fold into ConfiguredRepository
 */
public interface RepositoryConfig {

    boolean isReadOnly();

    void resetConfig();

    String getReposId();

    Map<String, String> getRepositoryBaseEntries();

    String[] getRepositoriesForGroups();

}
