/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * <p>
 * An <code>ELResolver</code> for resolving user or container managed beans.
 * </p>
 * <p>
 * A {@link BeanNameResolver} is required for its proper operation. The following example creates an
 * <code>ELResolver</code> that resolves the name "bean" to an instance of MyBean.
 *
 * <pre>
 * <code>
 * ELResovler elr = new BeanNameELResolver(new BeanNameResolver {
 *    public boolean isNameResolved(String beanName) {
 *       return "bean".equals(beanName);
 *    }
 *    public Object getBean(String beanName) {
 *       return "bean".equals(beanName)? new MyBean(): null;
 *    }
 * });
 * </code>
 * </pre>
 *
 * @since Jakarta Expression Language 3.0
 */
public class BeanNameELResolver extends ELResolver {

    private BeanNameResolver beanNameResolver;

    /**
     * Constructor
     *
     * @param beanNameResolver The {@link BeanNameResolver} that resolves a bean name.
     */
    public BeanNameELResolver(BeanNameResolver beanNameResolver) {
        this.beanNameResolver = beanNameResolver;
    }

    /**
     * If the base object is <code>null</code> and the property is a name that is resolvable by the BeanNameResolver,
     * returns the value resolved by the BeanNameResolver.
     *
     * <p>
     * If name is resolved by the BeanNameResolver, the <code>propertyResolved</code> property of the <code>ELContext</code>
     * object must be set to <code>true</code> by this resolver, before returning. If this property is not <code>true</code>
     * after this method is called, the caller should ignore the return value.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>
     * @param property The name of the bean.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * the value of the bean with the given name. Otherwise, undefined.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property instanceof String) {
            if (beanNameResolver.isNameResolved((String) property)) {
                context.setPropertyResolved(base, property);
                return beanNameResolver.getBean((String) property);
            }
        }

        return null;
    }

    /**
     * If the base is null and the property is a name that is resolvable by the BeanNameResolver, the bean in the
     * BeanNameResolver is set to the given value.
     *
     * <p>
     * If the name is resolvable by the BeanNameResolver, or if the BeanNameResolver allows creating a new bean, the
     * <code>propertyResolved</code> property of the <code>ELContext</code> object must be set to <code>true</code> by the
     * resolver, before returning. If this property is not <code>true</code> after this method is called, the caller can
     * safely assume no value has been set.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>
     * @param property The name of the bean
     * @param value The value to set the bean with the given name to.
     * @throws NullPointerException if context is <code>null</code>
     * @throws PropertyNotWritableException if the BeanNameResolver does not allow the bean to be modified.
     * @throws ELException if an exception was thrown while attempting to set the bean with the given name. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property instanceof String) {
            String beanName = (String) property;
            if (beanNameResolver.isNameResolved(beanName) || beanNameResolver.canCreateBean(beanName)) {
                beanNameResolver.setBeanValue(beanName, value);
                context.setPropertyResolved(base, property);
            }
        }
    }

    /**
     * If the base is null and the property is a name resolvable by the BeanNameResolver, return the type of the bean.
     *
     * <p>
     * If the name is resolvable by the BeanNameResolver, the <code>propertyResolved</code> property of the
     * <code>ELContext</code> object must be set to <code>true</code> by the resolver, before returning. If this property is
     * not <code>true</code> after this method is called, the caller can safely assume no value has been set.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>
     * @param property The name of the bean.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * the type of the bean with the given name. Otherwise, undefined.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property instanceof String) {
            if (beanNameResolver.isNameResolved((String) property)) {
                context.setPropertyResolved(true);
                return beanNameResolver.getBean((String) property).getClass();
            }
        }

        return null;
    }

    /**
     * If the base is null and the property is a name resolvable by the BeanNameResolver, attempts to determine if the bean
     * is writable.
     *
     * <p>
     * If the name is resolvable by the BeanNameResolver, the <code>propertyResolved</code> property of the
     * <code>ELContext</code> object must be set to <code>true</code> by the resolver, before returning. If this property is
     * not <code>true</code> after this method is called, the caller can safely assume no value has been set.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>
     * @param property The name of the bean.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>, then
     * <code>true</code> if the property is read-only or <code>false</code> if not; otherwise undefined.
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException if an exception was thrown while performing the property or variable resolution. The thrown
     * exception must be included as the cause property of this exception, if available.
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property instanceof String) {
            if (beanNameResolver.isNameResolved((String) property)) {
                context.setPropertyResolved(true);
                return beanNameResolver.isReadOnly((String) property);
            }
        }

        return false;
    }

    /**
     * Always returns <code>null</code>, since there is no reason to iterate through a list of one element: bean name.
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>.
     * @return <code>null</code>.
     */
    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    /**
     * Always returns <code>String.class</code>, since a bean name is a String.
     *
     * @param context The context of this evaluation.
     * @param base <code>null</code>.
     * @return <code>String.class</code>.
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return String.class;
    }
}
