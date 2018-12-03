/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metatype.validator.xml;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public class MetatypeObject extends MetatypeBase {
    private MetatypeDesignate parent;

    @XmlAttribute(name = "ocdref")
    private String ocdref;

    private MetatypeOcd matchingOcd = null;

    public String getOcdref() {
        return ocdref;
    }

    private void validateOcdref(boolean validateRefs) {
        if (ocdref == null)
            logMsg(MessageType.Error, "missing.attribute", "ocdref");
        else {
            String trimmed = ocdref.trim();
            if (trimmed.length() != ocdref.length())
                logMsg(MessageType.Info, "white.space.found", "ocdref", ocdref);

            matchingOcd = root.getMatchingOcd(trimmed);
            if (matchingOcd == null)
                logMsg(MessageType.Error, "ocd.not.validated", ocdref, "an OCD with the ID specified by 'ocdref' could not be found");
            else {
                matchingOcd.setOcdStats(getOcdStats());
                matchingOcd.setNlsKeys(getNlsKeys());
                matchingOcd.setRoot(root);
                matchingOcd.validate(validateRefs);
                setValidityState(matchingOcd.getValidityState());
            }
        }
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);

        // check if there are unknown elements
        checkIfUnknownElementsPresent();

        // check if there are unknown attributes
        checkIfUnknownAttributesPresent();

        validateOcdref(validateRefs);
    }

    protected void setParentDesignate(MetatypeDesignate parent) {
        this.parent = parent;
    }

    public MetatypeDesignate getParentDesignate() {
        return parent;
    }

    public MetatypeOcd getMatchingOcd() {
        return matchingOcd;
    }
}
