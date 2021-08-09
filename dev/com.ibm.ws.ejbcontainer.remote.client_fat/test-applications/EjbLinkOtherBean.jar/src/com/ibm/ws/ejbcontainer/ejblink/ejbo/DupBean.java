/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.ejblink.ejbo;

/**
 * Basic Stateless Bean implementation for testing ejb-link error when bean exists
 * in both .war and ejb-jar modules
 **/
public class DupBean {
    private static final String CLASS_NAME = DupBean.class.getName();

    public String getBeanName() {
        return CLASS_NAME;
    }

    public DupBean() {
        // intentionally blank
    }
}
