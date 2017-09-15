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
package com.ibm.ws.javaee.dd.ejb;

import java.util.List;

import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.SecurityRole;

/**
 * Represents &lt;assembly-descriptor>.
 */
public interface AssemblyDescriptor
{
    /**
     * @return &lt;security-role> as a read-only list
     */
    List<SecurityRole> getSecurityRoles();

    /**
     * @return &lt;method-permission> as a read-only list
     */
    List<MethodPermission> getMethodPermissions();

    /**
     * @return &lt;container-transaction> as a read-only list
     */
    List<ContainerTransaction> getContainerTransactions();

    /**
     * @return &lt;interceptor-binding> as a read-only list
     */
    List<InterceptorBinding> getInterceptorBinding();

    /**
     * @return &lt;message-destination> as a read-only list
     */
    List<MessageDestination> getMessageDestinations();

    /**
     * @return &lt;exclude-list>, or null if unspecified
     */
    ExcludeList getExcludeList();

    /**
     * @return &lt;application-exception> as a read-only list
     */
    List<ApplicationException> getApplicationExceptionList();
}
