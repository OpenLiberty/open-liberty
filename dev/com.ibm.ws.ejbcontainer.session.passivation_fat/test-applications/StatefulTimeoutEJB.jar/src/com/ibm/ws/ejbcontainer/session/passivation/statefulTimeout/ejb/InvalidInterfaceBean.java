/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb;

import java.util.logging.Logger;

import javax.ejb.Remove;
import javax.ejb.Stateful;

@Stateful
public class InvalidInterfaceBean implements InvalidInterface {
    private final static String CLASSNAME = InvalidInterfaceBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Override
    public long getInvocationTime() {
        return System.currentTimeMillis();
    }

    @Override
    @Remove
    public void remove() {
        svLogger.info("in remove method");
    }
}