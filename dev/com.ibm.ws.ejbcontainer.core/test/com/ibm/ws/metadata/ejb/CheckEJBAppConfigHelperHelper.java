/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ejs.container.ContainerConfigConstants;

public class CheckEJBAppConfigHelperHelper {
    public static void setValidationFailable() {
        System.setProperty(ContainerConfigConstants.checkAppConfigProp, "true");
        CheckEJBAppConfigHelper.refreshCheckEJBAppConfigSetting();
    }

    public static void unsetValidationFailable() {
        System.getProperties().remove(ContainerConfigConstants.checkAppConfigProp);
        CheckEJBAppConfigHelper.refreshCheckEJBAppConfigSetting();
    }

    public static void failable(PrivilegedExceptionAction<?> action) throws Exception {
        setValidationFailable();
        try {
            action.run();
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException(cause);
        } finally {
            unsetValidationFailable();
        }
    }
}
