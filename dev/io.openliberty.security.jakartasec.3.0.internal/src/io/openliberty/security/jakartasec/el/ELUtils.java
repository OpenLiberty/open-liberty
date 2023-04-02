/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.el;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import jakarta.el.ELProcessor;

public class ELUtils {

    private static final TraceComponent tc = Tr.register(ELUtils.class);

    public static final String EVAL_EXPRESSION_PATTERN_GROUP_NAME = "expr";
    // Looks for either "${...}" or "#{...}"
    public static final Pattern EVAL_EXPRESSION_PATTERN = Pattern.compile("(\\$|#)\\{(?<" + EVAL_EXPRESSION_PATTERN_GROUP_NAME + ">[^}]*)\\}");

    private static final ELHelper elHelper = new ELHelper();
    private static final ELProcessor elProcessor = new ELProcessor();

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    public static String evaluateStringAttribute(String attributeName, String attribute, String attributeDefault, boolean immediateOnly) {
        try {
            return elHelper.processString(attributeName, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attribute)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attribute, attributeDefault);

            return attributeDefault;
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    public static Boolean evaluateBooleanAttribute(String attributeName, boolean attribute, boolean attributeDefault, String attributeExpression, boolean immediateOnly) {
        try {
            return elHelper.processBoolean(attributeName, attributeExpression, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attributeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeExpression == null ? attribute : attributeExpression, attributeDefault);

            return attributeDefault;
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    public static Integer evaluateIntegerAttribute(String attributeName, int attribute, int attributeDefault, String attributeExpression, boolean immediateOnly) {
        try {
            return elHelper.processInt(attributeName, attributeExpression, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attributeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeExpression == null ? attribute : attributeExpression, attributeDefault);

            return attributeDefault;
        }
    }

    public static String[] evaluateStringArrayAttribute(String attributeName, String attribute, String[] attributeDefault, boolean immediateOnly) {
        String preEvaluatedValue = ELUtils.evaluateStringAttribute(attributeName, attribute, null, immediateOnly);
        if (preEvaluatedValue != null) {
            attribute = preEvaluatedValue;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = EVAL_EXPRESSION_PATTERN.matcher(attribute);
        while (matcher.find()) {
            // Extract and evaluate each expression within the string and build a new resulting string with the result(s)
            String exprGroup = matcher.group(EVAL_EXPRESSION_PATTERN_GROUP_NAME);
            String processedExp = elProcessor.eval(exprGroup);
            matcher.appendReplacement(sb, processedExp);
        }
        matcher.appendTail(sb);
        return createStringArrayFromDelimitedString(sb, ",");
    }

    private static String[] createStringArrayFromDelimitedString(StringBuffer sb, String delimiter) {
        String[] processedString = sb.toString().split(delimiter);
        List<String> entries = new ArrayList<>();
        for (String segment : processedString) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries.toArray(new String[entries.size()]);
    }

    private static void issueWarningMessage(String attributeName, Object valueProvided, Object attributeDefault) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.warning(tc, "JAKARTASEC_WARNING_CLAIM_DEF_CONFIG", new Object[] { attributeName, valueProvided, attributeDefault });
        }
    }

}
