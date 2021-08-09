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

public class SerObj implements Serializable {
    private static final long serialVersionUID = 6185017052603483497L;
    private final String strVal;
    transient private String transStrVal;

    public SerObj() {
        strVal = "";
        transStrVal = "";
    }

    public SerObj(String strValParm, String transStrValParm) {
        strVal = strValParm;
        transStrVal = transStrValParm;
    }

    public String getStrVal() {
        return strVal;
    }

    public String getTransStrVal() {
        return transStrVal;
    }

    @Override
    public SerObj clone() {
        return new SerObj(this.getStrVal(), this.getTransStrVal());
    }
}
