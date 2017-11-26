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
package jp.test.bean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
class MyBeanHolder {

    @Inject
    private MyBean bean;

    // public method
    public String test1() {
        return bean == null ? "FAILED" : "PASSED";
    }

    // package private method
    String test2() {
        return bean == null ? "FAILED" : "PASSED";
    }
}
