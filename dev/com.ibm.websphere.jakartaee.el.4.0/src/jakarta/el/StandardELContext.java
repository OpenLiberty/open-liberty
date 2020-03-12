/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates and others.
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A standard ELContext suitable for use in a stand alone environment. This class provides a default implementation of
 * an ELResolver that contains a number of useful ELResolvers. It also provides local repositories for the
 * FunctionMapper, VariableMapper, and BeanNameResolver.
 *
 * @since Jakarta Expression Language 3.0
 */
public class StandardELContext extends ELContext {

    /*
     * The ELResolver for this ELContext.
     */
    private ELResolver elResolver;

    /*
     * The list of the custom ELResolvers added to the ELResolvers. An ELResolver is added to the list when addELResolver is
     * called.
     */
    private CompositeELResolver customResolvers;

    /*
     * The ELResolver implementing the query operators.
     */
    private ELResolver streamELResolver;

    /*
     * The FunctionMapper for this ELContext.
     */
    private FunctionMapper functionMapper;

    /*
     * The pre-configured init function map;
     */
    private Map<String, Method> initFunctionMap;

    /*
     * The VariableMapper for this ELContext.
     */
    private VariableMapper variableMapper;

    /*
     * If non-null, indicates the presence of a delegate ELContext. When a Standard is constructed from another ELContext,
     * there is no easy way to get its private context map, therefore delegation is needed.
     */
    private ELContext delegate;

    /**
     * A bean repository local to this context
     */
    private Map<String, Object> beans = new HashMap<>();

    /**
     * Construct a default ELContext for a stand-alone environment.
     *
     * @param factory The ExpressionFactory
     */
    public StandardELContext(ExpressionFactory factory) {
        streamELResolver = factory.getStreamELResolver();
        initFunctionMap = factory.getInitFunctionMap();
    }

    /**
     * Construct a StandardELContext from another ELContext.
     *
     * @param context The ELContext that acts as a delegate in most cases
     */
    public StandardELContext(ELContext context) {
        delegate = context;

        // Copy all attributes except map and resolved
        CompositeELResolver compositeELResolver = new CompositeELResolver();
        compositeELResolver.add(new BeanNameELResolver(new LocalBeanNameResolver()));
        customResolvers = new CompositeELResolver();

        compositeELResolver.add(customResolvers);
        compositeELResolver.add(context.getELResolver());
        elResolver = compositeELResolver;

        functionMapper = context.getFunctionMapper();
        variableMapper = context.getVariableMapper();
        setLocale(context.getLocale());
    }

    @Override
    public void putContext(Class<?> key, Object contextObject) {
        if (delegate != null) {
            delegate.putContext(key, contextObject);
        } else {
            super.putContext(key, contextObject);
        }
    }

    @Override
    public Object getContext(Class<?> key) {
        if (delegate == null) {
            return super.getContext(key);
        }

        return delegate.getContext(key);
    }

    /**
     * Construct (if needed) and return a default ELResolver.
     *
     * <p>
     * Retrieves the <code>ELResolver</code> associated with this context. This is a <code>CompositeELResover</code>
     * consists of an ordered list of <code>ELResolver</code>s.
     *
     * <ol>
     * <li>A {@link BeanNameELResolver} for beans defined locally</li>
     * <li>Any custom <code>ELResolver</code>s</li>
     * <li>An <code>ELResolver</code> supporting the collection operations</li>
     * <li>A {@link StaticFieldELResolver} for resolving static fields</li>
     * <li>A {@link MapELResolver} for resolving Map properties</li>
     * <li>A {@link ResourceBundleELResolver} for resolving ResourceBundle properties</li>
     * <li>A {@link ListELResolver} for resolving List properties</li>
     * <li>An {@link ArrayELResolver} for resolving array properties</li>
     * <li>A {@link BeanELResolver} for resolving bean properties</li>
     * </ol>
     *
     * @return The ELResolver for this context.
     */
    @Override
    public ELResolver getELResolver() {
        if (elResolver == null) {
            CompositeELResolver resolver = new CompositeELResolver();
            resolver.add(new BeanNameELResolver(new LocalBeanNameResolver()));
            customResolvers = new CompositeELResolver();
            resolver.add(customResolvers);
            if (streamELResolver != null) {
                resolver.add(streamELResolver);
            }
            resolver.add(new StaticFieldELResolver());
            resolver.add(new MapELResolver());
            resolver.add(new ResourceBundleELResolver());
            resolver.add(new ListELResolver());
            resolver.add(new ArrayELResolver());
            resolver.add(new BeanELResolver());
            elResolver = resolver;
        }

        return elResolver;
    }

    /**
     * Add a custom ELResolver to the context. The list of the custom ELResolvers will be accessed in the order they are
     * added. A custom ELResolver added to the context cannot be removed.
     *
     * @param cELResolver The new ELResolver to be added to the context
     */
    public void addELResolver(ELResolver cELResolver) {
        getELResolver(); // make sure elResolver is constructed
        customResolvers.add(cELResolver);
    }

    /**
     * Get the local bean repository
     *
     * @return the bean repository
     */
    Map<String, Object> getBeans() {
        return beans;
    }

    /**
     * Construct (if needed) and return a default FunctionMapper.
     *
     * @return The default FunctionMapper
     */
    @Override
    public FunctionMapper getFunctionMapper() {
        if (functionMapper == null) {
            functionMapper = new DefaultFunctionMapper(initFunctionMap);
        }

        return functionMapper;
    }

    /**
     * Construct (if needed) and return a default VariableMapper() {
     *
     * @return The default Variable
     */
    @Override
    public VariableMapper getVariableMapper() {
        if (variableMapper == null) {
            variableMapper = new DefaultVariableMapper();
        }

        return variableMapper;
    }

    private static class DefaultFunctionMapper extends FunctionMapper {

        private Map<String, Method> functions;

        DefaultFunctionMapper(Map<String, Method> initMap) {
            functions = (initMap == null) ? new HashMap<>() : new HashMap<>(initMap);
        }

        @Override
        public Method resolveFunction(String prefix, String localName) {
            return functions.get(prefix + ":" + localName);
        }

        @Override
        public void mapFunction(String prefix, String localName, Method meth) {
            functions.put(prefix + ":" + localName, meth);
        }
    }

    private static class DefaultVariableMapper extends VariableMapper {

        private Map<String, ValueExpression> variables;

        @Override
        public ValueExpression resolveVariable(String variable) {
            if (variables == null) {
                return null;
            }

            return variables.get(variable);
        }

        @Override
        public ValueExpression setVariable(String variable, ValueExpression expression) {
            if (variables == null) {
                variables = new HashMap<>();
            }

            ValueExpression prev = null;
            if (expression == null) {
                prev = variables.remove(variable);
            } else {
                prev = variables.put(variable, expression);
            }

            return prev;
        }
    }

    private class LocalBeanNameResolver extends BeanNameResolver {

        @Override
        public boolean isNameResolved(String beanName) {
            return beans.containsKey(beanName);
        }

        @Override
        public Object getBean(String beanName) {
            return beans.get(beanName);
        }

        @Override
        public void setBeanValue(String beanName, Object value) {
            beans.put(beanName, value);
        }

        @Override
        public boolean isReadOnly(String beanName) {
            return false;
        }

        @Override
        public boolean canCreateBean(String beanName) {
            return true;
        }
    }
}
