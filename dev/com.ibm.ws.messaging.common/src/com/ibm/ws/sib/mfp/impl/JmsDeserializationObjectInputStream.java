/* *****************************************************************************
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
 * *****************************************************************************/
package com.ibm.ws.sib.mfp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.register;
import static com.ibm.ws.sib.utils.ras.SibTr.warning;
import static com.ibm.wsspi.kernel.service.jms.JmsDeserializationContext.currentFilter;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_BUNDLE;
import static com.ibm.ws.sib.mfp.MfpConstants.MSG_GROUP;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.serialization.DeserializationObjectInputStream;

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
public final class JmsDeserializationObjectInputStream extends DeserializationObjectInputStream {

    private static final TraceComponent tc = register(JmsDeserializationObjectInputStream.class, MSG_GROUP, MSG_BUNDLE);

    public JmsDeserializationObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(in, classLoader);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
        Class<?> resolvedClass = super.resolveClass(desc);

        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Applying deserialization filter to class '" + desc.getName() + "'");

        if (currentFilter().test(resolvedClass)) return resolvedClass;

        // ACCESS DENIED!
        warning(tc, "Class '" + desc.getName() + "' rejected by deserialization filter");
        throw new ClassNotFoundException(desc.getName() + " rejected by JMS deserialization filter");
    }
}
