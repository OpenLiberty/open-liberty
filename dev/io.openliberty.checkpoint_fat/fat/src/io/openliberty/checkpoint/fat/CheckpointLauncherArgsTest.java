/**
 *
 */
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.spi.CheckpointPhase.AFTER_APP_START;
import static io.openliberty.checkpoint.spi.CheckpointPhase.BEFORE_APP_START;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;

/**
 * Test various launch arguments. This will validate argument processing of the bin/server script
 *
 */

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointLauncherArgsTest {

    @Rule
    public TestName testName = new TestName();

    @After
    public void cleanup() throws Exception {
        server.stopServer();
    }

    @Server("checkpointLauncherArgs")
    public static LibertyServer server;

    @Test
    public void testValidCannonicalAAS() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(AFTER_APP_START, false, null).setPhaseArgument("afterAppStart");
        server.setCheckpoint(cpi);
        server.startServer();
        assertThat("Missing expected checkpoint requested message",
                   server.findStringsInLogs("CWWKC0451I: A server checkpoint \"afterAppStart\" was requested"), is(not(empty())));
    }

    @Test
    public void testValidCannonicalBAS() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(BEFORE_APP_START, false, null).setPhaseArgument("beforeAppStart");
        server.setCheckpoint(cpi);
        server.startServer();
        assertThat("Missing checkpoint requested message",
                   server.findStringsInLogs("CWWKC0451I: A server checkpoint \"beforeAppStart\" was requested"), is(not(empty())));
    }

    @Test
    public void testValidMixedCaseBAS() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(BEFORE_APP_START, false, null).setPhaseArgument("bEfOrEAPPSTART");
        server.setCheckpoint(cpi);
        server.startServer();
        assertThat("Missing checkpoint requested message",
                   server.findStringsInLogs("CWWKC0451I: A server checkpoint \"beforeAppStart\" was requested"), is(not(empty())));
    }

    @Test
    public void testValidBASInternalName() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(BEFORE_APP_START, false, null).setPhaseArgument("BEFORE_APP_START");
        server.setCheckpoint(cpi);
        server.startServer();
        assertThat("Missing checkpoint requested message",
                   server.findStringsInLogs("CWWKC0451I: A server checkpoint \"beforeAppStart\" was requested"), is(not(empty())));
    }

    @Test
    public void testValidMixedCaseAASInternalName() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(AFTER_APP_START, false, null).setPhaseArgument("AFTER_App_start");
        server.setCheckpoint(cpi);
        server.startServer();
        assertThat("Missing expected checkpoint requested message",
                   server.findStringsInLogs("CWWKC0451I: A server checkpoint \"afterAppStart\" was requested"), is(not(empty())));
    }

    //INVALID PHASE ARGS
    // Look for console log failure:
    //  CWWKE0954E: The specified (PHASE) checkpoint phase is empty or unknown.

    @Test
    public void testInvalidInactive() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(AFTER_APP_START, false, true, false, null).setPhaseArgument("INACTIVE");
        server.setCheckpoint(cpi);
        //get message that eventually ends up in console log. For some reason it's not there yet at this stage of execution.
        String msg = server.startServer().getStdout();
        Pattern pattern = Pattern.compile(".*CWWKE0954E:.*\\(inactive\\).*", Pattern.DOTALL);
        assertTrue("Missing invalid phase message", pattern.matcher(msg).matches());
    }

    @Test
    public void testInvalid() throws Exception {
        CheckpointInfo cpi = new CheckpointInfo(AFTER_APP_START, false, true, false, null).setPhaseArgument("libbybot");
        server.setCheckpoint(cpi);
        //get message that eventually ends up in console log. For some reason it's not there yet at this stage of execution.
        String msg = server.startServer().getStdout();
        Pattern pattern = Pattern.compile(".*CWWKE0954E:.*\\(libbybot\\).*", Pattern.DOTALL);
        assertTrue("Missing invalid phase message", pattern.matcher(msg).matches());
    }

}
