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
package com.ibm.ejblite.interceptor.v32.mix.ejb;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeDefaultInterceptors;

@Local(MixedSFLocal.class)
@Stateful
@ExcludeDefaultInterceptors
public class MixedSFInterceptorBean {
    private String ivString;

    public void setString(String str) {
        ivString = str;
    }

    public String getString() {
        return ivString;
    }

    @Remove
    public void destroy() {
    }
}
