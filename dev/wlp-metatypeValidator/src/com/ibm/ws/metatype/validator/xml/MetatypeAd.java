/*******************************************************************************
 * Copyright (c) 2013,2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metatype.validator.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.ibm.ws.metatype.validator.MetatypeValidator.MetatypeOcdStats;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public class MetatypeAd extends MetatypeBase {
    protected static final HashSet<String> validTypes = new HashSet<String>();
    protected static final HashSet<String> globalVariableNames = new HashSet<String>();
    protected static final Set<String> variableExpressionFunctionNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "servicePidOrFilter",
                                                                                                                                                      "count" })));

    static {
        validTypes.add("String");
        validTypes.add("Boolean");
        validTypes.add("Integer");
        validTypes.add("Long");
        validTypes.add("Byte");
        validTypes.add("Character");
        validTypes.add("Short");
        validTypes.add("Double");
        validTypes.add("Float");

        globalVariableNames.add("service.pid");
        globalVariableNames.add("id");
        globalVariableNames.add("server.output.dir");
        globalVariableNames.add("server.config.dir");
        globalVariableNames.add("wlp.server.name");
        globalVariableNames.add("defaultHostName");
        globalVariableNames.add("wlp.collective.auth.plugin");
    }

    // known attributes
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "name")
    private String name;
    @XmlAttribute(name = "description")
    private String description;
    @XmlAttribute(name = "required")
    private String required;
    @XmlAttribute(name = "type")
    private String type;
    @XmlAttribute(name = "default")
    private String defaultValue;
    @XmlAttribute(name = "cardinality")
    private String cardinality;
    @XmlAttribute(name = "group", namespace = IBMUI_NAMESPACE)
    private String ibmuiGroup;
    @XmlAttribute(name = "uiReference", namespace = IBMUI_NAMESPACE)
    private String ibmuiUIReference;
    @XmlAttribute(name = "type", namespace = IBM_NAMESPACE)
    private String ibmType;
    @XmlAttribute(name = "final", namespace = IBM_NAMESPACE)
    private String ibmFinal;
    @XmlAttribute(name = "flat", namespace = IBM_NAMESPACE)
    private String ibmFlat;
    @XmlAttribute(name = "reference", namespace = IBM_NAMESPACE)
    private String ibmReference;
    @XmlAttribute(name = "filter", namespace = IBM_NAMESPACE)
    private String ibmFilter;
    @XmlAttribute(name = "service", namespace = IBM_NAMESPACE)
    private String ibmService;
    @XmlAttribute(name = "serviceFilter", namespace = IBM_NAMESPACE)
    private String ibmServiceFilter;
    @XmlAttribute(name = "unique", namespace = IBM_NAMESPACE)
    private String ibmToken;
    @XmlAttribute(name = "token", namespace = IBM_NAMESPACE)
    private String ibmUnique;
    @XmlAttribute(name = "variable", namespace = IBM_NAMESPACE)
    private String ibmVariable;
    @XmlAttribute(name = "copyOf", namespace = IBM_NAMESPACE)
    private String ibmCopyOf;
    @XmlAttribute(name = "rename", namespace = IBM_NAMESPACE)
    private String ibmRename;
    @XmlAttribute(name = "min")
    private String min;
    @XmlAttribute(name = "max")
    private String max;
    @XmlAttribute(name = "requiresTrue", namespace = IBMUI_NAMESPACE)
    private String requiresTrue;
    @XmlAttribute(name = "requiresFalse", namespace = IBMUI_NAMESPACE)
    private String requiresFalse;
    @XmlElement(name = "Option")
    private final List<MetatypeAdOption> options = new LinkedList<MetatypeAdOption>();
    @XmlAttribute(name = "beta", namespace = IBM_NAMESPACE)
    private boolean beta;

    private MetatypeOcd parent;
    boolean isTypeValid = false;
    boolean isNlsRequired = true;

    protected void setParentOcd(MetatypeOcd ocd) {
        parent = ocd;
    }

    public MetatypeOcd getParentOcd() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    boolean localizationNeeded() {
        //any non-magic string means we need localization
        if (!MetatypeOcd.INTERNAL.equals(name) || !MetatypeOcd.INTERNAL_USE_ONLY.equals(description)) {
            return true;
        }
        return false;
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);

        if ("pid".equals(ibmType)) {
            isNlsRequired = false;
            if (ibmReference != null) {
                if (ibmFilter != null)
                    logMsgWithContext(MessageType.Error, "pidtype", "inconsistent.pid.info", "ibm:reference", ibmReference, "ibm:filter", ibmFilter);
                if (ibmService != null)
                    logMsgWithContext(MessageType.Error, "pidtype", "inconsistent.pid.info", "ibm:reference", ibmReference, "ibm:service", ibmService);
            }
            if (ibmFilter != null) {
                if (ibmService != null)
                    logMsgWithContext(MessageType.Error, "pidtype", "inconsistent.pid.info", "ibm:filter", ibmFilter, "ibm:service", ibmService);
            }
            if (ibmServiceFilter != null && ibmService == null)
                logMsgWithContext(MessageType.Error, "pidtype", "serviceFilter.without.service", ibmServiceFilter);
        } else {
            if (ibmReference != null)
                logMsgWithContext(MessageType.Error, "notpidtype", "not.pid.but", "ibm:reference", ibmReference);
            if (ibmFilter != null)
                logMsgWithContext(MessageType.Error, "notpidtype", "not.pid.but", "ibm:filter", ibmFilter);
            if (ibmService != null)
                logMsgWithContext(MessageType.Error, "notpidtype", "not.pid.but", "ibm:service", ibmService);
            if (ibmServiceFilter != null)
                logMsgWithContext(MessageType.Error, "notpidtype", "not.pid.but", "ibm:serviceFilter", ibmServiceFilter);
        }
        validateId();
        validateName();
        validateDescription();
        validateRequired();
        validateType();
        validateDefault();
        validateCardinality();
        validateMinMax();
        validateIbmCopyOf();
        validateIbmRename();
        validateRequiresTF();
        if (validateRefs) {
            validateIbmReference();
            validateIbmService();
            validateIbmUIReference();
        }
        if (ibmType != null) {
            if ("pid".equals(ibmType))
                validatePid();
            else if (ibmType.startsWith("duration"))
                validateDuration();
            else if ("password".equals(ibmType))
                validatePassword();
            else if ("onError".equals(ibmType))
                validateOnError();
            else if (ibmType.startsWith("location"))
                validateLocation();
            else if ("passwordHash".equals(ibmType))
                validatePasswordHash();
            else if ("token".equals(ibmType))
                validateToken();
            else
                logMsgWithContext(MessageType.Error, "ibm:type", "unknown.value", "ibm:type", ibmType);
        }

        // check if there are unknown elements
        checkIfUnknownElementsPresent();

        // check if there are unknown attributes
        checkIfUnknownAttributesPresent();

        for (MetatypeAdOption option : options) {
            option.setParentAd(this);
            option.validate(validateRefs);
            setValidityState(option.getValidityState());
        }
    }

    private void validateIbmCopyOf() {
        if (ibmCopyOf != null) {
            String trimmed = ibmCopyOf.trim();

            if (ibmCopyOf.length() != trimmed.length())
                logMsgWithContext(MessageType.Info, "ibm:copyOf", "white.space.found", "ibm:copyOf", ibmCopyOf);
        }
    }

    private void validateIbmRename() {
        if (ibmRename != null) {
            String trimmed = ibmRename.trim();

            if (ibmRename.length() != trimmed.length())
                logMsgWithContext(MessageType.Info, "ibm:rename", "white.space.found", "ibm:rename", ibmRename);
        }
    }

    private void validateMinMax() {
        if (type == null) {
            logMsgWithContext(MessageType.Warning, "min", "cannot.validate", "'min'", "'type' is missing");
            logMsgWithContext(MessageType.Warning, "max", "cannot.validate", "'max'", "'type' is missing");
        } else if ("Float".equals(type)) {
            Float float_min = null;
            Float float_max = null;

            if (min != null) {
                try {
                    float_min = Float.parseFloat(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Float", min);
                }
            }

            if (max != null) {
                try {
                    float_max = Float.parseFloat(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Float", max);
                }
            }

            if (float_min != null && float_max != null) {
                if (float_min >= float_max) {
                    logMsg(MessageType.Error, "invalid.range", id, float_min, float_max);
                }
            }
        } else if ("Double".equals(type)) {
            Double double_min = null;
            Double double_max = null;

            if (min != null) {
                try {
                    double_min = Double.parseDouble(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Double", min);
                }
            }

            if (max != null) {
                try {
                    double_max = Double.parseDouble(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Double", max);
                }
            }

            if (double_min != null && double_max != null) {
                if (double_min >= double_max) {
                    logMsg(MessageType.Error, "invalid.range", id, double_min, double_max);
                }
            }
        } else if ("Short".equals(type)) {
            Short short_min = null;
            Short short_max = null;

            if (min != null) {
                try {
                    short_min = Short.parseShort(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Short", min);
                }
            }

            if (max != null) {
                try {
                    short_max = Short.parseShort(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Short", max);
                }
            }

            if (short_min != null && short_max != null) {
                if (short_min >= short_max) {
                    logMsg(MessageType.Error, "invalid.range", short_min, short_max);
                }
            }
        } else if ("Byte".equals(type)) {
            Byte byte_min = null;
            Byte byte_max = null;

            if (min != null) {
                try {
                    byte_min = Byte.parseByte(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Byte", min);
                }
            }

            if (max != null) {
                try {
                    byte_max = Byte.parseByte(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Byte", max);
                }
            }

            if (byte_min != null && byte_max != null) {
                if (byte_min >= byte_max) {
                    logMsg(MessageType.Error, "invalid.range", byte_min, byte_max);
                }
            }
        } else if ("Integer".equals(type)) {
            Integer integer_min = null;
            Integer integer_max = null;

            if (min != null) {
                try {
                    integer_min = Integer.parseInt(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Integer", min);
                }
            }

            if (max != null) {
                try {
                    integer_max = Integer.parseInt(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Integer", max);
                }
            }

            if (integer_min != null && integer_max != null) {
                if (integer_min >= integer_max) {
                    logMsg(MessageType.Error, "invalid.range", integer_min, integer_max);
                }
            }
        } else if ("Long".equals(type)) {
            Long long_min = null;
            Long long_max = null;

            if (min != null) {
                try {
                    long_min = Long.parseLong(min);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "min", "invalid.type.2", "min", "Long", min);
                }
            }

            if (max != null) {
                try {
                    long_max = Long.parseLong(max);
                } catch (NumberFormatException e) {
                    logMsgWithContext(MessageType.Error, "max", "invalid.type.2", "max", "Long", max);
                }
            }

            if (long_min != null && long_max != null) {
                if (long_min >= long_max) {
                    logMsg(MessageType.Error, "invalid.range", long_min, long_max);
                }
            }
        }
    }

    private void validateCardinality() {
        if (cardinality != null) {
            try {
                Integer.parseInt(cardinality);
            } catch (NumberFormatException e) {
                logMsgWithContext(MessageType.Error, "cardinality", "cardinality.not.number", cardinality);
            }
        }
    }

    private void validateType() {
        if (type == null) {
            logMsgWithContext(MessageType.Error, "type", "missing.attribute", "type");
        } else {
            boolean typeFound = false;
            if (validTypes.contains(type))
                typeFound = true;

            if (!typeFound) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> iterator = validTypes.iterator();

                while (iterator.hasNext()) {
                    sb.append(iterator.next());
                    if (iterator.hasNext())
                        sb.append('|');
                }
                logMsgWithContext(MessageType.Error, "type", "invalid.value", "type", sb.toString(), type);
            } else
                isTypeValid = true;
        }
    }

    private void validateDefault() {
        if (defaultValue != null) {
            String trimmed = defaultValue.trim();
            if (trimmed.length() != defaultValue.length())
                logMsgWithContext(MessageType.Info, "default", "white.space.found", "default", defaultValue);

            if (isTypeValid) {
                boolean invalidDefault = true; // assume bad

                if ("String".equals(type))
                    invalidDefault = false;
                else if ("Boolean".equals(type) && ("true".equals(trimmed)) || "false".equals(trimmed))
                    invalidDefault = false;
                else if ("Integer".equals(type)) {
                    try {
                        Integer.parseInt(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Long".equals(type)) {
                    try {
                        Long.parseLong(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Short".equals(type)) {
                    try {
                        Short.parseShort(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Byte".equals(type)) {
                    try {
                        Byte.parseByte(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Float".equals(type)) {
                    try {
                        Float.parseFloat(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Double".equals(type)) {
                    try {
                        Double.parseDouble(trimmed);
                        invalidDefault = false;
                    } catch (NumberFormatException e) {
                        // no-op
                    }
                } else if ("Character".equals(type) && trimmed.length() == 1 && Character.isLetterOrDigit(trimmed.charAt(0))) {
                    invalidDefault = false;
                }

                if (invalidDefault) {
                    logMsgWithContext(MessageType.Error, "default", "DEFAULT_TYPE_MISMATCH", defaultValue);
                } else {
                    int openIndex = trimmed.indexOf("${");
                    int closeIndex = trimmed.indexOf('}', openIndex);

                    if (openIndex != -1 && closeIndex > openIndex) {
                        // lookup the property and make sure it exists within the same OCD
                        String property = trimmed.substring(openIndex + 2, closeIndex);

                        int openParenIndex = property.indexOf('(');
                        if (openParenIndex != -1 && property.endsWith(")")) {
                            String functionName = property.substring(0, openParenIndex);
                            if (variableExpressionFunctionNames.contains(functionName)) {
                                property = property.substring(openParenIndex + 1, property.length() - 1);
                            } else {
                                logMsgWithContext(MessageType.Error, "default", "unknown.function", property, functionName);
                                property = null;
                            }
                        }

                        if (property != null
                            && !globalVariableNames.contains(property)
                            && !property.matches("(.*)(\\.)(\\d+)(\\.)(.*)") // ignore if possibly referencing an attribute of a flattened config element
                            && !parent.doesAdElementExist(property)) {
                            logMsgWithContext(MessageType.Error, "default", "missing.property", property);
                        }
                    }
                }
            }
        }
    }

    private void validateId() {
        if (id != null) {
            String trimmed = id.trim();
            if (trimmed.length() != id.length())
                logMsgWithContext(MessageType.Info, "id", "white.space.found", "id", id);

            if (trimmed.isEmpty())
                logMsgWithContext(MessageType.Error, "id", "missing.attribute", "id");

            if (Character.isUpperCase(trimmed.charAt(0)))
                logMsg(MessageType.Error, "attribute.id.wrong.case", "id", id);
        } else
            logMsgWithContext(MessageType.Error, "id", "missing.attribute", "id");
    }

    private void validateName() {
        if (isNlsRequired) {
            if (name != null) {
                String trimmed = name.trim();
                if (trimmed.length() != name.length())
                    logMsgWithContext(MessageType.Info, "name", "white.space.found", "name", name);

                if (trimmed.isEmpty())
                    logMsgWithContext(MessageType.Error, "name", "missing.attribute", "name");
                else if (!"internal".equals(trimmed)) {
                    if (!trimmed.startsWith("%"))
                        logMsgWithContext(MessageType.Error, "name", "needs.translation", "name", name);
                    else {
                        String key = trimmed.substring(1);
                        if (!isNlsKeyValid(key))
                            logMsgWithContext(MessageType.Error, "name", "invalid.nls.key", key);
                    }
                }
            } else
                logMsgWithContext(MessageType.Error, "name", "missing.attribute", "name");
        }
    }

    private void validateDescription() {
        if (isNlsRequired) {
            if (name != null && "internal".equals(name.trim()))
                return;

            if (description != null) {
                String trimmed = description.trim();
                if (trimmed.length() != description.length())
                    logMsgWithContext(MessageType.Info, "description", "white.space.found", "description", description);

                if (trimmed.isEmpty())
                    logMsgWithContext(MessageType.Error, "description", "missing.attribute", "description");
                else if (!"internal use only".equals(trimmed)) {
                    if (!trimmed.startsWith("%"))
                        logMsgWithContext(MessageType.Error, "description", "needs.translation", "description", description);
                    else {
                        String key = trimmed.substring(1);
                        if (!isNlsKeyValid(key))
                            logMsgWithContext(MessageType.Error, "description", "invalid.nls.key", key);
                    }
                }
            } else
                logMsgWithContext(MessageType.Error, "description", "missing.attribute", "description");
        }
    }

    private void validateRequired() {
        if (required == null)
            logMsgWithContext(MessageType.Info, "required", "required.not.set");
        else {
            String trimmed = required.trim();
            if (trimmed.length() != required.length())
                logMsgWithContext(MessageType.Info, "required", "white.space.found", "required", required);

            if (!"true".equals(trimmed) && !"false".equals(trimmed))
                logMsgWithContext(MessageType.Error, "required", "invalid.value", "required", "true|false", required);
//            else if (defaultValue != null && "false".equals(trimmed))
//                logEvent(EventType.WARNING, "required.false.with.default.value", defaultValue);
        }
    }

    private void validateLocation() {

        if (!"String".equals(type)) {
            logMsgWithContext(MessageType.Error, "ibm:location", "invalid.type", "String", type);
            return;
        }

        if (!"location".equals(ibmType) && !"location(dir)".equals(ibmType) && !"location(file)".equals(ibmType))
            logMsgWithContext(MessageType.Error, "ibm:location", "unknown.location.type", "ibm:type", ibmType);
    }

    private void validateOnError() {
        if (!"String".equals(type)) {
            logMsgWithContext(MessageType.Error, "ibm:onError", "invalid.type", "String", type);
            return;
        }

        if (defaultValue != null && !"IGNORE".equals(defaultValue) && !"WARN".equals(defaultValue) && !"FAIL".equals(defaultValue))
            logMsgWithContext(MessageType.Error, "ibm:onError", "invalid.value", "default", "IGNORE|WARN|FAIL", defaultValue);
    }

    private void validatePassword() {
        if (!"String".equals(type))
            logMsgWithContext(MessageType.Error, "ibm:password", "invalid.type", "String", type);
    }

    private void validatePasswordHash() {
        if (!"String".equals(type))
            logMsgWithContext(MessageType.Error, "ibm:passwordHash", "invalid.type", "String", type);
    }

    private void validateToken() {
        if (!"String".equals(type))
            logMsgWithContext(MessageType.Error, "ibm:token", "invalid.type", "String", type);
    }

    private void validateDuration() {
        if (!"String".equals(type))
            logMsgWithContext(MessageType.Error, "duration", "invalid.type", "String", type);

        TimeUnit timeUnit = null;
        boolean skip = false;
        long duration = 0L, val = 0L;

        if (ibmType.equals("duration")) {
            logMsgWithContext(MessageType.Info, "duration", "generic.duration");
            timeUnit = TimeUnit.MILLISECONDS;
        } else if (ibmType.equals("duration(ns)"))
            timeUnit = TimeUnit.NANOSECONDS;
        else if (ibmType.equals("duration(ms)"))
            timeUnit = TimeUnit.MILLISECONDS;
        else if (ibmType.equals("duration(s)"))
            timeUnit = TimeUnit.SECONDS;
        else if (ibmType.equals("duration(m)"))
            timeUnit = TimeUnit.MINUTES;
        else if (ibmType.equals("duration(h)"))
            timeUnit = TimeUnit.HOURS;
        else if (ibmType.equals("duration(d)"))
            timeUnit = TimeUnit.DAYS;
        else {
            logMsgWithContext(MessageType.Error, "duration", "unknown.duration.unit", "ibm:type", ibmType);
            return;
        }

        if (defaultValue != null && !defaultValue.contains("${")) {
            int index = 0;
            while (true) {
                if (Character.isLetter(defaultValue.charAt(index))) {
                    String unit = defaultValue.substring(index);
                    String value = defaultValue.substring(0, index);

                    try {
                        duration = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        logMsgWithContext(MessageType.Error, "duration", "bad.duration.time", "default", defaultValue);
                        return;
                    }

                    if (!skip) {
                        if ("ms".equals(unit))
                            val = timeUnit.convert(duration, TimeUnit.MILLISECONDS);
                        else if ("s".equals(unit))
                            val = timeUnit.convert(duration, TimeUnit.SECONDS);
                        else if ("m".equals(unit))
                            val = timeUnit.convert(duration, TimeUnit.MINUTES);
                        else if ("h".equals(unit))
                            val = timeUnit.convert(duration, TimeUnit.HOURS);
                        else if ("d".equals(unit))
                            val = timeUnit.convert(duration, TimeUnit.DAYS);
                        else {
                            logMsgWithContext(MessageType.Error, "duration", "unknown.duration.unit", "default", unit);
                            return;
                        }

                        logMsgWithContext(MessageType.Info, "duration", "default.duration.conversion", defaultValue, val, timeUnit);
                    }

                    break;
                } else {
                    ++index;
                    if (index == defaultValue.length()) {
                        try {
                            duration = Long.parseLong(defaultValue);
                        } catch (NumberFormatException e) {
                            logMsgWithContext(MessageType.Error, "duration", "bad.duration.time", "default", defaultValue);
                            return;
                        }

                        val = timeUnit.convert(duration, timeUnit);
                        logMsgWithContext(MessageType.Info, "duration", "default.duration.conversion", defaultValue, val, timeUnit);

                        break;
                    }
                }
            }
        }
    }

    private void validatePid() {
        if (!"String".equals(type))
            logMsgWithContext(MessageType.Error, "pid", "invalid.type", "type", type);

        if (ibmReference == null && ibmFilter == null && ibmService == null)
            logMsgWithContext(MessageType.Error, "pid", "missing.attribute", "ibm:reference, ibm:filter or ibm:service");
    }

    private void validateIbmReference() {
        if (ibmReference != null) {
            String trimmed = ibmReference.trim();
            if (trimmed.length() != ibmReference.length())
                logMsgWithContext(MessageType.Info, "reference", "white.space.found", "ibm:reference", ibmReference);

            if (getOcdStats() != null) {
                boolean refFound = false;

                for (MetatypeOcdStats ocdStat : getOcdStats()) {
                    if (trimmed.equals(ocdStat.designateId)) {
                        refFound = true;
                        break;
                    }
                }

                if (!refFound)
                    logMsgWithContext(MessageType.Error, "reference", "ref.not.found", trimmed, "ibm:reference");
            }

            if (isNlsRequired) {
                if (name == null)
                    logMsgWithContext(MessageType.Error, "reference", "ref.translation.not.found", "???");
                else {
                    String nlsKey = name.trim().substring(1) + "$Ref";
                    if (!isNlsKeyValid(nlsKey))
                        logMsgWithContext(MessageType.Error, "reference", "ref.translation.not.found", nlsKey);
                }
            }
        }
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private void validateIbmService() {
        if (ibmService != null) {
            String trimmed = ibmService.trim();
            if (trimmed.length() != ibmService.length())
                logMsgWithContext(MessageType.Info, "service", "white.space.found", "ibm:service", ibmService);

            if (getOcdStats() != null) {
                boolean refFound = false;

                for (MetatypeOcdStats ocdStat : getOcdStats()) {
                    if (ocdStat.ibmObjectClass.contains(trimmed)) {
                        refFound = true;
                        break;
                    }
                }

                if (!refFound)
                    logMsgWithContext(MessageType.Error, "service", "service.not.found", trimmed, "ibm:service");
            }

            if (isNlsRequired) {
                if (name == null)
                    logMsgWithContext(MessageType.Error, "service", "service.translation.not.found", "???");
                else {
                    String nlsKey = name.trim().substring(1) + "$Ref";
                    if (!isNlsKeyValid(nlsKey))
                        logMsgWithContext(MessageType.Error, "service", "service.translation.not.found", nlsKey);
                }
            }
        }
        if (ibmServiceFilter != null) {
            try {
                FrameworkUtil.createFilter(ibmServiceFilter);
            } catch (InvalidSyntaxException e) {
                // The exception message includes the filter string.
                logMsgWithContext(MessageType.Error, "ibm:serviceFilter", "invalid.filter", ibmServiceFilter, e.getMessage());
            }
            if (WHITESPACE.matcher(ibmServiceFilter).matches()) {
                logMsgWithContext(MessageType.Error, "ibm:serviceFilter", "white.space.found", "ibm:serviceFilter", ibmServiceFilter);
            }
            if (ibmService == null)
                logMsgWithContext(MessageType.Error, "serviceFilter", "service.filter.without.service", ibmServiceFilter);
        }
    }

    private void validateIbmUIReference() {
        if (ibmuiUIReference != null) {
            String trimmed = ibmuiUIReference.trim();
            if (trimmed.length() != ibmuiUIReference.length())
                logMsgWithContext(MessageType.Info, "uiReference", "white.space.found", "ibm:uiReference", ibmuiUIReference);

            String[] pids = trimmed.split("[, ]+");
            for (String pid : pids) {
                if (getOcdStats() != null) {
                    boolean refFound = false;

                    for (MetatypeOcdStats ocdStat : getOcdStats()) {
                        if (pid.equals(ocdStat.designateId)) {
                            refFound = true;
                            break;
                        }
                    }

                    if (!refFound)
                        logMsgWithContext(MessageType.Error, "uiReference", "ref.not.found", pid, "ibm:uiReference");
                }
            }
            //no extra $Ref translation needed for these plain Ads.

        }
    }

    private void validateRequiresTF() {
        validateRequires("requiresTrue", requiresTrue);
        validateRequires("requiresFalse", requiresFalse);
    }

    private void validateRequires(String attrName, String value) {
        if (value != null) {
            MetatypeOcd ocd = getParentOcd();
            boolean foundReference = false;
            StringBuilder allowedValues = new StringBuilder();

            // Make sure that we point to another attribute under the same <OCD> element
            for (MetatypeAd ad : ocd.getAds()) {
                allowedValues.append(ad.id).append('|');
                if (value.equals(ad.id)) {
                    foundReference = true;
                    // Make sure that the attribute we point to is of type boolean
                    if (!ad.type.equalsIgnoreCase("boolean"))
                        logMsgWithContext(MessageType.Error, attrName, "invalid.type.2", attrName, "boolean", ad.type);
                }
            }
            if (!foundReference)
                logMsgWithContext(MessageType.Error, attrName, "invalid.value", attrName, allowedValues.toString(), value);
        }
    }

    public List<MetatypeAdOption> getOptions() {
        return options;
    }
}
