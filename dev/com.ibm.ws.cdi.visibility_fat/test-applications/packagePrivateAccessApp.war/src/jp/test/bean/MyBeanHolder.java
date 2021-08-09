/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jp.test.bean;

import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
class MyBeanHolder {

    @Inject
    private MyBean bean;

    // public method
    public void testPublicMethod() {
        assertNotNull(bean);
    }

    // package private method
    void testPackageMethod() {
        assertNotNull(bean);
    }
}
