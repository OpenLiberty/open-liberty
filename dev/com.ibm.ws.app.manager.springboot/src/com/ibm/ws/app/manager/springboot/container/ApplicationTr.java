/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ApplicationTr {

    private static final TraceComponent tc = Tr.register(ApplicationTr.class);

    public enum Type {
        ERROR_NEED_SPRING_BOOT_VERSION_15("error.need.springboot.version.15"),
        ERROR_NEED_SPRING_BOOT_VERSION_20("error.need.springboot.version.20"),
        ERROR_MISSING_SERVLET_FEATURE("error.missing.servlet"),
        ERROR_MISSING_WEBSOCKET_FEATURE("error.missing.websocket"),
        ERROR_UNSUPPORTED_SPRING_BOOT_VERSION("error.wrong.spring.boot.version"),
        WARNING_UNSUPPORTED_JAVA_VERSION("warning.java.version.not.supported"),
        ERROR_INVALID_PACKAGED_LIBERTY_JAR("error.invalid.packaged.liberty.jar"),
        ERROR_APP_NOT_FOUND_INSIDE_PACKAGED_LIBERTY_JAR("error.application.not.found.inside.packaged.liberty.jar");

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

}
