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

/**
 * Represents &lt;entity>.
 */
public interface Entity
                extends ComponentViewableBean
{
    /**
     * Represents "Bean" for {@link #getPersistenceTypeValue}.
     */
    int PERSISTENCE_TYPE_BEAN = 0;

    /**
     * Represents "Container" for {@link #getPersistenceTypeValue}.
     */
    int PERSISTENCE_TYPE_CONTAINER = 1;

    /**
     * Represents an unspecified value for {@link #getCMPVersionValue}.
     */
    int CMP_VERSION_UNSPECIFIED = -1;

    /**
     * Represents "1.x" for {@link #getCMPVersionValue}.
     */
    int CMP_VERSION_1_X = 0;

    /**
     * Represents "2.x" for {@link #getCMPVersionValue}.
     */
    int CMP_VERSION_2_X = 1;

    /**
     * @return &lt;ejb-class>
     */
    @Override
    String getEjbClassName();

    /**
     * @return &lt;persistence-type>
     *         <ul>
     *         <li>{@link #PERSISTENCE_TYPE_BEAN} - Bean
     *         <li>{@link #PERSISTENCE_TYPE_CONTAINER} - Container
     *         </ul>
     */
    int getPersistenceTypeValue();

    /**
     * @return &lt;prim-key-class>
     */
    String getPrimaryKeyName();

    /**
     * @return &lt;reentrant>
     */
    boolean isReentrant();

    /**
     * @return &lt;cmp-version>
     *         <ul>
     *         <li>{@link #CMP_VERSION_UNSPECIFIED} if unspecified
     *         <li>{@link #CMP_VERSION_1_X} - 1.x
     *         <li>{@link #CMP_VERSION_2_X} - 2.x
     *         </ul>
     */
    int getCMPVersionValue();

    /**
     * @return &lt;abstract-schema-name>, or null if unspecified
     */
    String getAbstractSchemaName();

    /**
     * @return &lt;cmp-field> as a read-only list
     */
    List<CMPField> getCMPFields();

    /**
     * @return &lt;primkey-field>, or null if unspecified
     */
    CMPField getPrimKeyField();

    /**
     * @return &lt;query> as a read-only list
     */
    List<Query> getQueries();
}
