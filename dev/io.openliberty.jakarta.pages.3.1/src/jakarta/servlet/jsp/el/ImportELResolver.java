/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation.
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
package jakarta.servlet.jsp.el;

import jakarta.el.ELContext;
import jakarta.el.ELClass;
import jakarta.el.ELResolver;
import jakarta.el.ELException;

/**
 * Defines variable resolution behavior for Class imports and static imports.
 *
 * @since JSP 3.1
 */
public class ImportELResolver extends ELResolver {

    /**
     * If the base object is <code>null</code>, searches the Class and static imports for an import with the given name
     * and returns it if an import exists with the given name.
     *
     * <p>
     * The <code>propertyResolved</code> property of the <code>ELContext</code> object must be set to <code>true</code>
     * by this resolver before returning if an import is matched. If this property is not <code>true</code> after this
     * method is called, the caller should ignore the return value.
     * </p>
     *
     * @param context  The context of this evaluation.
     * @param base     Only <code>null</code> is handled by this resolver. Other values will result in an immediate
     *                 return.
     * @param property The name of the import to resolve.
     * @return If the <code>propertyResolved</code> property of <code>ELContext</code> was set to <code>true</code>,
     *         then the import; otherwise undefined.
     * @throws NullPointerException if context is <code>null</code>
     * @throws ELException          if an exception was thrown while performing the property or variable resolution. The
     *                              thrown exception must be included as the cause property of this exception, if
     *                              available.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {

        if (context == null) {
            throw new NullPointerException();
        }

        // check to see if the property is an imported class
        if (base == null && property instanceof String && context.getImportHandler() != null) {
            String attribute = (String) property;
            Object value = null;
            Class<?> c = context.getImportHandler().resolveClass(attribute);
            if (c != null) {
                value = new ELClass(c);
                // A possible optimization is to set the ELClass
                // instance in an attribute map.
            }

            if (value == null) {
                // This might be an imported static field
                c = context.getImportHandler().resolveStatic(attribute);
                if (c != null) {
                    try {
                        value = c.getField(attribute).get(null);
                    } catch (IllegalArgumentException | IllegalAccessException |
                            NoSuchFieldException | SecurityException e) {
                        // Most (all?) of these should have been
                        // prevented by the checks when the import
                        // was defined.
                    }
                }
            }

            if (value != null) {
                context.setPropertyResolved(true);
            }
            return value;
        }
        return null;
    }

    /**
     * Always returns {@code null} since in normal usage {@link ScopedAttributeELResolver} will handle calls to
     * {@link ELResolver#getType(ELContext, Object, Object)}.
     *
     * @param context  The context of this evaluation.
     * @param base     Ignored
     * @param property Ignored
     * @return Always {@code null}
     * @throws NullPointerException if context is <code>null</code>
     * @throws ELException          if an exception was thrown while performing the property or variable resolution. The
     *                              thrown exception must be included as the cause property of this exception, if
     *                              available.
     */
    @Override
    public Class<Object> getType(ELContext context, Object base, Object property) {

        if (context == null) {
            throw new NullPointerException();
        }

        return null;
    }

    /**
     * Always a NO-OP since in normal usage {@link ScopedAttributeELResolver} will handle calls to
     * {@link ELResolver#setValue(ELContext, Object, Object, Object)}.
     * 
     * @param context  The context of this evaluation.
     * @param base     Ignored
     * @param property Ignored
     * @param val      Ignored
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException          if an exception was thrown while performing the property or variable resolution. The
     *                              thrown exception must be included as the cause property of this exception, if
     *                              available.
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object val) {
        if (context == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Always returns {@code false} since in normal usage {@link ScopedAttributeELResolver} will handle calls to
     * {@link ELResolver#isReadOnly(ELContext, Object, Object)}.
     *
     * @param context  The context of this evaluation.
     * @param base     Ignored
     * @param property Ignored
     * @return Always {@code false}
     * @throws NullPointerException if context is <code>null</code>.
     * @throws ELException          if an exception was thrown while performing the property or variable resolution. The
     *                              thrown exception must be included as the cause property of this exception, if
     *                              available.
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        if (context == null) {
            throw new NullPointerException();
        }
        return false;
    }

    /**
     * Always returns {@code null} since in normal usage {@link ScopedAttributeELResolver} will handle calls to
     * {@link ELResolver#getCommonPropertyType(ELContext, Object)}.
     *
     * @param context Ignored
     * @param base    Ignored
     * 
     * @return Always {@code null}
     */
    @Override
    public Class<String> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }
}
