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
package io.openliberty.wsoc.util.wsoc;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

/**
 *
 */
public class TestWsocContainer {

    private static WebSocketContainer c = null;

    public static synchronized WebSocketContainer getRef() {
        if (c == null) {
            c = ContainerProvider.getWebSocketContainer();
        }
        return c;
    }

}
