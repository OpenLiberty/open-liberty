/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

package com.ibm.websphere.event;

import java.util.concurrent.ExecutorService;

/**
 * An {@code ExecutorServiceFactory} encapsulates the mechanism used to
 * acquire an instance of a named {@link ExecutorService} for delivering
 * events to event handlers.
 */
public interface ExecutorServiceFactory {

    ExecutorService getExecutorService(String name);

}
