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
package com.ibm.ws.security.oauth20.plugins.jose4j;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.TraceConstants;

/**
 *
 */
public class CauseMsg {

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(CauseMsg.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    Throwable cause;
    String exceptionName;
    String exceptionMsg;

    /*
     * The class is to translate an generic Exception in to a cause string
     * for the structure message
     */
    @Trivial
    public CauseMsg(Throwable e) {
        this.cause = e;
        exceptionName = e.getClass().getSimpleName();
        exceptionMsg = e.getMessage();
    }

    /*
     * intend to be the error cause when it's is a child
     */
    @Override
    public String toString() {
        String causeMsg = "";
        causeMsg += getWord(exceptionName);
        causeMsg += getWord(exceptionMsg);
        return causeMsg;
    }

    /**
     * @param content
     * @return
     */
    String getWord(String content) {
        if (content != null && !content.isEmpty()) {
            return "[" + content + "]";
        } else {
            return "";
        }
    }

    /**
     * @return the cause
     */
    public Throwable getCause() {
        return cause;
    }

}
