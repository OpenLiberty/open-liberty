/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.annocache.targets;

import java.util.List;
import java.util.logging.Logger;

import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

public interface TargetsTableContainers {

    String getHashText();
    void log(Logger logger);

    //

    AnnotationTargetsImpl_Factory getFactory();

    //

    /**
     * <p>Tell if a name is contained by this table.</p>
     *
     * @param name The name to test.
     *
     * @return True if the name is contained by this table.  Otherwise, false.
     */
    boolean containsName(String name);

    /**
     * <p>Answer the names of this container table.</p>
     *
     * @return The names of this container table.
     */
    List<String> getNames();

    /**
     * <p>Answer the stamp associated with a name.  Answer
     * null if the name is not managed by this table, or if no
     * stamp is provided with the name.
     * 
     * @param name A name of interest.
     * 
     * @return The stamp associated with the name.
     */
    String getSignature(String name);
    
    /**
     * <p>Answer the scan policy associated with a name.  Answer
     * null if the name is not one managed by this container table.</p>
     *
     * @param name The name of interest.
     *
     * @return The scan policy of the name.
     */
    ScanPolicy getPolicy(String name);

    //
    
    /**
     * <p>Add a name and a scan policy to this container table.</p>
     *
     * <p>Names are kept in the order in which they were added.</p>
     *
     * @param name The name to add.
     * @param signature The signature associated with the name.
     * @param policy The scan policy associated with the name.
     */
    void addName(String name, String signature, ScanPolicy policy);    

    /**
     * <p>Add a name after a specified name.</p>
     *
     * <p>An {@link IndexOutOfBoundsException} exception is thrown if the
     * after name is not a name of the container table.</p>
     *
     * @param name The name to add.
     * @param signature The signature associated with the name.
     * @param policy The scan policy associated with the name.
     * @param afterName The name after which to add the new name.
     */
    void addNameAfter(String name, String signature, ScanPolicy policy, String afterName);    

    /**
     * <p>Add a name before a specified name.</p>
     *
     * <p>An {@link IndexOutOfBoundsException} exception is thrown if the
     * before name is not a name of the container table.</p>
     *
     * @param name The name to add.
     * @param signature The signature associated with the name.
     * @param policy The scan policy associated with the name.
     * @param beforeName The name before which to add the new name.
     */
    void addNameBefore(String name, String signature, ScanPolicy policy, String beforeName);    

    /**
     * <p>Remove a name from this container table.</p>
     *
     * @param name The name to remove.
     *
     * @return The scan policy of the name.  Null if the name was
     *         not a member of the container table.
     */
    ScanPolicy removeName(String name);
}
