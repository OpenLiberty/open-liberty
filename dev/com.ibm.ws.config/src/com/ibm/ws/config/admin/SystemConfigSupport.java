/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
package com.ibm.ws.config.admin;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface SystemConfigSupport {
    void openManagedServiceTrackers();

    void registerConfiguration(ConfigID configId, ExtendedConfiguration config);

    ExtendedConfiguration lookupConfiguration(ConfigID referenceId);

    ExtendedConfiguration findConfiguration(String alias);

    Set<ConfigID> getReferences(ConfigID configId);

    boolean waitForAll(Collection<Future<?>> endingFuturesForChanges, long timeout, TimeUnit unit);

    void fireMetatypeAddedEvent(String pid);

    void fireMetatypeRemovedEvent(String pid);
}
