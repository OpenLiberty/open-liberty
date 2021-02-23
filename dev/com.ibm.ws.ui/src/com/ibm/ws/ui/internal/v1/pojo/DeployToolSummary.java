/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ui.internal.validation.InvalidDeployValidationException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * Represents a deploy validation object, containing all the server-side validation that is required.
 * <p>
 * Example JSON: {password:"password"}
 */
public class DeployToolSummary implements SelfValidatingPOJO {
    static final int MAX_LENGTH = 4096;

    /**
     * The deploy tools passwordValidation status. This contains valid or invalid.
     * <b>This is serialized by Jackson</b>
     */
    private String passwordValidationStatus;

    /**
     * Default DeployToolSummary constructor.
     * Zero-argument constructor used by Jackson.
     * Should only be invoked directly in unit test.
     * 
     * @param initialzeDefaults
     */
    @Trivial
    DeployToolSummary() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * Construct a ToolEntry.
     * 
     * @param id See {@link #id}.
     * @param type See {@link #type}.
     */
    @Trivial
    public DeployToolSummary(final String passwordValidationStatus) {
        this.passwordValidationStatus = passwordValidationStatus;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setPassword(String passwordValidationStatus) {
        this.passwordValidationStatus = passwordValidationStatus;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getPasswordValidationStatus() {
        return passwordValidationStatus;
    }

    /** {@inheritDoc} */
    @Override
    public void validateSelf() throws InvalidDeployValidationException {
        //NOP
    }

    /**
     * Returns the appropriate String representation of the String.
     * Notably, if the str is null, then "null" (not quoted) is returned,
     * otherwise the String is quoted.
     * 
     * @param str
     * @return
     */
    @Trivial
    String getJSONString(final String str) {
        if (str == null) {
            return "null";
        } else {
            return "\"" + str + "\"";
        }
    }
}
