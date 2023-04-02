/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat.container;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;
import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.Assert;

/**
 * Example test class showing how to setup a regular predefined
 * TestContainer for use to test against.
 */
@RunWith(FATRunner.class)
public class ContainersTestBase extends FATServletClient {

    // Prebuilt image containing openJ9 and criu binaries. Based on the Docker file checked in at
    //   publish/files/Dockerfile
    final static String CONTAINER_FAT_BASE_DOCKER_IMAGE = "sambratton/liberty-checkpoint-fat:220401";

    static Path wlpDir;
    @Server("checkpoint.fat.container")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        wlpDir = FileSystems.getDefault().getPath(server.getInstallRoot()).toAbsolutePath();
        ShrinkHelper.defaultDropinApp(server, "servletA", "com.ibm.ws.testapp");
    }

    public static String serverName = "checkpoint.fat.container";

    @AfterClass
    public static void tearDown() throws Exception {
    }

    @Test
    public void checkpointRestoreWithApp() throws UnsupportedOperationException, IOException, InterruptedException {

        Consumer<DockerfileBuilder> dfbc = dfb -> dfb.from(ImageNameSubstitutor.instance()
                        .apply(DockerImageName.parse(CONTAINER_FAT_BASE_DOCKER_IMAGE))
                        .asCanonicalNameString())
                        .run("useradd -u 1001 -r -g 0 -s /usr/sbin/nologin default")
                        .run("apt-get update && "
                             + "apt-get install -y --no-install-recommends zip unzip openssl wget dumb-init vim && "
                             + "rm -rf /var/lib/apt/lists/* && " + "apt-get remove -y wget")
                        .withStatement(
                                       new MultiArgsStatement("COPY", "wlp", "/opt/ol/wlp"))
                        .withStatement(
                                       new MultiArgsStatement("COPY", "scripts/*", "/opt/ol/fat/scripts/"))
                        .user("0:0")
                        .run("chown", "-R", "1001:0", "/opt/ol/wlp")
                        .run("chmod", "-R", "+x", "/opt/ol/fat/scripts")
                        .run("chmod", "-R", "+x", "/opt/ol/wlp/bin")
                        .withStatement(new MultiArgsStatement("ENTRYPOINT", "/opt/ol/fat/scripts/launcher"))
                        .cmd("checkpoint", "checkpoint.fat.container", "applications")
                        .build();

        ImageFromDockerfile image = addFiles(new ImageFromDockerfile().withDockerfileFromBuilder(dfbc));

        GenericContainer<?> baseImageContainer = new GenericContainer<>(image)
                        .withLogConsumer(new SimpleLogConsumer(ContainersTestBase.class, "ContainersTest"))
                        .waitingFor(new LogMessageWaitStrategy()
                                        .withRegEx(".*Checkpoint action completed.*")
                                        .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 10)));
        GenericContainer checkpointContainer = null;
        try {

            baseImageContainer.setPrivilegedMode(true);
            //Run container. Launches server checkpoint in the container.
            baseImageContainer.start();

            ExecResult execResult;

            //Verify checkpoint messages and in-container server stopped
            execResult = baseImageContainer.execInContainer("/opt/ol/fat/scripts/execServer", "status", serverName);
            assertExecResult(execResult, 1, "\n.*Server .* is not running.*\n", null, "check server status");
            grepServerLog("Servlet started message not found.", baseImageContainer, "CWWKZ0001I:.*servletA started", serverName);
            grepServerLog("Checkpoint requested message not found.", baseImageContainer, "CWWKC0451I: A server checkpoint was requested.", serverName);

            //Create container with checkpoint
            String checkpointedImage = baseImageContainer.getDockerClient().commitCmd(baseImageContainer.getContainerId()).exec();
            checkpointContainer = new GenericContainer(checkpointedImage).withCommand("start checkpoint.fat.container")
                            .withImagePullPolicy((name) -> {
                                return false;
                            })
                            .waitingFor(new LogMessageWaitStrategy()
                                            .withRegEx(".*server start with checkpoint completed.*")
                                            .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 10)));;
            checkpointContainer.setPrivilegedMode(true);
            checkpointContainer.start();

            //restore in-container server and exercise apps
            grepServerLog("missing message: 'CWWKC0452I: The Liberty server process resumed operation from a checkpoint...'",
                          checkpointContainer, "CWWKC0452I:", serverName);
            execResult = checkpointContainer.execInContainer("/opt/ol/fat/scripts/execServer", "status", serverName);
            assertExecResult(execResult, 0, ".*Server .* is running with process ID.*", null, "launch server from checkpoint");

            execResult = checkpointContainer.execInContainer("curl", "-i", "http://localhost:9080/servletA/request");
            assertExecResult(execResult, null, ".*HTTP/1.1 200 OK.*Got ServletA.*", null, "Hit servlet url");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e1.printStackTrace(new PrintStream(baos));
            Assert.fail(baos.toString());
        } finally {
            if (baseImageContainer != null && baseImageContainer.isRunning()) {
                gatherLogs(baseImageContainer, "base-image", serverName);
                baseImageContainer.stop();
            }
            if (checkpointContainer != null && checkpointContainer.isRunning()) {
                gatherLogs(checkpointContainer, "checkpointed-server", serverName);
                baseImageContainer.stop();
            }
        }
    }

    //Files included in COPY commands from dockerfile must also be added here
    private ImageFromDockerfile addFiles(ImageFromDockerfile image) throws IOException {
        Files.walkFileTree(wlpDir, new AddFilesFromTree(wlpDir, image));
        Files.walkFileTree(FileSystems.getDefault().getPath("./lib/LibertyFATTestFiles/scripts"),
                           new AddFilesFromTree(FileSystems.getDefault().getPath("./lib/LibertyFATTestFiles/scripts"), image));
        return image;
    }

    public class AddFilesFromTree extends SimpleFileVisitor<Path> {

        public AddFilesFromTree(Path root, ImageFromDockerfile image) {
            this.image = image;
            this.root = root;
            treeBase = "/" + root.subpath(root.getNameCount() - 1, root.getNameCount()).toString() + "/";
        }

        private Path root;
        private String treeBase;
        private ImageFromDockerfile image;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            String path = treeBase + root.relativize(file).toString();
            image.withFileFromFile(treeBase + root.relativize(file).toString(),
                                   file.toFile());
            return FileVisitResult.CONTINUE;
        }
    }

    void gatherLogs(GenericContainer cont, String containerName, String serverName) throws UnsupportedOperationException, IOException, InterruptedException {
        String zipName = "/tmp/" + serverName + ".zip";
        String serverLogs = "/opt/ol/wlp/usr/servers/" + serverName;
        ExecResult execResult = cont.execInContainer("zip", "-r", zipName, serverLogs);
        assertExecResult(execResult, 0, null, null, "Unexpected error code zipping server logs.");
        cont.copyFileFromContainer(zipName, "./output/container." + containerName + '.' + serverName + ".zip");
    }

    public void grepServerLog(String message, GenericContainer cont, String regEx, String serverName) throws UnsupportedOperationException, IOException, InterruptedException {
        String serverLog = "/opt/ol/wlp/usr/servers/" + serverName + "/logs/messages.log";
        ExecResult execResult = cont.execInContainer("grep", regEx, serverLog);
        assertExecResult(execResult, 0, null, null, message);
    }

    /**
     * Fail test if Container.ExecResult does not match the provided (non-null) exitCode, stdout, or stderr
     *
     * @param result
     * @param expected              exitCode
     * @param expected_stdout_regEx Regex matching expected stdout
     * @param expected_stderr_regEx Regex matching expected stderr
     * @param command               Description of command passed to execInContainer.
     */
    public void assertExecResult(ExecResult result, Integer expected_exitCode, String expected_stdout_regEx, String expected_stderr_regEx, String operation) {
        Integer exitCode = result.getExitCode();
        String stdout = result.getStdout();
        String stderr = result.getStderr();

        System.out.println(result);
        if (expected_exitCode != null) {
            Assert.assertEquals("For operation '" + operation + "' expected exit code " + expected_exitCode + " got " + result,
                                expected_exitCode.intValue(), exitCode.intValue());
        }
        if (expected_stdout_regEx != null) {
            Matcher matcher = Pattern.compile(expected_stdout_regEx, Pattern.DOTALL).matcher(stdout);
            assertTrue("For operation '" + operation + "' expected stdout matching [" +
                       expected_stdout_regEx + "] . Got " + result,
                       matcher.matches());
        }
        if (expected_stderr_regEx != null) {
            Matcher matcher = Pattern.compile(expected_stderr_regEx, Pattern.DOTALL).matcher(stderr);
            assertTrue("For operation '" + operation + "' expected stderr matching [" +
                       expected_stderr_regEx + "]. Got  " + result,
                       matcher.matches());
        }
    }

}
