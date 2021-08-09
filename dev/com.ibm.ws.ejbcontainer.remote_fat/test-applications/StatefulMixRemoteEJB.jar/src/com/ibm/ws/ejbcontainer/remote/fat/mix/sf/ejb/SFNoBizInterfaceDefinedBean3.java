/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

public class SFNoBizInterfaceDefinedBean3 implements RemoteInterface1, RemoteInterface2 {

    public String locBizInterface1() {
        return "Used LocalInterface1";
    }

    public String locBizInterface2() {
        return "Used LocalInterface2";
    }

    @Override
    public String remoteBizInterface1() {
        return "Used RemoteInterface1";
    }

    @Override
    public String remoteBizInterface2() {
        return "Used RemoteInterface2";
    }

    @Override
    public void finish() {
    }
}
