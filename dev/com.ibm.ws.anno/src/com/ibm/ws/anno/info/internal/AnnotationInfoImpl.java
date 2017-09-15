/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;

/**
 * <p>Implementation of an annotation info object. An annotation info object
 * represents an occurrence of an annotation. Such occur either as directly
 * declared, on a package, class, field, or method, or as a child value of an
 * annotation value, or as a default value of an annotation method.</p>
 * 
 * <p>An annotation info has, primarily, an annotation class name, a table
 * of annotation values, which are keyed by method name, and a context
 * defining info store.</p>
 * 
 * <p>Annotation info objects are scoped to an info store: The info store
 * is necessary for resolving default values, which requires the resolution
 * of the annotation info annotation class name as a class info object. Child
 * default values are obtained as the method info default values from the
 * class info object. The resolution of the the annotation class name must
 * be performed in the same scope as provided the annotation info object: The
 * annotation class must be retrieved using the same class resolution rules as
 * were used for the initial annotation processing.</p>
 * 
 * <p>An annotation info may have an associated declaring info object (which
 * can be for a package, class, field, or method), and may have a table of
 * associated found info objects (for inherited class annotations, and for
 * annotations on inherited field and method objects). Annotations which
 * occur as child values of other annotations <em>do not</em> have a declaring
 * info object, and have an empty table of found objects. Annotations which
 * occur as method default values also do not have declaring or found info
 * objects.</p>
 * 
 * <p>Annotation values should never be null. This implementation allows
 * a null value only when resolution of the associated annotation class
 * information cannot be resolved.</p>
 */
public class AnnotationInfoImpl implements AnnotationInfo {

    private static final TraceComponent tc = Tr.register(AnnotationInfoImpl.class);
    public static final String CLASS_NAME = AnnotationInfoImpl.class.getName();

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    @Override
    public String toString() {
        return hashText;
    }

    //

