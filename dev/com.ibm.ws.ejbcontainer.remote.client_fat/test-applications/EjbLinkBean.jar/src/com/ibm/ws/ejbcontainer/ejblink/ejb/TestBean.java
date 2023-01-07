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

package com.ibm.ws.ejbcontainer.ejblink.ejb;

/**
 * Basic Stateless Bean implementation for testing ejb-link from the current module
 **/
public class TestBean {
    private static final String CLASS_NAME = TestBean.class.getName();

    public String getBeanName() {
        return CLASS_NAME;
    }

    public TestBean() {
        // intentionally blank
    }
}
