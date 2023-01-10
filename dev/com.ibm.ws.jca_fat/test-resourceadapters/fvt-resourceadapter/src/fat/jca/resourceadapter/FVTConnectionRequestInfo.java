/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package fat.jca.resourceadapter;

import javax.resource.spi.ConnectionRequestInfo;

public class FVTConnectionRequestInfo implements ConnectionRequestInfo {
    final String userName, password;

    FVTConnectionRequestInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FVTConnectionRequestInfo) {
            FVTConnectionRequestInfo cri = (FVTConnectionRequestInfo) o;
            return userName.equals(cri.userName) && password.equals(cri.password);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }
}
