/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.security.authorization.jacc;

import java.util.List;

public class MethodInfo {
    private final String methodName;
    private final String methodInterfaceName;
    private final List<String> paramList;

    public MethodInfo(String methodName, String methodInterfaceName, List<String> paramList) {
        this.methodName = methodName;
        this.methodInterfaceName = methodInterfaceName;
        this.paramList = paramList;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodInterfaceName() {
        return methodInterfaceName;
    }

    public List<String> getParamList() {
        return paramList;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("method : " ).append(methodName).append(" interface : ").append(methodInterfaceName).append(" parameters : ");
        if (paramList != null) {
            for (String s : paramList) {
                buf.append(s).append(", ");
            }
        } else {
            buf.append("null");
        }
        return buf.toString();
    }
}
