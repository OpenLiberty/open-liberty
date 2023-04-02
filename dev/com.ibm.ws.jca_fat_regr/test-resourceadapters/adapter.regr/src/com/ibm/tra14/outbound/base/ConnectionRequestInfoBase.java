/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
package com.ibm.tra14.outbound.base;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * @author kwok
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ConnectionRequestInfoBase implements ConnectionRequestInfo {

    private String user;
    private String password;

    public ConnectionRequestInfoBase(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj instanceof ConnectionRequestInfoBase) {
            ConnectionRequestInfoBase other = (ConnectionRequestInfoBase) obj;
            return isEqual(user, other.user) && isEqual(password, other.password);
        } else {
            return false;
        }
    }

    public int hashCode() {
        String result = "" + user + password;
        return result.hashCode();
    }

    private boolean isEqual(Object o1, Object o2) {
        if (o1 == null)
            return o2 == null;
        else
            return o1.equals(o2);
    }

    public String toString() {
        return "CCICxReqInfo:" + getUser() + ":" + getPassword();
    }
}
