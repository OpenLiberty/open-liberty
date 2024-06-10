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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.version.DataVersionCompatibility;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.comparison.Contains;
import io.openliberty.data.repository.comparison.EndsWith;
import io.openliberty.data.repository.comparison.GreaterThan;
import io.openliberty.data.repository.comparison.GreaterThanEqual;
import io.openliberty.data.repository.comparison.In;
import io.openliberty.data.repository.comparison.LessThan;
import io.openliberty.data.repository.comparison.LessThanEqual;
import io.openliberty.data.repository.comparison.Like;
import io.openliberty.data.repository.comparison.StartsWith;
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
import jakarta.data.exceptions.MappingException;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
@Component(configurationPid = "io.openliberty.data.internal.version.1.1",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataVersionCompatibility.class)
public class Data_1_1 implements DataVersionCompatibility {
    private static final String COMPARISON_ANNO_PACKAGE = In.class.getPackageName();
    private static final String FUNCTION_ANNO_PACKAGE = Rounded.class.getPackageName();

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
    public StringBuilder appendCondition(StringBuilder q, int qp,
                                         Method method, int p,
                                         String o_, String attrName,
                                         boolean isCollection, Annotation[] annos) {
        boolean ignoreCase = false;
        StringBuilder attributeExpr = new StringBuilder();

        Annotation comparisonAnno = null;
        List<Annotation> functionAnnos = new ArrayList<>();
        for (int a = annos.length - 1; a >= 0; a--) {
            String annoPackage = annos[a].annotationType().getPackageName();
            if (COMPARISON_ANNO_PACKAGE.equals(annoPackage)) {
                if (comparisonAnno == null)
                    comparisonAnno = annos[a];
                else
                    throw new MappingException("The " + Set.of(comparisonAnno, annos[a]) +
                                               " annotations cannot be combined on parameter " + (p + 1) + " of the " +
                                               method.getName() + " method of the " +
                                               method.getDeclaringClass().getName() + " repository."); // TODO NLS
            } else if (FUNCTION_ANNO_PACKAGE.equals(annoPackage)) {
                functionAnnos.add(annos[a]);
                String functionType = annos[a] instanceof Extract ? ((Extract) annos[a]).value().name() //
                                : annos[a] instanceof Rounded ? ((Rounded) annos[a]).value().name() //
                                                : annos[a].annotationType().getSimpleName();
                String functionCall = FUNCTION_CALLS.get(functionType);
                ignoreCase |= "LOWER(".equals(functionCall);
                attributeExpr.append(functionCall);
            }
        }

        attributeExpr.append(o_).append(attrName);

        for (Annotation anno : functionAnnos) {
            if (anno instanceof Rounded && ((Rounded) anno).value() == Rounded.Direction.NEAREST)
                attributeExpr.append(", 0)"); // round to zero digits beyond the decimal
            else
                attributeExpr.append(')');
        }

        if (isCollection)
            if (comparisonAnno != null && !(comparisonAnno instanceof Contains) || ignoreCase)
                throw new MappingException(new UnsupportedOperationException("The parameter annotation " +
                                                                             (ignoreCase ? "IgnoreCase" : comparisonAnno.annotationType().getSimpleName()) +
                                                                             " which is applied to entity property " + attrName +
                                                                             " is not supported for collection properties.")); // TODO NLS (future)

        if (comparisonAnno == null) { // Equals
            q.append(attributeExpr).append('=');
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof GreaterThan) {
            q.append(attributeExpr).append('>');
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof GreaterThanEqual) {
            q.append(attributeExpr).append(">=");
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof LessThan) {
            q.append(attributeExpr).append('<');
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof LessThanEqual) {
            q.append(attributeExpr).append("<=");
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof Contains) {
            if (isCollection) {
                q.append(" ?").append(qp).append(" MEMBER OF ").append(attributeExpr);
            } else {
                q.append(attributeExpr).append(" LIKE CONCAT('%', ");
                appendParam(q, ignoreCase, qp).append(", '%')");
            }
        } else if (comparisonAnno instanceof Like) {
            q.append(attributeExpr).append(" LIKE ");
            appendParam(q, ignoreCase, qp);
        } else if (comparisonAnno instanceof StartsWith) {
            q.append(attributeExpr).append(" LIKE CONCAT(");
            appendParam(q, ignoreCase, qp).append(", '%')");
        } else if (comparisonAnno instanceof EndsWith) {
            q.append(attributeExpr).append(" LIKE CONCAT('%', ");
            appendParam(q, ignoreCase, qp).append(')');
        } else if (comparisonAnno instanceof In) {
            if (ignoreCase)
                throw new MappingException("The " + Set.of("IgnoreCase", "In") +
                                           " annotations cannot be combined on parameter " + (p + 1) + " of the " +
                                           method.getName() + " method of the " +
                                           method.getDeclaringClass().getName() + " repository."); // TODO NLS
            q.append(attributeExpr).append(" IN ");
            appendParam(q, ignoreCase, qp);
        } else {
            throw new UnsupportedOperationException(comparisonAnno.annotationType().toString());
        }

        return q;
    }

