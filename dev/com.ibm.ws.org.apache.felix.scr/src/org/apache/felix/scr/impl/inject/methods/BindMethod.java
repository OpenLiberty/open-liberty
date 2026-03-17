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
package org.apache.felix.scr.impl.inject.methods;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.RefPair;
import org.apache.felix.scr.impl.inject.ScrComponentContext;
import org.apache.felix.scr.impl.inject.ValueUtils;
import org.apache.felix.scr.impl.inject.internal.ClassUtils;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.osgi.framework.BundleContext;


/**
 * Component method to be invoked on service (un)binding.
 */
public class BindMethod extends BaseMethod<BindParameters, List<ValueUtils.ValueType>>
implements org.apache.felix.scr.impl.inject.ReferenceMethod
{
    private final String m_referenceClassName;

    //initialized for cases where there is no method.
    private volatile List<ValueUtils.ValueType> m_paramTypes = Collections.emptyList();

    public BindMethod( final String methodName,
            final Class<?> componentClass,
            final String referenceClassName,
            final DSVersion dsVersion,
            final boolean configurableServiceProperties )
    {
        super( methodName, methodName != null, componentClass, dsVersion, configurableServiceProperties );
        m_referenceClassName = referenceClassName;
    }


    /**
     * Finds the method named in the {@link #m_methodName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a method is found or the root
     * of the class hierarchy is reached without finding a method.
     *
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    @Override
    protected MethodInfo<List<ValueUtils.ValueType>> doFindMethod( final Class<?> targetClass,
            final boolean acceptPrivate,
            final boolean acceptPackage,
            final ComponentLogger logger )
                    throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        // 112.3.1 The method is searched for using the following priority
        //  1 - ServiceReference single parameter
        //  2 - DS 1.3+ : ComponentServiceObjects single parameter
        //  3 - Service object single parameter
        //  4 - Service interface assignment compatible single parameter
        //  5 - DS 1.3+ : Single argument with Map
        //  6 - DS 1.1/DS 1.2 : two parameters, first the type of or assignment compatible with the service, the second Map
        //  7 - DS 1.3+ : one or more parameters of types ServiceReference, ServiceObjects, interface type,
        //                or assignment compatible to interface type, in any order.

        // flag indicating a suitable but inaccessible method has been found
        boolean suitableMethodNotAccessible = false;

        if (logger.isLogEnabled(Level.DEBUG))
        {
            logger.log(Level.DEBUG,
                    "doFindMethod: Looking for method {0}.{1}", null, targetClass.getName(), getMethodName());
        }

        // Case 1 - Service reference parameter
        Method method;
        try
        {
            method = getServiceReferenceMethod( targetClass, acceptPrivate, acceptPackage, logger );
            if ( method != null )
            {
                if (logger.isLogEnabled(Level.DEBUG))
                {
                    logger.log(Level.DEBUG, "doFindMethod: Found Method {0}",null, method);
                }
                return new MethodInfo<>(method, Collections.singletonList(ValueUtils.ValueType.ref_serviceReference));
            }
        }
        catch ( SuitableMethodNotAccessibleException ex )
        {
            suitableMethodNotAccessible = true;
        }

        // Case 2 - ComponentServiceObjects parameter
        if ( getDSVersion().isDS13() )
        {
            try
            {
                method = getComponentObjectsMethod( targetClass, acceptPrivate, acceptPackage, logger );
                if ( method != null )
                {
                    if (logger.isLogEnabled(Level.DEBUG))
                    {
                        logger.log(Level.DEBUG, "doFindMethod: Found Method {0}", null, method);
                    }
                    return new MethodInfo<>(method, Collections.singletonList(ValueUtils.ValueType.ref_serviceObjects));
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }
        }

        // for further methods we need the class of the service object
        final Class<?> parameterClass = ClassUtils.getClassFromComponentClassLoader( targetClass, m_referenceClassName, logger );
        if ( parameterClass != null )
        {

            if (logger.isLogEnabled(Level.DEBUG))
            {
                logger.log(
                    Level.DEBUG,
                    "doFindMethod: No method taking ServiceReference found, checking method taking {0}",
                    null, parameterClass.getName() );
            }

            // Case 3 - Service object parameter
            try
            {
                method = getServiceObjectMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                if ( method != null )
                {
                    if (logger.isLogEnabled(Level.DEBUG))
                    {
                        logger.log(Level.DEBUG, "doFindMethod: Found Method {0}", null, method);
                    }
                    return new MethodInfo<>(method,
                        Collections.singletonList(ValueUtils.ValueType.ref_serviceType));
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 4 - Service interface assignment compatible methods
            try
            {
                method = getServiceObjectAssignableMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                if ( method != null )
                {
                    if (logger.isLogEnabled(Level.DEBUG))
                    {
                        logger.log(Level.DEBUG, "doFindMethod: Found Method {0}", null, method);
                    }
                    return new MethodInfo<>(method,
                        Collections.singletonList(ValueUtils.ValueType.ref_serviceType));
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 5 - DS 1.3+ : Single argument with Map
            if ( getDSVersion().isDS13() )
            {
                try
                {
                    method = getMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                    if ( method != null )
                    {
                        if (logger.isLogEnabled(Level.DEBUG))
                        {
                            logger.log(Level.DEBUG,
                                "doFindMethod: Found Method {0}", null, method);
                        }
                        return new MethodInfo<>(method,
                            Collections.singletonList(ValueUtils.ValueType.ref_map));
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }
            }

            // signatures taking a map are only supported starting with DS 1.1
            if ( getDSVersion().isDS11() && !getDSVersion().isDS13() )
            {

                // Case 6 - same as case 3, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectWithMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                    if ( method != null )
                    {
                        if (logger.isLogEnabled(Level.DEBUG))
                        {
                            logger.log(Level.DEBUG,
                                "doFindMethod: Found Method {0}", null, method);
                        }
                        List<ValueUtils.ValueType> paramTypes = new ArrayList<>(2);
                        paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                        paramTypes.add(ValueUtils.ValueType.ref_map);
                        return new MethodInfo<>(method, paramTypes);
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

                // Case 6 - same as case 4, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectAssignableWithMapMethod( targetClass, parameterClass, acceptPrivate,
                            acceptPackage );
                    if ( method != null )
                    {
                        if (logger.isLogEnabled(Level.DEBUG))
                        {
                            logger.log(Level.DEBUG,
                                "doFindMethod: Found Method {0}", null, method);
                        }
                        List<ValueUtils.ValueType> paramTypes = new ArrayList<>(2);
                        paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                        paramTypes.add(ValueUtils.ValueType.ref_map);
                        return new MethodInfo<>(method,
                            paramTypes);
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

            }
            // Case 7 - Multiple parameters
            if ( getDSVersion().isDS13() )
            {
                for (Method m: targetClass.getDeclaredMethods())
                {
                    if (getMethodName().equals(m.getName())) {
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        boolean matches = true;
                        boolean specialMatch = true;
                        List<ValueUtils.ValueType> paramTypes = new ArrayList<>(parameterTypes.length);
                        for (Class<?> paramType: parameterTypes) {
                            if (paramType == ClassUtils.SERVICE_REFERENCE_CLASS)
                            {
                                if (specialMatch && parameterClass == ClassUtils.SERVICE_REFERENCE_CLASS)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ValueUtils.ValueType.ref_serviceReference);
                                }
                            }
                            else if (paramType == ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS)
                            {
                                if (specialMatch && parameterClass == ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ValueUtils.ValueType.ref_serviceObjects);
                                }
                            }
                            else if (paramType == ClassUtils.MAP_CLASS)
                            {
                                if (specialMatch && parameterClass == ClassUtils.MAP_CLASS)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ValueUtils.ValueType.ref_map);
                                }
                            }
                            else if (paramType.isAssignableFrom( parameterClass ) )
                            {
                                paramTypes.add(ValueUtils.ValueType.ref_serviceType);
                            }
                            // DS 1.4 : Logger and FormattedLogger
                            else if ( getDSVersion().isDS14() && ClassUtils.LOGGER_FACTORY_CLASS.equals(m_referenceClassName) )
                            {
                                 if ( paramType.getName().equals(ClassUtils.LOGGER_CLASS) )
                                 {
                                     paramTypes.add(ValueUtils.ValueType.ref_logger);
                                 }
                                 else if ( paramType.getName().equals(ClassUtils.FORMATTER_LOGGER_CLASS) )
                                 {
                                     paramTypes.add(ValueUtils.ValueType.ref_formatterLogger);
                                 }
                                 else
                                 {
                                     matches = false;
                                     break;
                                 }
                            }
                            else
                            {
                                matches = false;
                                break;
                            }
                        }
                        if (matches)
                        {
                            if ( accept( m, acceptPrivate, acceptPackage, returnValue() ) )
                            {
                                if (logger.isLogEnabled(Level.DEBUG))
                                {
                                    logger.log(Level.DEBUG,
                                        "doFindMethod: Found Method {0}", null, m);
                                }
                                return new MethodInfo<>(m, paramTypes);
                            }
                            suitableMethodNotAccessible = true;
                        }
                    }
                }
            }
        }
        else if (logger.isLogEnabled(Level.WARN))
        {
            logger.log(
                Level.WARN,
                    "doFindMethod: Cannot check for methods taking parameter class {0}: {1} does not see it", null, m_referenceClassName, targetClass.getName());
        }

        // if at least one suitable method could be found but none of
        // the suitable methods are accessible, we have to terminate
        if ( suitableMethodNotAccessible )
        {
            logger.log(Level.ERROR,
                    "doFindMethod: Suitable but non-accessible method found in class {0}",null,
                            targetClass.getName() );
            throw new SuitableMethodNotAccessibleException();
        }

        // no method found
        return null;
    }

    @Override
    protected void setTypes(List<ValueUtils.ValueType> types)
    {
        m_paramTypes = types;
    }

    /**
     * Returns a method taking a single <code>ServiceReference</code> object
     * as a parameter or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceReferenceMethod( final Class<?> targetClass, boolean acceptPrivate, boolean acceptPackage, ComponentLogger logger )
            throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
                { ClassUtils.SERVICE_REFERENCE_CLASS }, acceptPrivate, acceptPackage, logger );
    }

    private Method getComponentObjectsMethod( final Class<?> targetClass, boolean acceptPrivate, boolean acceptPackage, ComponentLogger logger )
            throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod(targetClass, getMethodName(),
                new Class[] { ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS }, acceptPrivate, acceptPackage,
                logger);
    }


    /**
     * Returns a method taking a single parameter of the exact type declared
     * for the service reference or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectMethod( final Class<?> targetClass, final Class<?> parameterClass, boolean acceptPrivate,
            boolean acceptPackage, ComponentLogger logger ) throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
                { parameterClass }, acceptPrivate, acceptPackage, logger );
    }


    /**
     * Returns a method taking a single object whose type is assignment
     * compatible with the declared service type or <code>null</code> if no
     * such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, ComponentLogger logger ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        if (logger.isLogEnabled(Level.DEBUG))
        {
            logger.log(
                Level.DEBUG,
                    "getServiceObjectAssignableMethod: Checking {0} declared method in class {1}", null, candidateBindMethods.length, targetClass.getName());
        }

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            Method method = candidateBindMethods[i];
            if (logger.isLogEnabled(Level.DEBUG))
            {
                logger.log(Level.DEBUG,
                    "getServiceObjectAssignableMethod: Checking {0}", null, method);
            }

            // Get the parameters for the current method
            Class<?>[] parameters = method.getParameterTypes();

            // Select only the methods that receive a single
            // parameter
            // and a matching name
            if ( parameters.length == 1 && method.getName().equals( getMethodName() ) )
            {

                if (logger.isLogEnabled(Level.DEBUG))
                {
                    logger.log(Level.DEBUG,
                        "getServiceObjectAssignableMethod: Considering {0}", null, method);
                }

                // Get the parameter type
                final Class<?> theParameter = parameters[0];

                // Check if the parameter type is ServiceReference
                // or is assignable from the type specified by the
                // reference's interface attribute
                if ( theParameter.isAssignableFrom( parameterClass ) )
                {
                    if ( accept( method, acceptPrivate, acceptPackage, false ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
                else if (logger.isLogEnabled(Level.DEBUG))
                {
                    logger.log(
                        Level.DEBUG,
                        "getServiceObjectAssignableMethod: Parameter failure: Required {0}; actual {1}",
                        null, theParameter, parameterClass.getName());
                }

            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }


    /**
     * Returns a method taking two parameters, the first being of the exact
     * type declared for the service reference and the second being a
     * <code>Map</code> or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectWithMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, ComponentLogger logger ) throws SuitableMethodNotAccessibleException,
    InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
                { parameterClass, ClassUtils.MAP_CLASS }, acceptPrivate, acceptPackage, logger );
    }


    /**
     * Returns a method taking two parameters, the first being an object
     * whose type is assignment compatible with the declared service type and
     * the second being a <code>Map</code> or <code>null</code> if no such
     * method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableWithMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            final Method method = candidateBindMethods[i];
            final Class<?>[] parameters = method.getParameterTypes();
            if ( parameters.length == 2 && method.getName().equals( getMethodName() ) )
            {

                // parameters must be refclass,map
                if ( parameters[0].isAssignableFrom( parameterClass ) && parameters[1] == ClassUtils.MAP_CLASS )
                {
                    if ( accept( method, acceptPrivate, acceptPackage, false ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }

    /**
     * Returns a method taking a single map parameter
     * or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, ComponentLogger logger ) throws SuitableMethodNotAccessibleException,
    InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
                { ClassUtils.MAP_CLASS }, acceptPrivate, acceptPackage, logger );
    }

    @Override
    public <S, T> boolean getServiceObject( final BindParameters parameters, BundleContext context )
    {
        //??? this resolves which we need.... better way?
        if (parameters.getRefPair().getServiceObject(parameters.getComponentContext()) == null
                && methodExists(parameters.getComponentContext().getLogger()))
        {
            if ( m_paramTypes.contains(ValueUtils.ValueType.ref_serviceType)
                 || m_paramTypes.contains(ValueUtils.ValueType.ref_logger)
                 || m_paramTypes.contains(ValueUtils.ValueType.ref_formatterLogger)) {
                return parameters.getRefPair().getServiceObject(parameters.getComponentContext(), context);
            }
        }
        return true;
    }

    @Override
    protected Object[] getParameters( Method method, BindParameters bp )
    {
        ScrComponentContext key = bp.getComponentContext();
        Object[] result = new Object[ m_paramTypes.size()];
        RefPair<?, ?> refPair = bp.getRefPair();
        int i = 0;
        for ( ValueUtils.ValueType pt: m_paramTypes )
        {
            result[i] = ValueUtils.getValue(getComponentClass().getName(), pt,
                method.getParameterTypes()[i], key, refPair, null);
            i++;
        }
        return result;
    }


    @Override
    protected String getMethodNamePrefix()
    {
        return "bind";
    }

}
