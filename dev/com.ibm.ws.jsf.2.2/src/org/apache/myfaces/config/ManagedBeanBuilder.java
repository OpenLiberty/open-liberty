/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
// PI47578      wtlucy  An UnsupportedOperationException is thrown with an eager ManagedBean

package org.apache.myfaces.config;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.myfaces.config.annotation.LifecycleProvider;
import org.apache.myfaces.config.annotation.LifecycleProvider2;
import org.apache.myfaces.config.annotation.LifecycleProviderFactory;
import org.apache.myfaces.config.element.ListEntries;
import org.apache.myfaces.config.element.ListEntry;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.config.element.ManagedProperty;
import org.apache.myfaces.config.element.MapEntries;
import org.apache.myfaces.config.element.MapEntry;
import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.util.ContainerUtils;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Create and initialize managed beans
 *
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a> (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 */
public class ManagedBeanBuilder
{
    //private static Log log = LogFactory.getLog(ManagedBeanBuilder.class);
    private static Logger log = Logger.getLogger(ManagedBeanBuilder.class.getName());
    private RuntimeConfig _runtimeConfig;
    public final static String REQUEST = "request";
    public final static String VIEW = "view";
    public final static String APPLICATION = "application";
    public final static String SESSION = "session";
    public final static String NONE = "none";
    
    /**
     * Comparator used to compare Scopes in the following order:
     * REQUEST VIEW SESSION APPLICATION NONE
     * @author Jakob Korherr
     */
    private final static Comparator<String> SCOPE_COMPARATOR
            = new Comparator<String>()
    {

        public int compare(String o1, String o2)
        {
            if (o1.equalsIgnoreCase(o2))
            {
                // the same scope
                return 0;
            }
            if (o1.equalsIgnoreCase(NONE))
            {
                // none is greater than any other scope
                return 1;
            }
            if (o1.equalsIgnoreCase(APPLICATION))
            {
                if (o2.equalsIgnoreCase(NONE))
                {
                    // application is smaller than none
                    return -1;
                }
                else
                {
                    // ..but greater than any other scope
                    return 1;
                }
            }
            if (o1.equalsIgnoreCase(SESSION))
            {
                if (o2.equalsIgnoreCase(REQUEST) || o2.equalsIgnoreCase(VIEW))
                {
                    // session is greater than request and view
                    return 1;
                }
                else
                {
                    // but smaller than any other scope
                    return -1;
                }
            }
            if (o1.equalsIgnoreCase(VIEW))
            {
                if (o2.equalsIgnoreCase(REQUEST))
                {
                    // view is greater than request
                    return 1;
                }
                else
                {
                    // ..but smaller than any other scope
                    return -1;
                }
            }
            if (o1.equalsIgnoreCase(REQUEST))
            {
                // request is smaller than any other scope
                return -1;
            }
            
            // not a valid scope
            throw new IllegalArgumentException(o1 + " is not a valid scope");
        }
        
    };

    @SuppressWarnings("unchecked")
    public Object buildManagedBean(FacesContext facesContext, ManagedBean beanConfiguration) throws FacesException
    {
        try
        {
            ExternalContext externalContext = facesContext.getExternalContext();
            LifecycleProvider lifecycleProvider = LifecycleProviderFactory
                    .getLifecycleProviderFactory( externalContext).getLifecycleProvider(externalContext);
            
            final Object bean = lifecycleProvider.newInstance(beanConfiguration.getManagedBeanClassName());

            switch (beanConfiguration.getInitMode())
            {
                case ManagedBean.INIT_MODE_PROPERTIES:
                    try
                    {
                        initializeProperties(facesContext, beanConfiguration, bean);
                    }
                    catch (IllegalArgumentException e)
                    {
                        throw new IllegalArgumentException(
                                e.getMessage()
                                        + " for bean '"
                                        + beanConfiguration.getManagedBeanName()
                                        + "' check the configuration to make sure all properties "
                                        + "correspond with get/set methods", e);
                    }
                    break;

                case ManagedBean.INIT_MODE_MAP:
                    if (!(bean instanceof Map))
                    {
                        throw new IllegalArgumentException("Class " + bean.getClass().getName()
                                + " of managed bean "
                                + beanConfiguration.getManagedBeanName()
                                + " is not a Map.");
                    }
                    initializeMap(facesContext, beanConfiguration.getMapEntries(), (Map<Object, Object>) bean);
                    break;

                case ManagedBean.INIT_MODE_LIST:
                    if (!(bean instanceof List))
                    {
                        throw new IllegalArgumentException("Class " + bean.getClass().getName()
                                + " of managed bean "
                                + beanConfiguration.getManagedBeanName()
                                + " is not a List.");
                    }
                    initializeList(facesContext, beanConfiguration.getListEntries(), (List<Object>) bean);
                    break;

                case ManagedBean.INIT_MODE_NO_INIT:
                    // no init values
                    break;

                default:
                    throw new IllegalStateException("Unknown managed bean type "
                            + bean.getClass().getName() + " for managed bean "
                            + beanConfiguration.getManagedBeanName() + '.');
            }
            
            // MYFACES-1761 if implements LifecycleProvider,
            //PostConstruct was already called, but if implements
            //LifecycleProvider2, call it now.
            if (lifecycleProvider instanceof LifecycleProvider2)
            {
                ((LifecycleProvider2)lifecycleProvider).postConstruct(bean);
            }
            return bean;
        }
        catch (IllegalAccessException e)
        {
            throw new FacesException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new FacesException(e);
        }
        catch (NamingException e)
        {
            throw new FacesException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new FacesException(e);
        }
        catch (InstantiationException e)
        {
            throw new FacesException(e);
        }

    }


    @SuppressWarnings("unchecked")
    private void initializeProperties(FacesContext facesContext, 
                                      ManagedBean beanConfiguration, Object bean)
    {
        ELResolver elResolver = facesContext.getApplication().getELResolver();
        ELContext elContext = facesContext.getELContext();

        for (ManagedProperty property : beanConfiguration.getManagedProperties())
        {
            Object value = null;

            switch (property.getType())
            {
                case ManagedProperty.TYPE_LIST:

                    // JSF 1.1, 5.3.1.3
                    // Call the property getter, if it exists.
                    // If the getter returns null or doesn't exist, create a java.util.ArrayList,
                    // otherwise use the returned Object ...
                    if (PropertyUtils.isReadable(bean, property.getPropertyName()))
                    {
                        value = elResolver.getValue(elContext, bean, property.getPropertyName());
                    }
                    
                    value = value == null ? new ArrayList<Object>() : value;

                    if (value instanceof List)
                    {
                        initializeList(facesContext, property.getListEntries(), (List<Object>)value);

                    }
                    else if (value != null && value.getClass().isArray())
                    {
                        int length = Array.getLength(value);
                        ArrayList<Object> temp = new ArrayList<Object>(length);
                        for (int i = 0; i < length; i++)
                        {
                            temp.add(Array.get(value, i));
                        }
                        initializeList(facesContext, property.getListEntries(), temp);
                        value = Array.newInstance(value.getClass().getComponentType(), temp.size());
                        length = temp.size();

                        for (int i = 0; i < length; i++)
                        {
                            Array.set(value, i, temp.get(i));
                        }
                    }
                    else
                    {
                        value = new ArrayList<Object>();
                        initializeList(facesContext, property.getListEntries(), (List<Object>) value);
                    }

                    break;
                case ManagedProperty.TYPE_MAP:

                    // JSF 1.1, 5.3.1.3
                    // Call the property getter, if it exists.
                    // If the getter returns null or doesn't exist, create a java.util.HashMap,
                    // otherwise use the returned java.util.Map .
                    if (PropertyUtils.isReadable(bean, property.getPropertyName()))
                    {
                        value = elResolver.getValue(elContext, bean, property.getPropertyName());
                    }
                    value = value == null ? new HashMap<Object, Object>() : value;

                    if (!(value instanceof Map))
                    {
                        value = new HashMap<Object, Object>();
                    }

                    initializeMap(facesContext, property.getMapEntries(), (Map<Object, Object>) value);
                    break;
                case ManagedProperty.TYPE_NULL:
                    break;
                case ManagedProperty.TYPE_VALUE:
                    // check for correct scope of a referenced bean
                    if (!isInValidScope(facesContext, property, beanConfiguration))
                    {
                        throw new FacesException("Property " + property.getPropertyName() +
                                " references object in a scope with shorter lifetime than the target scope " +
                                beanConfiguration.getManagedBeanScope());
                    }
                    value = property.getRuntimeValue(facesContext);
                    break;
                default:
                    throw new FacesException("unknown ManagedProperty type: "+ property.getType());
            }
            
            Class<?> propertyClass = null;

            if (property.getPropertyClass() == null)
            {
                propertyClass = elResolver.getType(elContext, bean, property.getPropertyName());
            }
            else
            {
                propertyClass = ClassUtils.simpleJavaTypeToClass(property.getPropertyClass());
            }
            
            if (null == propertyClass)
            {
                throw new IllegalArgumentException("unable to find the type of property " + property.getPropertyName());
            }
            
            Object coercedValue = coerceToType(facesContext, value, propertyClass);
            elResolver.setValue(elContext, bean, property.getPropertyName(), coercedValue);
        }
    }

    // We no longer use the convertToType from shared impl because we switched
    // to unified EL in JSF 1.2
    @SuppressWarnings("unchecked")
    public static <T> T coerceToType(FacesContext facesContext, Object value, Class<? extends T> desiredClass)
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            ExpressionFactory expFactory = facesContext.getApplication().getExpressionFactory();
            // Use coersion implemented by JSP EL for consistency with EL
            // expressions. Additionally, it caches some of the coersions.
            return (T)expFactory.coerceToType(value, desiredClass);
        }
        catch (ELException e)
        {
            String message = "Cannot coerce " + value.getClass().getName()
                    + " to " + desiredClass.getName();
            log.log(Level.SEVERE, message , e);
            throw new FacesException(message, e);
        }
    }


    /**
     * Checks if the scope of the property value is valid for a bean to be stored in targetScope.
     * If one of the scopes is a custom scope (since jsf 2.0), this method only checks the
     * references if the current ProjectStage is not Production.
     * @param facesContext
     * @param property           the property to be checked
     * @param beanConfiguration  the ManagedBean, which will be created
     */
    private boolean isInValidScope(FacesContext facesContext, ManagedProperty property, ManagedBean beanConfiguration)
    {
        if (!property.isValueReference())
        {
            // no value reference but a literal value -> nothing to check
            return true;
        }
        
        // get the targetScope (since 2.0 this could be an EL ValueExpression)
        String targetScope = null;
        if (beanConfiguration.isManagedBeanScopeValueExpression())
        {
            // the scope is a custom scope
            // Spec says, that the developer has to take care about the references
            // to and from managed-beans in custom scopes.
            // However, we do check the references, if we are not in Production stage
            if (facesContext.isProjectStage(ProjectStage.Production))
            {
                return true;
            }
            else
            {
                targetScope = getNarrowestScope(facesContext, 
                                                beanConfiguration
                                                    .getManagedBeanScopeValueExpression(facesContext)
                                                    .getExpressionString());
                // if we could not obtain a targetScope, return true
                if (targetScope == null)
                {
                    return true;
                }
            }
        }
        else
        {
            targetScope = beanConfiguration.getManagedBeanScope();
            if (targetScope == null)
            {
                targetScope = NONE;
            }
        }
        
        // optimization: 'request' scope can reference any value scope
        if (targetScope.equalsIgnoreCase(REQUEST))
        {
            return true;
        }
        
        String valueScope = getNarrowestScope(facesContext, 
                                              property.getValueBinding(facesContext)
                                                  .getExpressionString());
        
        // if we could not obtain a valueScope, return true
        if (valueScope == null)
        {
            return true;
        }
        
        // the target scope needs to have a shorter (or equal) lifetime than the value scope
        return (SCOPE_COMPARATOR.compare(targetScope, valueScope) <= 0);
    }

    /**
     * Gets the narrowest scope to which the ValueExpression points.
     * @param facesContext
     * @param valueExpression
     * @return
     */
    private String getNarrowestScope(FacesContext facesContext, String valueExpression)
    {
        List<String> expressions = extractExpressions(valueExpression);
        // exclude NONE scope, if there are more than one ValueExpressions (see Spec for details)
        String narrowestScope = expressions.size() == 1 ? NONE : APPLICATION;
        boolean scopeFound = false;
        
        for (String expression : expressions)
        {
            String valueScope = getScope(facesContext, expression);
            if (valueScope == null)
            {
                continue;
            }
            // we have found at least one valid scope at this point
            scopeFound = true;
            if (SCOPE_COMPARATOR.compare(valueScope, narrowestScope) < 0)
            {
                narrowestScope = valueScope;
            }
        }
        
        return scopeFound ? narrowestScope : null;
    }
    
    private String getScope(FacesContext facesContext, String expression)
    {
        String beanName = getFirstSegment(expression);
        ExternalContext externalContext = facesContext.getExternalContext();

        // check scope objects
        if (beanName.equalsIgnoreCase("requestScope"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("sessionScope"))
        {
            return SESSION;
        }
        if (beanName.equalsIgnoreCase("applicationScope"))
        {
            return APPLICATION;
        }

        // check implicit objects
        if (beanName.equalsIgnoreCase("cookie"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("facesContext"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("header"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("headerValues"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("param"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("paramValues"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("request"))
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("view")) // Spec says that view is considered to be in request scope
        {
            return REQUEST;
        }
        if (beanName.equalsIgnoreCase("application"))
        {
            return APPLICATION;
        }
        if (beanName.equalsIgnoreCase("initParam"))
        {
            return APPLICATION;
        }

        // not found so far - check all scopes
        final boolean startup = (externalContext instanceof StartupServletExternalContextImpl);
        if (!startup)
        {
            // request and session maps are only available at runtime - not at startup
            // (the following code would throw an UnsupportedOperationException).
            if (externalContext.getRequestMap().get(beanName) != null)
            {
                return REQUEST;
            }
            if (externalContext.getSessionMap().get(beanName) != null)
            {
                return SESSION;
            }
        }
        if (externalContext.getApplicationMap().get(beanName) != null)
        {
            return APPLICATION;
        }
        if (facesContext.getViewRoot() != null)
        {
            // PI47578: Don't create the view Map during startup
            Map<String, Object> viewMap = facesContext.getViewRoot().getViewMap(!startup);
            if (viewMap != null && viewMap.get(beanName) != null)
            {
                return VIEW;
            }
        }
        //not found - check mangaged bean config
        ManagedBean mbc = getRuntimeConfig(facesContext).getManagedBean(beanName);
        if (mbc != null)
        {
            // managed-bean-scope could be a EL ValueExpression (since 2.0)
            if (mbc.isManagedBeanScopeValueExpression())
            {   
                // the scope is a custom scope
                // Spec says, that the developer has to take care about the references
                // to and from managed-beans in custom scopes.
                // However, we do check the references, if we are not in Production stage
                if (facesContext.isProjectStage(ProjectStage.Production))
                {
                    return null;
                }
                else
                {
                    String scopeExpression = mbc.getManagedBeanScopeValueExpression(facesContext).getExpressionString();
                    return getNarrowestScope(facesContext, scopeExpression);
                }
            }
            else
            {
                return mbc.getManagedBeanScope();
            }
        }

        return null;
    }

    /**
     * Extract the first expression segment, that is the substring up to the first '.' or '['
     *
     * @param expression
     * @return first segment of the expression
     */
    private String getFirstSegment(String expression)
    {
        int indexDot = expression.indexOf('.');
        int indexBracket = expression.indexOf('[');

        if (indexBracket < 0)
        {

            return indexDot < 0 ? expression : expression.substring(0, indexDot);

        }

        if (indexDot < 0)
        {
            return expression.substring(0, indexBracket);
        }

        return expression.substring(0, Math.min(indexDot, indexBracket));

    }

    private List<String> extractExpressions(String expressionString)
    {
        List<String> expressions = new ArrayList<String>();
        for (String expression : expressionString.split("\\#\\{"))
        {
            int index = expression.indexOf('}');
            if (index >= 0)
            {
                expressions.add(expression.substring(0, index));
            }
        }
        return expressions;
    }


    private void initializeMap(FacesContext facesContext, MapEntries mapEntries, 
                               Map<? super Object, ? super Object> map)
    {
        Application application = facesContext.getApplication();
        
        Class<?> keyClass = (mapEntries.getKeyClass() == null)
                ? String.class : ClassUtils.simpleJavaTypeToClass(mapEntries.getKeyClass());
        
        Class<?> valueClass = (mapEntries.getValueClass() == null)
                ? String.class : ClassUtils.simpleJavaTypeToClass(mapEntries.getValueClass());
        
        ValueExpression valueExpression;
        ExpressionFactory expFactory = application.getExpressionFactory();
        ELContext elContext = facesContext.getELContext();

        for (Iterator<? extends MapEntry> iterator = mapEntries.getMapEntries(); iterator.hasNext();)
        {
            MapEntry entry = iterator.next();
            Object key = entry.getKey();

            if (ContainerUtils.isValueReference((String) key))
            {
                valueExpression = expFactory.createValueExpression(elContext, (String) key, Object.class);
                key = valueExpression.getValue(elContext);
            }

            if (entry.isNullValue())
            {
                map.put(coerceToType(facesContext, key, keyClass), null);
            }
            else
            {
                Object value = entry.getValue();
                if (ContainerUtils.isValueReference((String) value))
                {
                    valueExpression = expFactory.createValueExpression(elContext, (String) value, Object.class);
                    value = valueExpression.getValue(elContext);
                }
                
                map.put(coerceToType(facesContext, key, keyClass), coerceToType(facesContext, value, valueClass));
            }
        }
    }


    private void initializeList(FacesContext facesContext, ListEntries listEntries, List<? super Object> list)
    {
        Application application = facesContext.getApplication();
        
        Class<?> valueClass = (listEntries.getValueClass() == null)
                ? String.class : ClassUtils.simpleJavaTypeToClass(listEntries.getValueClass());
        
        ExpressionFactory expFactory = application.getExpressionFactory();
        ELContext elContext = facesContext.getELContext();

        for (Iterator<? extends ListEntry> iterator = listEntries.getListEntries(); iterator.hasNext();)
        {
            ListEntry entry = iterator.next();
            if (entry.isNullValue())
            {
                list.add(null);
            }
            else
            {
                Object value = entry.getValue();
                if (ContainerUtils.isValueReference((String) value))
                {
                    ValueExpression valueExpression = expFactory.createValueExpression(elContext, (String) value,
                                                                                       Object.class);
                    value = valueExpression.getValue(elContext);
                }
                
                list.add(coerceToType(facesContext, value, valueClass));
            }
        }
    }

    private RuntimeConfig getRuntimeConfig(FacesContext facesContext)
    {
        if (_runtimeConfig == null)
        {
            _runtimeConfig = RuntimeConfig.getCurrentInstance(facesContext.getExternalContext());
        }
        
        return _runtimeConfig;
    }
}
