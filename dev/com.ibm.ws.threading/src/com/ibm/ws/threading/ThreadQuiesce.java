/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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
package com.ibm.ws.threading;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Used to signal the executor service that threads should be quiesced
 */
@ProviderType
public interface ThreadQuiesce {

    boolean quiesceThreads();

    boolean quiesceThreads(long startTime);

    int getActiveThreads();

    boolean quiesceStarted();

    int getQuiesceTimeout();
}
