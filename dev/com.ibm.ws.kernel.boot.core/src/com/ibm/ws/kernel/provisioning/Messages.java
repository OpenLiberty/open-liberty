/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

/**
 * The classes in this package can not have a dependency on Tr because it is used
 * in environments where Tr is unavailable. So calls from this package to output
 * messages to a log use this interface, allowing multiple mechanisms of outputting
 * errors to be used.
 */
public interface Messages {
    public void warning(String key, Object... inserts);
}