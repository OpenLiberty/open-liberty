/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

public class ServletMessageConstants {

    /** field names */
    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
    public static final String CLAIMS_SUBJECT = "Claims Subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";
//    public static final String CONTEXT_SUBJECT = "OpenIdContext subject: ";

    /** messages **/
    public static final String SUBS_MISMATCH_NULL = "OpenIdContext subjects do NOT match since there are no claims";
    public static final String SUBS_MISMATCH_BOTH_NULL = "OpenIdContext subjects are null";
    public static final String SUBS_MATCH = "OpenIdContext subjects match";
    public static final String SUBS_CLAIMS_SUB_NULL = "OpenIdContext subjects do NOT match: claimsSub is null and does not match the contextSub:";
    public static final String SUBS_MISMATCH_PART1 = "OpenIdContext subjects do NOT match: claimsSub: ";
    public static final String SUBS_MISMATCH_PART2 = " does not match contextSub: ";

}