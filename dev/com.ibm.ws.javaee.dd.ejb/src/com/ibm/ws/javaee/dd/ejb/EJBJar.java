/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;

public interface EJBJar extends ModuleDeploymentDescriptor, DeploymentDescriptor {
    String DD_SHORT_NAME = "ejb-jar.xml";
    String DD_NAME_EJB = "META-INF/ejb-jar.xml";
    String DD_NAME_WEB = "WEB-INF/ejb-jar.xml";
    String[] DD_NAMES = { DD_NAME_EJB, DD_NAME_WEB };

    int VERSION_1_1 = 11;
    int VERSION_2_0 = 20;
    int VERSION_2_1 = 21;
    int VERSION_3_0 = 30;
    int VERSION_3_1 = 31;
    int VERSION_3_2 = 32;
    int VERSION_4_0 = 40; // Jakarta EE 9
    
    // Not an actual schema version.  This is used to enable
    // parsing of the Jakarta EE 10 elements in 4.0 descriptor.

    int VERSION_5_0 = 50; // Jakarta EE 10    

    int MAX_VERSION = 40;
    
    int[] VERSIONS = {
        VERSION_1_1, VERSION_2_0,
        VERSION_2_1,
        VERSION_3_0, VERSION_3_1, VERSION_3_2,
        VERSION_4_0
    };

    int[] DTD_VERSION = {
        VERSION_1_1, VERSION_2_0
    };
    
    int[] SCHEMA_VERSIONS = {
        VERSION_2_1,
        VERSION_3_0, VERSION_3_1, VERSION_3_2,
        VERSION_4_0
    };
    
    int[] ANNOTATION_ENABLED_VERSIONS = {
        VERSION_3_0, VERSION_3_1, VERSION_3_2,
        VERSION_4_0
    };
    
    int getVersionID();

    /**
     * @return &lt;metadata-complete&gt; if specified, false if unspecified, or
     *         false if {@link #getVersionID} is less than {@link #VERSION_3_0} (though
     *         these module versions are semantically metadata-complete per the EJB 3.0
     *         specification)
     */
    boolean isMetadataComplete();

    /**
     * @return &lt;session&gt;, &lt;entity&gt;, and &lt;message-driven&gt; as a read-only
     *         list
     */
    List<EnterpriseBean> getEnterpriseBeans();

    /**
     * @return &lt;interceptors&gt;, or null if unspecified
     */
    Interceptors getInterceptors();

    /**
     * @return &lt;relationships&gt;, or null if unspecified
     */
    Relationships getRelationshipList();

    /**
     * @return &lt;assembly-descriptor&gt;, or null if unspecified
     */
    AssemblyDescriptor getAssemblyDescriptor();

    /**
     * @return &lt;ejb-client-jar&gt;, or null if unspecified
     */
    String getEjbClientJar();
}
