/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.metatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.metatype.AttributeDefinitionProperties;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.Utils;

/**
 * Metatype Attribute Definition (AD)
 */
@Trivial
public class MetatypeAd {
    private static final TraceComponent tc = Tr.register(MetatypeAd.class);

    public static final Map<String, Integer> TYPES = new HashMap<String, Integer>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED = new HashMap<Class<?>, Class<?>>();

    static {
        TYPES.put("Boolean", AttributeDefinition.BOOLEAN);
        TYPES.put("Byte", AttributeDefinition.BYTE);
        TYPES.put("Character", AttributeDefinition.CHARACTER);
        TYPES.put("Double", AttributeDefinition.DOUBLE);
        TYPES.put("Float", AttributeDefinition.FLOAT);
        TYPES.put("Integer", AttributeDefinition.INTEGER);
        TYPES.put("Long", AttributeDefinition.LONG);
        TYPES.put("Short", AttributeDefinition.SHORT);
        TYPES.put("String", AttributeDefinition.STRING);

        PRIMITIVE_TO_BOXED.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_BOXED.put(byte.class, Byte.class);
        PRIMITIVE_TO_BOXED.put(char.class, Character.class);
        PRIMITIVE_TO_BOXED.put(double.class, Double.class);
        PRIMITIVE_TO_BOXED.put(float.class, Float.class);
        PRIMITIVE_TO_BOXED.put(int.class, Integer.class);
        PRIMITIVE_TO_BOXED.put(long.class, Long.class);
        PRIMITIVE_TO_BOXED.put(short.class, Short.class);
    }

    private final MetaTypeFactory metaTypeProviderFactory;

    public MetatypeAd(MetaTypeFactory mtpService) {
        this.metaTypeProviderFactory = mtpService;
    }

    public static boolean isTypeClassName(String className) {
        return getTypeName(className) != null;
    }

    public static String getTypeName(String className) {
        if (className.startsWith("java.lang.")) {
            String typeName = className.substring("java.lang.".length());
            if (TYPES.containsKey(typeName)) {
                return typeName;
            }
        }
        return null;
    }

    public static Class<?> getBoxedType(Class<?> primitiveType) {
        return PRIMITIVE_TO_BOXED.get(primitiveType);
    }

    private static final Set<String> FRACT_NUMBER_TYPES = new HashSet<String>(Arrays.asList("Double", "Float"));
    private static final Set<String> WHOLE_NUMBER_TYPES = new HashSet<String>(Arrays.asList("Byte", "Integer", "Long", "Short"));

    private String id;
    private Boolean required;
    private String type;
    private String ibmType;
    private List<String> cachedDefaultValue;
    private String defaultValue;
    private String name;
    private String description;
    private Boolean ibmFinal;
    private String ibmReference;
    private String ibmService;
    private Integer cardinality;
    private String min;
    private String max;
    @XmlElement(name = "Option")
    private final List<MetatypeAdOption> options = new LinkedList<MetatypeAdOption>();
    private String ibmuiGroup;
    private boolean recommendAuthAliasUsage = false;
    private boolean disableOptionLabelNLS = false;
    private String nlsKey;

    public void setNLSKey(String nlsKey) {
        this.nlsKey = nlsKey;
    }

    public String getNLSKey() {
        return nlsKey;
    }

    public void disableOptionLabelNLS() {
        disableOptionLabelNLS = true;
    }

    public boolean isOptionLabelNLSDisabled() {
        return disableOptionLabelNLS;
    }

    public void setRecommendAuthAliasUsage(boolean recommendAuthAliasUsage) {
        this.recommendAuthAliasUsage = recommendAuthAliasUsage;
    }

    public boolean isAuthAliasUsageRecommended() {
        return recommendAuthAliasUsage;
    }

