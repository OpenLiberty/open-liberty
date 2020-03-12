/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates and others.
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
 * A convenient class for writing an ELResolver to do custom type conversions.
 *
 * <p>
 * For example, to convert a String to an instance of MyDate, one can write
 *
 * <pre>
 * <code>
 *     ELProcessor elp = new ELProcessor();
 *     elp.getELManager().addELResolver(new TypeConverter() {
 *         Object convertToType(ELContext context, Object obj, Class&lt;?&gt; type) {
 *             if ((obj instanceof String) &amp;&amp; type == MyDate.class) {
 *                 context.setPropertyResolved(obj, type);
 *                 return (obj == null)? null: new MyDate(obj.toString());
 *             }
 *             return null;
 *         }
 *      };
 * </code>
 * </pre>
 *
 * @since Jakarta Expression Language 3.0
 */
public abstract class TypeConverter extends ELResolver {

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    /**
     * Converts an object to a specific type.
     *
     * <p>
     * An <code>ELException</code> is thrown if an error occurs during the conversion.
     * </p>
     *
     * @param context The context of this evaluation.
     * @param obj The object to convert.
     * @param targetType The target type for the conversion.
     * @throws ELException thrown if errors occur.
     */
    @Override
    abstract public Object convertToType(ELContext context, Object obj, Class<?> targetType);
}
