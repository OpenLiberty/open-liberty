/**
 *
 */
package com.ibm.ws.jdbc.fat.db2;

import java.time.Duration;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

public class DB2KerberosContainer extends Db2Container {

    public DB2KerberosContainer(Network network) {
        super("aguibert/db2-kerberos");
        withNetwork(network);
    }

    @Override
    protected void configure() {
        acceptLicense();
        withExposedPorts(50000);
        withEnv("KRB5_REALM", JDBCKerberosTest.KRB5_REALM);
        withEnv("KRB5_KDC", JDBCKerberosTest.KRB5_KDC);
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*SETUP SCRIPT COMPLETE.*$")
                        .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25)));
        withLogConsumer(DB2KerberosContainer::log);
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "[DB2]", msg);
    }

    @Override
    public String getUsername() {
        return "db2inst1";
    }

    @Override
    public String getPassword() {
        return "password";
    }

    @Override
    public String getDatabaseName() {
        return "testdb";
    }

    @Override
    public Db2Container withUsername(String username) {
        throw new UnsupportedOperationException("Username is hardcoded in container");
    }

    @Override
    public Db2Container withPassword(String password) {
        throw new UnsupportedOperationException("Password is hardcoded in container");
    }

    @Override
    public Db2Container withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("DB name is hardcoded in container");
    }

}
