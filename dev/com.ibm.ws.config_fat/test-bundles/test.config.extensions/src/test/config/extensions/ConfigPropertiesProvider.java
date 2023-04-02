/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package test.config.extensions;

import java.util.Dictionary;

interface ConfigPropertiesProvider {
    /**
     * Waits for a call to the ManagedServiceFactory with
     * the specified config id
     * 
     * 
     * @param pidStartsWith
     * @return the properties that the ManagedServiceFactory was called with
     */
    Dictionary<String, ?> getPropertiesForId(String id);
}
