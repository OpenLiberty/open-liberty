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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

/**
 * Context information for expression parsing and evaluation.
 *
 * <p>
 * To parse or evaluate an {@link Expression}, an <code>ELContext</code> must be provided. The <code>ELContext</code>
 * holds:
 * <ul>
 * <li>a reference to {@link FunctionMapper} that will be used to resolve Jakarta Expression Language Functions. This is
 * used only in parsing.</li>
 * <li>a reference to {@link VariableMapper} that will be used to resolve Jakarta Expression Language Variables. This is
 * used only in parsing.</li>
 * <li>a reference to the base {@link ELResolver} that will be consulted to resolve model objects and their
 * properties</li>
 * <li>a collection of all the relevant context objects for use by <code>ELResolver</code>s</li>
 * <li>state information during the evaluation of an expression, such as whether a property has been resolved yet</li>
 * <li>a reference to {@link ImportHandler} that will be consulted to resolve classes that have been imported</li>
 * <li>a reference to the arguments for the active {@link LambdaExpression}s</li>
 * <li>a reference to the list of registered evaluation listeners</li>
 * </ul>
 *
 * <p>
 * The collection of context objects is necessary because each <code>ELResolver</code> may need access to a different
 * context object. For example, Jakarta Server Pages and Jakarta Faces resolvers need access to a
 * <code>jakarta.servlet.jsp.JspContext</code> and a <code>jakarta.faces.context.FacesContext</code>, respectively.
 *
 * <p>
 * When used in a web container, the creation of <code>ELContext</code> objects is controlled through the underlying
 * technology. For example, in Jakarta Server Pages the <code>JspContext.getELContext()</code> factory method is used.
 * Some technologies provide the ability to add an {@link ELContextListener} so that applications and frameworks can
 * ensure their own context objects are attached to any newly created <code>ELContext</code>.
 *
 * <p>
 * When used in a stand-alone environment, {@link StandardELContext} provides a default <code>ELContext</code>, which is
 * managed and modified by {@link ELManager}.
 *
 * <p>
 * Because it stores state during expression evaluation, an <code>ELContext</code> object is not thread-safe. Care
 * should be taken to never share an <code>ELContext</code> instance between two or more threads.
 *
 * @see ELContextListener
 * @see ELContextEvent
 * @see ELResolver
 * @see FunctionMapper
 * @see VariableMapper
 * @see ImportHandler
 * @see LambdaExpression
 * @see StandardELContext
 * 
 * @since Jakarta Expression Language 2.1 and Jakarta Expression Language 3.0
 */
public abstract class ELContext {

    private boolean resolved;
    private HashMap<Class<?>, Object> map = new HashMap<>();
    private transient List<EvaluationListener> listeners;
    private Stack<Map<String, Object>> lambdaArgs;
    private ImportHandler importHandler;
    private Locale locale;

    /**
     * Called to indicate that a <code>ELResolver</code> has successfully resolved a given (base, property) pair. Use
     * {@link #setPropertyResolved(Object, Object)} if resolved is true and to notify {@link EvaluationListener}s.
     *
     * <p>
     * The {@link CompositeELResolver} checks this property to determine whether it should consider or skip other component
     * resolvers.
     * </p>
     *
     * @see CompositeELResolver
     * @param resolved true if the property has been resolved, or false if not.
     */
    public void setPropertyResolved(boolean resolved) {
        this.resolved = resolved;
    }

    /**
     * Called to indicate that a <code>ELResolver</code> has successfully resolved a given (base, property) pair and to
     * notify the {@link EvaluationListener}s.
     *
     * <p>
     * The {@link CompositeELResolver} checks this property to determine whether it should consider or skip other component
     * resolvers.
     *
     * @see CompositeELResolver
     * @param base The base object
     * @param property The property object
     *
     * @since Jakarta Expression Language 3.0
     */
    public void setPropertyResolved(Object base, Object property) {
        setPropertyResolved(true); // Don't set the variable here, for 2.2 users
                                   // ELContext may be overridden or delegated.
        notifyPropertyResolved(base, property);
    }

    /**
     * Returns whether an {@link ELResolver} has successfully resolved a given (base, property) pair.
     *
     * <p>
     * The {@link CompositeELResolver} checks this property to determine whether it should consider or skip other component
     * resolvers.
     *
     * @see CompositeELResolver
     * @return true if the property has been resolved, or false if not.
     */
    public boolean isPropertyResolved() {
        return resolved;
    }

