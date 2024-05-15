/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.v1_0;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.version.DataVersionCompatibility;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
@Component(configurationPid = "io.openliberty.data.internal.version.1.0",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataVersionCompatibility.class)
public class Data_1_0 implements DataVersionCompatibility {
    @Override
    @Trivial
    public StringBuilder appendCondition(StringBuilder q, int qp,
                                         Method method, int p,
                                         String o_, String attrName,
                                         boolean isCollection, Annotation[] annos) {
        return q.append(o_).append(attrName).append("=?").append(qp);
    }

    @Override
    @Trivial
    public StringBuilder appendConditionsForIdClass(StringBuilder q, int qp,
                                                    Method method, int p,
                                                    String o_, String[] idClassAttrNames,
                                                    Annotation[] annos) {
        q.append('(');

        int count = 0;
        for (String name : idClassAttrNames) {
            if (count != 0)
                q.append(" AND ");

            q.append(o_).append(name).append("=?").append(count++ + qp);
        }

        q.append(')');

        return q;
    }

    @Override
    @Trivial
    public Annotation getCountAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return null;
    }

    @Override
    @Trivial
    public String[] getSelections(Method method) {
        return null;
    }

    @Override
    @Trivial
    public String[] getUpdateAttributeAndOperation(Annotation[] annos) {
        throw new UnsupportedOperationException(); // unreachable
    }

    @Override
    @Trivial
    public boolean hasOrAnnotation(Annotation[] annos) {
        return false;
    }
}