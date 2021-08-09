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
package com.ibm.ws.javaee.ddmetadata.annotation;

import java.lang.annotation.Target;

/**
 * A document version for {@link DDRootElement}.
 */
@Target(value = {})
public @interface DDVersion {
    /**
     * The document version. The values are document specific but typically
     * follow a {@code major * 10 + minor} scheme. For example, if a document has
     * a version string of {@code 3.2}, the value would be {@code 32}.
     */
    int version();

    /**
     * The version for EE deployment descriptors. This is optional for IBM
     * binding and extension files. The values follow a {@code major * 10 + minor} scheme.
     * For example, EE 1.4 is {@code 14} and EE 7 is {@code 70}.
     */
    int eeVersion() default 0;

    /**
     * Indicates that the version is identified by the DTD public ID rather
     * than a {@code version} attribute on the root element. This cannot be used
     * with {@link #versionString}.
     */
    String dtdPublicId() default "";

    /**
     * Indicates that the version is identified by a {@code version} attribute
     * on the root element. The {@link #namespace} must also be specified. This
     * cannot be used with {@link #dtdPublicId}.
     */
    String versionString() default "";

    /**
     * The XML namespace for the document. This can only be used with {@link #versionString}.
     */
    String namespace() default "";
}
