/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * EJBInterceptorBinding is used to store the interceptor binding metadata
 * that is obtained from WCCM objects. This metadata is obtained from the
 * list of <interceptor-binding> stanzas read from ejb-jar.xml file of a
 * EJB 3 module and it augments the interceptor bindings specified
 * via annotation of the EJB classes in the module.
 * <p>
 * A separate EJBInterceptorBinding object is created for each
 * <interceptor-binding> stanza that appears in the ejb-jar.xml file.
 * The EJBInterceptorBinding with the EJB name of "*" is the default
 * interceptor binding that applies to all EJBs of the module.
 */
public class EJBInterceptorBinding
{
    private static final TraceComponent tc = Tr.register(EJBInterceptorBinding.class, "EJB3Interceptors", "com.ibm.ejs.container.container");

    private static final List<String> EMPTY_LIST = new LinkedList<String>();

    /**
     * Enum of possible WCCM InterceptorBinding styles as defined in EJB 3 specification.
     */
    public enum BindingStyle
    {
        STYLE1, // Style 1 is a default interceptor binding.
        STYLE2, // Style 2 is for a class level interceptor binding.
        STYLE3, // Style 3 is for a method level interceptor binding for a given method name.
        STYLE4 // Style 4 is for a method level interceptor binding for a given method signature.
    }

    /**
     * EJB name of the bean.
     */
    final String ivEjbName;

    /**
     * List of fully qualified interceptor class names. The binding style
     * set in ivBindingStyle determines whether the interceptors in list
     * are class level or method level interceptors. Note, the list may
     * be empty if the <interceptor-order> element is used for this binding.
     * In that case, {@link #ivInterceptorOrder} contains the ordered
     * list of fully qualified interceptor names.
     */
    final ArrayList<String> ivInterceptorClassNames = new ArrayList<String>();

    /**
     * Total ordering of interceptor class names or an empty list if the
     * the default ordering of interceptor names is not being overridden by the
     * <interceptor-order> element for this binding.
     */
    final ArrayList<String> ivInterceptorOrder = new ArrayList<String>();

    /**
     * Binding style - set to one of the BindingStyle enum constants defined
     * in this class.
     */
    final BindingStyle ivBindingStyle;

    /**
     * When binding style in ivBindingStyle is set to METHOD, this field
     * contains the name of the method.
     */
    final String ivMethodName;

    /**
     * When binding style in ivBindingStyle is set to METHOD, this field
     * contains the method signature. If binding applies to all methods
     * with the name in ivMethodName, then this field is set to null.
     */
    final List<String> ivMethodParms;

    /**
     * Set to Boolean.TRUE if exclude-default-interceptors in WCCM is set
     * to true, to Boolean.FALSE if set to false, and to null if not set.
     */
    Boolean ivExcludeDefaultLevelInterceptors;

    /**
     * Set to Boolean.TRUE if exclude-class-interceptors in WCCM is set
     * to true, to Boolean.FALSE if set to false, and to null if not set.
     */
    Boolean ivExcludeClassLevelInterceptors;

    /**
     * Create instance for default interceptor bindings for module.
     * 
     * @param names names is the list of default interceptor fully qualifed class names.
     *            Must be null if the orderList parameter provides a list of interceptor class names.
     * 
     * @param orderList is a total ordering of the list of default interceptor class names.
     */
    public EJBInterceptorBinding(final List<String> names, final List<String> orderList)
    {
        ivEjbName = "*";
        ivBindingStyle = BindingStyle.STYLE1;
        if ((names != null) && (!names.isEmpty())) // d472972
        {
            ivInterceptorClassNames.addAll(names);
        }
        if ((orderList != null) && (!orderList.isEmpty())) // d472972
        {
            ivInterceptorOrder.addAll(orderList);
        }
        ivMethodName = null;
        ivMethodParms = EMPTY_LIST;
    }

    /**
     * Create instance for class level interceptors of a specified
     * EJB in the module.
     * 
     * @param ejbName is the EJB name of the EJB.
     * 
     * @param names names is the list of class level interceptor fully qualifed class names.
     *            Must be null if the orderList parameter provides a list of interceptor class names.
     * 
     * @param orderList is a total ordering of the list of class level interceptor
     *            fully qualifed class names. The list must include both class names from
     *            annotation as well as from xml.
     */
    public EJBInterceptorBinding(final String ejbName, final List<String> names, final List<String> orderList)
    {
        ivEjbName = ejbName;
        ivBindingStyle = BindingStyle.STYLE2;
        if ((names != null) && (!names.isEmpty())) // d472972
        {
            ivInterceptorClassNames.addAll(names);
        }
        if ((orderList != null) && (!orderList.isEmpty())) // d472972
        {
            ivInterceptorOrder.addAll(orderList);
        }
        ivMethodName = null;
        ivMethodParms = EMPTY_LIST;
    }

