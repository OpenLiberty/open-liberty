/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.wsspi.persistence;

import java.io.Writer;

/**
 * This interface is implemented by components using the persistence
 * service which want to participate in automatic DDL generation.
 */
public interface DDLGenerationParticipant {

    /**
     * Called when the user requests DDL generation. Any DDL needed by
     * this service should be written to the provided Writer class.
     *
     * @param out The writer which should be passed to the persistence
     *            service for DDL generation.
     * @throws Exception if an error occurs.
     */
    public void generate(Writer out) throws Exception;

    /**
     * <p>Returns the name of the file (not including the file extension) into which DDL should be generated.
     * If multiple DDLGenerationParticipants specify the same file name, all will be used in an undefined
     * order and allowed to append to the same file. If null is returned, the DDLGenerationParticipant will
     * not be used to generate DDL.
     *
     * <p>The following conventions are recommended for file names.
     * <ul>
     * <li>If the databaseStore is a nested element, it is recommended to have the file name be the
     * config.displayId of the databaseStore. For example,
     * persistentExecutor[MyExecutor]/databaseStore[default-0]
     * <li>If the databaseStore is a top level element, it is recommended to have the file name be the
     * config.displayId of the databaseStore with an underscore character and the configuration element name
     * of the DDLGenerationParticipant appended to the end. For example,
     * databaseStore[MyDBStore]_persistentExecutor
     * </ul>
     *
     * @return the name of the file to use, or null if there is no DDL to generate.
     */
    public String getDDLFileName();
}
