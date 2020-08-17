/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cloudtx.ut.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.tx.jta.ut.util.TestServletBase;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

/**
 *
 */
public abstract class CloudTestServletBase extends TestServletBase {

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        XAResourceImpl.setStateFile(System.getenv("WLP_OUTPUT_DIR") + "/../shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);
        super.doGet(request, response);
    }
}
