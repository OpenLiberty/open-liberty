/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.version.DataVersionCompatibility;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Is;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.function.AbsoluteValue;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.ElementCount;
import io.openliberty.data.repository.function.Extract;
import io.openliberty.data.repository.function.Rounded;
import io.openliberty.data.repository.function.Trimmed;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Assign;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Find;
import jakarta.data.repository.Select;

/**
 * Capability that is specific to the version of Jakarta Data.
 */
@Component(configurationPid = "io.openliberty.data.internal.version.1.1",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataVersionCompatibility.class)
public class Data_1_1 implements DataVersionCompatibility {
    private static final TraceComponent tc = Tr.register(Data_1_1.class);

    private static final String FUNCTION_ANNO_PACKAGE = Rounded.class.getPackageName();

    private static final Map<String, String> FUNCTION_CALLS = new HashMap<>();
    static {
        FUNCTION_CALLS.put(AbsoluteValue.class.getSimpleName(), "ABS(");
        FUNCTION_CALLS.put(CharCount.class.getSimpleName(), "LENGTH(");
        FUNCTION_CALLS.put(ElementCount.class.getSimpleName(), "SIZE(");
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

    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = //
                    Set.of(Limit.class, Order.class,
                           Sort.class, Sort[].class,
                           PageRequest.class // TODO , Restriction.class
                    );

    @Override
    @Trivial
    public StringBuilder appendCondition(StringBuilder q, int qp,
                                         Method method, int p,
                                         String o_, String attrName,
                                         boolean isCollection, Annotation[] annos) {
        StringBuilder attributeExpr = new StringBuilder();

        Is.Op comparison = Is.Op.Equal;
        List<Annotation> functionAnnos = new ArrayList<>();
        for (int a = annos.length - 1; a >= 0; a--) {
            if (annos[a] instanceof Is) {
                comparison = ((Is) annos[a]).value();
            } else {
                String annoPackage = annos[a].annotationType().getPackageName();
                if (FUNCTION_ANNO_PACKAGE.equals(annoPackage)) {
                    functionAnnos.add(annos[a]);
                    String functionType = annos[a] instanceof Extract ? ((Extract) annos[a]).value().name() //
                                    : annos[a] instanceof Rounded ? ((Rounded) annos[a]).value().name() //
                                                    : annos[a].annotationType().getSimpleName();
                    String functionCall = FUNCTION_CALLS.get(functionType);
                    attributeExpr.append(functionCall);
                }
            }
        }

        Is.Op baseOp = comparison.base();
        boolean ignoreCase = comparison.ignoreCase();
        boolean negated = comparison.isNegative();

        if (ignoreCase)
            attributeExpr.append("LOWER(");

        if (attrName.charAt(attrName.length() - 1) != ')')
            attributeExpr.append(o_);

        attributeExpr.append(attrName);

        if (ignoreCase)
            attributeExpr.append(')');

        for (Annotation anno : functionAnnos) {
            if (anno instanceof Rounded && ((Rounded) anno).value() == Rounded.Direction.NEAREST)
                attributeExpr.append(", 0)"); // round to zero digits beyond the decimal
            else
                attributeExpr.append(')');
        }

        if (isCollection)
            if (ignoreCase ||
                baseOp != Is.Op.Equal) // TODO also have an operation for collection containing?
                throw new UnsupportedOperationException("The " + comparison.name() +
                                                        " comparison that is applied to entity attribute " +
                                                        attrName +
                                                        " is not supported for collection attributes."); // TODO NLS (future)

        switch (baseOp) {
            case Equal:
                q.append(attributeExpr).append(negated ? "<>" : '=');
                appendParam(q, ignoreCase, qp);
                break;
            case GreaterThan:
                q.append(attributeExpr).append('>');
                appendParam(q, ignoreCase, qp);
                break;
            case GreaterThanEqual:
                q.append(attributeExpr).append(">=");
                appendParam(q, ignoreCase, qp);
                break;
            case In:
                if (ignoreCase)
                    throw new UnsupportedOperationException(); // should be unreachable
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" IN ");
                appendParam(q, ignoreCase, qp);
                break;
            case LessThan:
                q.append(attributeExpr).append('<');
                appendParam(q, ignoreCase, qp);
                break;
            case LessThanEqual:
                q.append(attributeExpr).append("<=");
                appendParam(q, ignoreCase, qp);
                break;
            // TODO operation for collection containing?
            //case ???:
            //    q.append(" ?").append(qp) //
            //                    .append(negated ? " NOT" : "") //
            //                    .append(" MEMBER OF ").append(attributeExpr);
            //    break;
            case Like:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE ");
                appendParam(q, ignoreCase, qp);
                break;
            case Prefixed:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE CONCAT(");
                appendParam(q, ignoreCase, qp).append(", '%')");
                break;
            case Substringed:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE CONCAT('%', ");
                appendParam(q, ignoreCase, qp).append(", '%')");
                break;
            case Suffixed:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE CONCAT('%', ");
                appendParam(q, ignoreCase, qp).append(')');
                break;
            default:
                throw new UnsupportedOperationException(comparison.name());
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
            if (annos[a] instanceof Is) {
                Is.Op comparison = ((Is) annos[a]).value();
                if (comparison.base() != Is.Op.Equal)
                    throw new MappingException("The " + annos[a] +
                                               " annotation cannot be applied to a parameter of the " +
                                               method.getName() + " method of the " +
                                               method.getDeclaringClass().getName() +
                                               " repository because the parameter type is an IdClass."); // TODO NLS
                ignoreCase = comparison.ignoreCase();
                if (comparison.isNegative())
                    q.append(" NOT ");
            } else {
                String annoPackage = annos[a].annotationType().getPackageName();
                if (FUNCTION_ANNO_PACKAGE.equals(annoPackage))
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
    public boolean atLeast(int major, int minor) {
        return major == 1 && minor <= 1;
    }

    @Override
    @Trivial
    public Annotation getCountAnnotation(Method method) {
        return method.getAnnotation(Count.class);
    }

    @Override
    @Trivial
    public Class<?> getEntityClass(Find find) {
        return find.value();
    }

    @Override
    @Trivial
    public Annotation getExistsAnnotation(Method method) {
        return method.getAnnotation(Exists.class);
    }

    @Override
    @Trivial
    public String[] getSelections(AnnotatedElement element) {
        Annotation select = element.getAnnotation(Select.class);
        return select == null ? null : ((Select) select).value();
    }

    @Override
    @Trivial
    public String[] getUpdateAttributeAndOperation(Annotation[] annos) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String[] returnValue = null;
        for (Annotation anno : annos)
            if (anno instanceof Assign) {
                returnValue = new String[] { ((Assign) anno).value(), "=" };
                break;
            } else if (anno instanceof Add) {
                returnValue = new String[] { ((Add) anno).value(), "+" };
                break;
            } else if (anno instanceof Multiply) {
                returnValue = new String[] { ((Multiply) anno).value(), "*" };
                break;
            } else if (anno instanceof Divide) {
                returnValue = new String[] { ((Divide) anno).value(), "/" };
                break;
            } else if (anno instanceof SubtractFrom) {
                returnValue = new String[] { ((SubtractFrom) anno).value(), "-" };
                break;
            }

        if (trace && tc.isDebugEnabled()) {
            Object[] aa = new Object[annos.length];
            for (int a = 0; a < annos.length; a++)
                aa[a] = annos[a] == null ? null : annos[a].annotationType().getName();
            Tr.debug(this, tc, "getUpdateAttributeAndOperation", aa, returnValue);
        }
        return returnValue;
    }

    @Override
    @Trivial
    public boolean hasOrAnnotation(Annotation[] annos) {
        for (Annotation anno : annos)
            if (anno instanceof Or)
                return true;
        return false;
    }

    @Override
    @Trivial
    public String specialParamsForFind() {
        return "Limit, Order, Sort, Sort[], PageRequest, Restriction";
    }

    @Override
    @Trivial
    public String specialParamsForFindAndDelete() {
        return "Limit, Order, Sort, Sort[], Restriction";
    }

    @Override
    @Trivial
    public Set<Class<?>> specialParamTypes() {
        return SPECIAL_PARAM_TYPES;
    }
}