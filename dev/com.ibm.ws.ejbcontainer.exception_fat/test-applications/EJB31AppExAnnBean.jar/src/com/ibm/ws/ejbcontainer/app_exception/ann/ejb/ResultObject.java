/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.app_exception.ann.ejb;

import java.io.Serializable;

public class ResultObject implements Serializable {
    private static final long serialVersionUID = 8634077167945174783L;

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
