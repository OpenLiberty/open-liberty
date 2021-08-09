/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.omg.CORBA.portable.IDLEntity;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Just In Time deployment utility methods for mapping RMI to IDL for
 * Stub and Tie generation. <p>
 */
public final class RMItoIDL
{
    private static final TraceComponent tc = Tr.register(RMItoIDL.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    private static final String[] IDL_KEYWORDS = { // PM94096
    "abstract",
                    "any",
                    "attribute",
                    "boolean",
                    "case",
                    "char",
                    "component",
                    "const",
                    "consumes",
                    "context",
                    "custom",
                    "default",
                    "double",
                    "emits",
                    "enum",
                    "eventtype",
                    "exception",
                    "factory",
                    "false",
                    "finder",
                    "fixed",
                    "float",
                    "getraises",
                    "home",
                    "import",
                    "in",
                    "inout",
                    "interface",
                    "local",
                    "long",
                    "module",
                    "multiple",
                    "native",
                    "object",
                    "octet",
                    "oneway",
                    "out",
                    "primarykey",
                    "private",
                    "provides",
                    "public",
                    "publishes",
                    "raises",
                    "readonly",
                    "sequence",
                    "setraises",
                    "short",
                    "string",
                    "struct",
                    "supports",
                    "switch",
                    "true",
                    "truncatable",
                    "typedef",
                    "typeid",
                    "typeprefix",
                    "union",
                    "unsigned",
                    "uses",
                    "valuebase",
                    "valuetype",
                    "void",
                    "wchar",
                    "wstring",
    };

    private static boolean isIDLKeyword(String s) // PM94096
    {
        return Arrays.binarySearch(IDL_KEYWORDS, s, String.CASE_INSENSITIVE_ORDER) >= 0;
    }

    private static final String BOXED_IDL = "org_omg_boxedIDL_";

    /**
     * Returns the OMG IDL Exception name for the specified exception
     * class name. <p>
     * 
     * The name mangling performed by this method follows the OMG IDL
     * specification, which basically is as follows:
     * 
     * <ul>
     * <li> replace all '.' separators with '/'.
     * <li> prepend "IDL:"
     * <li> replace the ending "Exception" with "Ex" or add "Ex".
     * <li> (optionally) prepend 'J' if the name starts with '_',
     * or prepend '_' if the component name is an IDL keyword
     * <li> append ":1.0".
     * </ul>
     * 
     * For example, "javax.ejb.RemoveException" would be mapped to
     * "IDL:javax/ejb/RemoveEx:1.0".
     * 
     * @param exClassName fully qualified name of an exception class
     * @param mangleComponents true if component names should be mangled
     **/
    static String getIdlExceptionName(String exClassName, boolean mangleComponents)
    {
        StringBuilder idlName = new StringBuilder(256);

        idlName.append("IDL:");
        idlName.append(exClassName.replace('.', '/'));

        if (exClassName.endsWith("Exception"))
        {
            idlName.setLength(idlName.length() - 7);
        }
        else
        {
            idlName.append("Ex");
        }

        if (mangleComponents) // PM94096
        {
            for (int begin = 0; begin < idlName.length();)
            {
                int end = idlName.indexOf("/", begin);
                if (end == -1)
                {
                    end = idlName.length();
                }

                if (idlName.charAt(begin) == '_')
                {
                    idlName.insert(begin, 'J');
                    end++;
                }
                else
                {
                    String comp = idlName.substring(begin, end);
                    if (isIDLKeyword(comp))
                    {
                        idlName.insert(begin, '_');
                        end++;
                    }
                }

                begin = end + 1;
            }
        }

        idlName.append(":1.0");

        return idlName.toString();
    }

    /**
     * Returns an array of OMG IDL method names corresponding to the
     * specified java methods. <p>
     * 
     * Note: This is the original signature of this method, and the
     * implementation has one known incompatibility with RMIC.
     * It does not account for conflicts between the interface
     * name and one of the method/property names. See the new
     * signature of this method below for the correction to
     * this problem.
     * 
     * The mapping of java (RMI) method names to OMG IDL method names
     * is done in accordance with the CORBA specification. In brief,
     * the mapping rules are as follows:
     * 
     * <ul>
     * <li> Methods that follow the JavaBeans design patterns for simple
     * read-write properties or simple read-only properties are mapped
     * to OMG IDL interface attributes.
     * <li> For Java names that have leading underscores, the leading
     * underscore is replaced with "J_".
     * <li> For Java identifiers that contain illegal OMG IDL identifier
     * characters such as '$' or Unicode characters outside of
     * ISO Latin 1, any such illegal characters are replaced by
     * "U" followed by the 4 hexadecimal characters (in upper case)
     * representing the Unicode value.
     * <li> If a Java RMI/IDL method isn't overloaded, then the same method
     * name is used in OMG IDL as was used in Java.
     * <li> For other case-sensitive collisions, the rule is that if two
     * (or more) names that need to be defined in the same OMG IDL name
     * scope differ only in case, then a mangled name is generated
     * consisting of the original name followed by an underscore, followed
     * by an underscore separated list of decimal indices into the string,
     * where the indices identify all the upper case characters in the
     * original string.
     * <li> For overloaded RMI/IDL methods, the mangled OMG IDL name is formed
     * by taking the Java method name and then appending two underscores,
     * followed by each of the fully qualified OMG IDL types of the
     * arguments separated by two underscores.
     * </ul>
     * 
     * For specific details regarding the above, and for type specific mapping,
     * refer to the 'Java Language to IDL Mapping' chapter of the CORBA
     * specification. <p>
     * 
     * @param methods java methods for the remote interface.
     * @exception EJBConfigurationException if the methods are not
     *                valid RMI Remote methods.
     **/
    static String[] getIdlMethodNames(Method[] methods)
                    throws EJBConfigurationException
    {
        return getIdlMethodNames(methods, null, null); // d576626
    }

    /**
     * Returns an array of OMG IDL method names corresponding to the
     * specified java methods and remote interface. <p>
     * 
     * The 'idlNames' will be in the same order, corresponding to the methods.
     * However, for generating a Tie, the 'idlNames' may contain additional
     * entries at the end. This occurs when the class name conflicts with a
     * method or property name or an overloaded method has an IDLEntity as
     * a parameter or return value. The Tie will support BOTH names to be
     * compatible with both RMIC and the older versions of JITDeploy. <p>
     * 
     * Additional idl name entries will only be added to the end if the
     * compatibilityMethods parameter is non-null, and then the corresponding
     * method will be added to that parameter for each extra idl name. <p>
     * 
     * The mapping of java (RMI) method names to OMG IDL method names
     * is done in accordance with the CORBA specification. In brief,
     * the mapping rules are as follows:
     * 
     * <ul>
     * <li> Methods that follow the JavaBeans design patterns for simple
     * read-write properties or simple read-only properties are mapped
     * to OMG IDL interface attributes.
     * <li> For Java names that have leading underscores, the leading
     * underscore is replaced with "J_".
     * <li> For Java identifiers that contain illegal OMG IDL identifier
     * characters such as '$' or Unicode characters outside of
     * ISO Latin 1, any such illegal characters are replaced by
     * "U" followed by the 4 hexadecimal characters (in upper case)
     * representing the Unicode value.
     * <li> If a Java RMI/IDL method isn't overloaded, then the same method
     * name is used in OMG IDL as was used in Java.
     * <li> For other case-sensitive collisions, the rule is that if two
     * (or more) names that need to be defined in the same OMG IDL name
     * scope differ only in case, then a mangled name is generated
     * consisting of the original name followed by an underscore, followed
     * by an underscore separated list of decimal indices into the string,
     * where the indices identify all the upper case characters in the
     * original string.
     * <li> For overloaded RMI/IDL methods, the mangled OMG IDL name is formed
     * by taking the Java method name and then appending two underscores,
     * followed by each of the fully qualified OMG IDL types of the
     * arguments separated by two underscores.
     * <li> If the remote interface name collides with a method or property
     * name, then an "_" is appended to the method or property.
     * </ul>
     * 
     * For specific details regarding the above, and for type specific mapping,
     * refer to the 'Java Language to IDL Mapping' chapter of the CORBA
     * specification. <p>
     * 
     * @param methods java methods for the remote interface.
     * @param remoteInterface remote interface directly implemented by
     *            the Tie or Stub. Some methods may be inherited.
     * @param compatibilityMethods non-null if older (incorrect) idlNames should
     *            be returned and the corresponding methods will be added to this list.
     *            Expected to be set for generating a Tie.
     * @exception EJBConfigurationException if the methods are not
     *                valid RMI Remote methods.
     **/
    // refactored method - d576626
    static String[] getIdlMethodNames(Method[] methods,
                                      Class<?> remoteInterface,
                                      List<Method> compatibilityMethods)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getIdlMethodNames : " + methods.length);

        int numMethods = methods.length;

        String[] idlNames = new String[numMethods];
        boolean[] property = new boolean[numMethods];
        boolean[] overloaded = new boolean[numMethods];

        int capacity = (int) (numMethods * 1.5);
        HashMap<String, Integer> getProperties = new HashMap<String, Integer>(capacity);
        HashMap<String, Integer> isProperties = new HashMap<String, Integer>(capacity);
        HashMap<String, Integer> setProperties = new HashMap<String, Integer>(capacity);
        HashMap<String, Integer> overloadMap = new HashMap<String, Integer>(capacity);

        // First, identify all of the potential JavaBeans 'properties',
        // determined by methods that begin with 'get'or 'is'.  Not all of these
        // will be property methods, as there may be multiple get methods with
        // the same name (different return types) and 'is' methods that map to
        // the same property name.  Unfortunately, sorting out the real property
        // methods cannot be done until after any corresponding 'set' methods
        // have been identified (below).
        for (int i = 0; i < numMethods; ++i)
        {
            Method method = methods[i];
            String methodName = method.getName();
            Class<?>[] methodParams = method.getParameterTypes();

            if (methodParams.length == 0)
            {
                Integer duplicate = null;

                if (methodName.startsWith("get") &&
                    methodName.length() > 3 &&
                    onlyRemoteExceptions(method))
                {
                    String propertyName = getPropertyName(methodName, 3);
                    duplicate = getProperties.put(propertyName, i);
                }
                else if (methodName.startsWith("is") &&
                         methodName.length() > 2 &&
                         method.getReturnType() == Boolean.TYPE &&
                         onlyRemoteExceptions(method))
                {
                    String propertyName = getPropertyName(methodName, 2);
                    duplicate = isProperties.put(propertyName, i);
                }

                // It is not allowed for two 'get' or 'is' methods to map to
                // the same property name.  This can only occur where the
                // 'property' names differ only in the case of the first
                // letter; like 'isproperty' and 'isProperty'.
                if (duplicate != null)
                {
                    Class<?> remoteIntf = methods[duplicate].getDeclaringClass();
                    if (remoteIntf.isAssignableFrom(method.getDeclaringClass()))
                        remoteIntf = method.getDeclaringClass();

                    // Log the error and throw meaningful exception.          d450525
                    Tr.error(tc, "JIT_DUPLICATE_PROPERTY_METHODS_CNTR5105E",
                             new Object[] { remoteIntf.getName(),
                                           methods[duplicate].getName(),
                                           method.getName() });

                    // Also trace, to insure we see the entire method declarations
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "IDL name conflict (1): '" +
                                     methods[duplicate] + "' and '" + method + "'");

                    throw new EJBConfigurationException(remoteIntf.getName() + " is not a valid remote interface: " +
                                                        "the idl name for method '" + methods[duplicate] +
                                                        "' conflicts with method '" + method + "'");
                }
            }

            // Fill in the default idl name for all methods, including
            // set/get methods, as the final set of get/set methods
            // will not be known until all are processed.
            idlNames[i] = methodName;
        }

