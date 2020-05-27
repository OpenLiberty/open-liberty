/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.tools.ant;

import java.io.PrintStream;

import org.apache.tools.ant.BuildException;

import com.ibm.ws.jaxb.tools.internal.JaxbToolsConstants;
import com.ibm.ws.jaxb.tools.internal.JaxbToolsUtil;
import com.sun.tools.xjc.XJC2Task;

/**
 *
 */
public class XJCTask extends XJC2Task {
    private static final PrintStream err = System.err;
    private boolean targetExisted = false;

    @Override
    public void setTarget(String version) {
        super.setTarget(version);
        this.targetExisted = true;
    }

    @Override
    public void execute() throws BuildException {
        if (!targetExisted) {
            String errMsg = JaxbToolsUtil.formatMessage(JaxbToolsConstants.ERROR_PARAMETER_TARGET_MISSED_KEY);
            err.print(errMsg);
            return;
        }

        super.execute();
    }
}
