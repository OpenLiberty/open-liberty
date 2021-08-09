/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * J2EENameFactory is used to create J2EEName instances from a byte array.
 * 
 * @ibm-private-in-use
 */
public interface J2EENameFactory {

    /**
     * Creates a J2EEName from a serialized name using {@link J2EEName#getBytes()}.
     */
    public J2EEName create(byte[] bytes);

    /**
     * Creates a J2EEName using the application, module,
     * and component name.
     * 
     * @param app the application name
     * @param module the module name, or null for an application J2EEName
     * @param component the component name, or null for an application or
     *            module J2EEName
     */
    public J2EEName create(String app, String module, String component);

}