        // Collections of potential 'get' and 'is' methods have now been
        // identified.  The next step is to find and match the 'set' methods
        // up with the other two, since 'set' methods are only 'set' methods
        // if there is a corresponding 'get'/'is'.
        //
        // Also at this time, if a 'set' method is found that corresponds
        // to an 'is' method, then any corresponding 'get' method is removed
        // from the list, as 'is'/'set' matches have priority.
        for (int i = 0; i < numMethods; ++i)
        {
            Method method = methods[i];
            Class<?> methodReturn = method.getReturnType();
            Class<?>[] methodParams = method.getParameterTypes();

            if (methodReturn == Void.TYPE &&
                methodParams.length == 1)
            {
                String methodName = method.getName();
                Class<?> methodParam = methodParams[0];

                if (methodName.startsWith("set") &&
                    methodName.length() > 3 &&
                    onlyRemoteExceptions(method))
                {
                    Integer isIndex = null;
                    String propertyName = getPropertyName(methodName, 3);

                    // If the return type of this potential 'set' method is
                    // boolean, then first look for an 'is' method match,
                    // as 'is' method matches have higher priority.
                    if (methodParam == Boolean.TYPE)
                    {
                        isIndex = isProperties.get(propertyName);
                        if (isIndex != null)
                        {
                            // Property name matched... but the case of the
                            // original method must match, or it doesn't count.
                            if (methodName.regionMatches(3, methods[isIndex].getName(),
                                                         2, propertyName.length()))
                            {
                                // Note: When 'set' methods are overloaded, the one with
                                // the boolean signature takes priority, and so
                                // the put below may overlay any prior 'set' methods
                                // with a duplicate property name.  And, since this
                                // is a match on the 'is' method, any duplicate 'get'
                                // method needs to be removed as a property.
                                setProperties.put(propertyName, i);
                                getProperties.remove(propertyName);
                            }
                            else
                            {
                                isIndex = null;
                            }
                        }
                    }

                    // There was no matching 'is' method, so now try for a
                    // match with all possible 'get' methods.  As with 'is',
                    // the return type and original method case must match.
                    if (isIndex == null)
                    {
                        Integer getIndex = getProperties.get(propertyName);
                        if (getIndex != null &&
                            methodParam == methods[getIndex].getReturnType() &&
                            methodName.regionMatches(3, methods[getIndex].getName(),
                                                     3, propertyName.length()))
                        {
                            setProperties.put(propertyName, i);
                        }
                    }
                }
            }
        }

