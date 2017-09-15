/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface SystemConfigSupport {

    ExtendedConfiguration lookupConfiguration(ConfigID referenceId);

    Set<ConfigID> getReferences(ConfigID configId);

    void registerConfiguration(ConfigID configId, ExtendedConfiguration config);

    ExtendedConfiguration findConfiguration(String alias);

    boolean waitForAll(Collection<Future<?>> endingFuturesForChanges, long timeout, TimeUnit unit);

    void openManagedServiceTrackers();

    void fireMetatypeRemovedEvent(String pid);

    void fireMetatypeAddedEvent(String pid);
}
