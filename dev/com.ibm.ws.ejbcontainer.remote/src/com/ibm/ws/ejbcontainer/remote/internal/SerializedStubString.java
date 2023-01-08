/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.internal;

import java.io.Serializable;

/**
 * A wrapper for a stub that supports standard serialization.
 *
 * When a stub is read using read_value, the ORB will not reconnect the stub.
 * When rmic compatibility is enabled, stubs for interfaces that are not RMI/IDL
 * abstract interfaces will extend SerializableStub, which has a writeReplace
 * method to substitute an instance of SerialiedStub, but that instance will
 * contain a reference to the original stub which serialization will replace
 * with a reference back to the same SerializedStub instance. The original stub
 * is lost.
 *
 * The serialization service (via SerializedStubHandler) will compensate by replacing
 * the SerializedStub with an instance of this class, which holds a reference to the
 * String representation of the stub rather than the original stub. Then when read
 * back in the stringified stub will be converted back to the original stub and
 * narrowed in resolveObject(). The ORB will reconnect the stub.
 */
public class SerializedStubString implements Serializable {

    private static final long serialVersionUID = 221516676682695674L;

    private final String stub_string;
    private final Class<?> remote_interface;

    SerializedStubString(String stub, Class<?> remoteInterface) {
        stub_string = stub;
        remote_interface = remoteInterface;
    }

    String getStub() {
        return stub_string;
    }

    Class<?> getInterface() {
        return remote_interface;
    }
}
