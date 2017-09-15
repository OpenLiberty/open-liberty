/*******************************************************************************
 * Copyright (c) 1998, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.ejb.ComponentViewableBean;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Entity;
import com.ibm.ws.javaee.dd.ejb.Session;

/**
 * Common place to get deployed class names which rely on convention.
 * Also a set of utility functions for package and class names. <p>
 * 
 * Note that change of any file name implemented in this class must require
 * a parallel change in the ejbdeploy code generation process, otherwise the
 * container will never be able to find the generated classes to install
 * and/or start bean. <p>
 * 
 * Beginning with EJB 3.0, the EJB interface and class names may be configured
 * using annotations (as well as the deployment descriptor xml), so all of
 * the NameUtil methods were re-written to not require WCCM (EnterpriseBean),
 * however, all of the old method signatures must remain to maintain
 * compatibility with the existing EJBDeploy. Also, to improve performance,
 * the re-written methods were changed to instance methods, rather than static
 * methods; allowing the hashcode suffix to be computed once per EJB. <p>
 * 
 * Also introduced in EJB 3.0 were the Business Remote and Business Local
 * interfaces, and new methods were added to support these. Note that
 * EJBDeploy will not be enhanced to support these, only the new
 * Just-In-Time Deploy. <p>
 * 
 * Note that all methods, whether static or instance, will return the same
 * deployed class names, based on module version. This allows the EJB
 * Container to use the new instance methods, regardless of whether the
 * EJB is deployed using EJBDeploy (i.e. EJB 2.x or 1.x, or CMP) or
 * using the new Just-In-Time Deploy. <p>
 * 
 * For EJB 3.0, the methods produce slightly different deployed names than
 * prior EJB levels; to account for the fact that an EJB may now have
 * multiple local and remote interfaces. <p>
 * 
 * Note: Container Managed Entity beans will NOT be handled by the
 * Just-In-Time Deployer, and must still use the older naming
 * conventions to remain compatible with EJBDeploy. <p>
 * 
 * The actual format for 3.0 DTD and later (i.e. EJB3 naming convention) is:
 * 
 * EJS + (Local | Remote) + (C | N | A | <index>) + <type> + <EjbName> + _ + BuzzHash
 * 
 * Where:
 * C indicates this is for a Component interface (EJB 1.x and 2.x)
 * N indicates this is for a No-Interface View (LocalBean).
 * A indicates this is for an Aggregate view of all business interfaces
 * (local or remote)
 * index indicates this is for a Business interface, and the index
 * value is the array index for the interface in the list of
 * remote or local business interfaces.
 * type indicates the type of the EJB:
 * SG - Singleton
 * SL - Stateless
 * SF - Stateful
 * BMP - Bean Managed Entity
 * MB - ManagedBean
 * EjbName is the <ejb-name> value from the bean's deployment description
 * - ignoring all leading and trailing whites-space
 * - keeping all alphanumeric characters and replacing all other
 * characters with '_'
 * - limited to the first 32 chars
 * BuzzHash is the 8 digit hashcode of the full <ejb-name> value in the
 * bean's dd concatenated with all associated interfaces and
 * classes, including the primary key, and hashes all characters
 * as defined in the dd (i.e. no translation)
 * 
 * E.g.
 * EJSLocalCSLClaim_12345678
 * EJSRemote2SLClaim_12345678
 * EJSLocalCSLClaimHome_12345678
 * EJSCSLClaimHomeBean_12345678
 * 
 * The format for MDB's is:
 * <package name>.MDBProxy<bean name>_<hashcode>
 * 
 * The format is slightly different for WebService Endpoints:
 * 
 * WSEJBProxy + <type> + <EjbName> + _ + BuzzHash
 * 
 * Where the difference is 'WSEJBProxy' in place of EJS + 'Local | Remote',
 * and not including either 'C | <index>', since there is only one type of
 * WebService Endpoint.
 * 
 * E.g.
 * WSEJBProxySLClaim_12345678
 */

public final class NameUtil
{
    //121558
    private static final TraceComponent tc =
                    Tr.register(NameUtil.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    public static final String homeRemotePrefix = "EJSRemote";
    public static final String homeBeanPrefix = "EJS";
    public static final String remotePrefix = "EJSRemote";
    public static final String concreteBeanPrefix = "Concrete"; // f110762.2
    public static final String persisterPrefix = "EJSJDBCPersister";
    public static final String endpointPrefix = "WSEJBProxy"; // LI3294-35
    public static final String mdbProxyPrefix = "MDBProxy";

    public static final String homeLocalPrefix = "EJSLocal"; // f111627
    public static final String localPrefix = "EJSLocal"; // f111627

    public static final String deployPackagePrefix = "com.ibm.ejs.container.deploy."; // F58064

    public static final int Max_Gen_File_Size = 100; // d114199
    public static final int Max_EjbName_Size = 32; // d147734

    // BeanType constants for all methods that require a beanType parameter
    public static final String UNKNOWN = "UNKNOWN";
    public static final String SINGLETON = "SG"; //F743-508
    public static final String STATELESS = "SL";
    public static final String STATEFUL = "SF";
    public static final String BEAN_MANAGED = "BMP";
    public static final String CONTAINER_MANAGED = "CMP";
    public static final String MESSAGE_DRIVEN = "MDB";
    public static final String MANAGED_BEAN = "MB"; // F743-34301

    // Module Version constants for all methods that require a version parameter
    public static final int EJB_1X = 1;
    public static final int EJB_2X = 2;
    public static final int EJB_3X = 3;

    // Instance data that is used to formulate the deployed class names
    private String ivBeanName = null;
    private String ivFullBeanName = null; // d369262.3
    private String ivBeanType = null;
    private String ivHashSuffix = null;
    private int ivHashVersion = -1; // d369262.3
    private int ivVersion = -1;

    // EJB Interfaces/Classes that require deployed implementations.
    private final String ivRemoteHomeInterface;
    private final String ivRemoteInterface;
    private final String ivLocalHomeInterface;
    private final String ivLocalInterface;
    private final String[] ivBusinessRemote;
    private final String[] ivBusinessLocal;
    private final String ivBeanClass;
    private final String ivPrimaryKey;

    // --------------------------------------------------------------------------
    //
    // Static methods that must be maintained for use by the deployutils (Ant
    // task for EJBDeploy).  The EJBDeploy tool itself has its own copies of
    // this logic that would need to be updated if the logic changed (e.g.,
    // com.ibm.etools.ejbdeploy.plugin.NameGeneratorHelper).
    //
    // --------------------------------------------------------------------------

    private static String getRemoteInterfaceName(EnterpriseBean enterpriseBean)
    {
        if (enterpriseBean instanceof ComponentViewableBean)
        {
            return ((ComponentViewableBean) enterpriseBean).getRemoteInterfaceName();
        }

        return null;
    }

    /**
     * Return the name of the deployed class that implements the remote
     * interface of the bean. <p>
     * 
     * @param enterpriseBean bean impl class
     * @param isPost11DD true if bean is defined using later than 1.1 deployment description.
     */
    public static String getRemoteImplClassName(EnterpriseBean enterpriseBean, boolean isPost11DD) // d114199
    {
        String remoteInterfaceName = getRemoteInterfaceName(enterpriseBean);

        if (remoteInterfaceName == null) { // f111627
            return null; // f111627
        } // f111627
        String packageName = packageName(remoteInterfaceName);
        String remoteName = encodeBeanInterfacesName(enterpriseBean, isPost11DD, false, false, false); //d114199 d147734

        StringBuffer result = new StringBuffer();
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }
        result.append(remotePrefix);
        result.append(getUniquePrefix(enterpriseBean));
        result.append(remoteName);

        return result.toString();
    } // getRemoteImplClassName

