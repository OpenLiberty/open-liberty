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

package com.ibm.ws.zos.channel.wola.internal.msg;

import java.io.IOException;

/**
 * For failures while parsing WOLA messages.
 */
public class WolaMessageParseException extends IOException {

    /**
     * @param string
     */
    public WolaMessageParseException(String msg) {
        super(msg);
    }

}