    @Override
    @Trivial
    public StringBuilder appendConditionsForIdClass(StringBuilder q, int qp,
                                                    Method method, int p,
                                                    String o_, String[] idClassAttrNames,
                                                    Annotation[] annos) {
        boolean ignoreCase = false;
        for (int a = annos.length - 1; a >= 0; a--) {
            String annoPackage = annos[a].annotationType().getPackageName();
            if (COMPARISON_ANNO_PACKAGE.equals(annoPackage)) {
                throw new MappingException("The " + annos[a].annotationType().getSimpleName() +
                                           " annotation cannot be applied to a parameter of the " +
                                           method.getName() + " method of the " +
                                           method.getDeclaringClass().getName() +
                                           " repository because the parameter type is an IdClass."); // TODO NLS
            } else if (annos[a] instanceof IgnoreCase) {
                ignoreCase = true;
            } else if (annos[a] instanceof Not) {
                q.append(" NOT ");
            } else if (FUNCTION_ANNO_PACKAGE.equals(annoPackage)) {
                throw new MappingException("The " + annos[a].annotationType().getSimpleName() +
                                           " annotation cannot be applied to a parameter of the " +
                                           method.getName() + " method of the " +
                                           method.getDeclaringClass().getName() +
                                           " repository because the parameter type is an IdClass."); // TODO NLS

            }
        }

        q.append('(');

        int count = 0;
        for (String name : idClassAttrNames) {
            if (count != 0)
                q.append(" AND ");

            if (ignoreCase)
                q.append("LOWER(").append(o_).append(name).append(')');
            else
                q.append(o_).append(name);

            q.append('=');
            appendParam(q, ignoreCase, count++ + qp);
        }

        q.append(')');

        return q;
    }

    /**
     * Appends JQPL for a repository method parameter. Either of the form ?1 or LOWER(?1)
     *
     * @param q     builder for the JPQL query.
     * @param lower indicates if the query parameter should be compared in lower case.
     * @param num   parameter number.
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private static StringBuilder appendParam(StringBuilder q, boolean lower, int num) {
        q.append(lower ? "LOWER(?" : '?').append(num);
        return lower ? q.append(')') : q;
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
    public String[] getSelections(Method method) {
        Annotation select = method.getAnnotation(Select.class);
        return select == null ? null : ((Select) select).value();
    }

    @Override
    public String[] getUpdateAttributeAndOperation(Annotation[] annos) {
        for (Annotation anno : annos)
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
            }
        return null;
    }

    @Override
    @Trivial
    public boolean hasOrAnnotation(Annotation[] annos) {
        for (Annotation anno : annos)
            if (anno instanceof Or)
                return true;
        return false;
    }
}