        // Now, collapse the set of 'is' methods into the 'get' methods
        // removing duplicates.  Generally, 'get' takes priority, unless
        // the return type is boolean (same as the 'is'), then 'is'
        // has priority.
        //
        // Also, if there is a matching 'set' for the boolean 'is', then
        // 'is' has priority... but that was handled above.
        for (String propertyName : isProperties.keySet())
        {
            Integer getIndex = getProperties.get(propertyName);

            // If an 'is' and 'get' method have the same property name,
            // then insure the case of the method names match, or it is
            // considered an error... as there would be two 'property'
            // methods that would map to the same idl.  If the case of
            // the two methods do match, then one is just treated as
            // an ordinary (non property) method... go figure.
            if (getIndex != null)
            {
                Integer isIndex = isProperties.get(propertyName);
                String isMethodName = methods[isIndex].getName();
                String getMethodName = methods[getIndex].getName();
                if (!isMethodName.regionMatches(2, getMethodName,
                                                3, propertyName.length()))
                {
                    Class<?> remoteIntf = methods[isIndex].getDeclaringClass();
                    Class<?> getIntf = methods[getIndex].getDeclaringClass();
                    if (remoteIntf.isAssignableFrom(getIntf))
                        remoteIntf = getIntf;

                    // Log the error and throw meaningful exception.          d450525
                    Tr.error(tc, "JIT_DUPLICATE_PROPERTY_METHODS_CNTR5105E",
                             new Object[] { remoteIntf.getName(),
                                           isMethodName,
                                           getMethodName });

                    // Also trace, so we log which check determined the problem
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "IDL name conflict (2): " +
                                     isMethodName + " and " + getMethodName);

                    throw new EJBConfigurationException(remoteIntf.getName() + " is not a valid remote interface: " +
                                                        "the idl name for method " + isMethodName +
                                                        " conflicts with method " + getMethodName);
                }
            }

