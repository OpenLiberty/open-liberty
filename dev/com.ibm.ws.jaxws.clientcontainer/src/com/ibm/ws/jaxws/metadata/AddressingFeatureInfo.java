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
package com.ibm.ws.jaxws.metadata;

import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.AddressingFeature.Responses;

public class AddressingFeatureInfo implements WebServiceFeatureInfo {

    private static final long serialVersionUID = -8252887469136111502L;

    private boolean enabled;

    private boolean required;

    private Responses responses;

    public AddressingFeatureInfo() {
        enabled = true;
        required = false;
        responses = AddressingFeature.Responses.ALL;
    }

    public AddressingFeatureInfo(Addressing addressing) {
        this(addressing.enabled(), addressing.required(), addressing.responses());
    }

    public AddressingFeatureInfo(boolean enabled, boolean required, AddressingFeature.Responses responses) {
        this.enabled = enabled;
        this.required = required;
        this.responses = responses;
    }

    public AddressingFeatureInfo(boolean enabled, boolean required, String responsesValue) {
        this.enabled = enabled;
        this.required = required;
        if ("ANONYMOUS".equals(responsesValue)) {
            responses = AddressingFeature.Responses.ANONYMOUS;
        } else if ("NON_ANONYMOUS".equals(responsesValue)) {
            responses = AddressingFeature.Responses.NON_ANONYMOUS;
        } else {
            responses = AddressingFeature.Responses.ALL;
        }
    }

    @Override
    public WebServiceFeature getWebServiceFeature() {
        return new AddressingFeature(enabled, required, responses);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setResponses(AddressingFeature.Responses responses) {
        this.responses = responses;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public Responses getResponses() {
        return responses;
    }

    @Override
    public String toString() {
        return "AddressingFeatureInfo [enabled=" + enabled + ", required=" + required + ", responses=" + responses + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + (required ? 1231 : 1237);
        result = prime * result + ((responses == null) ? 0 : responses.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AddressingFeatureInfo other = (AddressingFeatureInfo) obj;
        if (enabled != other.enabled)
            return false;
        if (required != other.required)
            return false;
        if (responses != other.responses)
            return false;
        return true;
    }

}
