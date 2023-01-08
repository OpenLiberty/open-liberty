/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.ejblink.ejbwaro;

import javax.ejb.Stateless;

import com.ibm.ws.ejbcontainer.ejblink.ejb.AutoLinkLocalJarOtherWar;

/**
 * Basic Stateless Bean implementation for testing AutoLink to a bean in an
 * ejb-jar module and .war module
 **/
@Stateless
public class JarOtherWarBean implements AutoLinkLocalJarOtherWar {
    private static final String CLASS_NAME = JarOtherWarBean.class.getName();

    @Override
    public String getBeanName() {
        return CLASS_NAME;
    }

    public JarOtherWarBean() {
        // intentionally blank
    }
}
