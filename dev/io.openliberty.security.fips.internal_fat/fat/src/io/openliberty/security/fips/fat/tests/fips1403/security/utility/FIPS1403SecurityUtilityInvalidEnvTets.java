package io.openliberty.security.fips.fat.tests.fips1403.security.utility;

import com.ibm.websphere.simplicity.ProgramOutput;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static io.openliberty.security.fips.fat.tests.fips1403.security.utility.FIPS1403SecurityUtilityTests.SEC_CONF_FIPS_COMMAND;
import static io.openliberty.security.fips.fat.tests.fips1403.security.utility.FIPS1403SecurityUtilityTests.runSecurityUtilityCommand;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_IBMI, SkipIfSysProp.OS_ISERIES})
public class FIPS1403SecurityUtilityInvalidEnvTets {

    private static final String SERVER_NAME = "FIPSServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws IOException {
        JavaInfo ji = JavaInfo.forServer(server);
        assumeThat(FIPSTestUtils.validFIPS140_3Environment(ji), is(false));
    }

    @Test
    public void invalidFIPSEnvironmentCheck() throws Exception {
        ProgramOutput po = runSecurityUtilityCommand( new String[] {SEC_CONF_FIPS_COMMAND});
        // cxommand should fail for non-FIPS envs
        assertEquals("securityUtility configureFIPS did not result in expected return code.",1, po.getReturnCode());
        // the error test comes out on Stdout, not stderr
        String result = po.getStdout();
        assertTrue(!result.isEmpty());
        // Semeru version strings are most language agnostic part of the invalid env check
        assertTrue(result.contains("11.0.29.0, 17.0.17.0, 21.0.9.0, 25.0.1.0"));

    }
}
