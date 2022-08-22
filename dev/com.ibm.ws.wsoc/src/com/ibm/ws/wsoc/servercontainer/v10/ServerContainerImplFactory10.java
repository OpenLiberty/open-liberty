/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.servercontainer.v10;

import com.ibm.ws.wsoc.servercontainer.ServerContainerExt;
import com.ibm.ws.wsoc.servercontainer.ServletContainerFactory;

/*
 * Used for Websocket 1.0, 1.1 and 2.0
 */
public class ServerContainerImplFactory10 implements ServletContainerFactory {

    @Override
    public ServerContainerExt getServletContainer() {
        return new ServerContainerExt10();
    }

}
