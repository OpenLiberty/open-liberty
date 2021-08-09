/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

import java.util.List;

/**
 * Represents &lt;session>.
 */
public interface Session
                extends ComponentViewableBean,
                TimerServiceBean,
                TransactionalBean,
                SessionInterceptor
{
    /**
     * Represents an unspecified value for {@link #getSessionTypeValue}.
     */
    int SESSION_TYPE_UNSPECIFIED = -1;

    /**
     * Represents "Singleton" for {@link #getSessionTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.SessionType#SINGLETON
     */
    int SESSION_TYPE_SINGLETON = 2;

    /**
     * Represents "Stateful" for {@link #getSessionTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.SessionType#STATEFUL
     */
    int SESSION_TYPE_STATEFUL = 0;

    /**
     * Represents "Stateless" for {@link #getSessionTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.SessionType#STATELESS
     */
    int SESSION_TYPE_STATELESS = 1;

    /**
     * Represents an unspecified value for {@link #getConcurrencyManagementTypeValue}.
     */
    int CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED = -1;

    /**
     * Represents "Bean" for {@link #getConcurrencyManagementTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ConcurrencyManagementType#BEAN
     */
    int CONCURRENCY_MANAGEMENT_TYPE_BEAN = 1;

    /**
     * Represents "Container" for {@link #getConcurrencyManagementTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ConcurrencyManagementType#CONTAINER
     */
    int CONCURRENCY_MANAGEMENT_TYPE_CONTAINER = 0;

    /**
     * @return &lt;business-local> as a read-only list
     */
    List<String> getLocalBusinessInterfaceNames();

    /**
     * @return &lt;business-remote> as a read-only list
     */
    List<String> getRemoteBusinessInterfaceNames();

    /**
     * @return true if &lt;local-bean> is specified
     */
    boolean isLocalBean();

    /**
     * @return &lt;service-endpoint>, or null if unspecified
     */
    String getServiceEndpointInterfaceName();

    /**
     * @return &lt;session-type>
     *         <ul>
     *         <li>{@link #SESSION_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #SESSION_TYPE_SINGLETON} - Singleton
     *         <li>{@link #SESSION_TYPE_STATEFUL} - Stateful
     *         <li>{@link #SESSION_TYPE_STATELESS} - Stateless
     *         </ul>
     */
    int getSessionTypeValue();

    /**
     * @return &lt;stateful-timeout>, or null if unspecified or &lt;timeout> is
     *         not specified and the implementation does not require XSD validation
     */
    StatefulTimeout getStatefulTimeout();

    /**
     * @return true if &lt;init-on-startup> is specified
     * @see #isInitOnStartup
     */
    boolean isSetInitOnStartup();

    /**
     * @return &lt;init-on-startup> if specified
     * @see #isSetInitOnStartup
     */
    boolean isInitOnStartup();

    /**
     * @return &lt;concurrency-management-type>
     *         <ul>
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_BEAN} - Bean
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_CONTAINER} - Container
     *         </ul>
     */
    int getConcurrencyManagementTypeValue();

    /**
     * @return &lt;concurrent-method> as a read-only list
     */
    List<ConcurrentMethod> getConcurrentMethods();

    /**
     * @return &lt;depends-on>, or null if unspecified
     */
    DependsOn getDependsOn();

    /**
     * @return &lt;init-method> as a read-only list
     */
    List<InitMethod> getInitMethod();

    /**
     * @return &lt;remove-method> as a read-only list
     */
    List<RemoveMethod> getRemoveMethod();

    /**
     * @return &lt;async-method> as a read-only list
     */
    List<AsyncMethod> getAsyncMethods();

    /**
     * @return &lt;after-begin-method>, or null if unspecified
     */
    NamedMethod getAfterBeginMethod();

    /**
     * @return &lt;before-completion-method>, or null if unspecified
     */
    NamedMethod getBeforeCompletionMethod();

    /**
     * @return &lt;after-completion-method>, or null if unspecified
     */
    NamedMethod getAfterCompletionMethod();

    /**
     * @return true if &lt;passivation-capable> is specified
     * @see #isPassivationCapable
     */
    boolean isSetPassivationCapable();

    /**
     * @return &lt;passivation-capable> if specified
     * @see #isSetPassivationCapable
     */
    boolean isPassivationCapable();
}
