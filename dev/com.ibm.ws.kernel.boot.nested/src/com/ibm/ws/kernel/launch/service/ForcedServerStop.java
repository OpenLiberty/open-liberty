/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.kernel.launch.service;

/**
 * Marker interface. If this is registered in the service registry, then
 * the server was stopped with the {@code --force} option, and {@code ServerQuiesceListener}s
 * and their ilk should not be invoked.
 */
public class ForcedServerStop {

}
