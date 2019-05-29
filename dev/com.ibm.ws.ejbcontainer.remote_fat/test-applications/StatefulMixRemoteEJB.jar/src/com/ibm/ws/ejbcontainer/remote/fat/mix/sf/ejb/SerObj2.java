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

public class SerObj2 implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer intVal = null;
    private SerObj serObj = null;

    public SerObj2() {
        intVal = new Integer(-1);
        serObj = new SerObj("Crown", "Victoria");
    }

    public SerObj2(Integer intValParm, SerObj serObjParm) {
        intVal = intValParm;
        serObj = serObjParm.clone();
    }

    public Integer getIntegerVal() {
        return intVal;
    }

    public SerObj getSerObjVal() {
        return serObj;
    }

    @Override
    public SerObj2 clone() {
        return new SerObj2(this.getIntegerVal(), this.getSerObjVal());
    }
}
