/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.metatype.validator.xml;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public class MetatypeAdOption extends MetatypeBase {
    @XmlAttribute(name = "value")
    private String value;
    @XmlAttribute(name = "label")
    private String label;

    private MetatypeAd parent;

    protected void setParentAd(MetatypeAd parent) {
        this.parent = parent;
    }

    public MetatypeAd getParentAd() {
        return parent;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        if (value != null)
            sb.append("value='").append(value).append("' ");
        if (label != null)
            sb.append("label='").append(label).append("' ");

        sb.append('}');
        return sb.toString();
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);
        String trimmedLabel = null;
        String trimmedValue = null;

        if (label == null) {
            logMsgWithContext(MessageType.Error, value, "missing.attribute", "label");
        } else {
            trimmedLabel = label.trim();
            if (label.length() != trimmedLabel.length())
                logMsgWithContext(MessageType.Info, value, "white.space.found", "label", label);
        }

        if (value == null) {
            logMsgWithContext(MessageType.Error, value, "missing.attribute", "value");
        } else {
            trimmedValue = value.trim();
            if (value.length() != trimmedValue.length())
                logMsgWithContext(MessageType.Info, value, "white.space.found", "value", value);
        }

        // If we ever start translating AD Options, un-comment this section of code
        // so we can check whether something needs to be validated that wasn't.
//        if (trimmedLabel != null && trimmedValue != null) {
//            if (!trimmedLabel.equals(trimmedValue) && !trimmedLabel.startsWith("%"))
//                logMsgWithContext(MessageType.Error, "needs.translation", "label", label);
//        }

        // check if there are unknown elements
        checkIfUnknownElementsPresent();

        // check if there are unknown attributes
        checkIfUnknownAttributesPresent();
    }
}