    /**
     * Create instance for method level interceptors of a specified
     * method of an EJB in the module.
     * 
     * @param ejbName is the EJB name of the EJB.
     * 
     * @param names names is the list of method level interceptor fully qualifed names.
     *            Must be null if the orderList parameter provides a list of interceptor class names.
     * 
     * @param orderList is a total ordering of the list of method level interceptor
     *            fully qualifed class names. The list must include both class names from
     *            annotation as well as from xml.
     * 
     * @param method is the method name of the EJB.
     * 
     * @param methodParms is the List of string that indicate the parameter types
     *            for the method. An empty list or null is used to indicate a method that
     *            has no parameters.
     */
    public EJBInterceptorBinding(final String ejbName
                                 , final List<String> names
                                 , final List<String> orderList
                                 , String method
                                 , List<String> methodParms)
    {
        ivEjbName = ejbName;
        if ((names != null) && (!names.isEmpty())) // d472972
        {
            ivInterceptorClassNames.addAll(names);
        }
        if ((orderList != null) && (!orderList.isEmpty())) // d472972
        {
            ivInterceptorOrder.addAll(orderList);
        }
        ivMethodName = method;
        if (methodParms == null)
        {
            ivBindingStyle = BindingStyle.STYLE3; // d457352
            ivMethodParms = EMPTY_LIST;
        }
        else
        {
            ivBindingStyle = BindingStyle.STYLE4; // d457352
            ivMethodParms = methodParms;
        }
    }

    /**
     * Hide this method to force consumers to use other CTOR.
     */
    private EJBInterceptorBinding()
    {
        ivEjbName = ivMethodName = null;
        ivMethodParms = EMPTY_LIST;
        ivBindingStyle = null;
    }

    /**
     * Get the EJB name that this EJBInterceptorBinding is associated with.
     * 
     * @return name of EJB or "*" if this EJBInterceptorBinding is for a module that
     *         contains the EJB.
     */
    final public String getEJBName()
    {
        return ivEjbName;
    }

    /**
     * Get the binding style of this EJBInterceptorBinding object.
     * 
     * @return one of the enum values of the BindingStyle enum that is declared
     *         in this class. Indicates whether this interceptor binding is for
     *         the module, EJB class, or a method of the EJB.
     */
    final public BindingStyle getBindingStyle()
    {
        return ivBindingStyle;
    }

    /**
     * Get the name of the method this EJBInterceptorBinding is associated with.
     * 
     * @return name of the method when this.getBindingStyle() == BindingStyle.METHOD and
     *         null if this.getBindingStyle() returns any other BindingStyle enum constant.
     */
    final public String getMethodName()
    {
        return ivMethodName;
    }

    /**
     * Get the list of fully qualified interceptor class names that is associated with
     * this EJBInterceptorBinding object. Use the getBindingStyle method of this class to
     * determine if the returned list is default, class level, or method level list of
     * interceptor names.
     * 
     * @return list of fully qualified interceptor class names or an empty list
     *         if the <interceptor-order> elment is used for this binding. In that case,
     *         you must invoke {@link #getInterceptorOrder()} to get an order list
     *         of interceptor names.
     */
    final public List<String> getInterceptorNames()
    {
        return ivInterceptorClassNames;
    }

    /**
     * Get the ordered list of fully qualified interceptor class names
     * that is associated with this EJBInterceptorBinding object.
     * 
     * @return the ordered list of fully qualified interceptor names or an empty
     *         list if the <interceptor-order> element is not used for this binding.
     */
    final public List<String> getInterceptorOrder()
    {
        return ivInterceptorOrder;
    }

