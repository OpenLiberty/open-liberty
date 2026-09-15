/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.ws.classloading.genstubtest.ejb;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import com.ibm.ws.classloading.genstubtest.view.EndpointBeanRemote;

/**
 * This EJB is the remote endpoint that is loaded by both an EJB and a
 * servlet. Since the EJB JAR module and WAR module each has their own
 * copy of this bean's remote interface, there is a potential for class
 * cast exceptions - especially in parentLast configurations.
 */
@Stateless
@Remote(EndpointBeanRemote.class)
public class EndpointBean implements EndpointBeanRemote {

    @Override
    public String hello() {
        return "Hello";
    }
}
