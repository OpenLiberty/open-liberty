/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.feature;

/**
 * An implementation of this service is registered to indicate Liberty has started.
 * Components wanting to take an action on server startup should watch for this being
 * registered and take action then. This provides an asynchronous notification of startup.
 */
public interface ServerStarted {

}
