/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.work;

import java.util.Hashtable;
import java.util.logging.Logger;

import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointWrapper;

/**
 * <p>This abstract class extends FVTWorkImpl class. This class serves as a super class
 * of work implementation classes used in TRA. </p>
 */
public abstract class FVTGeneralWorkImpl extends FVTWorkImpl {
    private final static String CLASSNAME = FVTGeneralWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

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
        svLogger.entering(CLASSNAME, "getEndpointWrapper", new Object[] { this, key });

        if (firstInstanceKey != null && key.equals(firstInstanceKey)) {
            svLogger.exiting(CLASSNAME, "getEndpointWrapper", new Object[] { "return the first key stored in instance", instance });
            return instance;
        } else {
            if (instances == null) {
                svLogger.exiting(CLASSNAME, "getEndpointWrapper", "null");
                return null;
            }
            MessageEndpointWrapper endpointWrapper = (MessageEndpointWrapper) instances.get(key);

            svLogger.exiting(CLASSNAME, "getEndpointWrapper", endpointWrapper);
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
        svLogger.entering(CLASSNAME, "addEndpointWrapper", new Object[] { this, key, endpointWrapper });

        if (firstInstanceKey == null) {
            firstInstanceKey = key;
            instance = endpointWrapper;

            svLogger.exiting(CLASSNAME, "addEndpointWrapper", "set instance to the endpoint");
        } else {
            if (instances == null)
                instances = new Hashtable(3);

            instances.put(key, endpointWrapper);

            svLogger.exiting(CLASSNAME, "addEndpointWrapper", "Add the endpoint to the Hashtable");
        }
    }
}