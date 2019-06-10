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
package com.ibm.ws.jaxws;

// @TJJ 5/7/19
/**
 * This class is implemented by the JaxWsClientHandlerResolver, where the destroy() cleans handler instances.
 * It's also used by JaxwsIntanceManager.destroyIntsance() to clean-up instances of Destroyable objects
 */
public interface Destroyable {
    public void destroy();
}