            // So, just copy over all 'is' methods that are unique, or where the
            // 'get' method is also boolean; which results in the get method
            // becoming a non-property method. Also, 'is' methods that are not
            // copied here just become non-property methods.
            if (getIndex == null ||
                methods[getIndex].getReturnType() == Boolean.TYPE)
            {
                getProperties.put(propertyName, isProperties.get(propertyName));
            }
        }

        // And, to finish off 'get' property identification processing, set all
        // of the idl names for the get methods to the property name.
        for (String propertyName : getProperties.keySet())
        {
            int getIndex = getProperties.get(propertyName);
            idlNames[getIndex] = propertyName;
            property[getIndex] = true;
        }

        // Do something similar for the 'set' property names, except set them
        // all to 'null', so that they don't look like 'overrides', and so
        // they don't get mangled in a different way.  In the end, the 'set'
        // methods will be set based on the mangled 'get' property name.
        for (String propertyName : setProperties.keySet())
        {
            int setIndex = setProperties.get(propertyName);
            idlNames[setIndex] = null;
            property[setIndex] = true;
        }

        // Next, look for method/property names that differ only in case,
        // and 'mangle' them by adding the indexes of upper case letters.
        boolean[] caseDiff = new boolean[numMethods];
        for (int i = 0; i < numMethods; ++i)
        {
            String idlName = idlNames[i];

            // skip over methods that have already been identified as
            // having a case diff problem, and 'set' methods (null).
            if (caseDiff[i] == false &&
                idlName != null)
            {
                // Only need to look at methods after the current one,
                // as all prior methods have been processed.
                for (int j = i + 1; j < numMethods; ++j)
                {
                    String compName = idlNames[j];
                    if (idlName.equalsIgnoreCase(compName))
                    {
                        // Once the current method has been identified
                        // as having a case diff hit... all subsequent
                        // matches also need to be mangled, regardless
                        // of case.
                        if (caseDiff[i] == true)
                        {
                            caseDiff[j] = true;
                        }

                        // Otherwise, only mark for a case diff hit if
                        // the case is truly different.  An exact match
                        // is considered an 'overloaded' method, and will
                        // be processed later.
                        else if (!idlName.equals(compName))
                        {
                            caseDiff[i] = true;
                            caseDiff[j] = true;
                        }
                    }
                }
            }

            // Go ahead and mangle the current name now.  Other methods that
            // have been identified cannot be mangled until they are the current
            // method, so that methods after the current one don't match on
            // the mangled name, only the original name.
            if (caseDiff[i] == true)
            {
                idlNames[i] = getCaseDiffName(idlName);
            }
        }

        // Now that all of the property names have been set... and names
        // that differ only in case have been handled, it is time to
        // deal with special character.
        //
        // There are 3 types of special characters:
        // <ol>
        // <li> a leading '_' (which is prefixed by a 'J')
        // <li> illegal OMG IDL characters, like '$'
        // <li> unicode characters outside of ISO Latin 1
        // </ol>
        //
        // Other than the leading '_', the rest are converted to unicode.
        StringBuilder idlbldr = new StringBuilder(256);
        for (int i = 0; i < numMethods; ++i)
        {
            String idlName = idlNames[i];
            if (idlName != null)
            {
                idlbldr.setLength(0);
                idlbldr.append(idlNames[i]);
                idlNames[i] = convertSpecialCharacters(idlbldr);
            }
        }

        // Next, look for duplicate names (i.e. overloaded methods) and perform
        // the necessary mangling to make them unique. In the first pass, only
        // non-property methods are mangled, and property names are NOT
        // considered when looking for duplicates.
        for (int i = 0; i < numMethods; ++i)
        {
            if (property[i])
                continue;

            Integer Dup = overloadMap.put(idlNames[i], i);

            if (Dup != null)
            {
                int dup = Dup.intValue();

                if (!overloaded[dup] &&
                    !property[dup])
                {
                    idlNames[dup] = getOverloadedIdlName(idlNames[dup], methods[dup]);
                    overloaded[dup] = true;
                }

                if (!property[i])
                {
                    idlNames[i] = getOverloadedIdlName(idlNames[i], methods[i]);
                    overloaded[i] = true;
                }
            }
        }

        // Then, process overrides for the properties, including the
        // non-property methods in the duplicate search.
        overloadMap.clear();
        for (int i = 0; i < numMethods; ++i)
        {
            String idlName = idlNames[i];

            if (idlName == null) // Nothing to do for 'set' methods
                continue;

            Integer Dup = overloadMap.put(idlName, i);

            if (Dup != null)
            {
                int dup = Dup.intValue();

                if (!property[dup] &&
                    !property[i])
                {
                    Class<?> remoteIntf = methods[i].getDeclaringClass();
                    Class<?> dupIntf = methods[dup].getDeclaringClass();
                    if (remoteIntf.isAssignableFrom(dupIntf))
                        remoteIntf = dupIntf;

                    // Log the error and throw meaningful exception.          d450525
                    Tr.error(tc, "JIT_DUPLICATE_PROPERTY_METHODS_CNTR5105E",
                             new Object[] { remoteIntf.getName(),
                                           methods[i].getName(),
                                           methods[dup].getName() });

                    // Also trace, so we log which check determined the problem
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "IDL name conflict (3): '" +
                                     methods[i] + "' and '" + methods[dup] + "'");

                    throw new EJBConfigurationException(remoteIntf.getName() + " is not a valid remote interface: " +
                                                        "the idl name for method '" + methods[i] +
                                                        "' conflicts with method '" + methods[dup] + "'");
                }

                if (!overloaded[dup] &&
                    property[dup])
                {
                    idlNames[dup] = getOverloadedIdlName(idlNames[dup], methods[dup]);
                    overloaded[dup] = true;
                }

                if (property[i])
                {
                    idlNames[i] = getOverloadedIdlName(idlName, methods[i]);
                    overloaded[i] = true;
                }
            }
        }

        // Next, look for method/property names that match the remote interface
        // class name, differing only in case (IDL is not case sensitive). The
        // IDL name of the class must not conflict with the IDL name of the
        // method or property.  An '_' is added to the method or property to
        // resolve the conflict.                                           d576626
        if (remoteInterface != null)
        {
            // To match the IBM jdk rmic, convert the special characters in the
            // interface name before comparing it to the methods and properties
            // (this includes adding J at the beginning).  The Sun jdk rmic does
            // not appear to do this conversion of the interface name.
            idlbldr.setLength(0);
            idlbldr.append(remoteInterface.getSimpleName());
            String interfaceName = convertSpecialCharacters(idlbldr);

            for (int i = 0; i < numMethods; ++i)
            {
                String idlName = idlNames[i];
                if (interfaceName.equalsIgnoreCase(idlName))
                {
                    int conflict = -1;
                    String newIdlName = idlName + "_";

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "IDL name conflict between class " +
                                     "and method names : " + idlName + " -> " +
                                     newIdlName);

                    // Look for a conflict between this new idl name and another
                    // method or property with the same name. Note that this is
                    // being done BEFORE adding _set/_get to the property names
                    // as the conflict is with the property idl name itself, and
                    // not the get/set idl name.
                    for (int j = 0; j < numMethods; ++j)
                    {
                        if (newIdlName.equals(idlNames[j]))
                        {
                            conflict = j;
                            break;
                        }
                    }

                    // If the IDL names are being obtained for generating a Tie,
                    // then just add the new IDL name and corresponding method
                    // to the end of the lists. This way, the Tie generation code
                    // will support BOTH and will thus be compatible with Stubs
                    // generated by either RMIC or JITDeploy (before this fix).
                    // But, if the new IDL name had a conflict with yet another
                    // name, then just ignore it... as an RMIC Stub could not be
                    // generated for this class, so only the original name needs
                    // to be support for Stubs generated with JITDeploy.
                    if (compatibilityMethods != null)
                    {
                        if (conflict < 0)
                        {
                            idlNames = Arrays.copyOf(idlNames, idlNames.length + 1);
                            idlNames[idlNames.length - 1] = newIdlName;
                            compatibilityMethods.add(methods[i]); // d684761
                        }
                        else
                        {
                            // Also trace, so we know about the problem
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, INDENT + "IDL name conflict (4): '" +
                                             methods[i] + "' and '" + methods[conflict] + "'");
                        }
                    }

                    // Otherwise, if the IDL names are being obtained for
                    // generating  a Stub, then just replace the original name
                    // with the new name.  Stubs cannot support both, as they
                    // may only pass one name on a method call.  If the new
                    // idl name conflicts with yet another name, then throw
                    // an exception with the same text as RMIC.  Note that
                    // this code is only executed if the customer has asked
                    // to be compatible with RMIC.
                    else
                    {
                        if (conflict < 0)
                        {
                            idlNames[i] = newIdlName;
                        }
                        else
                        {
                            // Log the error and throw meaningful exception.
                            Tr.error(tc, "JIT_DUPLICATE_PROPERTY_METHODS_CNTR5105E",
                                     new Object[] { remoteInterface.getName(),
                                                   methods[i].getName(),
                                                   methods[conflict].getName() });

                            // Also trace, so we log which check determined the problem
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, INDENT + "IDL name conflict (4): '" +
                                             methods[i] + "' and '" + methods[conflict] + "'");

                            throw new EJBConfigurationException(remoteInterface.getName() +
                                                                " is not a valid remote interface: " +
                                                                "the idl name for method '" + methods[i] +
                                                                "' conflicts with method '" + methods[conflict] + "'");
                        }
                    }
                }
            }
        }

        // Almost done.  Now, add '_set_' as a prefix to all 'set'
        // property names.  Remember that the 'set' idl names have
        // all been set to 'null', and so the corresponding 'get'
        // name is used.
        for (String propertyName : setProperties.keySet())
        {
            int setIndex = setProperties.get(propertyName);
            String idlName = idlNames[getProperties.get(propertyName)];
            idlNames[setIndex] = "_set_" + idlName;
        }

        // This is it!  Now, add '_get_' as a prefix to all 'get'/'is'
        // property names.
        for (String propertyName : getProperties.keySet())
        {
            int getIndex = getProperties.get(propertyName);
            idlNames[getIndex] = "_get_" + idlNames[getIndex];
        }

        // Turns out there was a problem with the names for IDLEntity
        // parameters... which has now been fixed in WAS 8.0.  However,
        // for compatibility with older stubs, the older version of
        // the idlName and corresponding method will be added to the
        // end of the lists, so both will be supported in the Tie.         d684761
        if (compatibilityMethods != null)
        {
            for (int i = 0; i < numMethods; ++i)
            {
                String idlName = idlNames[i];
                if (overloaded[i] && idlName.contains(BOXED_IDL))
                {
                    idlName = idlName.replace(BOXED_IDL, "");
                    idlNames = Arrays.copyOf(idlNames, idlNames.length + 1);
                    idlNames[idlNames.length - 1] = idlName;
                    compatibilityMethods.add(methods[i]);
                }
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
        {
            for (int i = 0; i < numMethods; ++i)
            {
                Tr.debug(tc, INDENT + "RMItoIDL: " + methods[i].getName() +
                             " -> " + idlNames[i]);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getIdlMethodNames : " + idlNames.length);

        return idlNames;
    }

    /**
     * Returns true if the specified method only throws RemoteException
     * or subclasses of RemoteException. <p>
     * 
     * Only throwing a RemoteException is a requirement for valid
     * property methods (get/is/set). <p>
     * 
     * @param method the java method to check for RemoteExceptions only.
     **/
    private static boolean onlyRemoteExceptions(Method method)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626
        Class<?>[] exceptions = method.getExceptionTypes();

        for (Class<?> exception : exceptions)
        {
            if (!(RemoteException.class).isAssignableFrom(exception))
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, INDENT + method.getName() +
                                 " throws non-Remote exception " + exception.getName());

                return false;
            }
        }

        return true;
    }

    /**
     * Returns the OMG IDL property name for the specified java 'get', 'is',
     * or 'set' method. <p>
     * 
     * Basically, the OMG IDL property name is the same as the method name,
     * with the 'get', 'is', or 'set' string stripped off the front, and the
     * first letter changed to lowercase, if the second letter is NOT
     * uppercase. <p>
     * 
     * Note that the second letter is strictly for performance, as it is
     * expected that the caller will know if the method name begins with
     * 'get', 'is', or 'set'. <p>
     * 
     * @param methodName method name to be converted to a property name
     * @param beginIndex index to the first letter in the method name
     *            after 'get', 'is', or 'set' (i.e. the length of
     *            either 'get', 'is', or 'set').
     **/
    private static String getPropertyName(String methodName, int beginIndex)
    {
        String propertyName;
        int secondIndex = beginIndex + 1;

        if (methodName.length() <= secondIndex ||
            Character.isLowerCase(methodName.charAt(secondIndex)))
        {
            // no second letter, or second letter is lowercase, so lowercase
            // the first letter as well.
            StringBuilder namebldr = new StringBuilder(methodName);
            namebldr.setCharAt(beginIndex, Character.toLowerCase(namebldr.charAt(beginIndex)));
            propertyName = namebldr.substring(beginIndex);
        }
        else
        {
            // second letter is uppercase (or at least not lowercase, like '_'),
            // so leave the case of the first letter alone.
            propertyName = methodName.substring(beginIndex);
        }

        return propertyName;
    }

    /**
     * Returns a String with the 'mangling' required for idl names that differ
     * only in case. <p>
     * 
     * An '_' will be added, followed by an '_' separated list of all of the
     * indexes to all uppercase letters in the name. <p>
     * 
     * <ul>
     * <li> foo -> foo_
     * <li> CaseDiff -> CaseDiff_0_4
     * </ul>
     * 
     * @param idlName the idl name to be mangled.
     **/
    private static String getCaseDiffName(String idlName)
    {
        int idlLength = idlName.length();
        StringBuilder namebldr = new StringBuilder(idlName);

        for (int i = 0; i < idlLength; ++i)
        {
            char nextChar = namebldr.charAt(i);
            if (Character.isUpperCase(nextChar))
            {
                namebldr.append('_');
                namebldr.append(i);
            }
        }

        if (namebldr.length() == idlLength)
        {
            namebldr.append('_');
        }

        return namebldr.toString();
    }

    /**
     * Returns a String with the 'mangling' required for idl names that
     * contain special characters, including '_', '$', and all characters
     * not in Latin-1. <p>
     * 
     * From the CORBA spec: For Java identifiers that contain illegal OMG
     * IDL identifier characters such as '$' or Unicode characters outside
     * of ISO Latin 1, any such illegal characters are replaced by "U"
     * followed by the 4 hexadecimal characters (in upper case) representing
     * the Unicode value. So, the Java name a$b is mapped to aU0024b and
     * x\u03bCy is mapped to xU03BCy. <p>
     * 
     * For Java names that have leading underscores, the leading underscore
     * is replaced with "J_". So _fred is mapped to J_fred. <p>
     * 
     * @param idlName idlName to be converted
     **/
    private static String convertSpecialCharacters(StringBuilder idlName)
    {
        for (int i = 0; i < idlName.length(); ++i)
        {
            char c = idlName.charAt(i);

            if (c == '$' || c > 255)
            {
                idlName.replace(i, i + 1, "U");

                String hex = Integer.toHexString(c).toUpperCase();
                int numHex = hex.length();
                int numZero = 4 - numHex;

                while (numZero > 0)
                {
                    idlName.insert(++i, '0');
                    --numZero;
                }

                idlName.insert(i + 1, hex);
                i += numHex;
            }
        }

        if (idlName.charAt(0) == '_')
        {
            idlName.insert(0, 'J');
        }

        return idlName.toString();
    }

    /**
     * Returns the OMG IDL method name for a java overloaded method. <p>
     * 
     * For overloaded RMI/IDL methods, the mangled OMG IDL name is formed
     * by taking the Java method name and then appending two underscores,
     * followed by each of the fully qualified OMG IDL types of the
     * arguments separated by two underscores. <p>
     * 
     * @param idlName OMG IDL method name for the java method, which may include
     *            'mangling' for other conditions.
     * @param method java overloaded method.
     **/
    private static String getOverloadedIdlName(String idlName, Method method)
    {
        StringBuilder idlbldr = new StringBuilder(idlName);

        Class<?>[] args = method.getParameterTypes();

        if (args.length == 0)
        {
            idlbldr.append("__");
        }
        else
        {
            StringBuilder arrayStr = new StringBuilder();

            for (Class<?> argType : args)
            {
                idlbldr.append("__");

                arrayStr.setLength(0);
                int arrayDimension = 0;

                while (argType.isArray())
                {
                    ++arrayDimension;
                    argType = argType.getComponentType();
                }
                if (arrayDimension > 0)
                {
                    idlbldr.append("org_omg_boxedRMI_");
                    arrayStr.append("seq").append(arrayDimension).append("_");
                }

                if (argType.isPrimitive())
                {
                    if (arrayDimension > 0)
                        idlbldr.append(arrayStr.toString());

                    if (argType == Boolean.TYPE)
                    {
                        idlbldr.append("boolean");
                    }
                    else if (argType == Character.TYPE)
                    {
                        idlbldr.append("wchar");
                    }
                    else if (argType == Byte.TYPE)
                    {
                        idlbldr.append("octet");
                    }
                    else if (argType == Short.TYPE)
                    {
                        idlbldr.append("short");
                    }
                    else if (argType == Integer.TYPE)
                    {
                        idlbldr.append("long");
                    }
                    else if (argType == Long.TYPE)
                    {
                        idlbldr.append("long_long");
                    }
                    else if (argType == Float.TYPE)
                    {
                        idlbldr.append("float");
                    }
                    else if (argType == Double.TYPE)
                    {
                        idlbldr.append("double");
                    }
                }
                else
                {
                    int unqualifiedLength = 0;

                    if (argType == String.class)
                    {
                        idlbldr.append("CORBA_WStringValue");
                        unqualifiedLength = 12;
                    }
                    else if (argType == Class.class)
                    {
                        idlbldr.append("javax_rmi_CORBA_ClassDesc");
                        unqualifiedLength = 9;
                    }
                    else if (argType == org.omg.CORBA.Object.class)
                    {
                        idlbldr.append("Object");
                        unqualifiedLength = 6;
                    }
                    else
                    {
                        // IDL Entity implementations must be 'boxed'.         d684761
                        if (IDLEntity.class.isAssignableFrom(argType) &&
                            !argType.isInterface() &&
                            !Throwable.class.isAssignableFrom(argType))
                        {
                            idlbldr.append(BOXED_IDL);
                        }

                        String typeName = argType.getName();
                        idlbldr.append(typeName.replace('.', '_'));

                        int unqualifiedOffset = typeName.lastIndexOf('.') + 1;
                        unqualifiedLength = typeName.length() - unqualifiedOffset;
                    }

                    if (arrayDimension > 0)
                    {
                        int seqIndex = idlbldr.length() - unqualifiedLength;
                        idlbldr.insert(seqIndex, arrayStr.toString());
                    }
                }
            }
        }

        return idlbldr.toString();
    }

}
