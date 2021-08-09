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
package com.ibm.ws.wsoc.external.v11;

import com.ibm.ws.wsoc.external.SessionExt;
import com.ibm.ws.wsoc.external.WebSocketFactory;

/**
 * WebSocket 1.1 Factory implementation
 */
public class WebSocketFactoryV11 implements WebSocketFactory {

    @Override
    public SessionExt getWebSocketSession() {
        return new SessionExtV11();
    }

}
