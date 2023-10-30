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
package io.openliberty.cdi40.internal.fat.startupEvents.sharedLib;

import javax.naming.InitialContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;

@ApplicationScoped
public class JNDILookupBean {

    public BeanManager getBeanMangerViaJNDI() throws Exception {
        return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
    }

}