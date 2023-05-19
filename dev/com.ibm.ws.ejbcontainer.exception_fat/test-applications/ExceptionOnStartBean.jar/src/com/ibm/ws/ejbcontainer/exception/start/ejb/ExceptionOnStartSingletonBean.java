/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.exception.start.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.exception.start.util.ExceptionOnStartException;

/**
 * Enterprise Bean: ExceptionOnStartSingletonBean
 *
 * Class will fail to load because the exception on the throws clause is missing.
 */
@Singleton
public class ExceptionOnStartSingletonBean {

    public ExceptionOnStartSingletonBean() throws ExceptionOnStartException {
        System.out.println("StartupSingletonBean.<init>() called");
    }

    @PostConstruct
    private void postConstruct() throws EJBException {
        System.out.println("StartupSingletonBean.postConstruct() called");
    }

    public String sayHello(String name) {
        return ("Hello " + name + "!");
    }

}
