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

package com.ibm.ws.sib.api.jmsra.impl;

import com.ibm.ws.sib.api.jmsra.JmsJcaFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedQueueConnectionFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaManagedTopicConnectionFactory;

/**
 * Implementation of the abstract JmsJcaFactory class used for creating
 * instances of the managed connection factories. Used by the JMS API layer in a
 * non-JCA environment. If the managed connection factories are made part of the
 * public API then this factory will no longer be required.
 *  
 */
public final class JmsJcaFactoryImpl extends JmsJcaFactory {

    /*
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaFactory#createManagedConnectionFactory()
     */
    public JmsJcaManagedConnectionFactory createManagedConnectionFactory() {
        return new JmsJcaManagedConnectionFactoryImpl();
    }

    /*
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaFactory#createManagedQueueConnectionFactory()
     */
    public JmsJcaManagedQueueConnectionFactory createManagedQueueConnectionFactory() {
        return new JmsJcaManagedQueueConnectionFactoryImpl();
    }

    /*
     * @see com.ibm.ws.sib.api.jmsra.JmsJcaFactory#createManagedTopicConnectionFactory()
     */
    public JmsJcaManagedTopicConnectionFactory createManagedTopicConnectionFactory() {
        return new JmsJcaManagedTopicConnectionFactoryImpl();
    }

}
