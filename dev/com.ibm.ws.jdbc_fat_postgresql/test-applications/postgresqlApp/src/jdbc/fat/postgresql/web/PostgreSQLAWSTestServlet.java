/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.postgresql.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PostgreSQLAWSTestServlet")
public class PostgreSQLAWSTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/common-ds")
    private DataSource common_ds;

    @Resource(lookup = "jdbc/common-ds-sm")
    private DataSource common_sm;

    @Resource(lookup = "jdbc/common-ds-iam")
    private DataSource common_iam;

    @Resource(lookup = "jdbc/driver-ds")
    private DataSource driver_ds;

    // Verify wrapped DataSource class can be used to access PostgreSQL database
    @Test
    public void testCommonDataSource() throws Exception {
        //assert configuration
        assertNotNull(common_ds);
        assertTrue(common_ds.isWrapperFor(DataSource.class));

        //assert connection can be used
        try (Connection con = common_ds.getConnection(); Statement stmt = con.createStatement()) {
            //assert that the connection that is created used the basic URL
            assertTrue(con.getMetaData().getURL().contains("jdbc:postgresql:"));

            stmt.executeQuery("SELECT 1");
        }
    }

    // Verify wrapped DataSource using AWS Secrets Manager fails to contact the service (TODO)
    @Test
    @ExpectedFFDC({ "javax.resource.spi.ResourceAllocationException",
                    "software.amazon.awssdk.core.exception.SdkClientException" })
    public void testCommonDataSourceSM() {
        try (Connection con = common_sm.getConnection()) {
            fail("Should not have been able to create connection");
        } catch (SQLException e) {
            assertTrue(e.getMessage().startsWith("Unable to load credentials from any of the providers in the chain AwsCredentialsProviderChain"));
        }
    }

    // Verify wrapped DataSource using AWS Identity and Access Management fails to contact the service (TODO)
    @Test
    @ExpectedFFDC({ "javax.resource.spi.ResourceAllocationException",
                    "software.amazon.awssdk.core.exception.SdkClientException" })
    public void testCommonDataSourceIAM() {
        try (Connection con = common_iam.getConnection()) {
            fail("Should not have been able to create connection");
        } catch (SQLException e) {
            assertTrue(e.getMessage().startsWith("Unable to load credentials from any of the providers in the chain AwsCredentialsProviderChain"));
        }
    }

    // Verify wrapped Driver class can be used to access PostgreSQL database
    @Test
    public void testDriver() throws Exception {
        //assert driver cannot be unwrapped to logical driver
        assertNotNull(driver_ds);
        assertFalse(driver_ds.isWrapperFor(Driver.class));

        try (Connection con = driver_ds.getConnection(); Statement stmt = con.createStatement()) {
            //assert that the connection that is created used the basic URL
            assertTrue(con.getMetaData().getURL().contains("jdbc:postgresql:"));

            //assert that the driver name was correctly configured
            assertEquals("PostgreSQL JDBC Driver", con.getMetaData().getDriverName());

            //assert connection can be used
            stmt.executeQuery("SELECT 1");
        }
    }
}
