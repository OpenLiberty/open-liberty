/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package com.ibm.ws.jca.fat.regr;

/**
 * Class <code>RarTests</code> provides the string constants and
 * instance members necessary for testing J2C merge action and validation
 * behaviors for JCA 1.6 RAR annotation meta-data.
 */
public interface RarTests {

    public final static String

                    // The inventory of r80-jca16 test RARs, by display-name.

                    // Conventions for test RAR artifacts:
                    // <src-path>           = .../adapter.test/src/com/ibm/<ra-name>/...
                    // <bld-tgt-name>       = <ra-name>_<fat-bucket>_<suite>_<test-case>_<variation>
                    // <dd-file-name>       = <bld-tgt-name>-ra.xml
                    // <ra-display-name>    = <bld-tgt-name>
                    // <rar-file-name>      = <bld-tgt-name>.rar

                    // Test RAR names must match the <display-name> element of the Connector 
                    // in the deployment descriptor or in the @Conenctor annotation.

                    TRA_jca16_ann_ActivationMergeAction_ML = "TRA_jca16_ann_ActivationMergeAction_ML",
                    TRA_jca16_ann_ActivationMergeAction_NoML = "TRA_jca16_ann_ActivationMergeAction_NoML",
                    TRA_jca16_ann_ActivationValidator_NonJB1 = "TRA_jca16_ann_ActivationValidator_NonJB1",

                    TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT = "TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT",
                    TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT = "TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT",
                    TRA_jca16_ann_AdministeredObjectValidator_NonJavaBeanAdminObject = "TRA_jca16_ann_AdministeredObjectValidator_NonJavaBeanAdminObject",
                    TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray = "TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray",
                    TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement = "TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement",
                    TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesOneNotImplemented = "TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesOneNotImplemented",
                    TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesAllNotSuperclass = "TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesAllNotSuperclass",
                    TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces = "TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces",
                    TRA_jca16_ann_AdministeredObjectValidator_SingleClassSingleInterface = "TRA_jca16_ann_AdministeredObjectValidator_SingleClassSingleInterface",

                    //TRA_jca16_ann_AuthenticationMechanismValidator_InvLoc1                        = "TRA_jca16_ann_AuthenticationMechanismValidator_InvLoc1",  Obviated by final JCA 1.6    

                    TRA_jca16_ann_ConfigPropertyMergeAction_NoElement = "TRA_jca16_ann_ConfigPropertyMergeAction_NoElement",
                    TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn = "TRA_jca16_ann_ConfigPropertyMergeAction_NoElementAnn",

                    TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnConnectorValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_ConnectorValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf = "TRA_jca16_ann_ConfigPropertyValidator_NoPermittedAnnNoDDEntryNoIntf",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnConnectionDefValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_MCFValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnActivationValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_ActSpecValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnAdminObjectValidFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_DDEntryAdminObjectValidConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetGetFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnNoGetterFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnNoSetterFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnInvalidGetterFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnInvGetRetTypeFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnNonPublicSetterFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnInvSetterTypeFieldLevelConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnPrimitiveMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnBigIntegerMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnBooleanMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnIntegerMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnNonSetNamedMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnSetMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnMutatorXYZConfigProperty",
                    TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty = "TRA_jca16_ann_ConfigPropertyValidator_AnnXyzFieldLevelConfigProperty",

                    TRA_jca16_ann_ConnectionDefinitionMergeAction_NoCD = "TRA_jca16_ann_ConnectionDefinitionMergeAction_NoCD",
                    TRA_jca16_ann_ConnectionDefinitionMergeAction_NoORA = "TRA_jca16_ann_ConnectionDefinitionMergeAction_NoORA",
                    TRA_jca16_ann_ConnectionDefinitionMergeAction_SingleCD = "TRA_jca16_ann_ConnectionDefinitionMergeAction_SingleCD",
                    TRA_jca16_ann_ConnectionDefinitionValidator_NonJB1 = "TRA_jca16_ann_ConnectionDefinitionValidator_NonJB1",
                    TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1 = "TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1",

                    TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD = "TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD",
                    TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD = "TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD",
                    TRA_jca16_ann_ConnectionDefinitionsValidator_NonJB1 = "TRA_jca16_ann_ConnectionDefinitionsValidator_NonJB1",
                    TRA_jca16_ann_ConnectionDefinitionsValidator_NonMCF1 = "TRA_jca16_ann_ConnectionDefinitionsValidator_NonMCF1",

