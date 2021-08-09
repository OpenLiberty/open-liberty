/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import static com.ibm.ejs.container.ContainerProperties.ValidateMergedXML;
import static com.ibm.ejs.container.ContainerProperties.ValidateMergedXMLFail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.javaee.dd.ejb.ComponentViewableBean;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Session;

/**
 * Utility methods for accessing WCCM meta data.
 */
public class WCCMMetaDataUtil
{
    private static final TraceComponent tc = Tr.register(WCCMMetaDataUtil.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");
    private static final String MERGE = "     Merged DD ";

    private static boolean validationEnabled()
    {
        return (ValidateMergedXML || (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()));
    }

    /**
     * Compares the module level internal EJB Container metadata (from an
     * internal merge of XML and annotations) and the merge view produced
     * by WAS.metadatamgr and reports any mismatches by logging them or
     * throwing an exception, depending on the system property setting. <p>
     * 
     * @param mdo module metadata, including access to the merged DD.
     * @param cdos collection of component metadata; not null.
     * 
     * @throws EJBConfigurationException if errors were found and the system
     *             property has been configured for failure.
     */
    // d680497.1
    public static void validateMergedXML(ModuleInitData mid)
                    throws EJBConfigurationException
    {
        if (!validationEnabled()) {
            return;
        }

        EJBJar mergedEJBJar = mid.getMergedEJBJar();
        if (mergedEJBJar == null) { // managed beans only
            return;
        }

        List<String> errors = new ArrayList<String>();

        List<EnterpriseBean> mergedBeans = mergedEJBJar.getEnterpriseBeans();
        if (mergedBeans == null)
        {
            errors.add(MERGE + "contains no EJBs.");
            logErrors(mid.ivJ2EEName, errors);
            return;
        }

        TreeSet<String> mergedNames = new TreeSet<String>();
        for (EnterpriseBean mergedBean : mergedBeans) {
            mergedNames.add(mergedBean.getName());
        }

        for (BeanInitData bid : mid.ivBeans) {
            mergedNames.remove(bid.ivName);
        }

        if (!mergedNames.isEmpty()) {
            errors.add(MERGE + "contains extra EJBs named " + mergedNames);
        }

        logErrors(mid.ivJ2EEName, errors);
    }

    /**
     * Compares the component level internal EJB Container metadata (from an
     * internal merge of XML and annotations) and the merge view produced by
     * WAS.metadatamgr and reports any mismatches by logging them or throwing
     * an exception, depending on the system property setting. <p>
     * 
     * WCCMMetaData.touch should have been called recently on the WCCMMetaData
     * associated with the provided BeanMetaData. This method will not call
     * 'touch'. <p>
     * 
     * @param bmd the internal EJB Container metadata
     * 
     * @throws EJBConfigurationException if errors were found and the system
     *             property has been configured for failure.
     */
    public static void validateMergedXML(BeanMetaData bmd)
                    throws EJBConfigurationException
    {
        if (!validationEnabled()) {
            return;
        }

        if (bmd.metadataComplete || bmd.wccm == null ||
            bmd.isManagedBean() || bmd.isEntityBean())
        {
            return;
        }

        EJBJar mergedEJBJar = bmd.ivInitData.ivModuleInitData.getMergedEJBJar();
        if (mergedEJBJar == null)
        {
            return;
        }

        Session msb = null;
        List<String> errors = new ArrayList<String>();
        EnterpriseBean meb = WCCMMetaData.getEnterpriseBeanNamed(mergedEJBJar, bmd.enterpriseBeanName);

        if (meb == null)
        {
            errors.add(MERGE + "is missing an EJB named " + bmd.enterpriseBeanName);
            logErrors(bmd.j2eeName, errors);
            return;
        }

        // -----------------------------------------------------------------------
        // Common processing for all bean types / partial BMD
        // -----------------------------------------------------------------------

        // Required : <ejb-name>
        if (!bmd.enterpriseBeanName.equals(meb.getName()))
        {
            errors.add(MERGE + "<ejb-name> : " + meb.getName() +
                       ", expected : " + bmd.enterpriseBeanName);
        }

        if (meb instanceof ComponentViewableBean)
        {
            ComponentViewableBean cvb = (ComponentViewableBean) meb;

            // Required : <home> (if a home is present)
            String mebHomeName = cvb.getHomeInterfaceName();
            if (!equals(bmd.homeInterfaceClassName, mebHomeName))
            {
                errors.add(MERGE + "<home> : " + mebHomeName +
                           ", expected : " + bmd.homeInterfaceClassName);
            }

            // Required : <local-home> (if a local home is present)
            String mebLocalHomeName = cvb.getLocalHomeInterfaceName();
            if (!equals(bmd.localHomeInterfaceClassName, mebLocalHomeName))
            {
                errors.add(MERGE + "<local-home> : " + mebLocalHomeName +
                           ", expected : " + bmd.localHomeInterfaceClassName);
            }
        }

        // Required : <ejb-class>
        if (!bmd.enterpriseBeanClassName.equals(meb.getEjbClassName()))
        {
            errors.add(MERGE + "<ejb-class> : " + meb.getEjbClassName() +
                       ", expected : " + bmd.enterpriseBeanClassName);
        }

        // -----------------------------------------------------------------------
        // Session Bean processing / partial BMD
        // -----------------------------------------------------------------------

        if (bmd.isSessionBean())
        {
            if (meb.getKindValue() != EnterpriseBean.KIND_SESSION)
            {
                errors.add(MERGE + "bean type is incorrect, should be <session>");
                logErrors(bmd.j2eeName, errors);
                return;
            }

            msb = (Session) meb;

            // Required : <business-local> (if business local are present)
            List<String> msbLocalBusinessClasses = msb.getLocalBusinessInterfaceNames();
            String[] msbLocalBusiness = msbLocalBusinessClasses.toArray(new String[msbLocalBusinessClasses.size()]);

            if (!setEquals(bmd.ivBusinessLocalInterfaceClassNames, msbLocalBusiness))
            {
                errors.add(MERGE + "<business-local> : " + Arrays.toString(msbLocalBusiness) +
                           ", expected : " + Arrays.toString(bmd.ivBusinessLocalInterfaceClassNames));
            }

            // Required : <business-remote> (if business remote are present)
            List<String> msbRemoteBusinessClasses = msb.getRemoteBusinessInterfaceNames();
            String[] msbRemoteBusiness = msbRemoteBusinessClasses.toArray(new String[msbRemoteBusinessClasses.size()]);

            if (!setEquals(bmd.ivBusinessRemoteInterfaceClassNames, msbRemoteBusiness))
            {
                errors.add(MERGE + "<business-remote> : " + Arrays.toString(msbRemoteBusiness) +
                           ", expected : " + Arrays.toString(bmd.ivBusinessRemoteInterfaceClassNames));
            }

            // Required : <local-bean> (if no-interface present)
            if (bmd.ivLocalBean != msb.isLocalBean())
            {
                errors.add(MERGE + "<local-bean> : " + msb.isLocalBean() +
                           ", expected : " + bmd.ivLocalBean);
            }

            // Required : <session-type> (if not Stateless)
            int msbSessionType = msb.getSessionTypeValue();
            if ((bmd.type == InternalConstants.TYPE_STATELESS_SESSION &&
                msbSessionType != Session.SESSION_TYPE_STATELESS) ||
                (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION &&
                msbSessionType != Session.SESSION_TYPE_STATEFUL) ||
                (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION &&
                msbSessionType != Session.SESSION_TYPE_SINGLETON))
            {
                String msbTypeStr = msbSessionType == Session.SESSION_TYPE_UNSPECIFIED ? "unspecified" :
                                msbSessionType == Session.SESSION_TYPE_SINGLETON ? "Singleton" :
                                                msbSessionType == Session.SESSION_TYPE_STATEFUL ? "Stateful" :
                                                                msbSessionType == Session.SESSION_TYPE_STATELESS ? "Stateless" :
                                                                                "unknown";
                String typeStr = bmd.isStatelessSessionBean() ? "Stateless" :
                                bmd.isStatefulSessionBean() ? "Stateful" : "Singleton";
                errors.add(MERGE + "<session-type> : " + msbTypeStr +
                           ", expected : " + typeStr);
            }

            // Required : <transaction-type> (if not default)
            int mTxType = msb.getTransactionTypeValue();
            if ((bmd.usesBeanManagedTx &&
                mTxType != Session.TRANSACTION_TYPE_BEAN) ||
                (!bmd.usesBeanManagedTx &&
                mTxType != Session.TRANSACTION_TYPE_CONTAINER))
            {
                String mTxStr = mTxType == Session.TRANSACTION_TYPE_UNSPECIFIED ? "unspecified" :
                                mTxType == Session.TRANSACTION_TYPE_BEAN ? "Bean" :
                                                mTxType == Session.TRANSACTION_TYPE_CONTAINER ? "Container" :
                                                                "unknown";
                String txStr = bmd.usesBeanManagedTx ? "Bean" : "Container";
                errors.add(MERGE + "<transaction-type> : " + mTxStr +
                           ", expected : " + txStr);
            }
        }

        if (bmd.fullyInitialized &&
            meb instanceof ComponentViewableBean)
        {
            ComponentViewableBean cvb = (ComponentViewableBean) meb;

            // --------------------------------------------------------------------
            // Common processing for all bean types / completed BMD
            // --------------------------------------------------------------------

            // Optional : <remote> (may be derived from home.create, but will require)
            String mebRemoteName = cvb.getRemoteInterfaceName();
            String remoteName = (bmd.remoteInterfaceClass != null) ? bmd.remoteInterfaceClass.getName() : null;
            if (!equals(remoteName, mebRemoteName))
            {
                errors.add(MERGE + "<remote> : " + mebRemoteName +
                           ", expected : " + remoteName);
            }

            // Optional : <local> (may be derived from localhome.create, but will require)
            String mebLocalName = cvb.getLocalInterfaceName();
            String localName = (bmd.localInterfaceClass != null) ? bmd.localInterfaceClass.getName() : null;
            if (!equals(localName, mebLocalName))
            {
                errors.add(MERGE + "<local> : " + mebLocalName +
                           ", expected : " + localName);
            }

            // --------------------------------------------------------------------
            // Session Bean processing / completed BMD
            // --------------------------------------------------------------------

            if (msb != null)
            {
                // Required : <stateful-timeout> (if not default)
                if (bmd.sessionTimeout != msb.getStatefulTimeout().getTimeout())
                {
                    errors.add(MERGE + "<session-timeout> : " + msb.getStatefulTimeout().getTimeout() +
                               ", expected : " + bmd.sessionTimeout);
                }
            }

        }

        logErrors(bmd.j2eeName, errors);
    }

    /**
     * Convenience equality method that uses object equality, but supports both
     * objects being null.
     */
    // d680497.1
    static <T> boolean equals(T a, T b)
    {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Convenience equality method for sets represented by arrays, which uses
     * object equality, and supports either parameter being null and differences
     * in order. <p>
     * 
     * An zero length array is considered equal to null.
     */
    // d680497.1
    static <T> boolean setEquals(T[] a, T[] b)
    {
        if (a == null || a.length == 0) {
            return b == null || b.length == 0;
        }
        return new TreeSet<Object>(Arrays.asList(a)).equals(new TreeSet<Object>(Arrays.asList(b)));
    }

    /**
     * Perform the actual logging and throw an exception if errors were
     * present in the merged EJB DD. <p>
     * 
     * @param j2eeName identification of bean with errors
     * @param errors errors that were found
     * 
     * @throws EJBConfigurationException if errors were found and the system
     *             property has been configured for failure.
     */
    private static void logErrors(J2EEName j2eeName,
                                  List<String> errors)
                    throws EJBConfigurationException
    {
        if (errors.size() > 0)
        {
            String heading = "Merged EJB DD Validation results for " + j2eeName;

            // Log to SystemOut.log only if the property is enabled.      d680497.1
            if (ValidateMergedXML)
            {
                System.out.println(heading);
                for (String error : errors) {
                    System.out.println(error);
                }
            }
            else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, heading);
                for (String error : errors) {
                    Tr.debug(tc, error);
                }
            }

            // Errors will be in Systemout.log if this property is enabled
            if (ValidateMergedXMLFail)
            {
                throw new EJBConfigurationException("Merged EJB DD for " + j2eeName +
                                                    " is not correct. See SystemOut.log for details.");
            }
        }
    }
}
