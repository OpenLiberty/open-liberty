/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.app_exception.xml.ejb;

public class ResultObject {

    public Throwable t;
    public boolean isRolledBack;

    ResultObject(boolean isRolledBack, Throwable t) {
        this.isRolledBack = isRolledBack;
        this.t = t;
    }

    @Override
    public String toString() {
        return "[RB: " + isRolledBack + ", T: " + t.getClass() + "]";
    }
}
