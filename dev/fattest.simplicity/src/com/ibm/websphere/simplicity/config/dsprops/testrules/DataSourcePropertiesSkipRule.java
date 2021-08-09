/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.dsprops.testrules;

import java.util.Arrays;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.log.Log;

/**
 * This class can be used as a @Rule to process a @SkipIfDataSourceProperties annotation
 * specified on one or more @Test.
 * <p>
 * Before this rule is used, the <code>setDataSource()</code>
 * method must be called on this class to indicate which <code>DataSource</code>
 * should be examined for a nested type of <code>DataSourceProperties</code>.
 * If the <code>DataSource</code> is not set on this rule, then a null pointer exception will
 * occur when the first test that is annotated with @SkipIfDataSourceProperties is executed.
 * <p>
 * The @Test will not be run if the <code>DataSourceProperties</code> type nested under the
 * <code>DataSource</code> matches one of the <code>DataSourceProperties</code> specified
 * in the @SkipIfDataSourceProperties annotation.
 * <p>
 * For example this code would specify that the test should not be run with "DB2 with JCC" or
 * "Derby embedded".
 *
 * <pre>
 * <code>
 * import static com.ibm.websphere.simplicity.config.DataSourceProperties.*;
 * ...
 * private static DataSourcePropertiesSkipRule skipRule = new DataSourcePropertiesSkipRule();
 *
 * {@literal @Rule} public TestRule dataSourcePropertiesSkipRule = skipRule;
 *
 * {@literal @BeforeClass} public static void setUp() throws Exception {
 * // Get DataSource from server.xml. This DataSource must have
 * // a nested DataSourceProperties
 * skipRule.setDataSource(testDataSource);
 * }
 *
 * {@literal @SkipIfDataSourceProperties}({DB2_JCC, DERBY_EMBEDDED}) {@literal @Test} public void skipTestIf_DB2JCC_DerbyEmbedded() throws InterruptedException {
 * // test code
 * }
 * </code>
 * </pre>
 *
 */
public class DataSourcePropertiesSkipRule implements TestRule {

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Statement apply(final Statement statement, final Description description) {
        SkipIfDataSourceProperties annotation = description.getAnnotation(SkipIfDataSourceProperties.class);
        if (annotation == null) {
            return statement;
        }

        List<String> allowedDataSets = Arrays.asList(annotation.value());
        boolean skipTest = false;
        if (dataSource == null) {
            if (allowedDataSets.contains(DataSourcePropertiesOnlyRule.getConfigNameFromSysProp()))
                skipTest = true;
        } else {
            for (DataSourceProperties p : dataSource.getDataSourceProperties())
                if (allowedDataSets.contains(p.getElementName()))
                    skipTest = true;
        }

        if (skipTest)
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Log.info(description.getTestClass(), description.getMethodName(), "Test method is skipped due to DataSourcePropertiesSkipRule");
                }
            };
        else
            return statement;
    }
}