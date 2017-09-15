/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi;

/**
 * Constants for PMI service
 * 
 * @ibm-api
 */
public interface PmiConstants {
    public static final String PLATFORM_ALL = "all";
    public static final String PLATFORM_DISTRIBUTED = "distributed";
    public static final String PLATFORM_ZOS = "zos";

    /**
     * Unknown statistic ID
     */
    public static final int UNKNOWN_ID = -1;

    /** @deprecated No replacement */
    @Deprecated
    public static final int NOT_IN_SUBMODULE = -1;

    /** Indicates ALL statistics for a PMI module */
    public static final int ALL_DATA = -3;

    /** WebSphere internal use only */
    public static final int JAVA_TIME_CONVERT_RATIO = 1;

    // constants for levels
    /**
     * LEVEL_DISABLE indicates that a statitic will not be instrumented
     */
    public static final int LEVEL_DISABLE = 1000;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_ENABLE = 2;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_MAX = 15;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_HIGH = 7;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_MEDIUM = 3;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_LOW = 1;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_NONE = 0;

    /** @deprecated No replacement */
    @Deprecated
    public static final int LEVEL_UNDEFINED = -1;

    /**
     * Indicates the module is enabled using WebSphere 6.0 fine-grained option
     */
    public static final int LEVEL_FINEGRAIN = -2;

    // Copied from ActiveObjectStatus.java for AE/AEs support
    // This is used compare PmiClient.getAdminState() return value    
    // If ActiveObjectStatus values change this should also be changed.
    /** @deprecated No replacement */
    @Deprecated
    public static final int UNINITIALIZED = 0;
    /** @deprecated No replacement */
    @Deprecated
    public static final int INITIALIZING = 1;
    /** @deprecated No replacement */
    @Deprecated
    public static final int INITIALIZATION_FAILED = 2;
    /** @deprecated No replacement */
    @Deprecated
    public static final int RUNNING = 3;
    /** @deprecated No replacement */
    @Deprecated
    public static final int TERMINATING = 4;
    /** @deprecated No replacement */
    @Deprecated
    public static final int STOPPED = 5;
    /** @deprecated No replacement */
    @Deprecated
    public static final int LOST_CONTACT = 6;

    // WebSphere Edition/Version

    /** @deprecated No replacement */
    @Deprecated
    public static final int AE_40 = 1;

    /** @deprecated No replacement */
    @Deprecated
    public static final int AES_40 = 2;

    /** @deprecated No replacement */
    @Deprecated
    public static final int AE_35 = 3;

    /** @deprecated No replacement */
    @Deprecated
    public static final String LEVEL_NONE_STRING = "none";

    /** @deprecated No replacement */
    @Deprecated
    public static final String LEVEL_LOW_STRING = "low";

    /** @deprecated No replacement */
    @Deprecated
    public static final String LEVEL_MEDIUM_STRING = "medium";

    /** @deprecated No replacement */
    @Deprecated
    public static final String LEVEL_HIGH_STRING = "high";

    /** @deprecated No replacement */
    @Deprecated
    public static final String LEVEL_MAX_STRING = "maximum";

    /** Unknown statistic type */
    public static final int TYPE_UNDEFINED = -1;

    /** @deprecated No replacement */
    @Deprecated
    public static final int TYPE_INT = 1;

    /** J2EE CountStatistic (WebSphere PMI type PerfLong) */
    public static final int TYPE_LONG = 2;

    /** WebSphere PMI type PerfDouble */
    public static final int TYPE_DOUBLE = 3;

    /** J2EE TimeStatistic (WebSphere PMI type PerfStat) */
    public static final int TYPE_STAT = 4;

    /** J2EE BoundedRangeStatistic (WebSphere PMI type PerfLoad) */
    public static final int TYPE_LOAD = 5; // BoundedRangedStatistic

    /** WebSphere J2EE extension type AverageStatistic */
    public static final int TYPE_AVGSTAT = 6; // AverageStatistic

    /** J2EE RangeStatistic */
    public static final int TYPE_RANGE = 7; // RangeStatistic

    /** Invalid statistic type */
    public static final int TYPE_INVALID = -1;

