/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.config.xml;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface ConfigVariables {

    public Map<String, String> getUserDefinedVariables();

    public Map<String, String> getUserDefinedVariableDefaults();

    public Collection<LibertyVariable> getAllLibertyVariables();

    public List<String> getFileSystemVariableRootDirectories();
}
