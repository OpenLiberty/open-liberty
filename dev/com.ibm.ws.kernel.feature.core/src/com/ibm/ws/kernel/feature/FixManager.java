/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.Set;

/**
 * <p>The IFixManager is a service for getting a set of iFixes installed in the server.</p>
 */
public interface FixManager {

    /**
     * <p>Requests the set of iFixes that are installed on the server. The result reported
     * to the caller is never null, but may be an empty set if no features are installed.</p>
     *
     * @return the set of iFixes known to the iFix Manager.
     */
    public Set<String> getIFixes();

    /**
     * <p>Requests the set of tFixes that are installed on the server. The result reported
     * to the caller is never null, but may be an empty set if no features are installed.</p>
     *
     * @return the set of tFixes known to the iFix Manager.
     */
    public Set<String> getTFixes();

}