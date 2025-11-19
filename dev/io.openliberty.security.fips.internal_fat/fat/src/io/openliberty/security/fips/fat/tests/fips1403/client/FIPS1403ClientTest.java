package io.openliberty.security.fips.fat.tests.fips1403.client;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.utils.PrivHelper;
import io.openliberty.security.fips.fat.FIPSTestUtils;
import io.openliberty.security.fips.fat.tests.fips1403.server.FIPS1403ServerTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;

import static io.openliberty.security.fips.fat.FIPSTestUtils.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
@SkipIfSysProp({SkipIfSysProp.OS_ZOS, SkipIfSysProp.OS_IBMI, SkipIfSysProp.OS_ISERIES})
public class FIPS1403ClientTest {

    public static final String CLIENT_NAME = "FIPSClient";
    public static String expectedProvider="InvalidProvider";
    public static EnterpriseArchive earHAC;
    public static LibertyClient client;
    public static boolean GLOBAL_CLIENT_FIPS = false;
    public static String baseLibertySecurityProfileLocation;

    @BeforeClass
    public static void setup() throws Exception {
        client = LibertyClientFactory.getLibertyClient(CLIENT_NAME);
        JavaInfo ji = JavaInfo.forClient(client);
        assumeThat(FIPSTestUtils.validFIPS140_3Environment(ji), is(true));
        //HelloAppClient ear
        String APP_NAME = "HelloAppClient";
        JavaArchive jar = ShrinkHelper.buildJavaArchive(APP_NAME + ".jar", "com.ibm.ws.clientcontainer.HelloAppClient.test");
        earHAC = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                .addAsModule(jar)
                .addAsManifestResource(new File("test-applications/" + APP_NAME + ".ear/resources/META-INF/application.xml"));

        ShrinkHelper.exportAppToClient(client, earHAC);
        if (Boolean.parseBoolean(PrivHelper.getProperty("global.client.fips_140-3", "false"))) {
            Log.info(FIPS1403ServerTest.class,"setup","global.fips_140-3 is set, letting LibertyServer configure FIPS");
            GLOBAL_CLIENT_FIPS = true;
        }
        if (ji.majorVersion() > 8) {
            expectedProvider = "OpenJCEPlusFIPS";
            // temporarily enable Beta for Semeru
            // client.addEnvVar("JVM_ARGS","-Dcom.ibm.ws.beta.edition=true");
        } else {
            expectedProvider = "IBMJCEPlusFIPS";
        }

        baseLibertySecurityProfileLocation = client.getInstallRoot() + "/lib/security/fips140_3/FIPS140-3-Liberty.properties";
    }

    @Test
    public void clientJFIPS140_3JVMArgsTest() throws Exception {
        if (!GLOBAL_CLIENT_FIPS) {
            Log.info(FIPS1403ClientTest.class,"setup","Setting FIPS140-3 JVM Options");
            HashMap<String, String> opts = new HashMap<>();
            //Semeru >=11
            if (expectedProvider.equals("OpenJCEPlusFIPS")) {
                client.copyFileToLibertyClientRoot("publish/resources", "resources", STANDALONE_FIPS_PROFILE_FILENAME);
                opts.put("-Dsemeru.fips", "true");
                opts.put("-Dsemeru.customprofile", "OpenJCEPlusFIPS.FIPS140-3-Custom");
                opts.put("-Djava.security.properties", client.getClientRoot() + "/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
                // IBM SDK 8
            } else {
                opts.put("-Xenablefips140-3", null);
                opts.put("-Dcom.ibm.jsse2.usefipsprovider", "true");
                opts.put("-Dcom.ibm.jsse2.usefipsProviderName", "IBMJCEPlusFIPS");
            }

            client.setJvmOptions(opts);
        } else {
            Log.info(FIPS1403ClientTest.class,"setup","global.fips_140-3 is set, letting LibertyServer configure FIPS");
        }
        client.startClient();
        checkClientLogForFipsEnablementMessage(client, expectedProvider);
    }
    
    @Test
    public void clientFIPS140_3EnvTest() throws Exception {
        assumeThat(GLOBAL_CLIENT_FIPS, is(false));
        if(!GLOBAL_CLIENT_FIPS) {
            client.copyFileToLibertyClientRoot("publish/resources", "resources" , STANDALONE_FIPS_PROFILE_FILENAME);
            client.addEnvVar(ENABLE_FIPS140_3_ENV_VAR, client.getClientRoot()+"/resources/" + STANDALONE_FIPS_PROFILE_FILENAME);
        }
        client.startClient();
        checkClientLogForFipsEnablementMessage(client, expectedProvider);
    }

    @After
    public void teardown() throws Exception {
        client.setJvmOptions(new HashMap<>());
    }

}
