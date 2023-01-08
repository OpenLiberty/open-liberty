/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.jndi.lookup;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

@RequestScoped
public class MyBean {

    public String hello() {
        return "You found me!";
    }

    public BeanManager getBeanMangerViaJNDI() throws Exception {
        return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
    }

}
