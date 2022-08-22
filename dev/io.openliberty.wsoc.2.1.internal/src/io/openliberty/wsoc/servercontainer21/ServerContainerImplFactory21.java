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
package io.openliberty.wsoc.servercontainer21;

import com.ibm.ws.wsoc.servercontainer.ServerContainerExt;
import com.ibm.ws.wsoc.servercontainer.ServletContainerFactory;

import org.osgi.service.component.annotations.Component;

@Component(service = ServletContainerFactory.class, property = { "service.vendor=IBM" })
public class ServerContainerImplFactory21 implements ServletContainerFactory {

    //private static final ServerContainerExt21 serverContainerExt21 = new ServerContainerExt21();

    @Override
    public ServerContainerExt getServletContainer() {
        return new ServerContainerExt21();
    }

}
