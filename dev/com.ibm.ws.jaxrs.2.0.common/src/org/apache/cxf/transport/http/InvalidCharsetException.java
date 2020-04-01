/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.cxf.transport.http;

import java.io.IOException;

public class InvalidCharsetException extends IOException {

    public InvalidCharsetException(String m) {
        super(m);
    }

    private static final long serialVersionUID = 1676878985438910205L;

}
