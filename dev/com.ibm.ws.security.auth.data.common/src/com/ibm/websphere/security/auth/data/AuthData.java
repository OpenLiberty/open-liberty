/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.data;

import java.nio.file.Path;

/**
 * The AuthData interface is used to obtain the user and password from the configured auth data.
 */
public interface AuthData {

    /**
     * Gets the user name as defined in the configuration.
     *
     * @return the user name.
     */
    public String getUserName();

    /**
     * Gets the password as a char[] as defined in the configuration.
     *
     * @return the char[] representation of the password.
     */
    public char[] getPassword();

    public String getKrb5Principal();

    public Path getKrb5TicketCache();

}
