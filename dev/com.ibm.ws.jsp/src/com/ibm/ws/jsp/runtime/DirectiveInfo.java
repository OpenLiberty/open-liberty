/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp.runtime;

import java.util.List;

/**
 * Interface for retrieving page directive information.
 */
public interface DirectiveInfo {

    List<String> getImportClassList();

    List<String> getImportPackageList();

    List<String> getImportStaticList();

    boolean isErrorOnELNotFound();

}
