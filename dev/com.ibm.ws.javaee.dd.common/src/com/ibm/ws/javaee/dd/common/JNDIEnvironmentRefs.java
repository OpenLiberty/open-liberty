/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

import java.util.List;

import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;

/**
 * Represents the subset of the jndiEnvironmentRefsGroup group from the javaee
 * XSD that includes reference declarations only.
 * 
 * @see JNDIEnvironmentRefsGroup
 */
public interface JNDIEnvironmentRefs
{
    /**
     * @return &lt;env-entry> as a read-only list
     */
    List<EnvEntry> getEnvEntries();

    /**
     * @return &lt;ejb-ref> as a read-only list; {@link EJBRef#getKindValue} for all items in the list will return either {@link EJBRef#KIND_REMOTE} or {@link EJBRef#KIND_UNKNOWN}
     */
    List<EJBRef> getEJBRefs();

    /**
     * @return &lt;ejb-local-ref> as a read-only list, or null if the object
     *         does not support this element; {@link EJBRef#getKindValue} for all items
     *         in the list will return {@link EJBRef#KIND_LOCAL}.
     */
    List<EJBRef> getEJBLocalRefs();

    /**
     * @return &lt;service-ref> as a read-only list
     */
    List<ServiceRef> getServiceRefs();

    /**
     * @return &lt;resource-ref> as a read-only list
     */
    List<ResourceRef> getResourceRefs();

    /**
     * @return &lt;resource-env-ref> as a read-only list
     */
    List<ResourceEnvRef> getResourceEnvRefs();

    /**
     * @return &lt;message-destination-ref> as a read-only list
     */
    List<MessageDestinationRef> getMessageDestinationRefs();

    /**
     * @return &lt;persistence-context-ref> as a read-only list, or null if the
     *         object does not support this element
     */
    List<PersistenceContextRef> getPersistenceContextRefs();

    /**
     * @return &lt;persistence-unit-ref> as a read-only list
     */
    List<PersistenceUnitRef> getPersistenceUnitRefs();

    /**
     * @return &lt;data-source> as a read-only list
     */
    List<DataSource> getDataSources();

    /**
     * @return &lt;jms-connection-factory> as a read-only list
     */
    List<JMSConnectionFactory> getJMSConnectionFactories();

    /**
     * @return &lt;jms-destination> as a read-only list
     */
    List<JMSDestination> getJMSDestinations();

    /**
     * @return &lt;mail-session> as a read-only list
     */
    List<MailSession> getMailSessions();

    /**
     * @return &lt;connection-factory> as a read-only list
     */
    List<ConnectionFactory> getConnectionFactories();

    /**
     * @return &lt;administered-object> as a read-only list
     */
    List<AdministeredObject> getAdministeredObjects();
}
