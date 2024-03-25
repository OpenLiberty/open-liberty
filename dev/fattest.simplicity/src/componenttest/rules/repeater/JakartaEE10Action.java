/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package componenttest.rules.repeater;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import componenttest.rules.repeater.RepeatActions.SEVersion;

/**
 * Test repeat action that will do 2 things:
 * <ol>
 * <li>Invoke the Jakarta transformer on all war/ear files under the autoFVT/publish/ folder</li>
 * <li>Update all server.xml configs under the autoFVT/publish/ folder to use EE 10 features</li>
 * </ol>
 */
public class JakartaEE10Action extends JakartaEEAction {
    public static final String ID = EE10_ACTION_ID;

    private static final Map<String, String> DEFAULT_TRANSFORMATION_RULES = new HashMap<>();
    private static final Map<String, String> TRANSFORMATION_RULES_APPEND = new HashMap<>();
    private static boolean WIDEN = false;

    static {
        // Fill the default transformation rules for the transformer
        // The rules are copied from 'open-liberty/dev/wlp-jakartaee-transform/rules' to
        // the user 'autoFVT-templates' folder.
        //
        //   jakarta-selections.properties
        //   jakarta-renames.properties
        //   jakarta-versions.properties
        //   jakarta-bundles.properties
        //   jakarta-direct.properties
        //   jakarta-text.properties
        //   (other xml properties files as referenced by 'jakarta-text.properties'
        DEFAULT_TRANSFORMATION_RULES.put("-tr", TRANSFORMER_RULES_ROOT + "jakarta-renames.properties"); // Package renames
        DEFAULT_TRANSFORMATION_RULES.put("-ts", TRANSFORMER_RULES_ROOT + "jakarta-selections.properties"); // File selections and omissions
        DEFAULT_TRANSFORMATION_RULES.put("-tv", TRANSFORMER_RULES_ROOT + "jakarta-versions-ee10.properties"); // Package version updates
        DEFAULT_TRANSFORMATION_RULES.put("-tb", TRANSFORMER_RULES_ROOT + "jakarta-bundles.properties"); // bundle identity updates
        DEFAULT_TRANSFORMATION_RULES.put("-td", TRANSFORMER_RULES_ROOT + "jakarta-direct.properties"); // exact java string constant updates
        DEFAULT_TRANSFORMATION_RULES.put("-tf", TRANSFORMER_RULES_ROOT + "jakarta-text.properties"); // text updates
    }

    static final String[] EE10_FEATURES_ARRAY = {
                                                  "appClientSupport-2.0",
                                                  "jakartaee-10.0",
                                                  "webProfile-10.0",
                                                  "jakartaeeClient-10.0",
                                                  "componenttest-2.0", // replaces "componenttest-1.0"
                                                  "txtest-2.0",
                                                  "appAuthentication-3.0",
                                                  "appAuthorization-2.1",
                                                  "appSecurity-5.0",
                                                  "batch-2.1",
                                                  "beanValidation-3.0",
                                                  "cdi-4.0",
                                                  "concurrent-3.0",
                                                  "connectors-2.1",
                                                  "data-1.0", // TODO temporarily enabled with EE10 for testing/experimentation. Later, switch this to EE11
                                                  "expressionLanguage-5.0",
                                                  "enterpriseBeans-4.0",
                                                  "enterpriseBeansHome-4.0",
                                                  "enterpriseBeansLite-4.0",
                                                  "enterpriseBeansPersistentTimer-4.0",
                                                  "enterpriseBeansRemote-4.0",
                                                  "enterpriseBeansTest-2.0",
                                                  "mail-2.1",
                                                  "persistence-3.1",
                                                  "persistenceContainer-3.1",
                                                  "jsonp-2.1",
                                                  "jsonb-3.0",
                                                  "jsonpContainer-2.1",
                                                  "jsonbContainer-3.0",
                                                  "faces-4.0",
                                                  "facesContainer-4.0",
                                                  "pages-3.1",
                                                  "managedBeans-2.0",
                                                  "mdb-4.0",
                                                  "messaging-3.1",
                                                  "messagingClient-3.0",
                                                  "messagingServer-3.0",
                                                  "messagingSecurity-3.0",
                                                  "nosql-1.0", // TODO temporarily enabled with EE10 for testing/experimentation. Later, switch this to EE11 or whichever release Jakarta NoSQL goes into
                                                  "restfulWS-3.1",
                                                  "restfulWSClient-3.1",
                                                  "servlet-6.0",
                                                  "websocket-2.1",
                                                  "xmlBinding-4.0",
                                                  "xmlWS-4.0"
    };

    public static final Set<String> EE10_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_FEATURES_ARRAY)));

    public JakartaEE10Action() {
        // Remove the EE7 and EE8 features; replace them with the EE9 features
        super(EE10_FEATURE_SET);
        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        removeFeatures(JakartaEE11Action.EE11_FEATURE_SET);
        forceAddFeatures(false);
        withMinJavaLevel(SEVersion.JAVA11);
        withID(ID);
    }

    @Override
    public String toString() {
        return "JakartaEE10 FAT repeat action";
    }

    //

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional package transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalPackageTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tr", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional selection transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalSelectionTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-ts", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional version transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalVersionTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tv", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional bundle transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalBundleTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tb", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional string transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalStringTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-td", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * Specifies which file in the rules directory of the FAT will be used for
     * adding additional xml transformations.
     *
     * @param fileName The file name in the publish/rules directory to use for appending
     *
     */
    @Override
    public JakartaEE10Action withLocalXMLTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tf", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * The widen option in the transformer enables the transformer to handle things like jars
     * inside of other jars or zips inside of other zips. These are not the usual setup of
     * of bundles and applications, so it is only enabled by an argument to the transformer.
     */
    @Override
    public JakartaEE10Action withWiden() {
        WIDEN = true;
        return this;
    }

    /**
     * Invoke the Jakarta transformer on an application with added transformation rules.
     *
     * @param  appPath                   The application path to be transformed to Jakarta
     * @param  newAppPath                The application path of the transformed file (or <code>null<code>)
     * @param  transformationRulesAppend The map with the additional transformation rules to add
     * @return
     */
    @Override
    void transformApplication(Path appPath, Path newAppPath, Map<String, String> transformationRulesAppend) {
        staticTransformApplication(appPath, newAppPath, transformationRulesAppend);
    }

    /**
     * Invoke the Jakarta transformer on an application with added transformation rules.
     *
     * @param appPath                   The application path to be transformed to Jakarta
     * @param newAppPath                The application path of the transformed file (or <code>null<code>)
     * @param transformationRulesAppend The map with the additional transformation rules to add
     */
    static void staticTransformApplication(Path appPath, Path newAppPath, Map<String, String> transformationRulesAppend) {
        if (transformationRulesAppend != null) {
            TRANSFORMATION_RULES_APPEND.putAll(transformationRulesAppend);
        }
        transformApp(appPath, newAppPath, DEFAULT_TRANSFORMATION_RULES, TRANSFORMATION_RULES_APPEND, WIDEN);
    }
}
