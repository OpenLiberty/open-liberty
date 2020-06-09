/**
 *
 */
package com.ibm.ws.jdbc.fat.db2;

import java.nio.file.Paths;
import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.MountableFile;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    private static final String IMAGE = "gcavalcante8808/krb5-server";
    // Use direct hash instead of label since labels can be updated
    private static final String DIGEST = "2adbcde9ba41227a4b10c01b0f0b97e6d972099dd26b4cd1541acf642242ecf5";

    public KerberosContainer(Network network) {
        super(IMAGE + "@sha256:" + DIGEST);
        withNetwork(network);
    }

    @Override
    protected void configure() {
        withCopyFileToContainer(MountableFile.forHostPath(Paths.get("lib", "LibertyFATTestFiles", "kdc-server", "docker-entrypoint.sh"), 777),
                                "/docker-entrypoint.sh");
        withExposedPorts(88, 464, 749);
        withNetworkAliases(JDBCKerberosTest.KRB5_KDC);
        withEnv("KRB5_REALM", JDBCKerberosTest.KRB5_REALM);
        withEnv("KRB5_KDC", "localhost");
        withEnv("KRB5_PASS", JDBCKerberosTest.KRB5_PASS);
        withLogConsumer(KerberosContainer::log);
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*Principal \"MSSQLSvc/sqlserver:1433@" + JDBCKerberosTest.KRB5_REALM + "\" created.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 15 : 300)));
        withReuse(true);
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(FATSuite.class, "[KRB5]", msg);
    }
}
