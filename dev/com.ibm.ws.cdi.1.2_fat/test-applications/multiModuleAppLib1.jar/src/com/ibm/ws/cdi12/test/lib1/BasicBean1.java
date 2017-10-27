/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.lib1;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class BasicBean1 {

    @Inject
    private BasicBean1A bean1a;

    /**
     * @param string
     */
    public void setData(String string) {
        bean1a.setData(string);
    }

}
