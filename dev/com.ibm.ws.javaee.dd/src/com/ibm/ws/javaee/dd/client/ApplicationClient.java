/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.client;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;

/**
 * The application-client element is the root element of an
 * application client deployment descriptor. The application
 * client deployment descriptor describes the EJB components
 * and external resources referenced by the application
 * client.
 */
public interface ApplicationClient extends ModuleDeploymentDescriptor, DeploymentDescriptor, JNDIEnvironmentRefsGroup {

    static final String DD_NAME = "META-INF/application-client.xml";

    /**
     * Represents "1.2" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#J2EE_1_2_ID
     */
    int VERSION_1_2 = 12;

    /**
     * Represents "1.3" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#J2EE_1_3_ID
     */
    int VERSION_1_3 = 13;

    /**
     * Represents "1.4" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#J2EE_1_4_ID
     */
    int VERSION_1_4 = 14;

    /**
     * Represents "5" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#JEE_5_0_ID
     */
    int VERSION_5 = 50;

    /**
     * Represents "6" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#JEE_6_0_ID
     */
    int VERSION_6 = 60;

    /**
     * Represents "7" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#JEE_7_0_ID
     */
    int VERSION_7 = 70;
    
    /**
     * Represents "8" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#JEE_8_0_ID
     */
    int VERSION_8 = 80;

    /**
     * Represents "9" for {@link #getVersionID}.
     *
     * @see org.eclipse.jst.j2ee.internal.J2EEVersionConstants#JEE_9_0_ID
     */
    int VERSION_9 = 90;

    /**
     * @return the version
     *         <ul>
     *         <li>{@link #VERSION_1_2} - 1.2
     *         <li>{@link #VERSION_1_3} - 1.3
     *         <li>{@link #VERSION_1_4} - 1.4
     *         <li>{@link #VERSION_5} - 5
     *         <li>{@link #VERSION_6} - 6
     *         <li>{@link #VERSION_7} - 7
     *         <li>{@link #VERSION_8} - 8
     *         <li>{@link #VERSION_9} - 9
     *         </ul>
     */
    int getVersionID();

    /**
     * @return null; &lt;ejb-local-ref> is not supported
     */
    @Override
    List<EJBRef> getEJBLocalRefs();

    /**
     * @return null; &lt;persistence-context-ref> is not supported
     */
    @Override
    List<PersistenceContextRef> getPersistenceContextRefs();

    /**
     * @return &lt;callback-handler>, or null if unspecified
     */
    String getCallbackHandler();

    /**
     * @return &lt;message-destination> as a read-only list
     */
    List<MessageDestination> getMessageDestinations();

    /**
     * @return version="..." attribute value
     */
    String getVersion();

    /**
     * @return true if metadata-complete="..." attribute is specified
     */
    boolean isSetMetadataComplete();

    /**
     * @return metadata-complete="..." attribute value if specified
     */
    boolean isMetadataComplete();
}
