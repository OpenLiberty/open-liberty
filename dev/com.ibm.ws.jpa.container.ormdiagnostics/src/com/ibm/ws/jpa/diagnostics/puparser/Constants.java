/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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

package com.ibm.ws.jpa.diagnostics.puparser;

import com.ibm.ws.common.crypto.CryptoUtils;

public final class Constants {
    public final static String JVM_Property_PXML_DIGEST_ALGORITHM = "jpaormviewer.pxml.digest.algorithm";
    public final static String DEFAULT_DIGEST_ALGORITHM = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256;

    public final static String JPA_10_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml10";
    public final static String JPA_20_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml20";
    public final static String JPA_21_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml21";
    public final static String JPA_22_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml22";
    public final static String JPA_30_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml30";
    public final static String JPA_32_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.puparser.jaxb.puxml32";

    public final static String SUN_NAMESPACE = "http://java.sun.com/xml/ns/persistence"; // JPA 1.0 and 2.0
    public final static String JCP_NAMESPACE = "http://xmlns.jcp.org/xml/ns/persistence"; // JPA 2.1 and 2.2
    public final static String JAKARTA_NAMESPACE = "https://jakarta.ee/xml/ns/persistence"; // JPA 3.0 and 3.2

    public final static String SCHEMA_LOCATION_10 = "http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd";
    public final static String SCHEMA_LOCATION_20 = "http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd";
    public final static String SCHEMA_LOCATION_21 = "http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd";
    public final static String SCHEMA_LOCATION_22 = "http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd";
    public final static String SCHEMA_LOCATION_30 = "https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd";
    public final static String SCHEMA_LOCATION_32 = "https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd";
}
