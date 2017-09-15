/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import com.ibm.websphere.ras.annotation.Sensitive;

@Sensitive
public enum OpcodeType {
    TEXT_WHOLE,
    TEXT_PARTIAL_FIRST,
    TEXT_PARTIAL_CONTINUATION,
    TEXT_PARTIAL_LAST,
    BINARY_WHOLE,
    BINARY_PARTIAL_FIRST,
    BINARY_PARTIAL_CONTINUATION,
    BINARY_PARTIAL_LAST,
    CONNECTION_CLOSE,
    PING,
    PONG
}
