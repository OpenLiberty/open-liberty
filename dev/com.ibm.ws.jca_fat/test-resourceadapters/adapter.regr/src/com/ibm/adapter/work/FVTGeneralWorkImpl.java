/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.work;

import java.util.Hashtable;

import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.adapter.tra.FVTWorkImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>This abstract class extends FVTWorkImpl class. This class serves as a super class
 * of work implementation classes used in TRA. </p>
 */
public abstract class FVTGeneralWorkImpl extends FVTWorkImpl {

    private static final TraceComponent tc = Tr.register(FVTGeneralWorkImpl.class);

    /** The first message endpoint instance */
    MessageEndpointWrapper instance;

    /** The first message endpoint key */
    String firstInstanceKey;

    /** Message endpoint instances hashtable for storing the all the instances except the first one */
    Hashtable instances;

    /**
     * Constructor
     *
     * @param workName the name of the work
     */
    public FVTGeneralWorkImpl(String workName) {
        super(workName);
    }

    /**
     * Returns the firstInstanceKey.
     *
     * @return String
     */
    public String getFirstInstanceKey() {
        return firstInstanceKey;
    }

    /**
     * Returns the instance.
     *
     * @return MessageEndpointWrapper
     */
    public MessageEndpointWrapper getInstance() {
        return instance;
    }

    /**
     * Returns the instances.
     *
     * @return Hashtable
     */
    public Hashtable getInstances() {
        return instances;
    }

    /**
     * <p>Get the instance accroding to the key. return null if the key is not found.</p>
     *
     * @param key of the endpoint instance.
     *
     * @return a MessageEndpointWrapper instance
     */
    public MessageEndpointWrapper getEndpointWrapper(String key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getEndpointWrapper", new Object[] { this, key });

        if (firstInstanceKey != null && key.equals(firstInstanceKey)) {
            if (tc.isEntryEnabled())
                Tr.exit(
                        tc,
                        "getEndpointWrapper",
                        new Object[] { "return the first key stored in instance", instance });

            return instance;
        } else {
            if (instances == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getEndpointWrapper", "null");

                return null;
            }
            MessageEndpointWrapper endpointWrapper = (MessageEndpointWrapper) instances.get(key);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEndpointWrapper", endpointWrapper);

            return endpointWrapper;
        }

    }

    /**
     * <p>Add the instance to the instance variable or the instances Hashtable.</p>
     *
     * @param key of the endpoint instance.
     * @param endpointWrapper a MessageEndpointWrapper instance
     */
    protected void addEndpointWrapper(String key, MessageEndpointWrapper endpointWrapper) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addEndpointWrapper", new Object[] { this, key, endpointWrapper });

        if (firstInstanceKey == null) {
            firstInstanceKey = key;
            instance = endpointWrapper;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "addEndpointWrapper", "set instance to the endpoint");
        } else {
            if (instances == null)
                instances = new Hashtable(3);

            instances.put(key, endpointWrapper);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "addEndpointWrapper", "Add the endpoint to the Hashtable");
        }

    }

}
