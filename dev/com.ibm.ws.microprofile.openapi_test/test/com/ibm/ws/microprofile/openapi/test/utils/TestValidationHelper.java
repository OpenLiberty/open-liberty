/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.test.utils;

import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.impl.validation.ValidationHelper;

public class TestValidationHelper implements ValidationHelper {

    OASValidationResult result = new OASValidationResult();

    /** {@inheritDoc} */
    @Override
    public void addValidationEvent(ValidationEvent event) {
        if (result != null && event != null) {
            result.getEvents().add(event);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addOperationId(String operationId) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void addLinkOperationId(String operationId, String location) {

    }

    public void resetResults() {
        result = new OASValidationResult();
    }

    public OASValidationResult getResult() {
        return result;
    }

    public boolean hasEvents() {
        return !result.getEvents().isEmpty();
    }

    public int getEventsSize() {
        return result.getEvents().size();
    }

    @Override
    public String toString() {
        if (!hasEvents())
            return "No events";
        StringBuffer b = new StringBuffer();
        for (ValidationEvent event : result.getEvents()) {
            b.append("\n" + event.severity + "," + event.location + "," + event.message);
        }
        return b.toString();
    }
}
