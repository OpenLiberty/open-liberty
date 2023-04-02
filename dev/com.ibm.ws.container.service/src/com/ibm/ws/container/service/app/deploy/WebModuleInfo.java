/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy;

/**
 * Information about a web module
 */
public interface WebModuleInfo extends ModuleInfo {
    /**
     * Returns the context root for a web module
     *
     * @return
     */
    String getContextRoot();

    /**
     * Returns true if the default context root is being used, otherwise returns false
     *
     * @return
     */
    boolean isDefaultContextRootUsed();
}
