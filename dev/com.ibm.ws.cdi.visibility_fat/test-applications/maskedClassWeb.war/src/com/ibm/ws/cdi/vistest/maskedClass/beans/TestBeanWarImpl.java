/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.vistest.maskedClass.beans;

import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.cdi.vistest.maskedClass.beans.TestBean;

/**
 * A test bean implementation in the war.
 */
@ApplicationScoped
public class TestBeanWarImpl implements TestBean {

    @Override
    public String getMessage() {
        return "This is TestBean in the war";
    }
}
