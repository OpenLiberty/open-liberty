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

package com.ibm.ws.jpa.diagnostics.ormparser;

import com.ibm.ws.common.crypto.CryptoUtils;

public final class Constants {
    public final static String JVM_Property_ORMXML_DIGEST_ALGORITHM = "jpaormviewer.ormxml.digest.algorithm";
    public final static String DEFAULT_DIGEST_ALGORITHM = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256;

    public final static String JPA_10_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm10xml";
    public final static String JPA_20_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm20xml";
    public final static String JPA_21_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm21xml";
    public final static String JPA_22_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm22xml";
    public final static String JPA_30_JAXB_PACKAGE = "com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm30xml";
    public final static String JPA_DEFAULT_JAXB_PACKAGE = JPA_22_JAXB_PACKAGE;

}
