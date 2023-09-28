/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
package com.ibm.ws.app.manager.springboot.thin.container;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ApplicationTr {
    private static final TraceComponent tc = Tr.register(ApplicationTr.class);

    public enum Type {
        ERROR_INVALID_PACKAGED_LIBERTY_JAR("error.invalid.packaged.liberty.jar"),
        ERROR_APP_NOT_FOUND_INSIDE_PACKAGED_LIBERTY_JAR("error.application.not.found.inside.packaged.liberty.jar"),
        ERROR_UNSUPPORTED_SPRING_BOOT_VERSION("error.wrong.spring.boot.version");

        private final String msgKey;

        private Type(String msgKey) {
            this.msgKey = msgKey;
        }

        public String getMessageKey() {
            return msgKey;
        }
    }

    public static final void warning(Type type, Object... messageArgs) {
        Tr.warning(tc, type.getMessageKey(), messageArgs);
    }

    public static final void warning(String msgKey, Object... messageArgs) {
        Tr.warning(tc, msgKey, messageArgs);
    }

    public static final void error(String msgKey, Object... messageArgs) {
        Tr.error(tc, msgKey, messageArgs);
    }
}
