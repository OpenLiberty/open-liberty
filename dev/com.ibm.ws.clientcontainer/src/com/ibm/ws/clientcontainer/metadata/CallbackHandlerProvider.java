/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.metadata;

import javax.security.auth.callback.CallbackHandler;

/**
 * <p>This allows other bundles to register themselves as listeners for getting a login callback handler, which may be
 * defined in the application-client.xml of a client module.</p>
 * 
 */
public interface CallbackHandlerProvider {

    /**
     * Return the callback handler from the application-client.xml.
     * 
     * @return The callback handler in the application-client.xml, or null when a callback handler element is not specified.
     */
    public CallbackHandler getCallbackHandler();

}