                    TRA_jca16_ann_ConnectorMergeAction_NoElement = "TRA_jca16_ann_ConnectorMergeAction_NoElement",
                    TRA_jca16_ann_ConnectorMergeAction_SingleElement = "TRA_jca16_ann_ConnectorMergeAction_SingleElement",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElement = "TRA_jca16_ann_ConnectorMergeAction_MultiElement",

                    TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_DefaultElement1NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_DefaultElement1NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_DefaultElement2NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_DefaultElement2NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_DefaultElement3NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_DefaultElement3NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD = "TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD = "TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD",
                    TRA_jca16_ann_ConnectorMergeAction_SingleElementSingleInDD = "TRA_jca16_ann_ConnectorMergeAction_SingleElementSingleInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElement1NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElement1NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElement2NoneInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElement2NoneInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElement1SameInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElement1SameInDD",
                    TRA_jca16_ann_ConnectorMergeAction_MultiElementSingleInDD = "TRA_jca16_ann_ConnectorMergeAction_MultiElementSingleInDD",

                    TRA_jca16_ann_ConnectorValidator_NonJB0 = "TRA_jca16_ann_ConnectorValidator_NonJB0",
                    TRA_jca16_ann_ConnectorValidator_NonRA0 = "TRA_jca16_ann_ConnectorValidator_NonRA0",

                    //TRA_jca16_ann_SecurityPermissionValidator_InvLoc1 = "TRA_jca16_ann_SecurityPermissionValidator_InvLoc1",   Obviated by final JCA 1.6 

                    adapter_jca16_jbv_ResourceAdapterValidation_Success = "adapter_jca16_jbv_ResourceAdapterValidation_Success",
                    adapter_jca16_jbv_ResourceAdapterValidation_Failure = "adapter_jca16_jbv_ResourceAdapterValidation_Failure",
                    adapter_jca16_jbv_ResourceAdapterValidation_Embedded = "adapter_jca16_jbv_ResourceAdapterValidation_Embedded",
                    adapter_jca16_jbv_ResourceAdapterValidation_Embedded_Failure = "adapter_jca16_jbv_ResourceAdapterValidation_Embedded_Failure",
                    adapter_jca16_jbv_AdministeredObjectValidation_Failure = "adapter_jca16_jbv_AdministeredObjectValidation_Failure",
                    adapter_jca16_jbv_AdministeredObjectValidation_Success = "adapter_jca16_jbv_AdministeredObjectValidation_Success",
                    adapter_jca16_jbv_AdministeredObjectValidation_Embedded = "adapter_jca16_jbv_AdministeredObjectValidation_Embedded",
                    adapter_jca16_jbv_ActivationSpecValidation_Failure = "adapter_jca16_jbv_ActivationSpecValidation_Failure",
                    adapter_jca16_jbv_ActivationSpecValidation_Success = "adapter_jca16_jbv_ActivationSpecValidation_Success",
                    adapter_jca16_jbv_ActivationSpecValidation_Embedded = "adapter_jca16_jbv_ActivationSpecValidation_Embedded",
                    adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success = "adapter_jca16_jbv_ManagedConnectionFactoryValidation_Success",
                    adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure = "adapter_jca16_jbv_ManagedConnectionFactoryValidation_Failure",
                    adapter_jca16_jbv_ResourceAdapterValidation_Success_isolated = "adapter_jca16_jbv_ResourceAdapterValidation_Success_isolated",
                    adapter_jca16_jbv_AdministeredObjectValidation_Success_isolated = "adapter_jca16_jbv_AdministeredObjectValidation_Success_isolated",

                    adapter_jca16_insec_AnnotatedInboundSecurity = "adapter_jca16_insec_AnnotatedInboundSecurity",

                    // Sample Application Names
                    sampleapp_jca16_jbv_embeddedraApp = "sampleapp_jca16_jbv_embeddedra",
                    sampleapp_jca16_jbv_embeddedraApp_Failure = "sampleapp_jca16_jbv_embeddedraApp_Failure",
                    sampleapp_jca16_jbv_embeddedaoApp = "sampleapp_jca16_jbv_embeddedao",
                    sampleapp_jca16_jbv_standaloneassuccessApp = "sampleapp_jca16_jbv_standaloneassuccess",
                    sampleapp_jca16_jbv_standaloneasfailureApp = "sampleapp_jca16_jbv_standaloneasfailure",
                    sampleapp_jca16_jbv_embeddedasApp = "sampleapp_jca16_jbv_embeddedas",

