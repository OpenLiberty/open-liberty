/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeDefaultInterceptors;

@Local(MixedSFLocal.class)
@Remote(MixedSFRemote.class)
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
        // Intentionally blank
    }
}
