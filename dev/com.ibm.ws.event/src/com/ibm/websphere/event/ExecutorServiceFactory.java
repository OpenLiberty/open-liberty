/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
