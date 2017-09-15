/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.jsp;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;

/**
 * Represents the jsp-property-groupType type from the jsp XSD.
 */
public interface JSPPropertyGroup
                extends DescriptionGroup {

    /**
     * @return &lt;url-pattern> as a read-only list
     */
    List<String> getURLPatterns();

    /**
     * @return true if &lt;el-ignored> is specified
     * @see #isElIgnored
     */
    boolean isSetElIgnored();

    /**
     * @return &lt;el-ignored> if specified
     * @see #isSetElIgnored
     */
    boolean isElIgnored();

    /**
     * @return &lt;page-encoding>, or null if unspecified
     */
    String getPageEncoding();

    /**
     * @return true if &lt;scripting-invalid> is specified
     * @see #isScriptingInvalid
     */
    boolean isSetScriptingInvalid();

    /**
     * @return &lt;scripting-invalid> if specified
     * @see #isSetScriptingInvalid
     */
    boolean isScriptingInvalid();

    /**
     * @return true if &lt;is-xml> is specified
     * @see #isIsXml
     */
    boolean isSetIsXml();

    /**
     * @return &lt;is-xml> if specified
     * @see #isSetIsXml
     */
    boolean isIsXml();

    /**
     * @return &lt;include-prelude> as a read-only list
     */
    List<String> getIncludePreludes();

    /**
     * @return &lt;include-coda> as a read-only list
     */
    List<String> getIncludeCodas();

    /**
     * @return true if &lt;deferred-syntax-allowed-as-literal> is specified
     * @see #isDeferredSyntaxAllowedAsLiteral
     */
    boolean isSetDeferredSyntaxAllowedAsLiteral();

    /**
     * @return &lt;deferred-syntax-allowed-as-literal> if specified
     * @see #isSetDeferredSyntaxAllowedAsLiteral
     */
    boolean isDeferredSyntaxAllowedAsLiteral();

    /**
     * @return true if &lt;trim-directive-whitespaces> is specified
     * @see #isTrimDirectiveWhitespaces
     */
    boolean isSetTrimDirectiveWhitespaces();

    /**
     * @return &lt;trim-directive-whitespaces> if specified
     * @see #isSetTrimDirectiveWhitespaces
     */
    boolean isTrimDirectiveWhitespaces();

    /**
     * @return &lt;default-content-type>, or null if unspecified
     */
    String getDefaultContentType();

    /**
     * @return &lt;buffer>, or null if unspecified
     */
    String getBuffer();

    /**
     * @return true if &lt;error-on-undeclared-namespace> is specified
     * @see #isErrorOnUndeclaredNamespace
     */
    boolean isSetErrorOnUndeclaredNamespace();

    /**
     * @return &lt;error-on-undeclared-namespace> if specified
     * @see #isSetErrorOnUndeclaredNamespace
     */
    boolean isErrorOnUndeclaredNamespace();

}
