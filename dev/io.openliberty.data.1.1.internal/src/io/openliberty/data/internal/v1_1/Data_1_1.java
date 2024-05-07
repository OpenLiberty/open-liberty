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
package io.openliberty.data.internal.v1_1;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.version.DataVersionCompatibility;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.function.AbsoluteValue;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.ElementCount;
import io.openliberty.data.repository.function.Extract;
import io.openliberty.data.repository.function.IgnoreCase;
import io.openliberty.data.repository.function.Not;
import io.openliberty.data.repository.function.Rounded;
import io.openliberty.data.repository.function.Trimmed;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Assign;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
@Component(configurationPid = "io.openliberty.data.internal.version.1.1",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataVersionCompatibility.class)
public class Data_1_1 implements DataVersionCompatibility {
    private static final Map<String, String> FUNCTION_CALLS = new HashMap<>();
    static {
        FUNCTION_CALLS.put(AbsoluteValue.class.getSimpleName(), "ABS(");
        FUNCTION_CALLS.put(CharCount.class.getSimpleName(), "LENGTH(");
        FUNCTION_CALLS.put(ElementCount.class.getSimpleName(), "SIZE(");
        FUNCTION_CALLS.put(IgnoreCase.class.getSimpleName(), "LOWER(");
        FUNCTION_CALLS.put(Not.class.getSimpleName(), "NOT(");
        FUNCTION_CALLS.put(Rounded.Direction.DOWN.name(), "FLOOR(");
        FUNCTION_CALLS.put(Rounded.Direction.NEAREST.name(), "ROUND(");
        FUNCTION_CALLS.put(Rounded.Direction.UP.name(), "CEILING(");
        FUNCTION_CALLS.put(Trimmed.class.getSimpleName(), "TRIM(");
        FUNCTION_CALLS.put(Extract.Field.DAY.name(), "EXTRACT (DAY FROM ");
        FUNCTION_CALLS.put(Extract.Field.HOUR.name(), "EXTRACT (HOUR FROM ");
        FUNCTION_CALLS.put(Extract.Field.MINUTE.name(), "EXTRACT (MINUTE FROM ");
        FUNCTION_CALLS.put(Extract.Field.MONTH.name(), "EXTRACT (MONTH FROM ");
        FUNCTION_CALLS.put(Extract.Field.QUARTER.name(), "EXTRACT (QUARTER FROM ");
        FUNCTION_CALLS.put(Extract.Field.SECOND.name(), "EXTRACT (SECOND FROM ");
        FUNCTION_CALLS.put(Extract.Field.WEEK.name(), "EXTRACT (WEEK FROM ");
        FUNCTION_CALLS.put(Extract.Field.YEAR.name(), "EXTRACT (YEAR FROM ");
    }

    @Override
    @Trivial
    public Annotation getCountAnnotation(Method method) {
        return method.getAnnotation(Count.class);
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return method.getAnnotation(Exists.class);
    }

    @Override
    @Trivial
    public String getFunctionCall(Annotation anno) {
        String functionType = anno instanceof Extract ? ((Extract) anno).value().name() //
                        : anno instanceof Rounded ? ((Rounded) anno).value().name() //
                                        : anno.annotationType().getSimpleName();

        String functionCall = FUNCTION_CALLS.get(functionType);

        if (functionCall == null)
            throw new UnsupportedOperationException(anno.toString()); // should never occur
        return functionCall;
    }

    @Override
    @Trivial
    public String[] getSelections(Method method) {
        Annotation select = method.getAnnotation(Select.class);
        return select == null ? null : ((Select) select).value();
    }

    @Override
    @Trivial
    public String[] getUpdateAttributeAndOperation(Annotation anno) {
        if (anno instanceof Assign) {
            return new String[] { ((Assign) anno).value(), "=" };
        } else if (anno instanceof Add) {
            return new String[] { ((Add) anno).value(), "+" };
        } else if (anno instanceof Multiply) {
            return new String[] { ((Multiply) anno).value(), "*" };
        } else if (anno instanceof Divide) {
            return new String[] { ((Divide) anno).value(), "/" };
        } else if (anno instanceof SubtractFrom) {
            return new String[] { ((SubtractFrom) anno).value(), "-" };
        } else { // should be unreachable
            throw new UnsupportedOperationException(anno.toString());
        }
    }

    @Override
    @Trivial
    public boolean roundToNearest(Annotation anno) {
        return anno instanceof Rounded && ((Rounded) anno).value() == Rounded.Direction.NEAREST;
    }
}