/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;

/**
 * Represents &lt;ejb-jar>.
 */
public interface EJBJar extends ModuleDeploymentDescriptor {
    /**
     * Represents "1.1" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_1_1_ID
     */
    int VERSION_1_1 = 11;

    /**
     * Represents "2.0" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_2_0_ID
     */
    int VERSION_2_0 = 20;

    /**
     * Represents "2.1" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_2_1_ID
     */
    int VERSION_2_1 = 21;

    /**
     * Represents "3.0" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_3_0_ID
     */
    int VERSION_3_0 = 30;

    /**
     * Represents "3.1" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_3_1_ID
     */
    int VERSION_3_1 = 31;

    /**
     * Represents "3.2" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#EJB_3_2_ID
     */
    int VERSION_3_2 = 32;

    /**
     * Represents "4.0" for {@link #getVersionID}.
     */
    int VERSION_4_0 = 40;

    /**
     * @return the version
     *         <ul>
     *         <li>{@link #VERSION_1_1} - 1.1
     *         <li>{@link #VERSION_2_0} - 2.0
     *         <li>{@link #VERSION_2_1} - 2.1
     *         <li>{@link #VERSION_3_0} - 3.0
     *         <li>{@link #VERSION_3_1} - 3.1
     *         <li>{@link #VERSION_3_2} - 3.2
     *         <li>{@link #VERSION_4_0} - 4.0
     *         </ul>
     */
    int getVersionID();

    /**
     * @return &lt;metadata-complete> if specified, false if unspecified, or
     *         false if {@link #getVersionID} is less than {@link #VERSION_3_0} (though
     *         these module versions are semantically metadata-complete per the EJB 3.0
     *         specification)
     */
    boolean isMetadataComplete();

    /**
     * @return &lt;session>, &lt;entity>, and &lt;message-driven> as a read-only
     *         list
     */
    List<EnterpriseBean> getEnterpriseBeans();

    /**
     * @return &lt;interceptors>, or null if unspecified
     */
    Interceptors getInterceptors();

    /**
     * @return &lt;relationships>, or null if unspecified
     */
    Relationships getRelationshipList();

    /**
     * @return &lt;assembly-descriptor>, or null if unspecified
     */
    AssemblyDescriptor getAssemblyDescriptor();

    /**
     * @return &lt;ejb-client-jar>, or null if unspecified
     */
    String getEjbClientJar();
}
