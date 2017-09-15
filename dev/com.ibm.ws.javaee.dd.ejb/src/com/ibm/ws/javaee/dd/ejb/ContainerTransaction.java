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

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;container-transaction>.
 */
public interface ContainerTransaction
                extends Describable
{
    /**
     * Represents "NotSupported" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#NOT_SUPPORTED
     */
    int TRANS_ATTRIBUTE_NOT_SUPPORTED = 0;

    /**
     * Represents "Supports" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#SUPPORTS
     */
    int TRANS_ATTRIBUTE_SUPPORTS = 1;

    /**
     * Represents "Required" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#REQUIRED
     */
    int TRANS_ATTRIBUTE_REQUIRED = 2;

    /**
     * Represents "RequiresNew" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#REQUIRES_NEW
     */
    int TRANS_ATTRIBUTE_REQUIRES_NEW = 3;

    /**
     * Represents "Mandatory" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#MANDATORY
     */
    int TRANS_ATTRIBUTE_MANDATORY = 4;

    /**
     * Represents "Never" for {@link #getTransAttributeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.TransactionAttributeType#NEVER
     */
    int TRANS_ATTRIBUTE_NEVER = 5;

    /**
     * @return &lt;method> as a read-only list
     */
    List<Method> getMethodElements();

    /**
     * @return &lt;trans-attribute>
     *         <ul>
     *         <li>{@link #TRANS_ATTRIBUTE_NOT_SUPPORTED} - NotSupported
     *         <li>{@link #TRANS_ATTRIBUTE_SUPPORTS} - Supports
     *         <li>{@link #TRANS_ATTRIBUTE_REQUIRED} - Required
     *         <li>{@link #TRANS_ATTRIBUTE_REQUIRES_NEW} - RequiresNew
     *         <li>{@link #TRANS_ATTRIBUTE_MANDATORY} - Mandatory
     *         <li>{@link #TRANS_ATTRIBUTE_NEVER} - Never
     *         </ul>
     */
    int getTransAttributeTypeValue();
}
