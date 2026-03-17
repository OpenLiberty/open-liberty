/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package ra.ao;

import java.io.Serializable;

import jakarta.resource.spi.AdministeredObject;
import jakarta.resource.spi.ConfigProperty;

@AdministeredObject
public class DummyImpl implements DummyInterface, Serializable {

    private static final long serialVersionUID = 1L;

    @ConfigProperty
    private String message;

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }

}
