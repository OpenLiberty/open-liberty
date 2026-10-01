/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.sib.mfp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.jms.JmsDeserializationContext;

import java.util.function.Predicate;

/**
 * A subclass of {@link DeserializationObjectInputStream} that adds class-level
 * filtering for JMS {@code ObjectMessage} deserialization.
 *
 * <p>Filtering is controlled dynamically per-thread using
 * {@link com.ibm.wsspi.kernel.service.jms.JmsDeserializationContext}.
 * Callers install a {@link java.util.function.Predicate} using try-with-resources
 * before calling {@code ObjectMessage.getObject()}:
 *
 * <pre>
 * try (JmsDeserializationContext.FilterHandle h =
 *         JmsDeserializationContext.INSTANCE.setFilter(clazz -&gt; isPermitted(clazz))) {
 *     return objectMessage.getObject();
 * }
 * </pre>
 *
 * <p>When no filter is set on the calling thread, all classes are permitted
 * (fully backward-compatible default).
 */
public class JmsDeserializationObjectInputStream extends DeserializationObjectInputStream {

    private static final TraceComponent tc = SibTr.register(
            JmsDeserializationObjectInputStream.class,
            MfpConstants.MSG_GROUP,
            MfpConstants.MSG_BUNDLE);

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public JmsDeserializationObjectInputStream(InputStream in, ClassLoader classLoader)
            throws IOException {
        super(in, classLoader);
    }

    // -----------------------------------------------------------------------
    // resolveClass override — filtering logic
    // -----------------------------------------------------------------------

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws ClassNotFoundException {

        Class<?> resolvedClass = super.resolveClass(desc);

        Predicate<Class<?>> filter = JmsDeserializationContext.currentFilter();
        if (filter != null && !filter.test(resolvedClass)) {
            String className = desc.getName();
            SibTr.warning(tc, "Class '" + className + "' rejected by deserialization filter");
            throw new ClassNotFoundException(className + " rejected by JMS deserialization filter");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "Class '" + desc.getName() + "' permitted for deserialization");
        }

        return resolvedClass;
    }
}
