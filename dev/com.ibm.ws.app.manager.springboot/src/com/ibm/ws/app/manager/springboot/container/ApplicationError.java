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

/**
 *
 */
public class ApplicationError extends RuntimeException {
    private static final TraceComponent tc = Tr.register(ApplicationError.class);

    private static final long serialVersionUID = 1L;

    public enum Type {
        NEED_SPRING_BOOT_VERSION_15("error.need.springboot.version.15"),
        NEED_SPRING_BOOT_VERSION_20("error.need.springboot.version.20"),
        MISSING_SERVLET_FEATURE("error.missing.servlet");

        private final String msgKey;

        private Type(String msgKey) {
            this.msgKey = msgKey;
        }

        public String getMessageKey() {
            return msgKey;
        }
    }

    public final Type type;

    /**
     * @param type
     */
    public ApplicationError(Type type) {
        this("", type);
    }

    public ApplicationError(String message, Type type) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void log() {
        Tr.error(tc, type.getMessageKey());
    }
}
