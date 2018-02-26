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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent.Severity;
import com.ibm.ws.microprofile.openapi.impl.validation.ValidationHelper;

public class TestValidationHelper implements ValidationHelper {

    private final Set<String> operationIds = new HashSet<>();
    private final Map<String, Set<String>> linkOperationIds = new HashMap<String, Set<String>>();
    private static final TraceComponent tc = Tr.register(TestValidationHelper.class);

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
        return !operationIds.add(operationId);
    }

    /** {@inheritDoc} */
    @Override
    public void addLinkOperationId(String operationId, String location) {
        if (linkOperationIds.containsKey(operationId)) {
            linkOperationIds.get(operationId).add(location);
        } else {
            Set<String> locations = new HashSet<String>();
            locations.add(location);
            linkOperationIds.put(operationId, locations);
        }
        validateLinkOperationIds();
    }

    public void validateLinkOperationIds() {
        for (String k : linkOperationIds.keySet()) {
            if (!operationIds.contains(k)) {
                final String message = Tr.formatMessage(tc, "linkOperationIdInvalid", k);
                for (String location : linkOperationIds.get(k)) {
                    addValidationEvent(new ValidationEvent(Severity.ERROR, location, message));
                }
            }
        }
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
