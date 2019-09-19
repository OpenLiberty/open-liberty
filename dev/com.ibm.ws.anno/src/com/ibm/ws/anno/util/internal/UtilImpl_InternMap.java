/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.util.internal;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.util.Util_InternMap;

/**
 * String intern map implementation.
 */
public class UtilImpl_InternMap implements Util_InternMap {
    private static final TraceComponent tc = Tr.register(UtilImpl_InternMap.class);
    public static final String CLASS_NAME = UtilImpl_InternMap.class.getName();

    protected final String hashText;

    @Trivial
    @Override
    public String getHashText() {
        return hashText;
    }

    public static final int DEFAULT_LOG_THRESHHOLD = 1024 * 4;

    protected final int logThreshHold;

    @Trivial
    @Override
    public int getLogThreshHold() {
        return logThreshHold;
    }

    //

    // TODO: How does this factor across the factory/service implementation?
    /*
     * Validate a string against a value type. Answer null if the string is
     * valid. Answer a message ID describing the validation failure if the
     * string is not valid for the value type.
     * 
     * @return Null for a valid value; a non-null message ID for a non-valid value.
     */
    @Trivial
    public static String doValidate(String value, ValueType valueType) {
        String vMsg = null;

        switch (valueType) {
            case VT_CLASS_RESOURCE:
                if (value.contains("\\")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if (!value.endsWith(".class")) {
                    vMsg = "ANNO_UTIL_EXPECTED_CLASS";
                }
                break;

            case VT_CLASS_REFERENCE:
                if (value.contains("\\")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if (value.endsWith(".class")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_CLASS_NAME:
                if (value.contains("\\")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if (value.contains("/")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if (value.endsWith(".class")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_FIELD_NAME:
                if (value.contains("\\")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if (value.contains("/")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if (value.endsWith(".class")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_METHOD_NAME:
                if (value.contains("\\")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_BACKSLASH";
                } else if (value.contains("/")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_FORWARD_SLASH";
                } else if (value.endsWith(".class")) {
                    vMsg = "ANNO_UTIL_UNEXPECTED_CLASS";
                }
                break;

            case VT_OTHER:
                break;

            default:
                vMsg = "ANNO_UTIL_UNRECOGNIZED_TYPE";
                break;
        }

        return vMsg;
    }

    @Override
    @Trivial
    public String validate(String value, ValueType valueType) {
        return UtilImpl_InternMap.doValidate(value, valueType);
    }

    //

    @Trivial
    public UtilImpl_InternMap(UtilImpl_Factory factory, ValueType valueType, String name) {
        this(factory, valueType, DEFAULT_LOG_THRESHHOLD, name);
    }

    @Trivial
    public UtilImpl_InternMap(UtilImpl_Factory factory, ValueType valueType, int logThreshHold, String name) {
        super();

        this.factory = factory;

        this.logThreshHold = logThreshHold;

        this.name = name;

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this) + "(" + this.name + ")";

        this.valueType = valueType;
        this.checkValues = tc.isDebugEnabled();

        this.internMap = new HashMap<String, String>();
        this.lastReportedLength = 0;
        this.totalLength = 0;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Value type [ {1} ] Log threshhold [ {2} ]",
                                              this.hashText, this.valueType, Integer.valueOf(this.logThreshHold)));
        }
    }

    //

    protected final UtilImpl_Factory factory;

    @Override
    @Trivial
    public UtilImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final ValueType valueType;

    @Override
    @Trivial
    public ValueType getValueType() {
        return valueType;
    }

    protected boolean checkValues;

    @Trivial
    public boolean getCheckValues() {
        return checkValues;
    }

    //

    protected final String name;

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    // Name intern map ...

    protected final Map<String, String> internMap;
    protected int lastReportedLength; // Used for logging.
    protected int totalLength; // Used for logging.

    @Trivial
    protected Map<String, String> getInternMap() {
        return internMap;
    }

    @Trivial
    protected int getLastReportedLength() {
        return lastReportedLength;
    }

    @Override
    @Trivial
    public Collection<String> getValues() {
        return getInternMap().values();
    }

    @Override
    @Trivial
    public int getSize() {
        return getInternMap().size();
    }

    @Override
    @Trivial
    public int getTotalLength() {
        return totalLength;
    }

    /**
     * Intern a string value. Do force the value to be interned.
     * 
     * See {@link #intern(String, boolean) and {@link Util_InternMap#DO_FORCE}.
     * 
     * @param value The string value which is to be interned.
     * 
     * @return The interned string value. Only null if the string
     *         value is null.
     */
    @Override
    // Don't log this call: Rely on 'intern(String, boolean)' to log the intern call and result.    
    @Trivial
    public String intern(String value) {
        return intern(value, Util_InternMap.DO_FORCE);
    }

    /**
     * Intern a string string. Answer the prior interned value, if the
     * value is already interned. Otherwise, depending on the
     * forcing control parameter, answer null, or, store the value
     * and answer it as the newly interned value.
     * 
     * Emit a warning if validation is enabled and the value does
     * not match the value type set for this intern map.
     * 
     * Answer null for a null string value, and perform no validation.
     * Null is never stored in the intern map. A call to {@link #contains(String)} answers false for null.
     * 
     * @param value The string value which is to be interned.
     * @param doForce True or false telling if the value, if
     *            not already interned, should be interned.
     * 
     * @return The intern value for the string. Null if the
     *         value is not interned and the forcing control parameter
     *         is false.
     */
    // Not set as Trivial: We want to trace intern calls.
    @Override
    public String intern(String value, boolean doForce) {
        if (value == null) {
            return value;
        }

        if (checkValues) {
            String vMsgId = validate(value, valueType);
            if (vMsgId != null) {
                Tr.warning(tc, vMsgId, getHashText(), value, valueType);
            }
        }

        String priorIntern = internMap.get(value);
        if ((priorIntern != null) || !doForce) {
            return priorIntern;
        }

        priorIntern = value;
        internMap.put(value, value);

        totalLength += value.length();
        if ((totalLength - lastReportedLength) > logThreshHold) {
            lastReportedLength = totalLength;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Total [ {1} ]", hashText, Integer.valueOf(totalLength)));
            }
        }

        return priorIntern;
    }

    /**
     * Tell if a value is held by the intern map.
     * 
     * This is not an intern enabled call: Simple string equality
     * is used to test for containment. That is, this tests if
     * a value has been interned for the value, not whether the
     * value is the actual interned string value. A call to {@link #intern(String, boolean)} with a false forcing parameter,
     * plus a test for identity on the return value are necessary
     * to test if a string is the interned string value.
     * 
     * Null is never interned: Answer false for a null value.
     * 
     * @param value The value to test.
     */
    // Used by:
    // See: UtilImpl_BidirectionalMap.containsHeld(String)
    // And: UtilImpl_BidirectionalMap.containsHolder(String)
    @Override
    @Trivial
    public boolean contains(String value) {
        return internMap.containsKey(value);
    }

    //

    @Override
    @Trivial
    public void logState() {
        if (AnnotationServiceImpl_Logging.stateLogger.isDebugEnabled()) {
            log(AnnotationServiceImpl_Logging.stateLogger);
        }
    }

    @Override
    @Trivial
    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("BEGIN Intern Map [ {0} ]:", getHashText()));
        Tr.debug(logger, MessageFormat.format("  Log threshhold[ {0} ]", Integer.valueOf(getLogThreshHold())));
        Tr.debug(logger, MessageFormat.format("  Size [ {0} ]", Integer.valueOf(getSize())));
        Tr.debug(logger, MessageFormat.format("  Total Length [ {0} ]", Integer.valueOf(getTotalLength())));
        Tr.debug(logger, "END Intern Map");
    }
}
