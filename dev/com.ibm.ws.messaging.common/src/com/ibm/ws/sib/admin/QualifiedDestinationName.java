/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

public final class QualifiedDestinationName {

    private String _bus = null;
    private String _destination = null;

    public QualifiedDestinationName(String bus, String destination) {
        _bus = bus;
        _destination = destination;
    }

    public String getBus() {
        return _bus;
    }

    public String getDestination() {
        return _destination;
    }
}
