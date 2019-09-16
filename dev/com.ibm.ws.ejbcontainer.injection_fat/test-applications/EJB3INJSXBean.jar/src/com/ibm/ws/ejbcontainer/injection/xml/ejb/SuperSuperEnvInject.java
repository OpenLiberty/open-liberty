/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

public class SuperSuperEnvInject extends SuperOfSuperSuperEnvInject {
    protected int myNumber = 90210;
    private boolean field = false;
    protected String field2 = "No";
    public Character field3 = 'n';

    protected int field4 = 0;
    private float field5 = 1.11F;
    public short field6 = 1;

    protected void setField4(int field4) {
        this.field4 = field4;
    }

    @SuppressWarnings("unused")
    private void setField5(float field5) {
        this.field5 = field5;
    }

    public void setField6(short field6) {
        this.field6 = field6;
    }

    protected void setMyNumber(int myNumber) {
        this.myNumber = myNumber;
    }

    public int getMyNumber() {
        return myNumber;
    }

    public boolean getField() {
        return this.field;
    }

    public float getField5() {
        return this.field5;
    }
}
