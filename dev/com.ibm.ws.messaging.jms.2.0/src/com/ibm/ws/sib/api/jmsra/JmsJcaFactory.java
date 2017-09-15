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
package com.ibm.ws.sib.api.jmsra;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Factory class for creating instances of the managed connection factories.
 * Used by the JMS API layer in a non-JCA environment. If the managed connection
 * factories are made part of the public API then this factory will no longer be
 * required.
 */
abstract public class JmsJcaFactory {

    /**
     * Implementation class name.
     */
    private static final String FACTORY_CLASS = "com.ibm.ws.sib.api.jmsra.impl.JmsJcaFactoryImpl";

    /**
     * The singleton instance of this class.
     */
    private static JmsJcaFactory _instance;

    private static final TraceComponent TRACE = SibTr.register(
            JmsJcaFactory.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);

    private final static String FFDC_PROBE_1 = "1";

    static {

       
        try {

            final Class clazz = Class.forName(FACTORY_CLASS);
            _instance = (JmsJcaFactory) clazz.newInstance();

        } catch (final Exception exception) {

            // Disaster - the implementation JAR file is missing
            FFDCFilter.processException(exception,
                    "com.ibm.ws.sib.api.jmsra.JmsJcaFactory.<clinit>",
                    FFDC_PROBE_1);
            if (TRACE.isEventEnabled()) {
                SibTr.error(TRACE, "EXCEPTION_DURING_INIT_CWSJR1441", exception);
            }

        }

    }

    /**
     * Get the singleton JmsJcaFactory which is to be used for creating managed
     * connection factory instances.
     * 
     * @return the factory
     */
    public static JmsJcaFactory getInstance() {

        return _instance;

    }

    /**
     * Create a new managed connection factory.
     * 
     * @return the managed connection factory
     */
    public abstract JmsJcaManagedConnectionFactory createManagedConnectionFactory();

    /**
     * Create a new managed queue connection factory.
     * 
     * @return the managed queue connection factory
     */
    public abstract JmsJcaManagedQueueConnectionFactory createManagedQueueConnectionFactory();

    /**
     * Create a new managed topic connection factory.
     * 
     * @return the managed topic connection factory
     */
    public abstract JmsJcaManagedTopicConnectionFactory createManagedTopicConnectionFactory();

}
