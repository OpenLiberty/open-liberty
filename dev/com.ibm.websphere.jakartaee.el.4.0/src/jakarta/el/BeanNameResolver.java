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

/**
 * Resolves a bean by its known name. This class can be extended to return a bean object given its name, to set a value
 * to an existing bean, or to create a bean with the value.
 *
 * @see BeanNameELResolver
 *
 * @since Jakarta Expression Language 3.0
 */
public abstract class BeanNameResolver {

    /**
     * Returns whether the given name is resolved by the BeanNameResolver
     *
     * @param beanName The name of the bean.
     * @return true if the name is resolved by this BeanNameResolver; false otherwise.
     */
    public boolean isNameResolved(String beanName) {
        return false;
    }

    /**
     * Returns the bean known by its name.
     *
     * @param beanName The name of the bean.
     * @return The bean with the given name. Can be <code>null</code>.
     *
     */
    public Object getBean(String beanName) {
        return null;
    }

    /**
     * Sets a value to a bean of the given name. If the bean of the given name does not exist and if {@link #canCreateBean}
     * is <code>true</code>, one is created with the given value.
     *
     * @param beanName The name of the bean
     * @param value The value to set the bean to. Can be <code>null</code>.
     * @throws PropertyNotWritableException if the bean cannot be modified or created.
     */
    public void setBeanValue(String beanName, Object value) throws PropertyNotWritableException {
        throw new PropertyNotWritableException();
    }

    /**
     * Indicates if the bean of the given name is read-only or writable
     *
     * @param beanName The name of the bean
     * @return <code>true</code> if the bean can be set to a new value. <code>false</code> otherwise.
     */
    public boolean isReadOnly(String beanName) {
        return true;
    }

    /**
     * Allow creating a bean of the given name if it does not exist.
     *
     * @param beanName The name of the bean
     * @return <code>true</code> if bean creation is supported <code>false</code> otherwise.
     */
    public boolean canCreateBean(String beanName) {
        return false;
    }
}
