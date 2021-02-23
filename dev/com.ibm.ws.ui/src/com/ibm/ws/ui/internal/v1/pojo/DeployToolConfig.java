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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.TraceConstants;
import com.ibm.ws.ui.internal.validation.InvalidDeployValidationException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * Represents a deploy validation object, containing all the server-side validation that is required.
 * <p>
 * Example JSON: {password:"password"}
 */
@TraceOptions(messageBundle = TraceConstants.VALIDAITON_STRING_BUNDLE)
public class DeployToolConfig implements SelfValidatingPOJO {
    private static transient final TraceComponent tc = Tr.register(DeployToolConfig.class);
    static final int MAX_LENGTH = 4096;

    /**
     * The deploy tools password.
     * <b>This is serialized by Jackson</b>
     */
    private String password;

    /**
     * Default DeployValidation constructor.
     * Zero-argument constructor used by Jackson.
     * Should only be invoked directly in unit test.
     * 
     * @param initialzeDefaults
     */
    @Trivial
    DeployToolConfig() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * Construct a ToolEntry.
     * 
     * @param id See {@link #id}.
     * @param type See {@link #type}.
     */
    @Trivial
    public DeployToolConfig(final String password) {
        this.password = password;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Trivial
    public String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public void validateSelf() throws InvalidDeployValidationException {
        // check required fields
        if (password == null || "".equals(password.trim())) {
            throw new InvalidDeployValidationException(RequestNLS.formatMessage(tc, "RQD_FIELDS_MISSING", password));
        }
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof DeployToolConfig) {
            final DeployToolConfig that = (DeployToolConfig) o;

            boolean sameFields = true;
            sameFields &= (this.password == that.password) || (getPassword() != null && getPassword().equals(that.password));
            return sameFields;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc} <p>
     * A tool with no ID is not valid.
     * Therefore we don't really need to worry about the hash code.
     */
    @Trivial
    @Override
    public int hashCode() {
        return (password == null) ? 0 : password.hashCode();
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