    @XmlAttribute(name = "group", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0")
    public void setIbmuiGroup(String ibmuiGroup) {
        this.ibmuiGroup = ibmuiGroup;
    }

    public String getIbmUigroup() {
        return this.ibmuiGroup;
    }

    public List<MetatypeAdOption> getOptions() {
        return this.options;
    }

    @XmlAttribute(name = "min")
    public void setMin(String min) {
        this.min = min;
    }

    @XmlAttribute(name = "max")
    public void setMax(String max) {
        this.max = max;
    }

    @XmlAttribute(name = "final", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setFinal(Boolean ibmFinal) {
        this.ibmFinal = ibmFinal;
    }

    public boolean isFinal() {
        return ibmFinal != null && ibmFinal;
    }

    @XmlAttribute(name = "id")
    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "required")
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @XmlAttribute(name = "type")
    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "type", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setIbmType(String ibmType) {
        this.ibmType = ibmType;
    }

    @XmlAttribute(name = "default")
    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
        cachedDefaultValue = null;
    }

    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "description")
    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAttribute(name = "reference", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setReferencePid(String ibmReference) {
        this.ibmReference = ibmReference;
    }

    @XmlAttribute(name = "service", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setService(String ibmService) {
        this.ibmService = ibmService;
    }

    @XmlAttribute(name = "cardinality")
    public void setCardinality(Integer cardinality) {
        this.cardinality = cardinality;
        cachedDefaultValue = null;
    }

    public int getCardinality() {
        return cardinality == null ? 0 : cardinality;
    }

    public boolean getRequired() {
        return required != null && required;
    }

    public int getType() {
        Integer t = ibmType == null ? TYPES.get(type) : metaTypeProviderFactory.getIBMType(ibmType);
        return t == null ? -1 : t;
    }

    public String[] getDefaultValue() {
        if (defaultValue == null)
            return null;
        else if (cardinality == null || Math.abs(cardinality.intValue()) == 1)
            return new String[] { defaultValue };
        else {
            if (cachedDefaultValue == null)
                cachedDefaultValue = parse(defaultValue);
            return cachedDefaultValue.toArray(new String[cachedDefaultValue.size()]);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getReferencePid() {
        return ibmReference;
    }

    public String getService() {
        return ibmService;
    }

    /**
     * Parse a default value which might be delimited by commas (unless escaped by \) into a list.
     *
     * @param value default value
     * @return a list of values
     */
    private static List<String> parse(String value) {
        List<String> parts = new ArrayList<String>();
        StringBuilder buffer = null; // buffer that accumulates escaped parts
        for (int length = value.length(), start = 0, comma = value.indexOf(',', 0); // split on commas
        start >= 0; // break out of the loop when no parts remain
        start = comma < 0 ? -1 : (comma + 1), comma = comma == length ? -1 : value.indexOf(',', comma + 1)) {

            String part = value.substring(start, comma < 0 ? length : comma);
            if (part.length() > 0 && part.charAt(part.length() - 1) == '\\') {
                if (buffer == null)
                    buffer = new StringBuilder();
                buffer.append(part.substring(0, part.length() - 1)).append(',');
            } else {
                parts.add(buffer == null ? part : buffer.append(part).toString());
                buffer = null;
            }
        }
        if (buffer != null)
            parts.add(buffer.toString());
        return parts;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        sb.append("id='").append(id).append("' ");
        if (cardinality != null)
            sb.append("cardinality='").append(cardinality).append("' ");
        if (defaultValue != null)
            sb.append("defaultValue='").append(defaultValue).append("' ");
        if (description != null)
            sb.append("description='").append(description).append("' ");
        if (ibmFinal != null)
            sb.append("ibmFinal='").append(ibmFinal).append("' ");
        if (ibmReference != null)
            sb.append("ibmReference='").append(ibmReference).append("' ");
        if (ibmType != null)
            sb.append("ibmType='").append(ibmType).append("' ");
        if (ibmuiGroup != null)
            sb.append("ibmuiGroup='").append(ibmuiGroup).append("' ");
        if (max != null)
            sb.append("max='").append(max).append("' ");
        if (min != null)
            sb.append("min='").append(min).append("' ");
        if (name != null)
            sb.append("name='").append(name).append("' ");
        if (required != null)
            sb.append("required='").append(required).append("' ");
        if (type != null)
            sb.append("type='").append(type).append("' ");
        if (nlsKey != null)
            sb.append("nlsKey='").append(nlsKey).append("' ");
        if (options != null && !options.isEmpty()) {
            sb.append("options=[");
            for (int i = 0; i < options.size(); ++i) {
                sb.append(options.get(i));

                if (i + 1 != options.size())
                    sb.append(',');
            }
            sb.append(']');
        }

        sb.append('}');

        return sb.toString();
    }

    private String xmlSafeAttrString(String unsafe) {
        if (unsafe.matches(".*[<\"].*")) {
            unsafe = unsafe.replaceAll("<", "&lt;");
            unsafe = unsafe.replaceAll("\"", "&quot;");
        }
        return unsafe;
    }

    public String toMetatypeString(int padSpaces) {
        final String buffer = Utils.getSpaceBufferString(padSpaces);
        final String subBuffer = Utils.getSpaceBufferString(padSpaces + 1);
        StringBuilder sb = new StringBuilder(buffer).append("<AD ");

        sb.append("id=\"").append(id).append("\" type=\"").append("Character".equals(type) ? "Char" : type).append("\" ");
        if (ibmType != null)
            sb.append("ibm:type=\"").append(ibmType).append("\" ");
        if (required != null)
            sb.append("required=\"").append(required).append("\" ");
        if (ibmFinal != null)
            sb.append("ibm:final=\"").append(ibmFinal).append("\" ");
        if (defaultValue != null)
            sb.append("default=\"").append(defaultValue).append("\" ");
        if (ibmReference != null)
            sb.append("ibm:reference=\"").append(ibmReference).append("\" ");
        if (cardinality != null)
            sb.append("cardinality=\"").append(cardinality).append("\" ");
        if (ibmuiGroup != null)
            sb.append("ibmui:group=\"").append(ibmuiGroup).append("\" ");
        if (min != null)
            sb.append("min=\"").append(min).append("\" ");
        if (max != null)
            sb.append("max=\"").append(max).append("\" ");
        if (name != null)
            sb.append("name=\"").append(name).append("\" ");
        if (description != null)
            sb.append("description=\"").append(xmlSafeAttrString(description)).append("\" ");
        if (options == null || options.isEmpty())
            sb.append("/>");
        else {
            sb.append(">").append(Utils.NEW_LINE);

            for (MetatypeAdOption option : options)
                sb.append(subBuffer).append(option.toMetatypeString(0)).append(Utils.NEW_LINE);

            sb.append(buffer).append("</AD>");
        }
        return sb.toString();
    }

    public String getID() {
        return id;
    }

    public String[] getOptionLabels() {
        if (options.isEmpty())
            return null;

        String[] labels = new String[options.size()];

        for (int i = 0; i < options.size(); ++i)
            labels[i] = options.get(i).getLabel();

        return labels;
    }

    public String[] getOptionValues() {
        if (options.isEmpty())
            return null;

        String[] values = new String[options.size()];

        for (int i = 0; i < options.size(); ++i)
            values[i] = options.get(i).getValue();

        return values;
    }

    public String validate(String value) {
        if (min != null || max != null) {
            if (WHOLE_NUMBER_TYPES.contains(type)) {
                long val = Long.parseLong(value);
                if (min != null && val < Long.parseLong(min))
                    return Tr.formatMessage(tc, "J2CA9930.value.too.small", value, min, id);
                if (max != null && val > Long.parseLong(max))
                    return Tr.formatMessage(tc, "J2CA9931.value.too.large", value, max, id);
            } else if (FRACT_NUMBER_TYPES.contains(type)) {
                double val = Double.parseDouble(value);
                if (min != null && val < Double.parseDouble(min))
                    return Tr.formatMessage(tc, "J2CA9930.value.too.small", value, min, id);
                if (max != null && val > Double.parseDouble(max))
                    return Tr.formatMessage(tc, "J2CA9931.value.too.large", value, max, id);
            } else if ("String".equals(type)) {
                if (min != null && value.length() < Integer.parseInt(min))
                    return Tr.formatMessage(tc, "J2CA9932.value.too.short", value, min, id);
                if (max != null && value.length() > Integer.parseInt(max))
                    return Tr.formatMessage(tc, "J2CA9933.value.too.long", value, max, id);
            } else if ("Character".equals(type)) {
                char val = value.charAt(0);
                if (min != null && val < min.charAt(0))
                    return Tr.formatMessage(tc, "J2CA9930.value.too.small", value, min, id);
                if (max != null && val > max.charAt(0))
                    return Tr.formatMessage(tc, "J2CA9931.value.too.large", value, max, id);
            }
        }

        if (!options.isEmpty()) {
            boolean found = false;
            for (MetatypeAdOption option : options)
                if (option.getValue().equals(value)) {
                    found = true;
                    break;
                }
            if (!found)
                return Tr.formatMessage(tc, "J2CA9934.not.a.valid.option", value, id, Arrays.asList(getOptionValues()));
        }

        return ""; // validated
    }

    /**
     * @return
     */
    public AttributeDefinition getAttributeDefinition() {
        AttributeDefinitionProperties props = new AttributeDefinitionProperties(getID());
        props.setOptionLabels(getOptionLabels());
        props.setOptionValues(getOptionValues());
        props.setReferencePid(getReferencePid());
        props.setService(getService());
        props.setDescription(getDescription());
        props.setName(getName());
        props.setDefaultValue(getDefaultValue());
        props.setType(getType());
        props.setFinal(isFinal());
        props.setCardinality(getCardinality());

        return metaTypeProviderFactory.createAttributeDefinition(props);
    }
}
