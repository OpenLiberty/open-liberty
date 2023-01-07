/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.mix.ejb;

import javax.ejb.Stateless;

@Stateless(name = "SLEnvInjectTest")
public class SLEnvInjectTestBean implements SimpleSLLocal {
    @Override
    public String getString() {
        return "success";
    }
}