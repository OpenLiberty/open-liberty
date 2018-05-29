/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc1;

import java.util.logging.Logger;

/**
 * Bean implementation class for Enterprise Bean: CMTStatelessLocalBean
 **/
public class CMTStatelessLocalBean {
    private static final String CLASS_NAME = CMTStatelessLocalBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    public void tx_NotSupported() {
        svLogger.info("Method tx_NotSupported called successfully");
    }

    public CMTStatelessLocalBean() {}
}