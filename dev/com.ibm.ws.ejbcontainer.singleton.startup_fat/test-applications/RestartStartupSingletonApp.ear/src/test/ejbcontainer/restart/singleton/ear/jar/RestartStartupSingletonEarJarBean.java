/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package test.ejbcontainer.restart.singleton.ear.jar;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal;
import test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote;

@Startup
@Singleton
@Local(RestartStartupSingletonLocal.class)
@Remote(RestartStartupSingletonRemote.class)
public class RestartStartupSingletonEarJarBean {

    @Resource(name = "ThrowExceptionOnStart")
    boolean throwExceptionOnStart = false;

    @PostConstruct
    public void postConstruct() {
        if (throwExceptionOnStart) {
            throw new EJBException("Expected exception");
        }
    }

    public String getName() {
        return getClass().getSimpleName();
    }
}
