/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jpa.management;

/**
 * Identifies a provider-facing persistence-unit adapter and exposes its
 * version-neutral Liberty state.
 */
public interface PersistenceUnitInfoDelegate {
    /**
     * Returns the persistence-unit state represented by this adapter.
     *
     * @return the owning persistence-unit state
     */
    JPAPUnitInfo getPersistenceUnitState();
}
