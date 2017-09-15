/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.soe_reporting;

/**
 * Holds a set of constants which are shared by the result collection servlet and by the build process.
 * 
 * This separate file is used to avoid introducing a dependency on the Ant "Task" class in the servlet.
 */
public abstract class ResultCollectionConstants {

    // Parameter names under which data is sent to the servlet to be persisted.

    // The servlet contains a mapping from these parameter names to the table/column where
    // the data should be persisted to in the database.

    // TODO Where an identical named constant is below the in-use one, it means we really
    // want to rename the variable to something actually meaningful or readable but can't
    // as it's currently in use...

    public static final String PARAM_BUCKET_NAME = "BUCKETNAME";
    // public static final String PARAM_BUCKET_NAME = "BUCKET_NAME";

    public static final String PARAM_TEST_PASSES = "TESTSPASSED";
    // public static final String PARAM_TEST_PASSES = "TEST_PASSES";

    public static final String PARAM_TEST_FAILURES = "TESTSFAILED";
    // public static final String PARAM_TEST_FAILURES = "TEST_FAILURES";

    public static final String PARAM_TEST_ERRORS = "TESTERRORS";
    // public static final String PARAM_TEST_ERRORS = "TEST_ERRORS";

    public static final String PARAM_TOTAL_TESTS = "TOTALTESTS";
    // public static final String PARAM_TOTAL_TESTS = "TEST_TOTAL";

    public static final String PARAM_BUILD_ENGINE_NAME = "ENGINENAME";
    // public static final String PARAM_BUILD_ENGINE_NAME = "BUILD_ENGINE_NAME";

    public static final String PARAM_INSTALL_TYPE_NAME = "INSTALLTYPE";
    // public static final String PARAM_INSTALL_TYPE_NAME = "INSTALL_TYPE_NAME";

    public static final String PARAM_INSTALL_TYPE_TOPOLOGY = "TOPOLOGY";
    // public static final String PARAM_INSTALL_TYPE_TOPOLOGY = "INSTALL_TYPE_TOPOLOGY";

    public static final String PARAM_JVM_REPORTING_JVM = "JVMNAME";
    // public static final String PARAM_JVM_REPORTING_JVM = "JVM_NAME";

    public static final String PARAM_RELEASE_BUILD_LABEL = "RELEASEBUILDLABEL";
    // public static final String PARAM_RELEASE_BUILD_LABEL = "RELEASE_BUILD_LABEL";

    public static final String PARAM_TEST_BUILD_LABEL = "TESTBUILDLABEL";
    // public static final String PARAM_TEST_BUILD_LABEL = "TEST_BUILD_LABEL";

    public static final String PARAM_TEST_DATABASE_NAME = "DBNAME";
    // public static final String PARAM_TEST_DATABASE_NAME = "TEST_DATABASE_NAME";

    public static final String PARAM_OS_NAME = "OSNAME";
    // public static final String PARAM_OS_NAME = "OS_NAME";

    public static final String PARAM_OS_ARCHITECTURE = "OSARCHITECTURE";
    // public static final String PARAM_OS_ARCHITECTURE = "OS_ARCHITECTURE";

    public static final String PARAM_OS_REPORTING_OS = "SOEOS";
    // public static final String PARAM_OS_REPORTING_OS = "SOE_OS";

    public static final String PARAM_RELEASE_BUILD_REPORTING_TYPE = "RELEASE_BUILD_REPORTING_TYPE";
    public static final String PARAM_RELEASE_BUILD_REPORTING_IGNORE = "RELEASE_BUILD_REPORTING_IGNORE";
    public static final String PARAM_RELEASE_BUILD_REPORTING_VERSION = "RELEASE_BUILD_REPORTING_VERSION";
    public static final String PARAM_RELEASE_BUILD_DATE = "RELEASE_BUILD_DATE";
    public static final String PARAM_START_TIME_AND_DATE = "START_TIME_AND_DATE";
    public static final String PARAM_DURATION = "DURATION";
    public static final String PARAM_BUILD_RUN_ID = "BUILD_RUN_ID";

    public static final String VALUE_UNSPECIFIED = "Unspecified";

    // Log-scrape timeout failure additions.
    public static final String PARAM_FAILURE_TYPE = "FAILURE_TYPE";
    public static final String PARAM_WORKSPACE = "WORKSPACE";
    public static final String PARAM_TEST_CLASS = "TEST_CLASS";
    public static final String PARAM_TEST_METHOD = "TEST_METHOD";
    public static final String PARAM_TEST_LINE = "TEST_LINE";
    public static final String PARAM_IS_PERSONAL_BUILD = "IS_PERSONAL_BUILD";
    public static final String PARAM_TIME_OCCURRED = "TIME_OCCURRED";
    public static final String PARAM_TIME_WAITED_MILLIS = "TIME_WAITED_MILLIS";
    public static final String PARAM_SEARCH_REGEX = "SEARCH_REGEX";
    public static final String PARAM_JVM_VERSION = "JVM_VERSION";
    public static final String PARAM_JVM_VENDOR = "JVM_VENDOR";
    public static final String PARAM_JVM_ARCHITECTURE = "JVM_ARCHITECTURE";
}
