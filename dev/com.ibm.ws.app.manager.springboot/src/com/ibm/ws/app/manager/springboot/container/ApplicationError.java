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
import com.ibm.ws.app.manager.springboot.container.ApplicationTr.Type;

/**
 *
 */
public class ApplicationError extends RuntimeException {
    private static final TraceComponent tc = Tr.register(ApplicationError.class);

    private static final long serialVersionUID = 1L;

    public final Type type;
    public final Object[] messageArgs;

    /**
     * @param type
     * @param messageArgs
     */
    public ApplicationError(Type type, Object... messageArgs) {
        super(Tr.formatMessage(tc, type.getMessageKey(), messageArgs));
        this.type = type;
        this.messageArgs = messageArgs;
    }

    public Type getType() {
        return type;
    }

    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