    // constants for module/data hierarchy
    public static final int TYPE_ROOT = 10;
    public static final int TYPE_NODE = 11;
    public static final int TYPE_SERVER = TYPE_NODE + 1;
    public static final int TYPE_MODULE = TYPE_SERVER + 1;
    public static final int TYPE_INSTANCE = TYPE_MODULE + 1;
    public static final int TYPE_SUBMODULE = TYPE_MODULE + 2;
    public static final int TYPE_SUBINSTANCE = TYPE_MODULE + 3;
    public static final int TYPE_COLLECTION = TYPE_MODULE + 4;
    public static final int TYPE_DATA = TYPE_MODULE + 5;
    public static final int TYPE_CATEGORY = TYPE_MODULE + 6;
    public static final int TYPE_MODULEROOT = TYPE_MODULE + 11;

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_START = "<";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDLINE = "\">\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDTAG = "\"/>\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDCOLLECTION = "</PerfCollection>\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDMODULE = "</PerfModule>\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDSERVER = "</PerfServer>\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ENDNODE = "</PerfNode>\n";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_INT = "<PerfInt";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_LONG = "<PerfLong";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_DOUBLE = "<PerfDouble";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_STAT = "<PerfStat";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_LOAD = "<PerfLoad";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_COLLECTION = "PerfCollection";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_MODULE = "<PerfModule";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_SERVER = "<PerfServer";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_NODE = "<PerfNode";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_VIEW = "<PerfView";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_QUOTE = "\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_ID = " ID=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_NAME = " name=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_TIME = "\" time=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_VALUE = "\" value=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_COUNT = "\" count=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_TOTAL = "\" total=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_SUMOFSQUARES = "\" sumOfSquares=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_LASTVALUE = "\" lastValue=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_INTEGRAL = "\" integral=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String XML_CREATETIME = "\" createTime=\"";

    /** @deprecated No replacement */
    @Deprecated
    public static final String DEFAULT_MODULE_PREFIX = "com.ibm.websphere.pmi.xml.";

    public static final String BEAN_MODULE = "beanModule";
    public static final String BEAN_METHODS_SUBMODULE = BEAN_MODULE + ".methods";
    public static final String CONNPOOL_MODULE = "connectionPoolModule"; // for JDBC provider
    public static final String SYSTEM_MODULE = "systemModule";
    public static final String J2C_MODULE = "j2cModule"; // for J2C CF
    public static final String J2C_DS = "DataSource";
    public static final String J2C_CF = "ConnectionFactory";
    public static final String J2C_JMS_CONNECTIONS = "jmsConnections";
    public static final String THREADPOOL_MODULE = "threadPoolModule";
    public static final String TRAN_MODULE = "transactionModule";
    public static final String RUNTIME_MODULE = "jvmRuntimeModule";
    public static final String JVMPI_MODULE = "jvmpiModule";
    public static final String ORBPERF_MODULE = "orbPerfModule";
    public static final String INTERCEPTOR_SUBMODULE = ORBPERF_MODULE + ".interceptors";
    public static final String WEBAPP_MODULE = "webAppModule";
    public static final String SERVLET_SUBMODULE = WEBAPP_MODULE + ".servlets";
    public static final String SESSIONS_MODULE = "servletSessionsModule";
    public static final String CACHE_MODULE = "cacheModule";
    public static final String TEMPLATE_SUBMODULE = CACHE_MODULE + ".template";
    public static final String APPSERVER_MODULE = "pmi";
    public static final String WSGW_MODULE = "wsgwModule";

    public static final String WLM_MODULE = "wlmModule"; // 86523.26
    public static final String WLM_SERVER_MODULE = WLM_MODULE + ".server"; // 86523.26.2
    public static final String WLM_CLIENT_MODULE = WLM_MODULE + ".client"; // 86523.26.2

    public static final String WEBSERVICES_MODULE = "webServicesModule";
    public static final String WEBSERVICES_SUBMODULE = WEBSERVICES_MODULE + ".services";

    public static final String METHODS_SUBMODULE_SHORTNAME = "methods";
    public static final String SERVLETS_SUBMODULE_SHORTNAME = "servlets";

    public static final String ROOT_NAME = "pmiroot";

    /** @deprecated No replacement */
    @Deprecated
    public static final String ROOT_DESC = "pmiroot.desc";

    /** @deprecated No replacement */
    @Deprecated
    public static final String COLLECTION_DESC = ".col";

    /** @deprecated No replacement */
    @Deprecated
    public static final String PMI_DISABLE_STRING = "pmi=disable";

    /** @deprecated No replacement */
    @Deprecated
    public static final String LOAD_AVG = "pmi.avg";

    /// EJB types
    public static final String EJB_ENTITY = "ejb.entity";
    public static final String EJB_STATEFUL = "ejb.stateful";
    public static final String EJB_STATELESS = "ejb.stateless";
    public static final String EJB_SINGLETON = "ejb.singleton";
    public static final String EJB_MESSAGEDRIVEN = "ejb.messageDriven";
    public static final String MBEAN_NAME = "WebSphere:type=Perf";
    public static final String TR_GROUP = "pmi";
    public static final String MSG_BUNDLE = "com.ibm.ws.pmi.properties.PMIMessages";
}