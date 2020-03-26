/**
 *
 */
package com.ibm.ws.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jakarta.transformer.JakartaTransformer;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;

/**
 *
 */
public class JakartaEEAction extends FeatureReplacementAction {

    private static final Class<?> c = JakartaEEAction.class;

    public static final String ID = "EE9_FEATURES";

    static final String[] EE9_FEATURES_ARRAY = { "jakartaee-9.0", "webProfile-9.0", "jakartaeeClient-9.0", "servlet-5.0", "jdbc-4.2", "javaMail-2.0", "cdi-3.0", "jpa-3.0",
                                                 "beanValidation-3.0", "jaxrs-3.0", "jaxrsClient-3.0", "jsf-3.0", "appSecurity-3.0", "jsonp-2.0", "jsonb-2.0",
                                                 "componenttest-2.0" };
    public static final Set<String> EE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));

    public JakartaEEAction() {
        super(removeFeatures(), EE9_FEATURE_SET);
        withMinJavaLevel(8);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE9 compatibility";
    }

    private static Set<String> removeFeatures() {
        Set<String> removeFeatures = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures.addAll(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures.add("componenttest-1.0");
        return removeFeatures;
    }

    @Override
    public void setup() throws Exception {
        Log.info(c, "setup", "@AGG invoking EE9 action...");
        try {
            invokeTransformer();
        } catch (Throwable t) {
            Log.info(c, "setup", "@AGG error invoking xformer...");
            Log.error(c, "setup", t);
        }
        super.setup();
    }

    private void invokeTransformer() throws Exception {
        Log.info(c, "@AGG", "Path is: " + Paths.get("publish", "servers").toAbsolutePath());
        Files.walk(Paths.get("publish", "servers"))
                        .forEach(this::transformApp);
        Log.info(c, "@AGG", "Done with xformer");
    }

    private void transformApp(Path appPath) {
        if (!appPath.getFileName().toString().endsWith(".war"))
            return;
        Log.info(c, "@AGG", "Transform app: " + appPath);

        // Capture stdout/stderr streams
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        Path outputPath = appPath.resolveSibling(appPath.getFileName() + ".jakarta");
        Path backupPath = appPath.resolveSibling(appPath.getFileName() + ".backup");
        try {
            String[] args = new String[4];
            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output
            args[2] = "-o";
            args[3] = "-v";
            JakartaTransformer.main(args);
            if (outputPath.toFile().exists()) {
                Files.move(appPath, backupPath);
                Files.move(outputPath, appPath);
            } else {
                throw new RuntimeException("Jakarta transformer failed for: " + appPath);
            }
        } catch (Exception e) {
            Log.info(c, "@AGG", "Unable to transform app at path: " + appPath);
            Log.error(c, "@AGG", e);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            Log.info(c, "@AGG", baos.toString());
        }
    }

}
