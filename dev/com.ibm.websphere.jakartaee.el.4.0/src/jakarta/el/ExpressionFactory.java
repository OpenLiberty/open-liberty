/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * Provides an implementation for creating and evaluating Jakarta Expression Language expressions.
 *
 * <p>
 * Classes that implement the Jakarta Expression Language expression language expose their functionality via this
 * abstract class. An implementation supports the following functionalities.
 *
 * <ul>
 *   <li>Parses a <code>String</code> into a {@link ValueExpression} or {@link MethodExpression} instance for later
 *   evaluation.</li>
 *   <li>Implements an <code>ELResolver</code> for query operators</li>
 *   <li>Provides a default type coercion</li>
 * </ul>
 *
 * <p>
 * The {@link #newInstance} method can be used to obtain an instance of the implementation. Technologies such as
 * Jakarta Server Pages and Jakarta Faces provide access to an implementation via factory methods.
 *
 * <p>
 * The {@link #createValueExpression} method is used to parse expressions that evaluate to values (both l-values and
 * r-values are supported). The {@link #createMethodExpression} method is used to parse expressions that evaluate to a
 * reference to a method on an object.
 *
 * <p>
 * Resolution of model objects is performed at evaluation time, via the {@link ELResolver} associated with the
 * {@link ELContext} passed to the <code>ValueExpression</code> or <code>MethodExpression</code>.
 *
 * <p>
 * The ELContext object also provides access to the {@link FunctionMapper} and {@link VariableMapper} to be used when
 * parsing the expression. Jakarta Expression Language function and variable mapping is performed at parse-time, and the
 * results are bound to the expression. Therefore, the {@link ELContext}, {@link FunctionMapper}, and
 * {@link VariableMapper} are not stored for future use and do not have to be <code>Serializable</code>.
 *
 * <p>
 * The <code>createValueExpression</code> and <code>createMethodExpression</code> methods must be thread-safe. That is,
 * multiple threads may call these methods on the same <code>ExpressionFactory</code> object simultaneously.
 * Implementations should synchronize access if they depend on transient state. Implementations should not, however,
 * assume that only one object of each <code>ExpressionFactory</code> type will be instantiated; global caching should
 * therefore be static.
 *
 * <p>
 * The <code>ExpressionFactory</code> must be able to handle the following types of input for the
 * <code>expression</code> parameter:
 * <ul>
 *   <li>Single expressions using the <code>${}</code> delimiter (e.g. <code>"${employee.lastName}"</code>).</li>
 *   <li>Single expressions using the <code>#{}</code> delimiter (e.g. <code>"#{employee.lastName}"</code>).</li>
 *   <li>Literal text containing no <code>${}</code> or <code>#{}</code> delimiters (e.g. <code>"John Doe"</code>).</li>
 *   <li>Multiple expressions using the same delimiter (e.g. <code>"${employee.firstName}${employee.lastName}"</code> or
 *   <code>"#{employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using the same delimiter (e.g.
 *   <code>"Name: ${employee.firstName} ${employee.lastName}"</code>).</li>
 * </ul>
 *
 * <p>
 * The following types of input are illegal and must cause an {@link ELException} to be thrown:
 * <ul>
 *   <li>Multiple expressions using different delimiters (e.g.
 *   <code>"${employee.firstName}#{employee.lastName}"</code>).</li>
 *   <li>Mixed literal text and expressions using different delimiters(e.g.
 *   <code>"Name: ${employee.firstName} #{employee.lastName}"</code>).</li>
 * </ul>
 *
 * @since Jakarta Server Pages 2.1
 */
public abstract class ExpressionFactory {

    /**
     * Creates a new instance of a <code>ExpressionFactory</code>. This method uses the following ordered lookup procedure
     * to determine the <code>ExpressionFactory</code> implementation class to load:
     *
     * <ul>
     * <li>Use the Services API (as detailed in the JAR specification). If a resource with the name of
     * <code>META-INF/services/jakarta.el.ExpressionFactory</code> exists, then its first line, if present, is used as the
     * UTF-8 encoded name of the implementation class.</li>
     * <li>Use the properties file "lib/el.properties" in the JRE directory. If this file exists and it is readable by the
     * <code> java.util.Properties.load(InputStream)</code> method, and it contains an entry whose key is
     * "jakarta.el.ExpressionFactory", then the value of that entry is used as the name of the implementation class.</li>
     * <li>Use the <code>jakarta.el.ExpressionFactory</code> system property. If a system property with this name is defined,
     * then its value is used as the name of the implementation class.</li>
     * <li>Use a platform default implementation.</li>
     * </ul>
     *
     * @return a new <code>ExpressionFactory</code> instance
     */
    public static ExpressionFactory newInstance() {
        return ExpressionFactory.newInstance(null);
    }

    /**
     * Create a new instance of a <code>ExpressionFactory</code>, with optional properties.
     *
     * <p>
     * This method uses the same lookup procedure as the one used in <code>newInstance()</code>.
     *
     * <p>
     * If the argument <code>properties</code> is not null, and if the implementation contains a constructor with a single
     * parameter of type <code>java.util.Properties</code>, then the constructor is used to create the instance.
     *
     * <p>
     * Properties are optional and can be ignored by an implementation.
     *
     * <p>
     * The name of a property should start with "jakarta.el."
     *
     * <p>
     * The following are some suggested names for properties.
     * <ul>
     * <li>jakarta.el.cacheSize</li>
     * </ul>
     *
     * @param properties Properties passed to the implementation. If null, then no properties.
     *
     * @return a new <code>ExpressionFactory</code> instance
     */
    public static ExpressionFactory newInstance(Properties properties) {
        return (ExpressionFactory) FactoryFinder.find(ExpressionFactory.class,
                "jakarta.el.ExpressionFactory", "org.apache.el.ExpressionFactoryImpl", properties);
    }

    /**
     * Parses an expression into a {@link ValueExpression} for later evaluation. Use this method for expressions that refer
     * to values.
     *
     * <p>
     * This method should perform syntactic validation of the expression. If in doing so it detects errors, it should raise
     * an <code>ELException</code>.
     *
     * @param context The Jakarta Expression Language context used to parse the expression. The <code>FunctionMapper</code>
     * and <code>VariableMapper</code> stored in the ELContext are used to resolve functions and variables found in the
     * expression. They can be <code>null</code>, in which case functions or variables are not supported for this
     * expression. The object returned must invoke the same functions and access the same variable mappings regardless of
     * whether the mappings in the provided <code>FunctionMapper</code> and <code>VariableMapper</code> instances change
     * between calling <code>ExpressionFactory.createValueExpression()</code> and any method on
     * <code>ValueExpression</code>. Note that within Jakarta Expression Language, the ${} and #{} syntaxes are treated
     * identically. This includes the use of VariableMapper and FunctionMapper at expression creation time. Each is invoked
     * if not null, independent of whether the #{} or ${} syntax is used for the expression.
     * @param expression The expression to parse
     * @param expectedType The type the result of the expression will be coerced to after evaluation.
     * 
     * @return The parsed expression
     * 
     * @throws NullPointerException Thrown if expectedType is null.
     * @throws ELException Thrown if there are syntactical errors in the provided expression.
     */
    public abstract ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType);

    /**
     * Creates a ValueExpression that wraps an object instance.
     *
     * <p>
     * This method can be used to pass any object as a ValueExpression. The wrapper ValueExpression is read only, and
     * returns the wrapped object via its <code>getValue()</code> method, optionally coerced.
     * </p>
     *
     * @param instance The object instance to be wrapped.
     * @param expectedType The type the result of the expression will be coerced to after evaluation. There will be no
     * coercion if it is Object.class,
     * @throws NullPointerException Thrown if expectedType is null.
     * @return a ValueExpression that wraps an object instance
     */
    public abstract ValueExpression createValueExpression(Object instance, Class<?> expectedType);

    /**
     * Parses an expression into a {@link MethodExpression} for later evaluation. Use this method for expressions that refer
     * to methods.
     *
     * <p>
     * If the expression is a String literal, a <code>MethodExpression
     * </code> is created, which when invoked, returns the String literal, coerced to expectedReturnType. An ELException is
     * thrown if expectedReturnType is void or if the coercion of the String literal to the expectedReturnType yields an
     * error (see Section "1.16 Type Conversion").
     *
     * <p>
     * This method should perform syntactic validation of the expression. If in doing so it detects errors, it should raise
     * an <code>ELException</code>.
     *
     * @param context The Jakarta Expression Language context used to parse the expression. The <code>FunctionMapper</code>
     * and <code>VariableMapper</code> stored in the ELContext are used to resolve functions and variables found in the
     * expression. They can be <code>null</code>, in which case functions or variables are not supported for this
     * expression. The object returned must invoke the same functions and access the same variable mappings regardless of
     * whether the mappings in the provided <code>FunctionMapper</code> and <code>VariableMapper</code> instances change
     * between calling <code>ExpressionFactory.createMethodExpression()</code> and any method on
     * <code>MethodExpression</code>. Note that within the EL, the ${} and #{} syntaxes are treated identically. This
     * includes the use of VariableMapper and FunctionMapper at expression creation time. Each is invoked if not null,
     * independent of whether the #{} or ${} syntax is used for the expression.
     * @param expression The expression to parse
     * @param expectedReturnType The expected return type for the method to be found. After evaluating the expression, the
     * <code>MethodExpression</code> must check that the return type of the actual method matches this type. Passing in a
     * value of <code>null</code> indicates the caller does not care what the return type is, and the check is disabled.
     * @param expectedParamTypes The expected parameter types for the method to be found. Must be an array with no elements
     * if there are no parameters expected. It is illegal to pass <code>null</code>, unless the method is specified with
     * arguments in the Jakarta Expression Language expression, in which case these arguments are used for method selection,
     * and this parameter is ignored.
     * 
     * @return The parsed expression
     * 
     * @throws ELException Thrown if there are syntactical errors in the provided expression.
     * @throws NullPointerException if paramTypes is <code>null</code>.
     */
    public abstract MethodExpression createMethodExpression(ELContext context, String expression, Class<?> expectedReturnType, Class<?>[] expectedParamTypes);

    /**
     * Coerces an object to a specific type according to the Jakarta Expression Language type conversion rules. The custom
     * type conversions in the <code>ELResolver</code>s are not considered.
     *
     * <p>
     * An <code>ELException</code> is thrown if an error results from applying the conversion rules.
     *
     * @param obj The object to coerce.
     * @param targetType The target type for the coercion.
     * 
     * @return an object coerced to <code>targetType</code>
     * 
     * @throws ELException thrown if an error results from applying the conversion rules.
     */
    public abstract Object coerceToType(Object obj, Class<?> targetType);

    /**
     * Retrieves an ELResolver that implements the operations in collections.
     *
     * <p>
     * This ELResolver resolves the method invocation on the pair (<code>base</code>, <code>property</code>) when
     * <code>base</code> is a <code>Collection</code> or a <code>Map</code>, and <code>property</code> is the name of the
     * operation.
     * 
     * <p>
     * See the specification document for detailed descriptions of these operators, their arguments, and return values.
     *
     * @return The <code>ELResolver</code> that implements the Query Operators.
     *
     * @since Jakarta Expression Language 3.0
     */
    public ELResolver getStreamELResolver() {
        return null;
    }

    /**
     * Retrieve a function map containing a pre-configured function mapping.
     *
     * @return A initial map for functions, null if there is none.
     *
     * @since Jakarta Expression Language 3.0
     */
    public Map<String, Method> getInitFunctionMap() {
        return null;
    }
}
