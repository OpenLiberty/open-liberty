/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.traceinfo.ejbcontainer;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;

/**
 * Container for the BeanMetaData information used by the tracing tool.
 */
public class TEBeanMetaDataInfo implements TEInfoConstants
{
    private static final TraceComponent tc = Tr.register(TEBeanMetaDataInfo.class,
                                                         "TEExplorer",
                                                         "com.ibm.ws.traceinfo.ejbcontainer");

    /**
     * Helper method to convert a Class object to its textual representation.
     * If class is null, the "N/A" string is returned.
     */
    private static String turnNullClass2EmptyString(Class<?> cls)
    {
        return (cls == null) ? NotApplicable : cls.toString();
    }

    /**
     * Helper method to convert a String object to its textual representation.
     * If class is null, the <i>NullDefaultStr</i> is returned.
     */
    private static String turnNullString2EmptyString(String str)
    {
        return (str == null) ? NotApplicable : str.toString();
    }

    /**
     * Writes all the method info data representd by <i>methodInfos</i> to <i>sbuf</i>
     */
    private static void writeMethodInfo(StringBuffer sbuf, EJBMethodInfoImpl methodInfos[])
    {
        if (methodInfos == null)
        {
            sbuf.append("" + -1).append(DataDelimiter);
        } else
        {
            int size = methodInfos.length;
            sbuf.append("" + size).append(DataDelimiter);
            for (int i = 0; i < size; ++i)
            {
                EJBMethodInfoImpl info = methodInfos[i];
                sbuf
                                .append(i).append(DataDelimiter)
                                .append(info.getMethodName()).append(DataDelimiter)
                                .append(info.getJDIMethodSignature()).append(DataDelimiter)
                                .append(info.getTransactionAttribute().getValue()).append(DataDelimiter)
                                .append(info.getActivitySessionAttribute().getValue()).append(DataDelimiter)
                                .append(info.getIsolationLevel()).append(DataDelimiter)
                                .append(info.getReadOnlyAttribute() ? "true" : "false").append(DataDelimiter);
            }
        }
    }

    /**
     * Collects all the bean meta data information from the input BeanMetaData, EnterpriseBan
     * and dtdVersion objects, converts them to their textual representation and write it out
     * to the trace.log.
     */
    public static void writeTraceBeanMetaData(BeanMetaData bmd,
                                              int dtdVersion)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(BMD_Type_Str).append(DataDelimiter)
                            .append(BMD_Type).append(DataDelimiter)
                            .append(dtdVersion).append(DataDelimiter)
                            .append(bmd.j2eeName).append(DataDelimiter)
                            .append(bmd.simpleJndiBindingName).append(DataDelimiter);

