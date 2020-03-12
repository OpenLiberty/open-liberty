/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Defines property resolution behavior on arrays.
 *
 * <p>
 * This resolver handles base objects that are Java language arrays. It accepts any object as a property and coerces
 * that object into an integer index into the array. The resulting value is the value in the array at that index.
 * </p>
 *
 * <p>
 * This resolver can be constructed in read-only mode, which means that {@link #isReadOnly} will always return
 * <code>true</code> and {@link #setValue} will always throw <code>PropertyNotWritableException</code>.
 * </p>
 *
 * <p>
 * <code>ELResolver</code>s are combined together using {@link CompositeELResolver}s, to define rich semantics for
 * evaluating an expression. See the javadocs for {@link ELResolver} for details.
 * </p>
 *
 * @see CompositeELResolver
 * @see ELResolver
 * 
 * @since Jakarta Server Pages 2.1
 */
public class ArrayELResolver extends ELResolver {

    /**
     * Creates a new read/write <code>ArrayELResolver</code>.
     */
    public ArrayELResolver() {
        this.isReadOnly = false;
    }

    /**
     * Creates a new <code>ArrayELResolver</code> whose read-only status is determined by the given parameter.
     *
     * @param isReadOnly <code>true</code> if this resolver cannot modify arrays; <code>false</code> otherwise.
     */
    public ArrayELResolver(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    /**
     * If the base object is an array, returns the most general acceptable type for a value in this array.
     *
     * <p>
     * If the base is a <code>array</code>, the <code>propertyResolved</code> property of the <code>ELContext</code> object
     * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
     * this method is called, the caller should ignore the return value.
     * </p>
     *
     * <p>
     * Assuming the base is an <code>array</code>, this method will always return
     * <code>base.getClass().getComponentType()</code>, which is the most general type of component that can be stored at
     * any given index in the array.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to analyze. Only bases that are Java language arrays are handled by this resolver.
     * @param property The index of the element in the array to return the acceptable type for. Will be coerced into an
     * integer, but otherwise ignored by this resolver.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * the most general acceptable type; otherwise undefined.
     * @throws PropertyNotFoundException if the given index is out of bounds for this array.
     * @throws NullPointerException if context is <code>null</code>
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {

        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null && base.getClass().isArray()) {
            context.setPropertyResolved(true);
            int index = toInteger(property);
            if (index < 0 || index >= Array.getLength(base)) {
                throw new PropertyNotFoundException();
            }
            return base.getClass().getComponentType();
        }
        return null;
    }

    /**
     * If the base object is a Java language array, returns the value at the given index. The index is specified by the
     * <code>property</code> argument, and coerced into an integer. If the coercion could not be performed, an
     * <code>IllegalArgumentException</code> is thrown. If the index is out of bounds, <code>null</code> is returned.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code> property of the <code>ELContext</code> object
     * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
     * this method is called, the caller should ignore the return value.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to analyze. Only bases that are Java language arrays are handled by this resolver.
     * @param property The index of the value to be returned. Will be coerced into an integer.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * the value at the given index or <code>null</code> if the index was out of bounds. Otherwise, undefined.
     * @throws IllegalArgumentException if the property could not be coerced into an integer.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {

        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null && base.getClass().isArray()) {
            context.setPropertyResolved(base, property);
            int index = toInteger(property);
            if (index >= 0 && index < Array.getLength(base)) {
                return Array.get(base, index);
            }
        }
        return null;
    }

    /**
     * If the base object is a Java language array, attempts to set the value at the given index with the given value. The
     * index is specified by the <code>property</code> argument, and coerced into an integer. If the coercion could not be
     * performed, an <code>IllegalArgumentException</code> is thrown. If the index is out of bounds, a
     * <code>PropertyNotFoundException</code> is thrown.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code> property of the <code>ELContext</code> object
     * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
     * this method is called, the caller can safely assume no value was set.
     * </p>
     *
     * <p>
     * If this resolver was constructed in read-only mode, this method will always throw
     * <code>PropertyNotWritableException</code>.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to be modified. Only bases that are Java language arrays are handled by this resolver.
     * @param property The index of the value to be set. Will be coerced into an integer.
     * @param val The value to be set at the given index.
     * @throws ClassCastException if the class of the specified element prevents it from being added to this array.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws IllegalArgumentException if the property could not be coerced into an integer, or if some aspect of the
     * specified element prevents it from being added to this array.
     * @throws PropertyNotWritableException if this resolver was constructed in read-only mode.
     * @throws PropertyNotFoundException if the given index is out of bounds for this array.
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object val) {

        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null && base.getClass().isArray()) {
            context.setPropertyResolved(base, property);
            if (isReadOnly) {
                throw new PropertyNotWritableException();
            }
            Class<?> type = base.getClass().getComponentType();
            if (val != null && !type.isAssignableFrom(val.getClass())) {
                throw new ClassCastException();
            }
            int index = toInteger(property);
            if (index < 0 || index >= Array.getLength(base)) {
                throw new PropertyNotFoundException();
            }
            Array.set(base, index, val);
        }
    }

    /**
     * If the base object is a Java language array, returns whether a call to {@link #setValue} will always fail.
     *
     * <p>
     * If the base is a Java language array, the <code>propertyResolved</code> property of the <code>ELContext</code> object
     * must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code> after
     * this method is called, the caller should ignore the return value.
     * </p>
     *
     * <p>
     * If this resolver was constructed in read-only mode, this method will always return <code>true</code>. Otherwise, it
     * returns <code>false</code>.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to analyze. Only bases that are a Java language array are handled by this resolver.
     * @param property The index of the element in the array to return the acceptable type for. Will be coerced into an
     * integer, but otherwise ignored by this resolver.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * <code>true</code> if calling the <code>setValue</code> method will always fail or <code>false</code> if it is
     * possible that such a call may succeed; otherwise undefined.
     * @throws PropertyNotFoundException if the given index is out of bounds for this array.
     * @throws NullPointerException if context is <code>null</code>
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {

        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null && base.getClass().isArray()) {
            context.setPropertyResolved(true);
            int index = toInteger(property);
            if (index < 0 || index >= Array.getLength(base)) {
                throw new PropertyNotFoundException();
            }
        }
        return isReadOnly;
    }

    /**
     * Always returns <code>null</code>, since there is no reason to iterate through set set of all integers.
     *
     * <p>
     * The {@link #getCommonPropertyType} method returns sufficient information about what properties this resolver accepts.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to analyze. Only bases that are a Java language array are handled by this resolver.
     * @return <code>null</code>.
     */
    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    /**
     * If the base object is a Java language array, returns the most general type that this resolver accepts for the
     * <code>property</code> argument. Otherwise, returns <code>null</code>.
     *
     * <p>
     * Assuming the base is an array, this method will always return <code>Integer.class</code>. This is because arrays
     * accept integers for their index.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base The array to analyze. Only bases that are a Java language array are handled by this resolver.
     * @return <code>null</code> if base is not a Java language array; otherwise <code>Integer.class</code>.
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {

        if (base != null && base.getClass().isArray()) {
            return Integer.class;
        }
        return null;
    }

    private int toInteger(Object p) {

        if (p instanceof Integer) {
            return ((Integer) p).intValue();
        }
        if (p instanceof Character) {
            return ((Character) p).charValue();
        }
        if (p instanceof Boolean) {
            return ((Boolean) p).booleanValue() ? 1 : 0;
        }
        if (p instanceof Number) {
            return ((Number) p).intValue();
        }
        if (p instanceof String) {
            return Integer.parseInt((String) p);
        }
        throw new IllegalArgumentException();
    }

    private boolean isReadOnly;
}
