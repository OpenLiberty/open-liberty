/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import java.io.Serializable;

public class RmtObject implements Serializable {
    private static final long serialVersionUID = -3185034599717262437L;
    private String ivString = "";

    RmtObject() {
    }

    public RmtObject(String str) {
        ivString = str;
    }

    String getString() {
        return ivString;
    }
}
