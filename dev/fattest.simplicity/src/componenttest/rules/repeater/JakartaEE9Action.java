/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

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

/**
 * Test repeat action that will do 2 things:
 * <ol>
 * <li>Invoke the Jakarta transformer on all war/ear files under the autoFVT/publish/servers/ folder</li>
 * <li>Update all server.xml configs under the autoFVT/publish/servers/ folder to use EE 9 features</li>
 * </ol>
 */
public class JakartaEE9Action extends FeatureReplacementAction {

    private static final Class<?> c = JakartaEE9Action.class;

    public static final String ID = "EE9_FEATURES";

    static final String[] EE9_FEATURES_ARRAY = { "jakartaee-9.0", "webProfile-9.0", "jakartaeeClient-9.0", "servlet-5.0", "jdbc-4.2", "javaMail-2.0", "cdi-3.0", "jpa-3.0",
                                                 "beanValidation-3.0", "jaxrs-3.0", "jaxrsClient-3.0", "jsf-3.0", "appSecurity-3.0", "jsonp-2.0", "jsonb-2.0",
                                                 "componenttest-2.0" };
    public static final Set<String> EE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));

    public JakartaEE9Action() {
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
        // Transform apps
        Files.walk(Paths.get("publish", "servers"))
                        .forEach(JakartaEE9Action::transformApp);
        // Transform server.xml's
        super.setup();
    }

    /**
     * Invokes the Jakarta transformer on a given application (ear or war).
     * After completion, the transformed application will be available at the $appPath,
     * and the original application will be available at $appPath.backup
     * 
     * @param appPath The application path to be transformed to Jakarta
     */
    public static void transformApp(Path appPath) {
        final String m = "transformApp";
        if (!appPath.getFileName().toString().endsWith(".war") &&
            !appPath.getFileName().toString().endsWith(".ear"))
            return;
        Log.info(c, m, "Transforming app: " + appPath);

        // Capture stdout/stderr streams
        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        try {
            Class.forName("com.ibm.ws.jakarta.transformer.JakartaTransformer");
        } catch (Throwable e) {
            throw new RuntimeException("Unable to load the JakartaTransformer class. " +
                                       "Did you remember to include 'addRequiredLibraries.dependsOn addJakartaTransformer' in the FATs build.gradle file?");
        }

        Path outputPath = appPath.resolveSibling(appPath.getFileName() + ".jakarta");
        Path backupPath = appPath.resolveSibling(appPath.getFileName() + ".backup");
        try {
            // Invoke the jakarta transformer
            String[] args = new String[3];
            args[0] = appPath.toAbsolutePath().toString(); // input
            args[1] = outputPath.toAbsolutePath().toString(); // output
            args[2] = "-v"; // verbose
            JakartaTransformer.main(args);

            // Swap out the transformed file with the original
            if (outputPath.toFile().exists()) {
                Files.move(appPath, backupPath);
                Files.move(outputPath, appPath);
            } else {
                throw new RuntimeException("Jakarta transformer failed for: " + appPath);
            }
        } catch (Exception e) {
            Log.info(c, m, "Unable to transform app at path: " + appPath);
            Log.error(c, m, e);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            Log.info(c, m, baos.toString());
        }
    }

}
