/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
 * <li>Update all server.xml configs under the autoFVT/publish/ folder to use EE 11 features</li>
 * </ol>
 */
public class JakartaEE11Action extends JakartaEEAction {
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

    static final String[] EE11_FEATURES_ARRAY = {
                                                  "appClientSupport-2.0",
                                                  "jakartaee-11.0",
                                                  "webProfile-11.0",
                                                  "jakartaeeClient-11.0",
                                                  "componenttest-2.0", // replaces "componenttest-1.0"
                                                  "txtest-2.0",
                                                  "appAuthentication-3.1",
                                                  "appAuthorization-3.0",
                                                  "appSecurity-6.0",
                                                  "batch-2.1",
                                                  "beanValidation-3.1",
                                                  "cdi-4.1",
                                                  "concurrent-3.1",
                                                  "connectors-2.1",
                                                  "data-1.0",
                                                  "expressionLanguage-6.0",
                                                  "enterpriseBeans-4.0",
                                                  "enterpriseBeansHome-4.0",
                                                  "enterpriseBeansLite-4.0",
                                                  "enterpriseBeansPersistentTimer-4.0",
                                                  "enterpriseBeansRemote-4.0",
                                                  "enterpriseBeansTest-2.0",
                                                  "mail-2.1",
                                                  "persistence-3.2",
                                                  "persistenceContainer-3.2",
                                                  "jsonp-2.1",
                                                  "jsonb-3.0",
                                                  "jsonpContainer-2.1",
                                                  "jsonbContainer-3.0",
                                                  "faces-4.1",
                                                  "facesContainer-4.1",
                                                  "pages-4.0",
                                                  "mdb-4.0",
                                                  "messaging-3.1",
                                                  "messagingClient-3.0",
                                                  "messagingServer-3.0",
                                                  "messagingSecurity-3.0",
                                                  "nosql-1.0",
                                                  "restfulWS-4.0",
                                                  "restfulWSClient-4.0",
                                                  "servlet-6.1",
                                                  "websocket-2.2",
                                                  "xmlBinding-4.0",
                                                  "xmlWS-4.0"
    };

    private static final String[] EE11_ONLY_FEATURES_ARRAY_LOWERCASE = {
                                                                         "jakartaee-11.0",
                                                                         "webprofile-11.0",
                                                                         "jakartaeeclient-11.0",
                                                                         "appauthentication-3.1",
                                                                         "appauthorization-3.0",
                                                                         "appsecurity-6.0",
                                                                         "beanvalidation-3.1",
                                                                         "cdi-4.1",
                                                                         "concurrent-3.1",
                                                                         "data-1.0",
                                                                         "expressionlanguage-6.0",
                                                                         "persistence-3.2",
                                                                         "persistencecontainer-3.2",
                                                                         "faces-4.1",
                                                                         "facescontainer-4.1",
                                                                         "pages-4.0",
                                                                         "restfulws-4.0",
                                                                         "restfulwsclient-4.0",
                                                                         "servlet-6.1",
                                                                         "websocket-2.2"
    };

    public static final Set<String> EE11_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_FEATURES_ARRAY)));
    public static final Set<String> EE11_ONLY_FEATURE_SET_LOWERCASE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_ONLY_FEATURES_ARRAY_LOWERCASE)));

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // This constructor is purposely not public to force people to use the JakartaEEAction class and                 //
    // the FeatureReplacementAction.EE11_FEATURES() method instead of referencing this class directly                //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected JakartaEE11Action() {
        // Remove the EE7, EE8, EE9 and EE10 features; replace them with the EE11 features
        super(EE11_FEATURE_SET);
        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE7FeatureReplacementAction.EE7_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        removeFeatures(JakartaEE10Action.EE10_FEATURE_SET);
        forceAddFeatures(false);
        withMinJavaLevel(SEVersion.JAVA17);
        withID(EE11_ACTION_ID);
    }

    @Override
    public String toString() {
        return "JakartaEE11 FAT repeat action";
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
    public JakartaEE11Action withLocalPackageTransformAppend(String fileName) {
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
    public JakartaEE11Action withLocalSelectionTransformAppend(String fileName) {
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
    public JakartaEE11Action withLocalVersionTransformAppend(String fileName) {
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
    public JakartaEE11Action withLocalBundleTransformAppend(String fileName) {
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
    public JakartaEE11Action withLocalStringTransformAppend(String fileName) {
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
    public JakartaEE11Action withLocalXMLTransformAppend(String fileName) {
        TRANSFORMATION_RULES_APPEND.put("-tf", TRANSFORMER_RULES_APPEND_ROOT + fileName);
        return this;
    }

    /**
     * The widen option in the transformer enables the transformer to handle things like jars
     * inside of other jars or zips inside of other zips. These are not the usual setup of
     * of bundles and applications, so it is only enabled by an argument to the transformer.
     */
    @Override
    public JakartaEE11Action withWiden() {
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
