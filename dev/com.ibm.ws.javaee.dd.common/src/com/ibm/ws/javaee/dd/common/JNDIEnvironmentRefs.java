/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
public interface JNDIEnvironmentRefs {
    /**
     * @return &lt;env-entry&gt; as a read-only list
     */
    List<EnvEntry> getEnvEntries();

    /**
     * @return &lt;ejb-ref&gt; as a read-only list; {@link EJBRef#getKindValue} for all items in the list will return either {@link EJBRef#KIND_REMOTE} or {@link EJBRef#KIND_UNKNOWN}
     */
    List<EJBRef> getEJBRefs();

    /**
     * @return &lt;ejb-local-ref&gt; as a read-only list, or null if the object
     *         does not support this element; {@link EJBRef#getKindValue} for all items
     *         in the list will return {@link EJBRef#KIND_LOCAL}.
     */
    List<EJBRef> getEJBLocalRefs();

    /**
     * @return &lt;service-ref&gt; as a read-only list
     */
    List<ServiceRef> getServiceRefs();

    /**
     * @return &lt;resource-ref&gt; as a read-only list
     */
    List<ResourceRef> getResourceRefs();

    /**
     * @return &lt;resource-env-ref&gt; as a read-only list
     */
    List<ResourceEnvRef> getResourceEnvRefs();

    /**
     * @return &lt;message-destination-ref&gt; as a read-only list
     */
    List<MessageDestinationRef> getMessageDestinationRefs();

    /**
     * @return &lt;persistence-context-ref&gt; as a read-only list, or null if the
     *         object does not support this element
     */
    List<PersistenceContextRef> getPersistenceContextRefs(); // Jakarta EE 10

    /**
     * @return &lt;persistence-unit-ref&gt; as a read-only list
     */
    List<PersistenceUnitRef> getPersistenceUnitRefs();

    /**
     * @return &lt;context-service&gt; as a read-only list
     */
    List<ContextService> getContextServices();

    /**
     * @return &lt;data-source&gt; as a read-only list
     */
    List<DataSource> getDataSources();

    /**
     * @return &lt;jms-connection-factory&gt; as a read-only list
     */
    List<JMSConnectionFactory> getJMSConnectionFactories();

    /**
     * @return &lt;jms-destination&gt; as a read-only list
     */
    List<JMSDestination> getJMSDestinations();

    /**
     * @return &lt;mail-session&gt; as a read-only list
     */
    List<MailSession> getMailSessions();

    /**
     * @return &lt;managed-executor&gt; as a read-only list
     */
    List<ManagedExecutor> getManagedExecutors(); // Jakarta EE 10

    /**
     * @return &lt;managed-scheduled-executor&gt; as a read-only list
     */
    List<ManagedScheduledExecutor> getManagedScheduledExecutors(); // Jakarta EE 10

    /**
     * @return &lt;managed-thread-factory&gt; as a read-only list
     */
    List<ManagedThreadFactory> getManagedThreadFactories(); // Jakarta EE 10

    /**
     * @return &lt;connection-factory&gt; as a read-only list
     */
    List<ConnectionFactory> getConnectionFactories();

    /**
     * @return &lt;administered-object&gt; as a read-only list
     */
    List<AdministeredObject> getAdministeredObjects();
}