            if (bmd.type == InternalConstants.TYPE_STATELESS_SESSION) {

                sbuf
                                .append("Stateless")
                                .append(" Session Bean").append(DataDelimiter);
                if (bmd.usesBeanManagedTx) {
                    sbuf.append("BMT").append(DataDelimiter);
                } else {
                    sbuf.append("CMT").append(DataDelimiter);
                }
                sbuf.append("N/A").append(DataDelimiter); // dxxxxxx

            } else if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {

                sbuf
                                .append("Stateful")
                                .append(" Session Bean").append(DataDelimiter);
                if (bmd.usesBeanManagedTx) {
                    sbuf.append("BMT").append(DataDelimiter);
                } else {
                    sbuf.append("CMT").append(DataDelimiter);
                }
                sbuf.append("N/A").append(DataDelimiter);

            } else if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION) { //F743-508.CodRev

                sbuf
                                .append("Singleton")
                                .append(" Session Bean").append(DataDelimiter);
                if (bmd.usesBeanManagedTx) {
                    sbuf.append("BMT").append(DataDelimiter);
                } else {
                    sbuf.append("CMT").append(DataDelimiter);
                }
                sbuf.append("N/A").append(DataDelimiter);

            } else if (bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY) {

                sbuf
                                .append("BMP")
                                .append(" Entity Bean").append(DataDelimiter)
                                .append("CMT").append(DataDelimiter)
                                .append("N/A").append(DataDelimiter);

            } else if (bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) {

                sbuf
                                .append("CMP")
                                .append(" Entity Bean").append(DataDelimiter)
                                .append("CMT").append(DataDelimiter);
                int cmpV = bmd.getCMPVersion();
                sbuf.append(cmpV).append(".x").append(DataDelimiter);

            } else if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                sbuf.append("Message Driven Bean").append(DataDelimiter);
                if (bmd.usesBeanManagedTx) {
                    sbuf.append("BMT").append(DataDelimiter);
                } else {
                    sbuf.append("CMT").append(DataDelimiter);
                }
                sbuf.append("N/A").append(DataDelimiter);
            }

            sbuf
                            .append(turnNullClass2EmptyString(bmd.homeInterfaceClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.remoteInterfaceClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.remoteImplClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.homeRemoteImplClass)).append(DataDelimiter)

                            .append(turnNullClass2EmptyString(bmd.localHomeInterfaceClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.localInterfaceClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.localImplClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.homeLocalImplClass)).append(DataDelimiter);

            sbuf
                            .append(turnNullClass2EmptyString(bmd.homeBeanClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.enterpriseBeanClass)).append(DataDelimiter)
                            .append(turnNullClass2EmptyString(bmd.enterpriseBeanAbstractClass)).append(DataDelimiter)

                            .append(turnNullClass2EmptyString(bmd.pKeyClass)).append(DataDelimiter)

                            .append(bmd.minPoolSize).append(DataDelimiter)
                            .append(bmd.maxPoolSize).append(DataDelimiter)
                            .append(bmd.reentrant).append(DataDelimiter)
                            .append(bmd.sessionTimeout).append(DataDelimiter)
                            .append(bmd.isPreFindFlushEnabled).append(DataDelimiter)
                            .append(bmd.optionACommitOption ? "A"
                                            : bmd.optionBCommitOption ? "B"
                                                            : bmd.optionCCommitOption ? "C"
                                                                            : "Unknown").append(DataDelimiter);

            writeMethodInfo(sbuf, bmd.methodInfos);
            writeMethodInfo(sbuf, bmd.localMethodInfos);
            writeMethodInfo(sbuf, bmd.homeMethodInfos);
            writeMethodInfo(sbuf, bmd.localHomeMethodInfos);

            ResourceRefConfigList resRefList = bmd._resourceRefList;
            int resourcesDefined = resRefList.size();
            sbuf.append(resourcesDefined).append(DataDelimiter);
            for (int i = 0; i < resourcesDefined; ++i)
            {
                ResourceRefConfig resRef = resRefList.getResourceRefConfig(i);
                sbuf
                                .append(turnNullString2EmptyString(resRef.getName())).append(DataDelimiter)
                                .append(turnNullString2EmptyString(resRef.getDescription())).append(DataDelimiter)
                                .append(turnNullString2EmptyString(resRef.getJNDIName())).append(DataDelimiter)
                                .append(turnNullString2EmptyString(resRef.getType())).append(DataDelimiter)
                                .append(resRef.getIsolationLevel()).append(DataDelimiter)
                                .append(resRef.getSharingScope()).append(DataDelimiter)
                                .append(resRef.getAuth()).append(DataDelimiter);
            }
            // d214462 Begins
            // EJB 2.1 - TimedObject           d174057.3
            sbuf.append((dtdVersion >= BeanMetaData.J2EE_EJB_VERSION_2_1 && bmd.isTimedObject)).append(DataDelimiter);

            // MDB 2.0/2.1 and greater ActivationSpec/MessageDestination    LI2110-46
            sbuf
                            .append(turnNullString2EmptyString(bmd.ivActivationSpecJndiName)).append(DataDelimiter)
                            .append(turnNullString2EmptyString(bmd.ivMessageDestinationJndiName)).append(DataDelimiter);

            writeMethodInfo(sbuf, bmd.timedMethodInfos);
            writeMethodInfo(sbuf, bmd.wsEndpointMethodInfos);

            // EJB 2.1 - WebService Endpoint Interface / Wrapper       d174057.3
            sbuf.append(turnNullClass2EmptyString(bmd.webserviceEndpointInterfaceClass)).append(DataDelimiter);

            /**** N O T E S ****/
            // All new metadata MUST append to the sbuf to maintain backward compatibility

            //          sbuf.append( turnNullClass2EmptyString( bmd.webserviceWrapperClass ) ).append( DataDelimiter )

            // d214462 Ends
            Tr.debug(tc, sbuf.toString());
        }
    }

    /**
     * Returns true if trace for this class is enabled. This is used to guard the
     * caller to avoid unncessary processing before the trace is depositied.
     */
    // d173022
    public static boolean isTraceEnabled()
    {
        return (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled());
    }
}
