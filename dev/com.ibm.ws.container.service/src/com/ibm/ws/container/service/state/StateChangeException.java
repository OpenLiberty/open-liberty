/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.container.service.state;

public class StateChangeException extends Exception {

    private static final long serialVersionUID = 7229330995297804384L;

    public StateChangeException(String s) {
        super(s);
    }

    public StateChangeException(String s, Throwable t) {
        super(s, t);
    }

    public StateChangeException(Throwable t) {
        super(t);
    }
}
