/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.ejb;

//@Stateless (name="SLEnvInjectTest"), defined in xml
public class SLEnvInjectTestBean implements SimpleSLLocal {
    @Override
    public String getString() {
        return "success";
    }
}