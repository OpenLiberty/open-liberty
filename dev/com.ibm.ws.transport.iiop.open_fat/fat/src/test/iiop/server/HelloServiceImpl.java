/*******************************************************************************
 * Copyright (c) 2014-2023 IBM Corporation and others.
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
package test.iiop.server;

import java.rmi.RemoteException;

import org.osgi.service.component.annotations.Component;

import test.iiop.common.HelloService;

@Component(immediate = true)
public class HelloServiceImpl extends AbstractRemoteService<HelloServiceImpl> implements HelloService {
    @Override
    public void sayHello() throws RemoteException {
        System.out.println("Hello, world! I am your humble servant.");
    }
}
