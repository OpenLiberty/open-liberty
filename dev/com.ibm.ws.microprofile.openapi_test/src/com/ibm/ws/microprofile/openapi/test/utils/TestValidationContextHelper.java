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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class TestValidationContextHelper implements Context {

    private final OpenAPIImpl openAPI;
    private final Deque<String> pathSegments = new ArrayDeque<>();

    public TestValidationContextHelper(OpenAPIImpl openAPI) {
        this.openAPI = openAPI;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.openapi.internal.utils.OpenAPIModelWalker.Context#getModel()
     */
    @Override
    public OpenAPI getModel() {
        return openAPI;
    }

    @Override
    public String getLocation() {
        return getLocation(null);
    }

    @Override
    public String getLocation(String suffix) {
        final Iterator<String> i = pathSegments.descendingIterator();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (i.hasNext()) {
            if (!first) {
                sb.append('/');
            }
            sb.append(i.next());
            first = false;
        }
        if (suffix != null && !suffix.isEmpty()) {
            sb.append('/');
            sb.append(suffix);
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.openapi.internal.utils.OpenAPIModelWalker.Context#getParent()
     */
    @Override
    public Object getParent() {
        // TODO Auto-generated method stub
        return null;
    }
}