    private static String getLocalInterfaceName(EnterpriseBean enterpriseBean)
    {
        if (enterpriseBean instanceof ComponentViewableBean)
        {
            return ((ComponentViewableBean) enterpriseBean).getLocalInterfaceName();
        }

        return null;
    }

    /**
     * Return the name of the deployed class that implements the local
     * interface of the bean. <p>
     * 
     * @param enterpriseBean bean impl class
     * @param isPost11DD true if bean is defined using later than 1.1 deployment description.
     */
    public static String getLocalImplClassName(EnterpriseBean enterpriseBean, boolean isPost11DD) // d114199
    {
        String localInterfaceName = getLocalInterfaceName(enterpriseBean);

        if (localInterfaceName == null) {
            return null;
        }
        String packageName = packageName(localInterfaceName);
        String localName = encodeBeanInterfacesName(enterpriseBean, isPost11DD, true, false, false); //d114199 d147734

        StringBuffer result = new StringBuffer();
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }
        result.append(localPrefix);
        result.append(getUniquePrefix(enterpriseBean));
        result.append(localName);

        return result.toString();
    } // getRemoteImplClassName

    private static String getHomeInterfaceName(EnterpriseBean enterpriseBean)
    {
        if (enterpriseBean instanceof ComponentViewableBean)
        {
            return ((ComponentViewableBean) enterpriseBean).getHomeInterfaceName();
        }

        return null;
    }

    /**
     * Return the name of the deployed class that implements the home
     * interface of the bean. <p>
     * 
     * @param enterpriseBean bean impl class
     * @param isPost11DD true if bean is defined using later than 1.1 deployment description.
     */
    public static String getHomeRemoteImplClassName(EnterpriseBean enterpriseBean, boolean isPost11DD) // d114199
    {
        String homeInterfaceName = getHomeInterfaceName(enterpriseBean);

        if (homeInterfaceName == null) {
            return null;
        }
        String remoteInterfaceName = getRemoteInterfaceName(enterpriseBean); // d147362
        String packageName = packageName(remoteInterfaceName); // d147362
        String homeName = encodeBeanInterfacesName(enterpriseBean, isPost11DD, false, true, false); //d114199 d147734

        StringBuffer result = new StringBuffer();
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }
        result.append(homeRemotePrefix);
        result.append(getUniquePrefix(enterpriseBean));
        result.append(homeName);

        return result.toString();
    }

    private static String getLocalHomeInterfaceName(EnterpriseBean enterpriseBean)
    {
        if (enterpriseBean instanceof ComponentViewableBean)
        {
            return ((ComponentViewableBean) enterpriseBean).getLocalHomeInterfaceName();
        }

        return null;
    }

    /**
     * Return the name of the deployed class that implements the local home
     * interface of the bean. <p>
     * 
     * @param enterpriseBean bean impl class
     * @param isPost11DD true if bean is defined using later than 1.1 deployment description.
     */
    public static String getHomeLocalImplClassName(EnterpriseBean enterpriseBean, boolean isPost11DD) // d114199
    {
        String homeLocalInterfaceName = getLocalHomeInterfaceName(enterpriseBean);

        if (homeLocalInterfaceName == null) {
            return null;
        }
        String localInterfaceName = getLocalInterfaceName(enterpriseBean); // d147362
        String packageName = packageName(localInterfaceName); // d147362
        String homeLocalName = encodeBeanInterfacesName(enterpriseBean, isPost11DD, true, true, false); //d114199 d147734

        StringBuffer result = new StringBuffer();
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }
        result.append(homeLocalPrefix);
        result.append(getUniquePrefix(enterpriseBean));
        result.append(homeLocalName);

        return result.toString();
    }

    /**
     * Returns the name of the deployed class that implements an aggregation
     * of all business local interfaces of the bean. <p>
     * 
     * The aggregate name is derived from any business local implementation
     * class name by replacing the index with the character 'A'. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the business remote implementation class names are as follows: <p>
     * 
     * EJB 3.x Aggregate : EJSLocalASLClaim_12345678
     * 
     * @param localImplClassName the first ([0]) business local implementation
     *            class name.
     * 
     * @return the name of the deployed class that implements the local
     *         interfaces.
     */
    // F743-34304
    public static String getAggregateLocalImplClassName(String localImplClassName)
    {
        StringBuilder aggregateName = new StringBuilder(localImplClassName);
        int index = localImplClassName.lastIndexOf(localPrefix);
        aggregateName.setCharAt(index + localPrefix.length(), 'A');
        return aggregateName.toString();
    }

    /**
     * Return the name of the deployed home bean class. Assumption here is
     * the package name of the local and remote interfaces are the same. This
     * method uses the last package name found in either the remote or local
     * interface, if one or the other exist. If neither is found, null is returned.
     * 
     * @param enterpriseBean WCCM object for EnterpriseBean
     * @param isPost11DD true if bean is defined using later than 1.1 deployment description.
     */
    public static String getHomeBeanClassName(EnterpriseBean enterpriseBean, boolean isPost11DD) // d114199
    {
        String packageName = null;

        String homeInterfaceName = getHomeInterfaceName(enterpriseBean);

        // LIDB2281.24.2 made several changes to code below, to accommodate case
        // where neither a remote home nor a local home is present (EJB 2.1 allows
        // this).
        if (homeInterfaceName == null) { // f111627.1
            homeInterfaceName = getLocalHomeInterfaceName(enterpriseBean); // f111627.1
        }

        if (homeInterfaceName != null) {
            packageName = packageName(homeInterfaceName);

            StringBuffer result = new StringBuffer();
            if (packageName != null) {
                result.append(packageName);
                result.append('.');
            }
            result.append(homeBeanPrefix);
            result.append(getUniquePrefix(enterpriseBean));

            String homeName = encodeBeanInterfacesName(enterpriseBean, isPost11DD, false, true, true); // d114199 d147734
            result.append(homeName);

            return result.toString();
        } else {
            return null;
        }

    }

    /**
     * Return the name of the concrete bean class using following
     * template:
     * 
     * concrete bean class name = <package name>.Concrete<bean class>
     **/
    // f110762.2
    public static String getConcreteBeanClassName(EnterpriseBean enterpriseBean)
    {
        String beanClassName = enterpriseBean.getEjbClassName();
        String packageName = packageName(beanClassName);
        String beanName = encodeBeanInterfacesName(enterpriseBean, true, false, false, false); // d147734

        StringBuffer result = new StringBuffer();
        if (packageName != null)
        {
            result.append(packageName);
            result.append('.');
        }

        result.append(concreteBeanPrefix);
        result.append(beanName);

        return result.toString();
    }

    /*
     * Return the name of the deployed persister class using following
     * template:
     * 
     * persistence class name = <package name>.EJSJDBCPersister<bean class>
     */
    public static String getDeployedPersisterClassName(EnterpriseBean enterpriseBean)
    {
        String beanClassName = enterpriseBean.getEjbClassName();
        String packageName = packageName(beanClassName);
        String beanName = relativeName(beanClassName);

        StringBuffer result = new StringBuffer();
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }
        result.append(persisterPrefix);
        result.append(getUniquePrefix(enterpriseBean));
        result.append(beanName);

        return result.toString();
    }

    // TODO TKB: Delete Me after calling code changed to use instance method
    /**
     * Return the name of the deployed persister class using following
     * template:
     * 
     * persistence class name = <package name>.EJSJDBCPersister<bean class>
     * 
     * This signature was introduced as part of an effort to isolate
     * references in EJB Container to WCCM objects. The original signature
     * (above) must be maintained for compatibility with EJBDeploy.
     */
    // d366807.3
    public static String getDeployedPersisterClassName(Object enterpriseBean)
    {
        return getDeployedPersisterClassName((EnterpriseBean) enterpriseBean);
    }

    //
    // Utilities
    //

    public static String relativeName(String className)
    {
        return relativeName(className, ".");
    }

    public static String relativeName(String className, String delim)
    {
        String tmp = null;

        StringTokenizer st = new StringTokenizer(className, delim, false);
        while (st.hasMoreElements()) {
            tmp = st.nextToken();
        }
        return tmp;
    }

    public static String packageName(String className)
    {
        int index = className.lastIndexOf('.');
        if (index != -1)
            return className.substring(0, index);
        else
            return null;
    }

    /**
     * Returns the bean type specific part of the generated class names. <p>
     * 
     * This value must match the same algorithm as used by the deploytool,
     * and is part of the prefix for the following generated classes:
     * <ul>
     * <li> Remote Wrapper Class Name
     * <li> Local Wrapper Class Name
     * <li> Remote Home Wrapper Class Name
     * <li> Local Home Wrapper Class Name
     * <li> Home Bean Class Name
     * <li> CMP 1.x Persister Class Name
     * </ul> <p>
     * 
     * The possible returned values are:
     * <ul>
     * <li> Singleton - Singleton Session Bean
     * <li> Stateless - Stateless Session Bean
     * <li> Stateful - Stateful Session Bean
     * <li> CMP - Container Managed Persistence Entity Bean
     * <li> BMP - Bean Managed Persistence Entity Bean
     * </ul> <p>
     * 
     * Note that MessageDriven Beans do not have a unique prefix value since
     * they do not have any generated classes. This method should never be
     * called for MDBs. <p>
     **/
    // d170742
    public static String getUniquePrefix(EnterpriseBean enterpriseBean)
    {
        String prefix = "";

        switch (enterpriseBean.getKindValue())
        {
            case EnterpriseBean.KIND_SESSION:
                switch (((Session) enterpriseBean).getSessionTypeValue())
                {
                    case Session.SESSION_TYPE_STATELESS:
                        prefix = "Stateless";
                        break;
                    case Session.SESSION_TYPE_STATEFUL:
                        prefix = "Stateful";
                        break;
                    case Session.SESSION_TYPE_SINGLETON:
                        prefix = "Singleton";
                        break;
                    default:
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(tc, "Unknown session bean type");
                        break;
                }
                break;

            case EnterpriseBean.KIND_ENTITY:
                if (((Entity) enterpriseBean).getPersistenceTypeValue() == Entity.PERSISTENCE_TYPE_CONTAINER)
                {
                    prefix = "CMP";
                }
                else
                {
                    prefix = "BMP";
                }
                break;

            case EnterpriseBean.KIND_MESSAGE_DRIVEN:
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "EJB Internal Error - " +
                                 "MessageDriven has no unique prefix");
                break;

            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) //130050
                    Tr.event(tc, "Unknown bean type");
        }

        return prefix;
    }

    public static String getCreateTableStringsMethodNameBase()
    {
        return "getCreateTableSQLStrings_";
    }

    // d149791 Begins
    public static final String getHashStr(EnterpriseBean enterpriseBean)
    {
        StringBuffer hashStr = new StringBuffer(enterpriseBean.getName());

        String remoteHomeInterfaceName = getHomeInterfaceName(enterpriseBean);
        if (remoteHomeInterfaceName != null)
        {
            hashStr.append(remoteHomeInterfaceName);
        }
        String remoteBusinessInterfaceName = getRemoteInterfaceName(enterpriseBean);
        if (remoteBusinessInterfaceName != null)
        {
            hashStr.append(remoteBusinessInterfaceName);
        }
        String localHomeInterfaceName = getLocalHomeInterfaceName(enterpriseBean);
        if (localHomeInterfaceName != null)
        {
            hashStr.append(localHomeInterfaceName);
        }
        String localBusinessInterfaceName = getLocalInterfaceName(enterpriseBean);
        if (localBusinessInterfaceName != null)
        {
            hashStr.append(localBusinessInterfaceName);
        }
        String beanClassName = enterpriseBean.getEjbClassName();
        hashStr.append(beanClassName);
        if (enterpriseBean.getKindValue() == EnterpriseBean.KIND_ENTITY)
        {
            hashStr.append(((Entity) enterpriseBean).getPrimaryKeyName());
        }
        return hashStr.toString();
    }

    // d149791 Ends

    // TODO TKB: Delete Me after calling code changed to use instance method
    /**
     * This signature was introduced as part of an effort to isolate
     * references in EJB Container to WCCM objects. The original signature
     * (above) must be maintained for compatibility with EJBDeploy.
     */
    // d366807.3
    public static final String getHashStr(Object enterpriseBean)
    {
        return getHashStr((EnterpriseBean) enterpriseBean);
    }

    // d114199 Begins
    // This returns the encoded name based on the bean class and its interface(s).
    //  The format of the encoded name is:
    //
    //  For 1.1 DTD (i.e. pre V5.0 naming convention)
    //      { remoteInterfaceClassName | localinterfaceClassName } + [ "Bean" ]
    //  E.g.
    //      Claim
    //      ClaimBean
    //
    //  For 2.0 DTD (i.e. V5.0 naming convention)
    //      EjbName + [ "Home" ] + [ "Bean" ] + "_" + BuzzHash
    //  Where:
    //      EjbName is the <ejb-name> value from the bean's deployment description
    //          - ignore all leading and trailing whites-space (i.e. String.trim() )
    //          - keep all alphanumeric characters and replace all other characters with '_'
    //          - limits to the first 32 chars
    //      BuzzHash is the hashcode of the full <ejb-name> value in the bean's dd
    //          - ignore all leading and trailing white-spaces (i.e. String.trim() )
    //          - hash all characters as defined in the dd (i.e. no translation)
    //  E.g.
    //      Claim_012345678
    //      Claim_012345678
    //      ClaimBean_012345678
    //      ClaimHomeBean_012345678
    //
    // @param enterpriseBean bean impl class
    // @param isPost11DD true if bean is defined using later than 1.1 deployment description.
    // @param isLocal true if local interface is requested
    // @param isHome true if home interface is requested
    // @param isBean true if home bean file name is requested
    //
    private static final String encodeBeanInterfacesName(EnterpriseBean enterpriseBean, boolean isPost11DD, boolean isLocal, boolean isHome, boolean isBean) // d147734
    {
        String rtnStr = null;
        if (!isPost11DD)
        { // container managed 1.x bean naming convention
          // pre 2.0 EJB specification don't have local interface, check of isLocal
          //  is a precaution for an invalid call, which returns null back.
            if (isHome)
            { // cm 1.x home interface
                if (isLocal)
                { // cm 1.x local home interface
                    rtnStr = getLocalHomeInterfaceName(enterpriseBean); // d149791
                } else
                { // cm 1.x remote home interface
                    rtnStr = getHomeInterfaceName(enterpriseBean); // d149791
                }
                // d147734 Begins
                if (isBean)
                { // move "Bean" append from the caller to here so that the hashcode is the last element
                    rtnStr += "Bean";
                }
                // d147734 Ends
            } else
            { // cm 1.x business interface
                if (isLocal)
                { // cm 1.x local business interface
                    rtnStr = getLocalInterfaceName(enterpriseBean); // d149791
                } else
                { // cm 1.x remote business interface
                    rtnStr = getRemoteInterfaceName(enterpriseBean); // d149791
                }
            }
            return relativeName(rtnStr);
        } else
        { // container managed 2.x bean naming convention
          // d147734 Begins
          // the if clause's statements replaces the old implementation in the else clause
            String ejbName = enterpriseBean.getName();
            rtnStr = translateEjbName(ejbName);

            String hashStr = getHashStr(enterpriseBean); // d149791

            if (isHome)
            { // New append to file name to differentiate between business & home wrappers
                rtnStr += "Home";
            }
            if (isBean)
            { // move "Bean" append from the caller to here so that the hashcode is the last element
                rtnStr += "Bean";
            }
            // d147734 Ends
            //  append the <ejb-name> BuzzHash as bean's unique identifier
            return rtnStr + "_" + BuzzHash.computeHashStringMid32Bit(hashStr);
        }
    }

    // d114199 Ends

    // d179573 Begins
    private static boolean allHexDigits(String str, int start, int end)
    {
        boolean rtn = true;
        for (int i = start; i < end; ++i)
        {
            if ("0123456789abcdefABCDEF".indexOf(str.charAt(i)) == -1)
            {
                rtn = false;
                break;
            }
        }
        return rtn;
    }

    /**
     * Attempts to find the new file name originated from input oldName using the
     * the new modified BuzzHash algorithm.
     * This method detects if the oldName contains a valid trailing hashcode and
     * try to compute the new one. If the oldName has no valid hashcode in the
     * input name, null is return.
     * For ease of invocation regardless of the type of file passes in, this method
     * only changes the hash value and will not recreate the complete file name from
     * the EnterpriseBean.
     * 
     * @return the new file name using the oldName but with a new hash code, or null if no
     *         new name can be constructed.
     */
    public static final String updateFilenameHashCode(EnterpriseBean enterpriseBean, String oldName)
    {
        String newName = null;
        int len = oldName.length();
        int last_ = (len > 9 && (oldName.charAt(len - 9) == '_')) ? len - 9 : -1;
        // input file name must have a trailing "_" follows by 8 hex digits
        // and check to make sure the last 8 characters are all hex digits
        if (last_ != -1 && allHexDigits(oldName, ++last_, len))
        {
            String hashStr = getHashStr(enterpriseBean);
            newName = oldName.substring(0, last_) + BuzzHash.computeHashStringMid32Bit(hashStr, true);
        }
        return newName;
    }

    // d179573 Ends

    // TODO TKB: Delete Me after calling code changed to use instance method
    /*
     * This signature was introduced as part of an effort to isolate
     * references in EJB Container to WCCM objects. The original signature
     * (above) must be maintained for compatibility with EJBDeploy.
     */// d366807.3
    public static final String updateFilenameHashCode(Object enterpriseBean, String oldName)
    {
        return updateFilenameHashCode((EnterpriseBean) enterpriseBean, oldName);
    }

    // d147734 Begins
    /**
     * Translate the input ejbname to be used by ejbdeploy generated file name
     * according to the following rules.
     * 1. Leading and trailing white spaces are trimmed.
     * 2. All non-alphanumeric characters are replaced by "_" to avoid invalid
     * file name character since <ejb-name> can be in any form.
     * 3. Limit the return string to a pre-defined length Max_EjbName_Size.
     */
    private static String translateEjbName(String ejbName)
    {
        // trim leading and trailing blanks
        ejbName = ejbName.trim();
        // limits to the first Max_EjbName_Size characters
        int len = ejbName.length();
        if (len > Max_EjbName_Size)
        {
            len = Max_EjbName_Size;
        }
        // translate non-alphanumeric characters to "_"
        char translated[] = new char[len];
        for (int i = 0; i < len; ++i)
        {
            char curChar = ejbName.charAt(i);
            translated[i] = Character.isLetterOrDigit(curChar) ? curChar : '_';
        }
        return new String(translated);
    }

    public static void dumpClassNames(EnterpriseBean enterpriseBean, boolean isPost11DD)
    {
        System.out.println("<NameUtil><EjbName          > " + enterpriseBean.getName() + ":" + isPost11DD);
        System.out.println("<NameUtil><RemoteWrapper    > " + getRemoteImplClassName(enterpriseBean, isPost11DD));
        System.out.println("<NameUtil><RemoteHomeWrapper> " + getHomeRemoteImplClassName(enterpriseBean, isPost11DD));
        System.out.println("<NameUtil><LocalWrapper     > " + getLocalImplClassName(enterpriseBean, isPost11DD));
        System.out.println("<NameUtil><LocalHomeWrapper > " + getHomeLocalImplClassName(enterpriseBean, isPost11DD));
        System.out.println("<NameUtil><HomeBean         > " + getHomeBeanClassName(enterpriseBean, isPost11DD));
        System.out.println("<NameUtil><ConcreateBean    > " + getConcreteBeanClassName(enterpriseBean));
        System.out.println("<NameUtil><Persister        > " + getDeployedPersisterClassName(enterpriseBean));
    }

    // d147734 Ends

    // --------------------------------------------------------------------------
    //
    // Instance methods used by EJB Container for EJB 3.0 and performance
    //
    // --------------------------------------------------------------------------

    /**
     * Creates an instance of NameUtil that pre-formulates common parts
     * of the deployed classes names, providing a significant performance
     * advantage over the use of the static methods. <p>
     * 
     * Also, unlike the static NameUtil methods, the instance construction
     * and instance methods are independent of the metadata source
     * (i.e. independent of WCCM or annotation processing). <p>
     * 
     * Note: MessageDriven beans do not have generated classes, however
     * a NameUtil may be created for a MessageDriven bean to allow
     * the user to have EJB type independent code. <p>
     * 
     * @param beanName the EJB name.
     * @param remoteHomeInterface fully qualified remote home interface name.
     * @param remoteInterface fully qualified remote interface name.
     * @param localHomeInterface fully qualified local home interface name.
     * @param localInterface fully qualified local interface name.
     * @param remoteBusinessInterfaces fully qualified remote business
     *            interface names.
     * @param localBusinessInterfaces fully qualified local business
     *            interface names.
     * @param beanClass fully qualified bean implementation class name.
     * @param primaryKey fully qualified primary key class name.
     * @param beanType bean type constant from NameUtil class.
     * @param version module version constant from NameUtil class.
     * 
     * @throws IllegalArgumentException if the beanName parameter is null or
     *             a valid constant is not specified for either the beanType
     *             or version parameters.
     **/
    // d366807.3
    public NameUtil(String beanName,
                    String remoteHomeInterface,
                    String remoteInterface,
                    String localHomeInterface,
                    String localInterface,
                    String remoteBusinessInterfaces[],
                    String localBusinessInterfaces[],
                    String beanClass,
                    String primaryKey,
                    String beanType,
                    int version)
    {

        // The bean name must be specified (as it is integral to determining
        // many of the generated class names, and is required by the spec.
        // Note that not all characters that are allowed for a bean name
        // may be used in a class name, so when saving the bean name, it
        // is first translated (to valid characters) and truncated to a
        // max size.  This translation is then only performed once, and not
        // for each generated class name.
        if (beanName == null)
            throw new IllegalArgumentException("Bean Name not specified.");

        ivBeanName = translateEjbName(beanName);
        ivFullBeanName = beanName; // d369262.3

        // The module version is saved internally, and is used when generating
        // each of the different deployed class names to determine if an older
        // format of the name is required for backward compatibility.
        // Note that CMP Entity beans in an EJB 3.x module are treated as 2.x,
        // since the JIT Deploy does not support them.
        if (version < EJB_1X || version > EJB_3X)
            throw new IllegalArgumentException("Unsupported module version : " +
                                               version);

        if (version == EJB_3X && beanType == CONTAINER_MANAGED)
            ivVersion = EJB_2X;
        else
            ivVersion = version;

        // The 'bean type' is a string that is appended as part of the prefix
        // for many of the deployed names, and starting with EJB 3.0, this
        // string has been shortened to save space. In order to retain backward
        // compatibility, the bean type string must be mapped to the older
        // (longer) string that was used in prior releases
        if (beanType != SINGLETON && beanType != STATELESS && beanType != STATEFUL &&
            beanType != CONTAINER_MANAGED && beanType != BEAN_MANAGED &&
            beanType != MESSAGE_DRIVEN && beanType != MANAGED_BEAN)
            throw new IllegalArgumentException("Unsupported bean type : " + beanType);

        switch (ivVersion)
        {
            case EJB_3X:
                ivBeanType = beanType;
                break;

            case EJB_2X:
            case EJB_1X:
                if (beanType == STATELESS)
                    ivBeanType = "Stateless";
                else if (beanType == STATEFUL)
                    ivBeanType = "Stateful";
                else
                    ivBeanType = beanType; // BMP and CMP
                break;
        }

        // Save all of the interface names here that would have been
        // obtained from the WCCM enterpriseBean in prior releases.
        ivRemoteHomeInterface = remoteHomeInterface;
        ivRemoteInterface = remoteInterface;
        ivLocalHomeInterface = localHomeInterface;
        ivLocalInterface = localInterface;
        ivBusinessRemote = remoteBusinessInterfaces;
        ivBusinessLocal = localBusinessInterfaces;
        ivBeanClass = beanClass;
        ivPrimaryKey = primaryKey;

        // Calculate the 8 digit (hex) hash code suffix once, so that
        // it does not have to be re-calculated for each of the
        // many deployed class names.  Note that the full 'beanName'
        // must be used here (to maintain compatibility with prior
        // releases), and NOT the 'translated bean name'.
        // Also, the original (not 'modified') algorithm is used for EJB 1.x
        // and EJB 2.x modules, just like prior releases.  EJBDeploy also
        // appears to use the original algorithm, unless a name conflict is
        // detected.  However, for EJB 3.x and later, the new 'modified'
        // hash algorithm will be used to better account for patterns
        // in the hash string.
        ivHashSuffix = getHashSuffix(ivVersion >= EJB_3X);
    }

    /**
     * Returns the name of the deployed class that implements the
     * business remote interface of the bean, at the specified index into the
     * array of business remote interfaces passed on the constructor. <p>
     * 
     * This method is new for EJB 3.0 modules; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the business remote implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSRemote3SLClaim_12345678
     * EJB 2.x (and CMP) : Not Supported
     * EJB 1.x : Not Supported
     * 
     * @param index index into the array of business remote interfaces.
     * 
     * @return the name of the deployed class that implements the remote
     *         interface.
     * 
     * @throws IllegalStateException if this method is called for an EJB 1.x
     *             or 2.x module level OR no business remote interfaces exists.
     * @throws ArrayIndexOutOfBoundsException if the index specified falls
     *             out of the bounds of the array of business remote interfaces
     *             provided on the constructor.
     **/
    // d366807.3
    public String getBusinessRemoteImplClassName(int index)
    {
        if (ivBusinessRemote == null || ivVersion < EJB_3X)
            throw new IllegalStateException("Remote Business interfaces are not " +
                                            "supported in EJB 1.x and 2.x modules");

        String remoteInterface = ivBusinessRemote[index];

        StringBuffer result = new StringBuffer();

        String packageName = packageName(remoteInterface);
        if (packageName != null) {
            if (packageName.startsWith("java.")) {
                // Only the boot class loader can define "java." classes.     F58064
                result.append(deployPackagePrefix);
            }
            result.append(packageName);
            result.append('.');
        }

        result.append(remotePrefix); // EJSRemote
        result.append(index); // Business index
        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    } // getBusinessRemoteImplClassName

    /**
     * Returns the name of the deployed class that implements the
     * business local interface of the bean, at the specified index into the
     * array of business local interfaces passed on the constructor. <p>
     * 
     * This method is new for EJB 3.0 modules; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the business remote implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSLocal3SLClaim_12345678
     * EJB 2.x (and CMP) : Not Supported
     * EJB 1.x : Not Supported
     * 
     * @param index index into the array of business local interfaces.
     * 
     * @return the name of the deployed class that implements the local
     *         interface.
     * 
     * @throws IllegalStateException if this method is called for an EJB 1.x
     *             or 2.x module level OR no business local interfaces exists.
     * @throws ArrayIndexOutOfBoundsException if the index specified falls
     *             out of the bounds of the array of business local interfaces
     *             provided on the constructor.
     **/
    // d366807.3
    public String getBusinessLocalImplClassName(int index)
    {
        if (ivBusinessLocal == null || ivVersion < EJB_3X)
            throw new IllegalStateException("Local Business interfaces are not " +
                                            "supported in EJB 1.x and 2.x modules");

        String localInterface = ivBusinessLocal[index];

        // If this is a No-Interface View (LocalBean), then include an 'N' in
        // the name, otherwise use the business index.                   F743-1756
        String bindex = null;
        if (index == 0 && localInterface.equals(ivBeanClass)) {
            bindex = "N";
        } else {
            bindex = Integer.toString(index);
        }

        StringBuffer result = new StringBuffer();

        String packageName = packageName(localInterface);
        if (packageName != null) {
            if (packageName.startsWith("java.")) {
                // Only the boot class loader can define "java." classes.     F58064
                result.append(deployPackagePrefix);
            }
            result.append(packageName);
            result.append('.');
        }

        result.append(localPrefix); // EJSLocal
        result.append(bindex); // Business index or 'N'
        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    } // getBusinessLocalImplClassName

    /**
     * Returns the name of the deployed class that implements the
     * component remote interface of the bean. <p>
     * 
     * This method is new for EJB 3.0 modules, and produces a slightly
     * different name than prior EJB module levels; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * For modules prior to EJB 3.0, and CMP Entity beans, this method will
     * return exactly the same name as the static method by the same name
     * (used by EJBDeploy for EJB 1.x and 2.x modules), except the hash
     * suffix will be added for EJB 1.x... instead of adding it later when
     * the class is loaded. For EJB 1.x, the hash suffix may be removed
     * later in updateFilenameHashCode. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the remote implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSRemoteCSLClaim_12345678
     * EJB 2.x (and CMP) : EJSRemoteStatelessClaim_12345678
     * EJB 1.x : EJSRemoteClaim_12345678 (based on interface)
     * 
     * @return the name of the deployed class that implements the remote
     *         interface, or null when no remote interface.
     **/
    // d366807.3
    public String getRemoteImplClassName()
    {
        if (ivRemoteInterface == null)
            return null;

        // -----------------------------------------------------------------------
        // The component remote implementation name was changed in EJB 2.x to
        // use the bean name and hash suffix, and only modified slightly again
        // in EJB 3.0.  The only 3 differences in EJB 3.0 are :
        //
        //   1 - "C" indicating component interface added
        //   2 - beantype shortened for Stateless and Stateful (now SL/SF)
        //   3 - hash code uses modified algorithm, and includes business interfaces
        //
        // The last two differences are pre-calculated in the constructor, and
        // so the code here appears common.
        // -----------------------------------------------------------------------

        StringBuffer result = new StringBuffer();

        String packageName = packageName(ivRemoteInterface);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(remotePrefix); // EJSRemote

        if (ivVersion >= EJB_3X)
            result.append("C"); // Component interface

        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.

        if (ivVersion >= EJB_2X)
        {
            result.append(ivBeanName); // First 32 characters
            result.append('_');
            result.append(ivHashSuffix); // 8 digit hashcode
        }
        else
        {
            result.append(relativeName(ivRemoteInterface));
            if (ivHashVersion > 0) // d369262.3
            {
                result.append('_');
                result.append(ivHashSuffix); // 8 digit hashcode
            }
        }

        return result.toString();
    } // getRemoteImplClassName

    /**
     * Returns the name of the deployed class that implements the
     * component local interface of the bean. <p>
     * 
     * This method is new for EJB 3.0 modules, and produces a slightly
     * different name than prior EJB module levels; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * For modules prior to EJB 3.0, and CMP Entity beans, this method will
     * return exactly the same name as the static method by the same name
     * (used by EJBDeploy for EJB 1.x and 2.x modules). <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the local implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSLocalCSLClaim_12345678
     * EJB 2.x (and CMP) : EJSLocalStatelessClaim_12345678
     * EJB 1.x : Not Supported
     * 
     * @return the name of the deployed class that implements the local
     *         interface, or null when no local interface.
     * 
     * @throws IllegalStateException if this method is called for an EJB 1.x
     *             module level AND a local interface exists.
     **/
    // d366807.3
    public String getLocalImplClassName()
    {
        if (ivLocalInterface == null)
            return null;

        if (ivVersion == EJB_1X)
            throw new IllegalStateException("Local interfaces are not supported " +
                                            "in EJB 1.x modules");

        // -----------------------------------------------------------------------
        // The component local implementation name was new in EJB 2.x and
        // only modified slightly in EJB 3.0.  The only 3 differences are :
        //
        //   1 - "C" indicating component interface added
        //   2 - beantype shortened for Stateless and Stateful (now SL/SF)
        //   3 - hash code uses modified algorithm, and includes business interfaces
        //
        // The last two differences are pre-calculated in the constructor, and
        // so the code here appears common.
        // -----------------------------------------------------------------------

        StringBuffer result = new StringBuffer();

        String packageName = packageName(ivLocalInterface);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(localPrefix); // EJSLocal

        if (ivVersion >= EJB_3X)
            result.append("C"); // Component interface

        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    } // getLocalImplClassName

    /**
     * Returns the name of the deployed class that implements the
     * component remote home interface of the bean. <p>
     * 
     * This method is new for EJB 3.0 modules, and produces a slightly
     * different name than prior EJB module levels; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * For modules prior to EJB 3.0, and CMP Entity beans, this method will
     * return exactly the same name as the static method by the same name
     * (used by EJBDeploy for EJB 1.x and 2.x modules), except the hash
     * suffix will be added for EJB 1.x... instead of adding it later when
     * the class is loaded. For EJB 1.x, the hash suffix may be removed
     * later in updateFilenameHashCode. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the remote home implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSRemoteCSLClaimHome_12345678
     * EJB 2.x (and CMP) : EJSRemoteStatelessClaimHome_12345678
     * EJB 1.x : EJSRemoteClaimHome_12345678 (based on interface)
     * 
     * @return the name of the deployed class that implements the remote
     *         home interface, or null when no remote home interface.
     **/
    // d366807.3
    public String getHomeRemoteImplClassName()
    {
        if (ivRemoteHomeInterface == null)
            return null;

        // -----------------------------------------------------------------------
        // The component remote home implementation name was changed in EJB 2.x to
        // use the bean name and hash suffix, and only modified slightly again
        // in EJB 3.0.  The only 3 differences in EJB 3.0 are :
        //
        //   1 - "C" indicating component interface added
        //   2 - beantype shortened for Stateless and Stateful (now SL/SF)
        //   3 - hash code uses modified algorithm, and includes business interfaces
        //
        // The last two differences are pre-calculated in the constructor, and
        // so the code here appears common.
        // -----------------------------------------------------------------------

        StringBuffer result = new StringBuffer();

        // Note: uses remote interface package, not remote home package (historical).
        String packageName = packageName(ivRemoteInterface);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(homeRemotePrefix); // EJSRemote

        if (ivVersion >= EJB_3X)
            result.append("C"); // Component interface

        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.

        if (ivVersion >= EJB_2X)
        {
            result.append(ivBeanName); // First 32 characters
            result.append("Home");
            result.append('_');
            result.append(ivHashSuffix); // 8 digit hashcode
        }
        else
        {
            result.append(relativeName(ivRemoteHomeInterface));
            if (ivHashVersion > 0) // d369262.3
            {
                result.append('_');
                result.append(ivHashSuffix); // 8 digit hashcode
            }
        }

        return result.toString();
    } // getHomeRemoteImplClassName

    /**
     * Returns the name of the deployed class that implements the
     * component local home interface of the bean. <p>
     * 
     * This method is new for EJB 3.0 modules, and produces a slightly
     * different name than prior EJB module levels; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * For modules prior to EJB 3.0, and CMP Entity beans, this method will
     * return exactly the same name as the static method by the same name
     * (used by EJBDeploy for EJB 1.x and 2.x modules). <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the local home implementation class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSLocalCSLClaimHome_12345678
     * EJB 2.x (and CMP) : EJSLocalStatelessClaimHome_12345678
     * EJB 1.x : Not Supported
     * 
     * @return the name of the deployed class that implements the local
     *         home interface, or null when no remote home interface.
     * 
     * @throws IllegalStateException if this method is called for an EJB 1.x
     *             module level AND a local home interface exists.
     **/
    // d366807.3
    public String getHomeLocalImplClassName()
    {
        if (ivLocalHomeInterface == null)
            return null;

        if (ivVersion == EJB_1X)
            throw new IllegalStateException("Local interfaces are not supported " +
                                            "in EJB 1.x modules");

        // -----------------------------------------------------------------------
        // The component local home implementation name was new in EJB 2.x and
        // only modified slightly in EJB 3.0.  The only 3 differences are :
        //
        //   1 - "C" indicating component interface added
        //   2 - beantype shortened for Stateless and Stateful (now SL/SF)
        //   3 - hash code uses modified algorithm, and includes business interfaces
        //
        // The last two differences are pre-calculated in the constructor, and
        // so the code here appears common.
        // -----------------------------------------------------------------------

        StringBuffer result = new StringBuffer();

        // Note: uses local interface package, not local home package (historical).
        String packageName = packageName(ivLocalInterface);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(homeLocalPrefix); // EJSLocal

        if (ivVersion >= EJB_3X)
            result.append("C"); // Component interface

        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(ivBeanName); // First 32 characters
        result.append("Home");
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    } // getHomeLocalImplClassName

    /**
     * Returns the name of the deployed home bean class. A deployed home
     * bean class will exist only if a remote or local home interface
     * has been defined. So, null will be returned for EJBs that contain
     * just a WebService Endpoint interface or EJB 3.0 Business Interfaces,
     * and no Component Remote or Local interfaces. <p>
     * 
     * This method is new for EJB 3.0 modules, and produces a slightly
     * different name than prior EJB module levels; to account for the fact
     * that an EJB may now have multiple local and remote interfaces. <p>
     * 
     * For modules prior to EJB 3.0, and CMP Entity beans, this method will
     * return exactly the same name as the static method by the same name
     * (used by EJBDeploy for EJB 1.x and 2.x modules), except the hash
     * suffix will be added for EJB 1.x... instead of adding it later when
     * the class is loaded. For EJB 1.x, the hash suffix may be removed
     * later in updateFilenameHashCode. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. Some examples for
     * the home bean class names are as follows: <p>
     * 
     * EJB 3.x (except CMP) : EJSCSLClaimHome_12345678
     * EJB 2.x (and CMP) : EJSRemoteStatelessClaimHome_12345678
     * EJB 1.x : EJSRemoteClaimHome_12345678 (based on interface)
     * 
     * @return the name of the deployed home bean class if there is a
     *         component home interface; otherwise null.
     **/
    // d366807.3
    public String getHomeBeanClassName()
    {
        // There is no generated home bean if there is neither a remote or
        // local home interface (webservice endpoint or EJB 3.0 without a
        // component interface).
        String homeInterface = (ivRemoteHomeInterface == null) ? ivLocalHomeInterface
                        : ivRemoteHomeInterface;
        if (homeInterface == null)
            return null;

        // -----------------------------------------------------------------------
        // The component home bean name was changed in EJB 2.x to use the bean
        // name and hash suffix, and only modified slightly again in EJB 3.0.
        //  The only 3 differences in EJB 3.0 are :
        //
        //   1 - "C" indicating component interface added
        //   2 - beantype shortened for Stateless and Stateful (now SL/SF)
        //   3 - hash code uses modified algorithm, and includes business interfaces
        //
        // The last two differences are pre-calculated in the constructor, and
        // so the code here appears common.
        // -----------------------------------------------------------------------

        StringBuffer result = new StringBuffer();

        // Note: uses remote home interface package by default.
        String packageName = packageName(homeInterface);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(homeBeanPrefix); // EJS

        if (ivVersion >= EJB_3X)
            result.append("C"); // Component interface

        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.

        if (ivVersion >= EJB_2X)
        {
            result.append(ivBeanName); // First 32 characters
            result.append("HomeBean");
            result.append('_');
            result.append(ivHashSuffix); // 8 digit hashcode
        }
        else
        {
            result.append(relativeName(ivRemoteHomeInterface));
            result.append("Bean");
            if (ivHashVersion > 0) // d369262.3
            {
                result.append('_');
                result.append(ivHashSuffix); // 8 digit hashcode
            }
        }

        return result.toString();
    } // getHomeBeanClassName

    /**
     * Returns the name of the deployed concrete bean class for EJB 2.x
     * Container Managed Entity beans. Will return null if the module
     * level is < 2.0, or the EJB is not a Container Managed Entity. <p>
     * 
     * This method will return exactly the same name as the static method
     * by the same name (used by EJBDeploy for CMP beans). <p>
     * 
     * The deployed name format is:
     * 
     * <package name>.Concrete<bean name>_<hashcode>
     * 
     * @return the name of the deployed concrete bean class, or null
     *         for non-cmp Entity beans.
     **/
    // d366807.3
    public String getConcreteBeanClassName()
    {
        if (ivVersion != EJB_2X &&
            ivBeanType != CONTAINER_MANAGED)
            return null;

        StringBuffer result = new StringBuffer();

        String packageName = packageName(ivBeanClass);
        if (packageName != null)
        {
            result.append(packageName);
            result.append('.');
        }

        result.append(concreteBeanPrefix); // Concrete
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    } // getConcreteBeanClassName

    /**
     * Return the name of the EJB 1.x deployed persister class using following
     * template:
     * 
     * This method will return exactly the same name as the static method
     * by the same name (used by EJBDeploy for CMP 1.x beans). <p>
     * 
     * The deployed name format is:
     * 
     * <package name>.EJSJDBCPersisterCMP<bean class>
     **/
    // d366807.3
    public String getDeployedPersisterClassName()
    {
        StringBuffer result = new StringBuffer();

        String packageName = packageName(ivBeanClass);
        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(persisterPrefix); // EJSJDBCPersister
        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(relativeName(ivBeanClass));

        return result.toString();
    }

    /**
     * Return the name of the WebService Endpoint EJB proxy class using the
     * following template:
     * 
     * The deployed name format is:
     * 
     * <package name>.WSEJBProxySL<bean name>_<hashcode>
     * 
     * Returns the name of the deployed class that implements the WebService
     * Endpoint methods of the bean. A WebService Endpoint EJB proxy class
     * will only be used if there are EJB Interceptors, to allow the EJB
     * Container a way to intercept the WebService Endpoint call, and invoke
     * the EJB Interceptors. <p>
     * 
     * This method is new for WebSphere 7.0, and although the WebServices
     * integration with EJB Container has switched for all module levels
     * (EJB 1.0 through 3.0 and beyond), since the WebService Endpoint
     * EJB Proxy class is only used when there are EJB Interceptors, it
     * will never be used for modules prior to EJB 3.0. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. An example for the
     * WebService Endpoint implementation class name is as follows: <p>
     * 
     * WSEJBProxySLClaim_12345678
     * 
     * @return the name of the deployed class that implements the WebService
     *         Endpoint methods.
     **/
    // LI3294-35 d497921
    public String getWebServiceEndpointProxyClassName()
    {
        StringBuilder result = new StringBuilder();

        // Use the package of the EJB implementation.
        String packageName = packageName(ivBeanClass);

        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(endpointPrefix); // WSEJBProxy
        result.append(ivBeanType); // SL/SF/BMP/CMP, etc.
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    }

    /**
     * Return the name of the MDB proxy class using the
     * following template:
     * 
     * The deployed name format is:
     * 
     * <package name>.MDBProxy<bean name>_<hashcode>
     * 
     * Returns the name of the deployed class that implements the
     * interface methods of the MDB bean. <p>
     * 
     * See the NameUtil class overview documentation for a complete
     * description of the deployed name format. An example for the
     * MDB proxy implementation class name is as follows: <p>
     * 
     * MDBProxyClaim_12345678
     * 
     * @return the name of the deployed class that implements the MDB
     *         interface methods.
     **/
    public String getMDBProxyClassName()
    {
        StringBuilder result = new StringBuilder();

        // Use the package of the MDB implementation.
        String packageName = packageName(ivBeanClass);

        if (packageName != null) {
            result.append(packageName);
            result.append('.');
        }

        result.append(mdbProxyPrefix); // MDBProxy
        result.append(ivBeanName); // First 32 characters
        result.append('_');
        result.append(ivHashSuffix); // 8 digit hashcode

        return result.toString();
    }

    /**
     * Returns the hashcode suffix to be appended at the end of all
     * generated class names associated with an EJB. The returned
     * value will always be a String of 8 hex digits. <p>
     * 
     * The hashcode suffix is generated by concatenating all the
     * EJB associated classes/interfaces, and then calculating
     * the hashcode of that String using the BuzzHash algorithm. <p>
     * 
     * This method is new for EJB 3.0 modules, largely to separate the
     * name utility functions from WCCM and for improved performance
     * (since it is only called once per EJB). <p>
     * 
     * NOTE: The original (not 'modified') version of the hash algorithm
     * is used by default for EJB 1.x and EJB 2.x modules (just as
     * in prior releases). Although the 'modified' version is
     * better able to handle repeating String patterns, it appears
     * that EJBDeploy only uses the the modified version when a
     * conflict is detected. Since the vast majority of deployed
     * classes will use the original algorithm, it is much more
     * likely the EJB Container will successfully load a deployed
     * class on the first attempt if the original hash algorithm
     * is attempted first. If a deployed class fails to load, then
     * an attempt will be made to load the class with a named based
     * on the 'modified' hash algorithm. <p>
     * 
     * CAUTION: This method must continue to return the same suffix
     * as returned in prior releases in order to remain compatible
     * with EJBDeploy. See static method getHashStr(). <p>
     * 
     * @param modified true indicates the modified version of the hash
     *            algorithm should be used.
     * 
     * @return the hashcode suffix associated with an EJB.
     **/
    // d366807.3
    private final String getHashSuffix(boolean modified)
    {
        StringBuffer hashStr = new StringBuffer(ivFullBeanName);

        if (ivRemoteHomeInterface != null)
        {
            hashStr.append(ivRemoteHomeInterface);
        }
        if (ivRemoteInterface != null)
        {
            hashStr.append(ivRemoteInterface);
        }
        if (ivLocalHomeInterface != null)
        {
            hashStr.append(ivLocalHomeInterface);
        }
        if (ivLocalInterface != null)
        {
            hashStr.append(ivLocalInterface);
        }
        if (ivBusinessRemote != null)
        {
            for (int i = 0; i < ivBusinessRemote.length; ++i)
            {
                hashStr.append(ivBusinessRemote[i]);
            }
        }
        if (ivBusinessLocal != null)
        {
            for (int i = 0; i < ivBusinessLocal.length; ++i)
            {
                hashStr.append(ivBusinessLocal[i]);
            }
        }
        hashStr.append(ivBeanClass);
        if (ivPrimaryKey != null)
        {
            hashStr.append(ivPrimaryKey);
        }

        ivHashVersion = modified ? 2 : 1; // d369262.3

        return BuzzHash.computeHashStringMid32Bit(hashStr.toString(), modified);
    } // getHashSuffix

    /**
     * Returns the next possible generated class name to attempt to load.
     * If the specified name contains the original hash suffix, then the
     * returned class name will be updated using the 'modified' hash
     * algorithm. If the specified name already contains the 'modified'
     * hash suffix, then for EJB 1.1 module levels, the class name returned
     * will have no suffix; otherwise null is returned. <p>
     * 
     * Also, invoking this method updates the NameUtil instance so that
     * subsequent generated class names returned will use the 'modified'
     * hashcode. <p>
     * 
     * This method detects if the file name contains a valid trailing hashcode
     * and trys to compute the new one. If the input file name has no valid
     * hashcode, null is returned. <p>
     * 
     * For ease of invocation regardless of the type of file passed in, this
     * method only changes the hash value and will not recreate the complete
     * file name. <p>
     * 
     * This method works the same as the static method by the same name, but
     * has the advantage that once a class fails to load using one version of
     * the hashcode, all subsequent generated class names will be automatically
     * switched to include the other hash suffix; avoiding other class load
     * failures. <p>
     * 
     * @param fileName name of the generated class file to be updated
     * @return the file name using the old hash code, or null if no
     *         new name can be constructed.
     **/
    // d366807.3
    public final String updateFilenameHashCode(String fileName)
    {
        String nextName = null;
        int len = fileName.length();
        int last_ = (len > 9 && (fileName.charAt(len - 9) == '_')) ? len - 9 : -1;
        // input file name must have a trailing "_" follows by 8 hex digits
        // and check to make sure the last 8 characters are all hex digits
        if (last_ != -1 && allHexDigits(fileName, ++last_, len))
        {
            // If version 1 hash suffix, then update to version 2 has suffix
            if (ivHashVersion == 1) // d369262.3
            {
                ivHashSuffix = getHashSuffix(true);
                nextName = fileName.substring(0, last_) + ivHashSuffix;
            }
            // if version 2 hash suffix, and EJB 1.1, then remove hash suffix
            else if (ivHashVersion == 2 && ivVersion == EJB_1X) // d369262.3
            {
                ivHashVersion = 0;
                nextName = fileName.substring(0, (last_ - 1));
            }
            // All other scenarios.... just fall out and return null
        }
        return nextName;
    } // updateFilenameHashCode

} // NameUtil
