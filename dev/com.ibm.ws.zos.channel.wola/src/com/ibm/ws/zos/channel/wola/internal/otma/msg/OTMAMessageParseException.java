/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.otma.msg;

/**
 * For failures while parsing OTMA messages.
 */
public class OTMAMessageParseException extends Exception {

    public OTMAMessageParseException(String msg) {
        super(msg);
    }

    public OTMAMessageParseException(String msg, Exception e) {
        super(msg, e);
    }

    public OTMAMessageParseException(Exception e) {
        super(e);
    }
}
