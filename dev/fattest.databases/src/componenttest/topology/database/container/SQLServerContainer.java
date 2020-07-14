/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.LicenseAcceptance;


//TODO This class can be removed once we have updated past org.testcontainers:mssqlserver:1.14.3
// since this code went in: https://github.com/testcontainers/testcontainers-java/pull/2085
public class SQLServerContainer<SELF extends SQLServerContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "sqlserver";
    public static final String IMAGE = "mcr.microsoft.com/mssql/server";
    public static final String DEFAULT_TAG = "2019-CU2-ubuntu-16.04";

    public static final Integer MS_SQL_SERVER_PORT = 1433;
    public static final Integer MSSQL_DTC_TCP_PORT = 51000;
    public static final Integer MSSQL_RPC_PORT = 135;
    private String username = "SA";
    private String password = "A_Str0ng_Required_Password";

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    private static final Pattern[] PASSWORD_CATEGORY_VALIDATION_PATTERNS = new Pattern[]{
        Pattern.compile("[A-Z]+"),
        Pattern.compile("[a-z]+"),
        Pattern.compile("[0-9]+"),
        Pattern.compile("[^a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE)
    };

    public SQLServerContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public SQLServerContainer(final String dockerImageName) {
        super(dockerImageName);
        withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(MS_SQL_SERVER_PORT);
    }

    @Override
    protected void configure() {
        // If license was not accepted programmatically, check if it was accepted via resource file
        if (!getEnvMap().containsKey("ACCEPT_EULA")) {
            LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
            acceptLicense();
        }

        addExposedPort(MS_SQL_SERVER_PORT);
        addExposedPort(MSSQL_DTC_TCP_PORT);
        addExposedPort(MSSQL_RPC_PORT);
        
        addEnv("MSSQL_RPC_PORT", MSSQL_RPC_PORT.toString());
        addEnv("MSSQL_DTC_TCP_PORT", MSSQL_DTC_TCP_PORT.toString());
        addEnv("SA_PASSWORD", password);
    }

    /**
     * Accepts the license for the SQLServer container by setting the ACCEPT_EULA=Y
     * variable as described at <a href="https://hub.docker.com/_/microsoft-mssql-server">https://hub.docker.com/_/microsoft-mssql-server</a>
     */
    public SELF acceptLicense() {
        addEnv("ACCEPT_EULA", "Y");
        return self();
    }

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:sqlserver://" + getContainerIpAddress() + ":" + getMappedPort(MS_SQL_SERVER_PORT);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public SELF withPassword(final String password) {
        checkPasswordStrength(password);
        this.password = password;
        return self();
    }

    private void checkPasswordStrength(String password) {

        if (password == null) {
            throw new IllegalArgumentException("Null password is not allowed");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password should be at least 8 characters long");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Password can be up to 128 characters long");
        }

        long satisfiedCategories = Stream.of(PASSWORD_CATEGORY_VALIDATION_PATTERNS)
            .filter(p -> p.matcher(password).find())
            .count();

        if (satisfiedCategories < 3) {
            throw new IllegalArgumentException(
                "Password must contain characters from three of the following four categories:\n" +
                    " - Latin uppercase letters (A through Z)\n" +
                    " - Latin lowercase letters (a through z)\n" +
                    " - Base 10 digits (0 through 9)\n" +
                    " - Non-alphanumeric characters such as: exclamation point (!), dollar sign ($), number sign (#), " +
                    "or percent (%)."
            );
        }
    }
}