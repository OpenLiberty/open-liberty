/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.websphere.security;

import java.util.List;
import java.util.Map;

/**
 * Defines read-only API contract for UserRegistry implementations.
 * Methods must not have side-effects which alter contents of the UserRegistry.
 * UserRegistry implementations must support dynamic configuration updates.
 */
public interface AttributeReader {

    /**
     * Returns the attributes for <i>userSecurityName</i>.
     *
     * @param userSecurityName the name of the user.
     * @param attributeNames
     * @return a Map of attributes for the user.
     *         <code>null</code> is not returned.
     * @exception EntryNotFoundException  if userSecurityName does not exist.
     * @exception CustomRegistryException if there is any registry specific problem
     **/
    Map<String, Object> getAttributesForUser(String userSecurityName, List<String> attributeNames) throws EntryNotFoundException, CustomRegistryException;

    /**
     * Gets a list of users that match an <i>attributeName</i> with specified
     * <i>value</i> in the UserRegistry.
     * The maximum number of users returned is defined by the <i>limit</i>
     * argument. This is very useful in situations where there are thousands of
     * users in the UserRegistry and getting all of them at once is not
     * practical.
     *
     * @param attributeName the attributeName to match.
     * @param value         the value of the attributeName to match.
     * @param limit         the maximum number of users that should be returned.
     *                          A value of 0 implies get all the users.
     *                          Specifying a negative value returns an empty Result.
     * @return a <i>Result</i> object that contains the list of users
     *         requested and a flag to indicate if more users exist.
     *         <code>null</code> is not returned.
     * @exception CustomRegistryException if there is any UserRegistry specific problem
     **/
    Result findUsersByAttribute(String attributeName, String value, int limit) throws CustomRegistryException;

}
