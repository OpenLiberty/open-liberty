/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.dynamicconfigadapter;

import javax.resource.spi.ConnectionRequestInfo;

public class DynaCfgConnectionRequestInfo implements ConnectionRequestInfo {
    final String userName, password;

    DynaCfgConnectionRequestInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DynaCfgConnectionRequestInfo) {
            DynaCfgConnectionRequestInfo cri = (DynaCfgConnectionRequestInfo) o;
            return (userName == null ? cri.userName == null : userName.equals(cri.userName))
                   && (password == null ? cri.password == null : password.equals(cri.password));
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return userName == null ? 0 : userName.hashCode();
    }
}