    /**
     * <p>Construct a new annotation info object. An annotation info object represents a single
     * specific occurrence of an annotation.</p>
     * 
     * <p>The required class name tells the type of the annotation occurrence.</p>
     * 
     * <p>An info store is required for default value processing: Assignment of default values
     * requires that the class info of the annotation type be obtained. Default values
     * are stored as default value on the methods of the class.</p>
     * 
     * <p>Note that the info store provides the correct context information for resolving
     * the annotation class. While many classloading contexts provide access to <code>java<code>
     * and <code>javax</code> annotations, user scenarios may create new annotation types,
     * and these must be resolved using the same class lookup information as was used
     * to locate target class or package of the annotation occurrence.</p>
     * 
     * <p>Package, class, field, and method annotations are recorded to the info store.</p>
     * 
     * @param annotationClassName The name of the annotation type of the new annotation info object.
     * @param infoStore The store relative to which the annotation info object is created.
     */
    public AnnotationInfoImpl(String annotationClassName, InfoStoreImpl infoStore) {
        super();

        String methodName = "<init>";

        this.hashText = CLASS_NAME + "@" + Integer.toString((new Object()).hashCode()) + " ( " + annotationClassName + " )";

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, getHashText());
        }

        this.infoStore = infoStore;

        this.annotationClassName = infoStore.internClassName(annotationClassName);
        this.annotationClassInfo = null;
        this.isInherited = false;

        this.values = null;

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    //

    protected InfoStoreImpl infoStore;

    @Override
    public InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    //

    protected String annotationClassName;

    @Override
    public String getAnnotationClassName() {
        return annotationClassName;
    }

    protected ClassInfoImpl annotationClassInfo;
    protected boolean isInherited;

    protected void resolveAnnotationClassInfo() {
        this.annotationClassInfo = getInfoStore().getDelayableClassInfo(getAnnotationClassName());
        this.isInherited = getAnnotationClassInfo().isAnnotationPresent(AnnotationInfo.JAVA_LANG_ANNOTATION_INHERITED);

    }

    @Override
    public ClassInfoImpl getAnnotationClassInfo() {
        if (this.annotationClassInfo == null) {
            resolveAnnotationClassInfo();
        }

        return this.annotationClassInfo;
    }

    @Override
    public boolean isInherited() {
        if (this.annotationClassInfo == null) {
            resolveAnnotationClassInfo();
        }

        return this.isInherited;
    }

    //

    protected Map<String, AnnotationValueImpl> values;

    @Override
    public boolean isValueDefaulted(String name) {
        return ((this.values == null) || !this.values.containsKey(name));
    }

    // Retrieve the value of the specified method within this annotation
    // occurrence.

    @Override
    public AnnotationValueImpl getValue(String name) {
        AnnotationValueImpl annotationValue = ((this.values == null) ? null : this.values.get(name));

        if (annotationValue == null) {
            ClassInfoImpl useAnnotationClassInfo = getAnnotationClassInfo();
            MethodInfoImpl methodInfo = useAnnotationClassInfo.getMethod(name);
            if (methodInfo == null) {
                Tr.warning(tc, "ANNO_ANNOINFO_NO_METHOD", getHashText(), useAnnotationClassInfo.getHashText(), name);
                // leave the value null
            } else {
                annotationValue = methodInfo.getAnnotationDefaultValue();
            }
        }

        return annotationValue;
    }

    @Override
    public AnnotationValueImpl getCachedAnnotationValue(String name) {
        AnnotationValueImpl annotationValue = ((this.values == null) ? null : this.values.get(name));

        return annotationValue;
    }

    public void addAnnotationValue(String name, AnnotationValueImpl value) {

        if (this.values == null) {
            this.values = new HashMap<String, AnnotationValueImpl>();
        }

        AnnotationValueImpl oldValue = this.values.put(name, value);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Value Name [ {1} ] Value Enum Class [ {2} ] Value [ {3} ]",
                                              getHashText(), name, value.getEnumClassName(), value.getObjectValue()));

            if (oldValue != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Old value Name [ {1} ] Value Enum Class [ {2} ] Value [ {3} ]",
                                                  getHashText(), name, oldValue.getEnumClassName(),
                                                  oldValue.getObjectValue()));
            }
        }
    }

    // Helper to create and add an annotation value which wraps a specified
    // base value.

    public AnnotationValueImpl addAnnotationValue(String name, Object value) {
        AnnotationValueImpl annotationValue = new AnnotationValueImpl(value);

        addAnnotationValue(name, annotationValue);

        return annotationValue;
    }

    // Helper to create and add an annotation value which wraps a specified
    // enumeration value.  Enumeration values are stored as the combination of
    // the enumeration class name and the enumeration literal value.

    public AnnotationValueImpl addAnnotationValue(String name, String enumClassName, String enumName) {
        AnnotationValueImpl annotationValue = new AnnotationValueImpl(enumClassName, enumName);

        addAnnotationValue(name, annotationValue);

        return annotationValue;
    }

    //

    @Override
    public AnnotationInfoImpl getAnnotationValue(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getAnnotationValue());
    }

    @Override
    public List<? extends AnnotationValue> getArrayValue(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getArrayValue());
    }

    @Override
    public Boolean getBoolean(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getBoolean());
    }

    @Override
    public boolean getBooleanValue(String name) {
        return getValue(name).getBooleanValue();
    }

    @Override
    public Byte getByte(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getByte());
    }

    @Override
    public byte getByteValue(String name) {
        return getValue(name).getByteValue();
    }

    @Override
    public Character getCharacter(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getCharacter());
    }

    @Override
    public char getCharValue(String name) {
        return getValue(name).getCharValue();
    }

    @Override
    public String getClassNameValue(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getClassNameValue());
    }

    @Override
    public Double getDouble(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getDouble());
    }

    @Override
    public double getDoubleValue(String name) {
        return getValue(name).getDoubleValue();
    }

    @Override
    public String getEnumClassName(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getEnumClassName());
    }

    @Override
    public String getEnumValue(String name) {
        AnnotationValueImpl annotationValue = getValue(name);

        return ((annotationValue == null) ? null : annotationValue.getEnumValue());
    }

    //

    //  because of the global state access.
    @Trivial
    public void log(TraceComponent logger) {

        Tr.debug(logger, MessageFormat.format("Annotation [ {0} ]", getAnnotationClassName()));

        // Only log the non-defaulted values.
        for (String valueName : this.values.keySet()) {
            AnnotationValueImpl nextValue = getValue(valueName);

            String valueEnumClassName = nextValue.getEnumClassName();
            Object valueValue = nextValue.getObjectValue();
            // works for all value types, e.g., Object, Enumerated (as String), Class, Array

            if (valueEnumClassName != null) {
                Tr.debug(logger,
                         MessageFormat.format("  Value: Name [ {0} ] Enum Type [ {1} ] Value [ {2} ]",
                                              new Object[] { valueName, valueEnumClassName, valueValue }));
            } else {
                Tr.debug(logger, MessageFormat.format("  Value: Name [ {0} ] Value [ {1} ]",
                                                      new Object[] { valueName, valueValue }));
            }
        }
    }
}