    /**
     * Associates a context object with this <code>ELContext</code>.
     *
     * <p>
     * The <code>ELContext</code> maintains a collection of context objects relevant to the evaluation of an expression.
     * These context objects are used by <code>ELResolver</code>s. This method is used to add a context object to that
     * collection.
     * </p>
     *
     * <p>
     * By convention, the <code>contextObject</code> will be of the type specified by the <code>key</code>. However, this is
     * not required and the key is used strictly as a unique identifier.
     * </p>
     *
     * @param key The key used by an @{link ELResolver} to identify this context object.
     * @param contextObject The context object to add to the collection.
     * @throws NullPointerException if key is null or contextObject is null.
     */
    public void putContext(Class<?> key, Object contextObject) {
        if (key == null || contextObject == null) {
            throw new NullPointerException();
        }

        map.put(key, contextObject);
    }

    /**
     * Returns the context object associated with the given key.
     *
     * <p>
     * The <code>ELContext</code> maintains a collection of context objects relevant to the evaluation of an expression.
     * These context objects are used by <code>ELResolver</code>s. This method is used to retrieve the context with the
     * given key from the collection.
     * </p>
     *
     * <p>
     * By convention, the object returned will be of the type specified by the <code>key</code>. However, this is not
     * required and the key is used strictly as a unique identifier.
     * </p>
     *
     * @param key The unique identifier that was used to associate the context object with this <code>ELContext</code>.
     * @return The context object associated with the given key, or null if no such context was found.
     * @throws NullPointerException if key is null.
     */
    public Object getContext(Class<?> key) {
        if (key == null) {
            throw new NullPointerException();
        }

        return map.get(key);
    }

    /**
     * Retrieves the <code>ELResolver</code> associated with this context.
     *
     * <p>
     * The <code>ELContext</code> maintains a reference to the <code>ELResolver</code> that will be consulted to resolve
     * variables and properties during an expression evaluation. This method retrieves the reference to the resolver.
     * </p>
     *
     * <p>
     * Once an <code>ELContext</code> is constructed, the reference to the <code>ELResolver</code> associated with the
     * context cannot be changed.
     * </p>
     *
     * @return The resolver to be consulted for variable and property resolution during expression evaluation.
     */
    public abstract ELResolver getELResolver();

    /**
     * Retrieves the <code>ImportHandler</code> associated with this <code>ELContext</code>.
     *
     * @return The import handler to manage imports of classes and packages.
     * 
     * @since Jakarta Expression Language 3.0
     */
    public ImportHandler getImportHandler() {
        if (importHandler == null) {
            importHandler = new ImportHandler();
        }

        return importHandler;
    }

    /**
     * Retrieves the <code>FunctionMapper</code> associated with this <code>ELContext</code>.
     *
     * @return The function mapper to be consulted for the resolution of Jakarta Expression Language functions.
     */
    public abstract FunctionMapper getFunctionMapper();

    /**
     * Get the <code>Locale</code> stored by a previous invocation to {@link #setLocale}. If this method returns non
     * <code>null</code>, this <code>Locale</code> must be used for all localization needs in the implementation. The
     * <code>Locale</code> must not be cached to allow for applications that change <code>Locale</code> dynamically.
     *
     * @return The <code>Locale</code> in which this instance is operating. Used primarily for message localization.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the <code>Locale</code> for this instance.
     *
     * <p>
     * This method may be called by the party creating the instance, such as Jakarta Faces or Jakarta Server Pages, to
     * enable the Jakarta Expression Language implementation to provide localized messages to the user. If no
     * <code>Locale</code> is set, the implementation must use the locale returned by <code>Locale.getDefault( )</code>.
     *
     * @param locale the locale for this instance
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Retrieves the <code>VariableMapper</code> associated with this <code>ELContext</code>.
     *
     * @return The variable mapper to be consulted for the resolution of Jakarta Expression Language variables.
     */
    public abstract VariableMapper getVariableMapper();

    /**
     * Registers an evaluation listener to the ELContext.
     *
     * @param listener The listener to be added.
     *
     * @since Jakarta Expression Language 3.0
     */
    public void addEvaluationListener(EvaluationListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        listeners.add(listener);
    }

    /**
     * Returns the list of registered evaluation listeners.
     *
     * @return The list of registered evaluation listeners.
     *
     * @since Jakarta Expression Language 3.0
     */
    public List<EvaluationListener> getEvaluationListeners() {
        return listeners;
    }

