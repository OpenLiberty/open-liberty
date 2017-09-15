/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.configurator;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import com.ibm.ws.wsoc.external.WebSocketContainerExt;

/**
 *
 */
public class DefaultContainerProvider extends ContainerProvider {

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.ContainerProvider#getContainer()
     */
    @Override
    protected WebSocketContainer getContainer() {
        return new WebSocketContainerExt();
    }
}
