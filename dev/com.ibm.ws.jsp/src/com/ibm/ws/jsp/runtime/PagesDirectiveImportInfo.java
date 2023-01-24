/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jsp.runtime;

import java.util.List;

/**
 * Interface for retrieving page directive information as well as page import information.
 */
public interface PagesDirectiveImportInfo {

    // Import Info methods

    List<String> getImportClassList();

    List<String> getImportPackageList();

    List<String> getImportStaticList();

    // Directive Info methods

    boolean isErrorOnELNotFound();

}
