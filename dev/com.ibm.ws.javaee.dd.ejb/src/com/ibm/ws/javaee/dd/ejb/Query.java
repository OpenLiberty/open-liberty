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

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;query>.
 */
public interface Query
                extends Describable
{
    /**
     * Represents an unspecified value for {@link #getResultTypeMappingValue}.
     */
    int RESULT_TYPE_MAPPING_UNSPECIFIED = -1;

    /**
     * Represents "Local" for {@link #getResultTypeMappingValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ReturnTypeMapping#LOCAL
     */
    int RESULT_TYPE_MAPPING_LOCAL = 0;

    /**
     * Represents "Remote" for {@link #getResultTypeMappingValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ReturnTypeMapping#REMOTE
     */
    int RESULT_TYPE_MAPPING_REMOTE = 1;

    /**
     * @return &lt;query-method>
     */
    QueryMethod getQueryMethod();

    /**
     * @return &lt;result-type-mapping>
     *         <ul>
     *         <li>{@link #RESULT_TYPE_MAPPING_UNSPECIFIED} if unspecified
     *         <li>{@link #RESULT_TYPE_MAPPING_LOCAL} - Local
     *         <li>{@link #RESULT_TYPE_MAPPING_REMOTE} - Remote
     *         </ul>
     */
    int getResultTypeMappingValue();

    /**
     * @return &lt;ejb-ql>
     */
    String getEjbQL();
}