                    sampleapp_jca16_jbv_embeddedra_ejbvalconfigApp = "sampleapp_jca16_jbv_embeddedra_ejbvalconfig",
                    sampleapp_jca16_jbv_embeddedravalconfig_ejbApp = "sampleapp_jca16_jbv_embeddedravalconfig_ejb",
                    sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfigApp = "sampleapp_jca16_jbv_embeddedravalconfig_ejbvalconfig",
                    sampleapp_jca16_jbv_ejbApp = "sampleapp_jca16_jbv_ejb",
                    sampleapp_jca16_jbv_ejbvalconfigApp = "sampleapp_jca16_jbv_ejbvalconfig",
                    Jbvapp = "Jbvapp",
                    sampleapp_jca16_insec_inboundsecurityApp = "sampleapp_jca16_insec_inboundsecurityApp",

                    // Annotation Merge and Validation Messages
                    ENTITY_DOESNT_IMPL_REQD_IF_J2CA0147 = "J2CA0147",
                    ENTITY_NOT_A_JAVABEAN_J2CA0219 = "J2CA0219",
                    ENTITY_NOT_A_JAVABEAN_J2CA7002 = "J2CA7002",

                    //INVALID_ANNOTATION_LOCATION_J2CA0221 = "J2CA0221", F743-15626.3 Obviated by final JCA 1.6 
                    INVALID_ADMIN_OBJECT_J2CA0222 = "J2CA0222",
                    INVALID_CONFIG_PROP_USAGE_J2CA0229 = "J2CA0229",
                    INVALID_CONFIG_PROP_USAGE_J2CA0230 = "J2CA0230",
                    INVALID_CONFIG_PROP_USAGE_J2CA9906E = "J2CA9906E",
                    INVALID_CONFIG_PROP_USAGE_J2CA0232 = "J2CA0232",
                    INVALID_CONFIG_PROP_USAGE_J2CA0233 = "J2CA0233",
                    INVALID_CONFIG_PROP_USAGE_J2CA0234 = "J2CA0234",
                    INVALID_CONFIG_PROP_USAGE_J2CA7002 = "J2CA7002",
                    INVALID_CONFIG_PROP_USAGE_J2CA9927 = "J2CA9927",

                    RESOURCE_XML_MODIFIED = "ADMR0010I",
                    APPLICATION_STARTED_WSVR0221I = "WSVR0221I",
                    UNABLE_TO_START_APP_ADMA0116W = "ADMA0116W",
                    CONSTRAINT_VIOLATION_J2CA0238E = "J2CA0238E",

                    COMPOSITION_UNIT_STARTUP_FAILURE = "WSVR0194E",

                    AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677 = "J2CA0677E",
                    MULTIPLE_CALLERPRINCIPALCALLBACKS_NOT_SUPPORTED_J2CA0676 = "J2CA0676E",
                    EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673 = "J2CA0673W",
                    ERROR_HANDLING_CALLBACK_J2CA0672 = "J2CA0672E",
                    SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671 = "J2CA0671E",
                    INVALID_USER_NAME_IN_PRINCIPAL_J2CA0670 = "J2CA0670E",
                    CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669 = "J2CA0669E",
                    CUSTOM_CREDENTIALS_MISSING_J2CA0668 = "J2CA0668E",
                    USER_NAME_MISMATCH_J2CA0675 = "J2CA0675E",
                    INVALID_GROUP_ENCOUNTERED_J2CA0678 = "J2CA0678W",
                    INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674 = "J2CA0674E",
                    PASSWORD_VALIDATION_FAILED_J2CA0684 = "J2CA0684E",
                    AUTHORIZATION_FAILED_SECJ0053E = "CWWKS9400A",
                    // JCA 1.6 DD Element Values

                    AUTHMECHANISMTYPE_BASICPASSWORD = "BasicPassword",
                    AUTHMECHANISMTYPE_KERBV5 = "Kerbv5",
                    AUTHMECHANISM_CREDENTIALIF_PASSWORD = "javax.resource.spi.security.PasswordCredential",
                    AUTHMECHANISM_CREDENTIALIF_GSS = "org.ietf.jgss.GSSCredential",
                    INSTALL_FAILED_J2CA9926E = "J2CA9926E",
                    AUTHMECHANISM_CREDENTIALIF_GENERIC = "javax.resource.spi.security.GenericCredential";

} // interface RarTests
