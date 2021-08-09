/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import java.io.Serializable;

import javax.ejb.Remove;

public class StatefulSerBase implements Serializable {
    private static final long serialVersionUID = 2470078434895890155L;
    private String baseString = "Empty";

    public String getBaseString() {
        return baseString;
    }

    public void setBaseString(String str) {
        baseString = str;
    }

    @Remove
    public void finish() {
    }
}
