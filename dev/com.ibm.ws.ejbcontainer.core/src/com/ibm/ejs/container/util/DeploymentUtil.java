/*******************************************************************************
 * Copyright (c) 1998, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType;

/**
 * This class contains helper methods for deploying EJBs into EJS. <p>
 */

public final class DeploymentUtil
{
    private static final TraceComponent tc = Tr.register(DeploymentUtil.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    public static final String declaredUncheckedAreSystemExceptions =
                    "com.ibm.websphere.ejbcontainer.declaredUncheckedAreSystemExceptions"; // d660332
    public static final boolean DeclaredUncheckedAreSystemExceptions = System.getProperty
                    (declaredUncheckedAreSystemExceptions, "true").equalsIgnoreCase("true"); // d660332

    public static final String declaredRemoteAreApplicationExceptions =
                    "com.ibm.websphere.ejbcontainer.declaredRemoteAreApplicationExceptions";
    public static final boolean DeclaredRemoteAreApplicationExceptions =
                    Boolean.getBoolean(declaredRemoteAreApplicationExceptions);

    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Property: DeclaredUncheckedAreSystemExceptions = " + DeclaredUncheckedAreSystemExceptions);
            Tr.debug(tc, "Property: DeclaredRemoteAreApplicationExceptions = " + DeclaredRemoteAreApplicationExceptions);
        }
    }

    /**
     * Get method name string of form:
     *
     * methodName(<class name of arg1>,<class name of arg2>,...)
     */
    public static String methodKey(Method m)
    {
        StringBuffer result = new StringBuffer(m.getName());

        result.append("(");

        Class<?> argTypes[] = m.getParameterTypes();
        for (int i = 0; i < argTypes.length; i++) {
            result.append(argTypes[i].getName());
            result.append(",");
        }
        result.append(")");
        return result.toString();
    }

    /**
     * Returns all the methods belonging to an interface (including
     * its super interfaces). Filters out methods belonging to the
     * javax.ejb.EJBObject interface (except remove) and method synonyms
     * (in case method overrides are present in the inheritance hierarchy).
     *
     * The same as getMethods(), except including remove method of
     * interface javax.ejb.EJBObject
     *
     * This method used only by EJBDeploy.
     */
    public static Method[] getAllMethods(Class<?> intf)
    {
        // Changed from using a Vector and Hashtable to using an
        // ArrayList and HashMap to improve performance.                 d366807.3

        Method[] allMethods = intf.getMethods();
        ArrayList<Method> result = new ArrayList<Method>(allMethods.length);
        HashMap<String, Method> methodNameTable = new HashMap<String, Method>();

        for (int i = 0; i < allMethods.length; i++) {
            Method m = allMethods[i];

            //---------------------------
            // Filter out static methods
            //---------------------------

            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            String mKey = methodKey(m);
            String interfaceName = m.getDeclaringClass().getName();
            if ((!(interfaceName.equals("javax.ejb.EJBObject") || // d135803
                interfaceName.equals("javax.ejb.EJBLocalObject"))) // d135803
                || m.getName().equals("remove")) { // d135803
                Method synonym = methodNameTable.get(mKey);
                if (synonym == null) {
                    methodNameTable.put(mKey, m);
                    result.add(m); // d366807.3
                } else {
                    //---------------------------------------------
                    // Method declared on most specific class wins
                    //---------------------------------------------
                    Class<?> mClass = m.getDeclaringClass();
                    Class<?> sClass = synonym.getDeclaringClass();
                    if (sClass.isAssignableFrom(mClass)) {
                        methodNameTable.put(mKey, m);
                        result.set(result.indexOf(synonym), m); // d366807.3
                    }
                }
            }
        }

        return sortMethods(result);
    }

    /**
     * Returns all the methods belonging to an interface (including
     * its super interfaces except the EJBObject and EJBLocal
     * interface). Filters out method synonyms (in case method
     * overrides are present in the inheritance hierarchy).
     *
     * This method returns the same method array as calling
     * getMethods(intf, null), but this method signature must
     * be maintained to support EJBDeploy.
     */
    public static Method[] getMethods(Class<?> intf)
    {
        return getMethods(intf, null); // d366807.3
    } // getMethods

    /**
     * Returns all the methods belonging to a component interface and the
     * corresponding (local or remote) business interfaces (including the
     * super interfaces, except the EJBObject and EJBLocal interface). <p>
     *
     * Also, filters out static methods and method synonyms (in case method
     * overrides are present in the inheritance hierarchy). <p>
     *
     * A combined list of methods from both the component and business
     * interfaces is desired, since any configured attributes (like
     * transaction or security) must apply to both the component and
     * busintess interfaces of the same type (local or remote). <p>
     *
     * @param componentInterface Local or Remote Component interface
     * @param businessInterfaces Array of Local or Remote Business interfaces
     *
     * @return all methods belonging to the component and business interfaces.
     */
    // d366807
    public static Method[] getMethods(Class<?> componentInterface,
                                      Class<?>[] businessInterfaces)
    {
        int numMethods = 0;
        Method[] methods = null;
        int numBusinessInterfaces = 0;
        HashMap<String, Method> methodNameTable = new HashMap<String, Method>();

        if (componentInterface != null)
        {
            methods = componentInterface.getMethods();
            numMethods = methods.length;
        }

        if (businessInterfaces != null)
        {
            numBusinessInterfaces = businessInterfaces.length;
        }

        ArrayList<Method> result = new ArrayList<Method>(numMethods + (numBusinessInterfaces * 10));

        //------------------------------------------------------------------------
        // First, iterate over the component interface methods (i.e. list
        // returned by "getMethods") and filter out methods belonging to the
        // EJBObject and EJBLocalObject interface, static methods, and
        // method synonyms.
        //
        // Method synonyms correspond to overrides. For all synonyms,
        // keep the method instance declared on the most specific class.
        // How do you determine the most specific class? Don't know.
        // For now, if the method is declared on the interface class
        // it always overrides others.
        //------------------------------------------------------------------------

        for (int i = 0; i < numMethods; i++)
        {
            Method m = methods[i];

            // Filter out static methods
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            String mKey = methodKey(m);
            String interfaceName = m.getDeclaringClass().getName();
            if (!interfaceName.equals("javax.ejb.EJBObject") && // f111627.1
                !interfaceName.equals("javax.ejb.EJBLocalObject")) // f111627.1
            {
                Method synonym = methodNameTable.get(mKey);
                if (synonym == null)
                {
                    methodNameTable.put(mKey, m);
                    result.add(m);
                }
                else
                {
                    // Method declared on most specific class wins
                    Class<?> mClass = m.getDeclaringClass();
                    Class<?> sClass = synonym.getDeclaringClass();
                    if (sClass.isAssignableFrom(mClass))
                    {
                        methodNameTable.put(mKey, m);
                        result.set(result.indexOf(synonym), m);
                    }
                }
            }
        }

        //------------------------------------------------------------------------
        // Second, iterate over the business interface methods (i.e. list
        // returned by "getMethods") and filter out static methods, and
        // method synonyms.                                              d366807.3
        //
        // Note: Business interfaces do NOT implement EJBObject or EJBLocalObject.
        //------------------------------------------------------------------------

        for (int i = 0; i < numBusinessInterfaces; ++i)
        {
            methods = businessInterfaces[i].getMethods();
            numMethods = methods.length;
            result.ensureCapacity(result.size() + numMethods);

            for (int j = 0; j < numMethods; j++)
            {
                Method m = methods[j];

                // Filter out static and bridge methods
                if (Modifier.isStatic(m.getModifiers()) || m.isBridge()) {
                    continue;
                }

                // Filter out Object methods for No-Interface view         F743-1756
                Class<?> declaring = m.getDeclaringClass();
                if (declaring == Object.class) {
                    continue;
                }

                String mKey = methodKey(m);

                // d583041 - The container is responsible for implementing these
                // methods, so do not include them in business interfaces.
                if ((mKey.equals("equals(java.lang.Object,)") && m.getReturnType() == Boolean.TYPE) ||
                    (mKey.equals("hashCode()") && m.getReturnType() == Integer.TYPE))
                {
                    continue;
                }

                Method synonym = methodNameTable.get(mKey);
                if (synonym == null)
                {
                    methodNameTable.put(mKey, m);
                    result.add(m);
                }
                else
                {
                    // Method declared on most specific class wins
                    Class<?> mClass = m.getDeclaringClass();
                    Class<?> sClass = synonym.getDeclaringClass();
                    if (sClass.isAssignableFrom(mClass))
                    {
                        methodNameTable.put(mKey, m);
                        result.set(result.indexOf(synonym), m);
                    }
                }
            }
        }

        //------------------------------------------------------------------------
        // Finally, return the methods sorted alphabetically.
        //------------------------------------------------------------------------

        return sortMethods(result);
    } // getMethods

    /**
     * Sort an ArrayList of methods using a simple insertion sort. <p>
     *
     * Replaced prior version of sortMethods(Method methods[]) to improve
     * performance by allowing the caller to avoid creating an intermediate
     * array object.... and just pass an ArrayList directly. <p>
     *
     * @param methods list of methods to be sorted.
     *
     * @return array of sorted methods.
     */
    // d366807.3
    private static Method[] sortMethods(ArrayList<Method> methods)
    {
        int numMethods = methods.size();
        Method result[] = new Method[numMethods];

        // Insert each element of given list of methods into result
        // array in sorted order.

        for (int i = 0; i < numMethods; i++)
        {
            Method currMethod = methods.get(i);
            String currMethodName = currMethod.toString();
            int insertIndex = 0;
            while (insertIndex < i)
            {
                if (currMethodName.compareTo(result[insertIndex].toString()) <= 0)
                {
                    break;
                }
                insertIndex++;
            }
            for (int j = insertIndex; j <= i; j++)
            {
                Method tmpMethod = result[j];
                result[j] = currMethod;
                currMethod = tmpMethod;
            }
        }
        return result;
    } // sortMethods

    /**
     * Returns a list of the non-public (and non-static) methods declared
     * on the EJB class, or inherited from a super class (excluding those
     * methods from java.lang.Object). <p>
     *
     * For methods which have been overriden (i.e. same name and parameters),
     * only the override will be included. <p>
     *
     * @param ejbClass the EJB implementation class.
     * @param publicMethods list of previously identified public methods
     *
     * @return List of non-public (non-static) methods declared on or inherited
     *         by the EJB implementation class.
     **/
    // F743-1756
    public static ArrayList<Method> getNonPublicMethods(final Class<?> ejbClass,
                                                        final Method[] publicMethods)
    {

        return AccessController.doPrivileged(new PrivilegedAction<ArrayList<Method>>() {
            @Override
            public ArrayList<Method> run() {
                return getNonPublicMethodsPrivileged(ejbClass, publicMethods);
            }
        });
    }

    private static ArrayList<Method> getNonPublicMethodsPrivileged(Class<?> ejbClass,
                                                                   Method[] publicMethods)
    {
        Class<?> thisClass = ejbClass;
        HashMap<String, ArrayList<Method>> methodMap = new HashMap<String, ArrayList<Method>>();

        while (thisClass != null && thisClass != Object.class)
        {
            Method[] thisMethods = thisClass.getDeclaredMethods();
            for (Method thisMethod : thisMethods)
            {
                int modifiers = thisMethod.getModifiers();

                // Skip static and bridge methods
                if (Modifier.isStatic(modifiers) ||
                    thisMethod.isBridge()) // F743-1756CodRv
                {
                    continue;
                }

                // Include non-public methods (not overridden)
                if (!Modifier.isPublic(modifiers))
                {
                    boolean found = false;
                    String methodName = thisMethod.getName();
                    ArrayList<Method> existingMethods = methodMap.get(methodName);
                    if (existingMethods == null)
                    {
                        // Detect duplicte with public methods - may occur if a
                        // protected method is overriden and made public.      d608130
                        for (Method publicMethod : publicMethods)
                        {
                            if (methodsMatch(thisMethod, publicMethod))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                        {
                            existingMethods = new ArrayList<Method>();
                            existingMethods.add(thisMethod);
                            methodMap.put(methodName, existingMethods);
                        }
                    }
                    else
                    {
                        for (Method existingMethod : existingMethods)
                        {
                            if (methodsMatch(thisMethod, existingMethod))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (!found)
                        {
                            // Detect duplicte with public methods - may occur if a
                            // protected method is overriden and made public.   d608130
                            for (Method publicMethod : publicMethods)
                            {
                                if (methodsMatch(thisMethod, publicMethod))
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found)
                        {
                            existingMethods.add(thisMethod);
                        }
                    }
                }
            }
            thisClass = thisClass.getSuperclass();
        }

        ArrayList<Method> entireMethodList = new ArrayList<Method>();
        for (ArrayList<Method> list : methodMap.values())
        {
            entireMethodList.addAll(list);
        }

        return entireMethodList;
    }

    public static boolean methodsMatch(Method m1, Method m2)
    {
        return m1 == m2 ||
               (m1.getName().equals(m2.getName()) &&
               Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes()));
    }

    public enum DeploymentTarget // d660332
    {
        STUB,
        TIE,
        WRAPPER,
    }

    /**
     * Returns a list of 'checked'/Application exceptions, and also
     * performs validation. <p>
     *
     * RemoteException is never a 'checked' exception, and exceptions that
     * are subclasses of other 'checked' exceptions will either be eliminated,
     * or sorted in parent-last order to avoid 'unreachable' code. <p>
     *
     * The following rules from the EJB Specification will be checked:
     * <ul>
     * <li> Only Remote interfaces that implement java.rmi.Remote may
     * throw RemoteException.
     * <li> Application exceptions must not subclass RemoteException. For interfaces
     * that implement java.rmi.Remote, they will be tolerated; they will not appear
     * in returned list.
     * <li> Only EJB 3.0 or later applications may have Application exceptions
     * that subclass RuntimeException.
     * <li> All methods on an interface that implements java.rmi.Remote
     * must throw RemoteException or superclass of RemoteException.
     * </ul>
     *
     * This method is designed for use when generating the EJB Wrappers,
     * to determine which exceptions will require 'catch' blocks, and
     * when generating Ties and Stubs, to properly add code that
     * returns the 'checked' exceptions to the client. <p>
     *
     * @param method Java method to determine checked exceptions for.
     * @param isRmiRemote true if the interface implements java.rmi.Remote.
     * @param target the deployment target for generating code
     *
     * @return an array of checked/application exceptions that must be
     *         handled by the generated wrapper.
     **/
    // d413752.1 d413752.2
    public static Class<?>[] getCheckedExceptions(Method method,
                                                  boolean isRmiRemote,
                                                  DeploymentTarget target)
                    throws EJBConfigurationException
    {
        return getCheckedExceptions(method, isRmiRemote, target, null);
    }

    /**
     * Returns a list of 'checked'/Application exceptions, and also
     * performs validation. <p>
     *
     * RemoteException is never a 'checked' exception, and exceptions that
     * are subclasses of other 'checked' exceptions will either be eliminated,
     * or sorted in parent-last order to avoid 'unreachable' code. <p>
     *
     * The following rules from the EJB Specification will be checked:
     * <ul>
     * <li> Only Remote interfaces that implement java.rmi.Remote may
     * throw RemoteException.
     * <li> Application exceptions must not subclass RemoteException unless a
     * custom property is specified. For interfaces that implement java.rmi.Remote,
     * they will be tolerated; they will not appear in returned list.
     * <li> Only EJB 3.0 or later applications may have Application exceptions
     * that subclass RuntimeException.
     * <li> All methods on an interface that implements java.rmi.Remote
     * must throw RemoteException or superclass of RemoteException.
     * </ul>
     *
     * This method is designed for use when generating the EJB Wrappers,
     * to determine which exceptions will require 'catch' blocks, and
     * when generating Ties and Stubs, to properly add code that
     * returns the 'checked' exceptions to the client. <p>
     *
     * @param method Java method to determine checked exceptions for.
     * @param isRmiRemote true if the interface implements java.rmi.Remote.
     * @param target the deployment target for generating code
     * @param wrapperType wrapper type if target is WRAPPER and validation is
     *            required, or null otherwise
     *
     * @return an array of checked/application exceptions that must be
     *         handled by the generated wrapper.
     **/
    // d413752.1 d413752.2
    public static Class<?>[] getCheckedExceptions(Method method,
                                                  boolean isRmiRemote,
                                                  DeploymentTarget target,
                                                  EJBWrapperType wrapperType)
                    throws EJBConfigurationException
    {
        return getCheckedExceptions(method, isRmiRemote, target, wrapperType,
                                    DeclaredUncheckedAreSystemExceptions,
                                    DeclaredRemoteAreApplicationExceptions);
    }

    /**
     * Returns a list of 'checked'/Application exceptions, and also
     * performs validation. <p>
     *
     * RemoteException is never a 'checked' exception, and exceptions that
     * are subclasses of other 'checked' exceptions will either be eliminated,
     * or sorted in parent-last order to avoid 'unreachable' code. <p>
     *
     * The following rules from the EJB Specification will be checked:
     * <ul>
     * <li> Only Remote interfaces that implement java.rmi.Remote may
     * throw RemoteException.
     * <li> Application exceptions must not subclass RemoteException unless
     * declaredRemoteAreApplicationExceptions is true. For interfaces that
     * implement java.rmi.Remote, they will be tolerated; they will not appear
     * in returned list.
     * <li> Only EJB 3.0 or later applications may have Application exceptions
     * that subclass RuntimeException.
     * <li> All methods on an interface that implements java.rmi.Remote
     * must throw RemoteException or superclass of RemoteException.
     * </ul>
     *
     * This method is designed for use when generating the EJB Wrappers,
     * to determine which exceptions will require 'catch' blocks, and
     * when generating Ties and Stubs, to properly add code that
     * returns the 'checked' exceptions to the client. <p>
     *
     * @param method Java method to determine checked exceptions for.
     * @param isRmiRemote true if the interface implements java.rmi.Remote.
     * @param target the deployment target for generating code
     * @param wrapperType wrapper type if target is WRAPPER and validation is
     *            required, or null otherwise
     * @param declaredUncheckedAreSystemExceptions true if unchecked exceptions
     *            on the throws clause should be considered as system exceptions
     * @param declaredRemoteAreApplicationExceptions true if RemoteExceptions
     *            on the throws clause should be considered as application exceptions
     *
     * @return an array of checked/application exceptions that must be
     *         handled by the generated wrapper.
     **/
    // d413752.1 d413752.2
    public static Class<?>[] getCheckedExceptions(Method method,
                                                  boolean isRmiRemote,
                                                  DeploymentTarget target,
                                                  EJBWrapperType wrapperType,
                                                  boolean declaredUncheckedAreSystemExceptions,
                                                  boolean declaredRemoteAreApplicationExceptions)
                    throws EJBConfigurationException
    {
        boolean throwsRemoteException = false;
        Class<?>[] exceptions = method.getExceptionTypes();
        int numExceptions = exceptions.length;
        ArrayList<Class<?>> checkedExceptions = new ArrayList<Class<?>>(numExceptions);

        for (Class<?> exception : exceptions)
        {
            // --------------------------------------------------------------------
            // Only Remote interfaces that implement java.rmi.Remote may throw
            // java.rmi.RemoteException.
            // --------------------------------------------------------------------
            if (exception == RemoteException.class)
            {
                // Required for RMI Remote and allowed for 2.x local by EJBDeploy,
                // so no warning for remote or 2.x local interfaces
                if (!isRmiRemote && (wrapperType != EJBWrapperType.LOCAL) && (wrapperType != EJBWrapperType.LOCAL_HOME))
                {
                    // This is only a warning since the spec says 'should' and not
                    // must... but let them know it is a bad idea.            d450525
                    Tr.warning(tc, "JIT_INVALID_THROW_REMOTE_CNTR5101W",
                               new Object[] { method.getName(),
                                             method.getDeclaringClass().getName() });
                }
                throwsRemoteException = true;
                continue; // skip - remote allowed, but not 'checked' exception
            }

            // --------------------------------------------------------------------
            // Application exceptions must not subclass RemoteException.
            // --------------------------------------------------------------------
            else if ((RemoteException.class).isAssignableFrom(exception))
            {
                // Tolerate for stubs since the server EJB might allow.    RTC116527
                if (target == DeploymentTarget.STUB)
                {
                    // Keep the exception.
                }
                // For tie/wrapper, only allow if a custom property is specified and
                // only then for business interfaces.                      RTC116527
                else if (declaredRemoteAreApplicationExceptions &&
                         (wrapperType == null ||
                          wrapperType == EJBWrapperType.BUSINESS_REMOTE ||
                         wrapperType == EJBWrapperType.BUSINESS_LOCAL))
                {
                    // Keep the exception.
                }
                // For tie/wrapper that extends RMI Remote, ignore since a superclass
                // exception (Exception or RemoteException) must be present and will
                // handle reporting as an unchecked exception. Also ignoring for 2.x
                // local interfaces, since they were tolerated by EJBDeploy.
                else if (isRmiRemote || wrapperType == EJBWrapperType.LOCAL || wrapperType == EJBWrapperType.LOCAL_HOME)
                {
                    // Remove the exception; tolerated and ignored, not checked
                    continue;
                }
                else
                {
                    // Log the error and throw meaningful exception.             d450525
                    String className = method.getDeclaringClass().getName();
                    Tr.error(tc, "JIT_INVALID_SUBCLASS_REMOTE_EX_CNTR5102E",
                             new Object[] { exception.getName(),
                                           method.getName(),
                                           className });
                    throw new EJBConfigurationException("Application exception " + exception.getName() +
                                                        " defined on method " + method.getName() + " of interface " +
                                                        className + " must not subclass java.rmi.RemoteException.");
                }
            }

            // --------------------------------------------------------------------
            // Per the spec, unchecked exceptions (RuntimeException and Error)
            // should not be considered application.  They are also ignored by
            // rmic when generating stubs and ties.                         d660332
            // --------------------------------------------------------------------
            else if (RuntimeException.class.isAssignableFrom(exception) ||
                     Error.class.isAssignableFrom(exception)) // 651626.1
            {
                // For stubs, we keep unchecked exceptions so that they can handle
                // IDL strings from ties generated by previous releases where
                // unchecked exceptions were not filtered.
                if (target == DeploymentTarget.STUB)
                {
                    // Keep the exception.
                }
                // For wrappers, we keep unchecked exceptions if the user has
                // indicated that unchecked exceptions should be considered
                // application exceptions.
                else if (target == DeploymentTarget.WRAPPER &&
                         !declaredUncheckedAreSystemExceptions)
                {
                    // Keep the exception.
                }
                else
                {
                    // Remove the exception.
                    continue;
                }
            }

            // --------------------------------------------------------------------
            // Per the spec, application exceptions must subclass Exception
            // (not Throwable or Error)
            // --------------------------------------------------------------------
            else if (!Exception.class.isAssignableFrom(exception)) // d608631
            {
                String className = method.getDeclaringClass().getName();
                Tr.error(tc, "JIT_INVALID_NOT_EXCEPTION_SUBCLASS_CNTR5107E",
                         new Object[] { exception.getName(),
                                       method.getName(),
                                       className });

                throw new EJBConfigurationException("The " + exception.getName() +
                                                    " application exception defined on the " + method.getName() +
                                                    " method of the " + className +
                                                    " class must be defined as a subclass of the java.lang.Exception class.");
            }

            // --------------------------------------------------------------------
            // Per the RMI specification, Remote interface methods must throw
            // RemoteException or any superclass, so Exception and IOException
            // count as throwing RemoteException.
            // --------------------------------------------------------------------
            else if (exception.isAssignableFrom(RemoteException.class)) {
                throwsRemoteException = true;
            }

            // --------------------------------------------------------------------
            // Exception is a valid application exception... but it cannot just
            // be added to the end of the list, since it may be a subclass of
            // other exceptions on the throws clause, which could result in
            // 'unreachable' code when adding 'catch' blocks.
            //
            // Instead, do one of two things:
            // 1 - Remove any exceptions that are subclasses of other exceptions.
            //     This is useful when a catch block is being added for each
            //     checked exception, and the processing done in the parent
            //     exception would be the same as any subclasses.  For example,
            //     in a generated wrapper, catch blocks for all checked exceptions
            //     just call 'setCheckedException'... so no need to duplicate that
            //     for all subclass exceptions.
            // 2 - Insert the current exception ahead of the first exception
            //     already in the list that is a parent.  This eliminates the
            //     'unreachable' code problem, and also ends up with the
            //     exceptions in the original order if there are no subclasses.
            // --------------------------------------------------------------------

            if (target == DeploymentTarget.WRAPPER) // d660332
            {
                // -----------------------------------------------------------------
                // Remove any already added exceptions that are subclasses of the
                // current exception.  Ignore the current exception if a superclass
                // has already been added.                                 RTC116527
                // -----------------------------------------------------------------
                boolean add = true;
                for (Iterator<Class<?>> iter = checkedExceptions.iterator(); iter.hasNext();)
                {
                    Class<?> checkedEx = iter.next();
                    if (exception.isAssignableFrom(checkedEx))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "getCheckedExceptions: ignoring " +
                                         checkedEx.getName() + ", subclass of " +
                                         exception.getName());
                        iter.remove();
                    }
                    else if (checkedEx.isAssignableFrom(exception))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "getCheckedExceptions: ignoring " +
                                         exception.getName() + ", subclass of " +
                                         checkedEx.getName());
                        add = false;
                        break;
                    }
                }

                if (add)
                {
                    checkedExceptions.add(exception);
                }
            }
            else
            {
                // -----------------------------------------------------------------
                // Sort exceptions, so that subclasses are ahead of parents
                // -----------------------------------------------------------------
                int j;
                int numChecked = checkedExceptions.size();
                for (j = 0; j < numChecked; ++j)
                {
                    Class<?> checkedEx = checkedExceptions.get(j);
                    if (checkedEx.isAssignableFrom(exception))
                        break;
                }

                checkedExceptions.add(j, exception);
            }
        }

        // -----------------------------------------------------------------------
        // After all exceptions have been processed, make sure that a
        // RemoteException was present if the interface implements java.rmi.Remote
        // -----------------------------------------------------------------------
        if (isRmiRemote && !throwsRemoteException)
        {
            // Log the error and throw meaningful exception.                d450525
            String className = method.getDeclaringClass().getName();
            Tr.error(tc, "JIT_MISSING_REMOTE_EX_CNTR5104E",
                     new Object[] { method.getName(), className });
            throw new EJBConfigurationException("Method " + method.getName() + " of interface " + className +
                                                " must throw java.rmi.RemoteException");
        }

        return checkedExceptions.toArray(new Class[checkedExceptions.size()]);
    }

} // DeploymentUtil

