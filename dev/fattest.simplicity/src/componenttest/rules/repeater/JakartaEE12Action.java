/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * <li>Update all server.xml configs under the autoFVT/publish/ folder to use EE 12 features</li>
 * </ol>
 */
public class JakartaEE12Action extends JakartaEEAction {
    public static final String ID = EE12_ACTION_ID;

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
        DEFAULT_TRANSFORMATION_RULES.put("-tv", TRANSFORMER_RULES_ROOT + "jakarta-versions-ee11.properties"); // Package version updates
        DEFAULT_TRANSFORMATION_RULES.put("-tb", TRANSFORMER_RULES_ROOT + "jakarta-bundles.properties"); // bundle identity updates
        DEFAULT_TRANSFORMATION_RULES.put("-td", TRANSFORMER_RULES_ROOT + "jakarta-direct.properties"); // exact java string constant updates
        DEFAULT_TRANSFORMATION_RULES.put("-tf", TRANSFORMER_RULES_ROOT + "jakarta-text.properties"); // text updates
    }

    // FAT tests use a mix of enabled features and not yet enabled
    // features, which is necessary for the FATs to run.
    static final String[] EE12_FEATURES_ARRAY = {
                                                  "appClientSupport-2.0",
                                                  "jakartaee-12.0",
                                                  "webProfile-12.0",
                                                  "jakartaeeClient-12.0",
                                                  "componenttest-2.0", // replaces "componenttest-1.0"
                                                  "txtest-2.0",
                                                  "appAuthentication-3.1",
                                                  "appAuthorization-3.0",
                                                  "appSecurity-7.0",
                                                  "batch-2.2",
                                                  "validation-4.0",
                                                  "cdi-5.0",
                                                  "concurrent-3.2",
                                                  "connectors-2.2",
                                                  "data-1.1",
                                                  "dataContainer-1.1",
                                                  "expressionLanguage-6.1",
                                                  "enterpriseBeans-4.0",
                                                  "enterpriseBeansHome-4.0",
                                                  "enterpriseBeansLite-4.0",
                                                  "enterpriseBeansPersistentTimer-4.0",
                                                  "enterpriseBeansRemote-4.0",
                                                  "enterpriseBeansTest-2.0",
                                                  "mail-2.2",
                                                  "persistence-4.0",
                                                  "persistenceContainer-4.0",
                                                  "jsonp-2.2",
                                                  "jsonb-3.1",
                                                  "jsonpContainer-2.2",
                                                  "jsonbContainer-3.1",
                                                  "faces-5.0",
                                                  "facesContainer-5.0",
                                                  "pages-4.1",
                                                  "mdb-4.0",
                                                  "messaging-3.1",
                                                  "messagingClient-3.0",
                                                  "messagingServer-3.0",
                                                  "messagingSecurity-3.0",
                                                  "nosql-1.0",
                                                  "restfulWS-5.0",
                                                  "restfulWSClient-5.0",
                                                  "servlet-6.2",
                                                  "websocket-2.3",
                                                  "xmlBinding-4.0",
                                                  "xmlWS-4.0",
                                                  "xmlWSClient-4.0"
    };

    public static final Set<String> EE12_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE12_FEATURES_ARRAY)));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // This constructor is purposely not public to force people to use the JakartaEEAction class and                 //
    // the FeatureReplacementAction.EE12_FEATURES() method instead of referencing this class directly                //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected JakartaEE12Action() {
        // Remove the EE7, EE8, EE9, EE10 and EE11 features; replace them with the EE12 features
        super(EE12_FEATURE_SET);
        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        removeFeatures(JakartaEE10Action.EE10_FEATURE_SET);
        removeFeatures(JakartaEE11Action.EE11_FEATURE_SET);
        forceAddFeatures(false);
        withMinJavaLevel(SEVersion.JAVA21);
        withID(EE12_ACTION_ID);
    }

    @Override
    public String toString() {
        return "JakartaEE12 FAT repeat action (" + getID() + ")";
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
    public JakartaEE12Action withLocalPackageTransformAppend(String fileName) {
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
    public JakartaEE12Action withLocalSelectionTransformAppend(String fileName) {
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
    public JakartaEE12Action withLocalVersionTransformAppend(String fileName) {
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
    public JakartaEE12Action withLocalBundleTransformAppend(String fileName) {
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
    public JakartaEE12Action withLocalStringTransformAppend(String fileName) {
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
    public JakartaEE12Action withLocalXMLTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tf", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * The widen option in the transformer enables the transformer to handle things like jars
     * inside of other jars or zips inside of other zips. These are not the usual setup of
     * of bundles and applications, so it is only enabled by an argument to the transformer.
     */
    @Override
    public JakartaEE12Action withWiden() {
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