    /**
     * Notifies the listeners before an Jakarta Expression Language expression is evaluated
     *
     * @param expr The Jakarta Expression Language expression string to be evaluated
     */
    public void notifyBeforeEvaluation(String expr) {
        if (getEvaluationListeners() == null) {
            return;
        }

        for (EvaluationListener listener : getEvaluationListeners()) {
            listener.beforeEvaluation(this, expr);
        }
    }

    /**
     * Notifies the listeners after an Jakarta Expression Language expression is evaluated
     *
     * @param expr The Jakarta Expression Language expression string that has been evaluated
     */
    public void notifyAfterEvaluation(String expr) {
        if (getEvaluationListeners() == null) {
            return;
        }

        for (EvaluationListener listener : getEvaluationListeners()) {
            listener.afterEvaluation(this, expr);
        }
    }

    /**
     * Notifies the listeners when the (base, property) pair is resolved
     *
     * @param base The base object
     * @param property The property Object
     */
    public void notifyPropertyResolved(Object base, Object property) {
        if (getEvaluationListeners() == null) {
            return;
        }

        for (EvaluationListener listener : getEvaluationListeners()) {
            listener.propertyResolved(this, base, property);
        }
    }

    /**
     * Inquires if the name is a LambdaArgument
     *
     * @param arg A possible Lambda formal parameter name
     * @return true if arg is a LambdaArgument, false otherwise.
     */
    public boolean isLambdaArgument(String arg) {
        if (lambdaArgs == null) {
            return false;
        }

        for (int i = lambdaArgs.size() - 1; i >= 0; i--) {
            Map<String, Object> lmap = lambdaArgs.elementAt(i);
            if (lmap.containsKey(arg)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the Lambda argument associated with a formal parameter. If the Lambda expression is nested within other
     * Lambda expressions, the arguments for the current Lambda expression is first searched, and if not found, the
     * arguments for the immediate nesting Lambda expression then searched, and so on.
     *
     * @param arg The formal parameter for the Lambda argument
     * @return The object associated with formal parameter. Null if no object has been associated with the parameter.
     * 
     * @since Jakarta Expression Language 3.0
     */
    public Object getLambdaArgument(String arg) {
        if (lambdaArgs == null) {
            return null;
        }

        for (int i = lambdaArgs.size() - 1; i >= 0; i--) {
            Map<String, Object> lmap = lambdaArgs.elementAt(i);
            Object v = lmap.get(arg);
            if (v != null) {
                return v;
            }
        }

        return null;
    }

    /**
     * Installs a Lambda argument map, in preparation for the evaluation of a Lambda expression. The arguments in the map
     * will be in scope during the evaluation of the Lambda expression.
     *
     * @param args The Lambda arguments map
     * 
     * @since Jakarta Expression Language 3.0
     */
    public void enterLambdaScope(Map<String, Object> args) {
        if (lambdaArgs == null) {
            lambdaArgs = new Stack<>();
        }

        lambdaArgs.push(args);
    }

    /**
     * Exits the Lambda expression evaluation. The Lambda argument map that was previously installed is removed.
     *
     * @since Jakarta Expression Language 3.0
     */
    public void exitLambdaScope() {
        if (lambdaArgs != null) {
            lambdaArgs.pop();
        }
    }

    /**
     * Converts an object to a specific type. If a custom converter in the <code>ELResolver</code> handles this conversion,
     * it is used. Otherwise the standard coercions is applied.
     *
     * <p>
     * An <code>ELException</code> is thrown if an error occurs during the conversion.
     *
     * @param obj The object to convert.
     * @param targetType The target type for the conversion.
     * 
     * @return object converted to <code>targetType</code>
     * @throws ELException thrown if errors occur.
     *
     * @since Jakarta Expression Language 3.0
     */
    public Object convertToType(Object obj, Class<?> targetType) {
        boolean propertyResolvedSave = isPropertyResolved();
        try {
            setPropertyResolved(false);
            ELResolver elResolver = getELResolver();
            if (elResolver != null) {
                Object res = elResolver.convertToType(this, obj, targetType);
                if (isPropertyResolved()) {
                    return res;
                }
            }
        } catch (ELException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ELException(ex);
        } finally {
            setPropertyResolved(propertyResolvedSave);
        }

        ExpressionFactory exprFactory = (ExpressionFactory) getContext(ExpressionFactory.class);
        if (exprFactory == null) {
            exprFactory = ELUtil.getExpressionFactory();
        }

        return exprFactory.coerceToType(obj, targetType);
    }

}
