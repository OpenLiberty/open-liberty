/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver.internal.namespace;

import org.osgi.resource.Namespace;

/**
 * Namespace definition for core product kernel capabilities and requirements such as the product edition.
 */
public class ProductNamespace extends Namespace {

    /** Namespace name for core product kernel capabilities and requirements */
    public static final String PRODUCT_NAMESPACE = "com.ibm.ws.repo.product";
    /** The attribute name for the product version */
    public static final String CAPABILITY_VERSION_ATTRIBUTE = "version";
    /** The attribute name for the product ID */
    public static final String CAPABILITY_PRODUCT_ID_ATTRIBUTE = "productId";
    /** The attribute name for the install type */
    public static final String CAPABILITY_INSTALL_TYPE_ATTRIBUTE = "installType";
    /** The attribute name for the license type */
    public static final String CAPABILITY_LICENSE_TYPE_ATTRIBUTE = "licenseType";
    /** The attribute name for the edition */
    public static final String CAPABILITY_EDITION_ATTRIBUTE = "edition";

}
