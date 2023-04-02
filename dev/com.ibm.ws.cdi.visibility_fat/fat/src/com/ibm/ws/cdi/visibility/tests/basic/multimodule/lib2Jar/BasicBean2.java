/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.basic.multimodule.lib2Jar;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi.visibility.tests.basic.multimodule.lib1Jar.BasicBean1;

@RequestScoped
public class BasicBean2 {

    @Inject
    private BasicBean1 bean1;

    /**
     * @param string
     */
    public void setData(String string) {
        bean1.setData(string);
    }

}
