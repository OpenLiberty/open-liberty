/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import javax.resource.spi.ConnectionRequestInfo;

public class FVT10ConnectionRequestInfo implements ConnectionRequestInfo {
    final String userName, password;

    FVT10ConnectionRequestInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FVT10ConnectionRequestInfo) {
            FVT10ConnectionRequestInfo cri = (FVT10ConnectionRequestInfo) o;
            return userName.equals(cri.userName) && password.equals(cri.password);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        if (userName != null && password != null) {
            return userName.hashCode() + password.hashCode();
        } else {
            return super.hashCode();
        }
    }
}