    /**
     * Set whether to exclude class level interceptors from a
     * method level EJBInterceptorBinding object.
     * 
     * @param exclude must be boolean true if class level interceptors are to be
     *            excluded for this binding.
     * 
     * @throws EJBConfigurationException is thrown if if this EJBInterceptorBinding object is not a
     *             method level EJBInterceptorBinding object (e.g. STYLE 1 or 2 binding).
     */
    public void setExcludeClassLevelInterceptors(boolean exclude) throws EJBConfigurationException
    {
        if (ivBindingStyle == BindingStyle.STYLE3 || ivBindingStyle == BindingStyle.STYLE4)
        {
            ivExcludeClassLevelInterceptors = (exclude) ? Boolean.TRUE : Boolean.FALSE;
        }
        else
        {
            // CNTR0224E: The {0} enterprise bean is missing a method-name tag on the 
            // exclude-class-interceptors element in the interceptor-binding element of the deployment descriptor.
            Tr.error(tc, "INVALID_EXCLUDE_CLASS_INTERCEPTORS_CNTR0224E", new Object[] { ivEjbName });
            throw new EJBConfigurationException("exclude-class-interceptors element can only be applied to a method of EJB");
        }
    }

    /**
     * Set whether to exclude default interceptors from either a class or
     * method level EJBInterceptorBinding object.
     * 
     * @param exclude must be boolean true if default interceptors are to be
     *            excluded for this binding.
     * 
     * @throws EJBConfigurationException is thrown if if this EJBInterceptorBinding object is a
     *             default interceptor EJBInterceptorBinding object
     *             (e.g. this.getBindingStyle() == BindingStyle.STYLE1).
     */
    public void setExcludeDefaultInterceptors(boolean exclude) throws EJBConfigurationException
    {
        if (ivBindingStyle == BindingStyle.STYLE1)
        {
            // CNTR0225E: The exclude-default-interceptors element in the deployment
            // descriptor is not valid for a style 1 interceptor-binding element.
            Tr.error(tc, "INVALID_EXCLUDE_DEFAULT_INTERCEPTORS_CNTR0225E"); // d463727
            throw new EJBConfigurationException(" CNTR0225E: Invalid use of the exclude-default-interceptors"
                                                + " element in interceptor-binding for EJB name \"*\"");
        }
        else
        {
            ivExcludeDefaultLevelInterceptors = (exclude) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    /**
     * @return list of fully qualified parameter type class names
     *         for the method of a style 4 binding.
     */
    final public List<String> getMethodParms()
    {
        return ivMethodParms;
    }

    /**
     * @return boolean true if and only if default interceptors
     *         are excluded by this binding.
     */
    final public boolean defaultInterceptorsExcluded()
    {
        return ivExcludeDefaultLevelInterceptors;
    }

    /**
     * @return boolean true if and only if class level interceptors
     *         are excluded by this binding.
     */
    final public boolean classLevelInterceptorsExcluded()
    {
        return ivExcludeClassLevelInterceptors;
    }

    /**
     * Perform a Tr.dump of this EJBInterceptorBinding object.
     */
    public void dump()
    {
        Object allData[] = new Object[] {
                                         this,
                                         "EJBName                     = " + ivEjbName,
                                         "method name                 = " + ivMethodName,
                                         "method parameters           = " + ivMethodParms,
                                         "binding style               = " + ivBindingStyle,
                                         "exclude default inteceptors = " + ivExcludeDefaultLevelInterceptors,
                                         "exclude class inteceptors   = " + ivExcludeClassLevelInterceptors,
        }; // d457352

        int totalLength = allData.length;

        String[] interceptorOrder = null;
        if (ivInterceptorOrder != null)
        {
            interceptorOrder = new String[ivInterceptorOrder.size()];
            interceptorOrder = ivInterceptorOrder.toArray(interceptorOrder);
            totalLength += interceptorOrder.length + 2;
        }

        String[] interceptorNames = null;
        if (ivInterceptorClassNames != null)
        {
            interceptorNames = new String[ivInterceptorClassNames.size()];
            interceptorNames = ivInterceptorClassNames.toArray(interceptorNames);
            totalLength += interceptorNames.length + 2;
        }

        int index = 0;
        Object dumpData[] = new Object[totalLength];
        System.arraycopy(allData, 0, dumpData, index, allData.length);
        index += allData.length;

        if (interceptorOrder != null)
        {
            dumpData[index++] = "Interceptor Order: ";
            dumpData[index++] = " ";
            System.arraycopy(interceptorOrder, 0, dumpData, index, interceptorOrder.length);
            index += interceptorOrder.length;
        }

        if (interceptorNames != null)
        {
            dumpData[index++] = "Interceptor Class Names: ";
            dumpData[index++] = " ";
            System.arraycopy(interceptorNames, 0, dumpData, index, interceptorNames.length);
            index += interceptorNames.length;
        }

        Tr.dump(tc, "-- EJBInterceptorBinding Dump --", dumpData);
        Tr.dump(tc, "-- EJBInterceptorBinding Dump End --");
    }

}
