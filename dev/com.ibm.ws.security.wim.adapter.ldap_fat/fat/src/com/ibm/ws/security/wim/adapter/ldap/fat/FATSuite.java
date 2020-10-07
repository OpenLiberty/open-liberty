/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ URAPIs_TDSLDAPTest.class,
                URAPIs_ADLDAPTest.class,
                URAPIs_TDSLDAP_SSLTest.class,
                URAPIs_ADLDAP_SSLTest.class,
                URAPIs_Federation_2LDAPsTest.class,
                URAPIs_ADLDAP_NoIdTest.class,
                URAPIs_CustomLDAPTest.class,
                URAPIs_SUNLDAPTest.class,
                URAPIs_SUNLDAP_SSLTest.class,
                URAPIs_Federation_2LDAPs_2RealmsTest.class,
                URAPIs_TDSLDAP_FailoverTest.class,
                URAPIs_TDSLDAPTest_URAttrMappingVar1.class,
                URAPIs_TDSLDAPTest_URAttrMappingVar2.class,
                URAPIs_TDSLDAPTest_URAttrMappingVar3.class,
                URAPIs_SUNLDAPTest_URAttrMappingVar4.class,
                URAPIs_TDS_EmptyInputsTest.class,
                URAPIs_MultipleLDAPsTest.class,
                URAPIs_SUNLDAP_DefaultConfigTest.class,
                CertificateLoginTest.class,
                CertificateLoginOddOIDTest.class,
                CertificateLoginTestWithSquareBraceInCertificateFilter.class,
                LDAPRegistryDynamicUpdateTest.class,
                LDAPReferralTest.class,
                URAPIs_TDSLDAP_NestedGroupTest.class,
                URAPIs_ADLDAP_NestedGroupTest.class,
                FATTestCustom.class,
                FATTestIDS.class,
                FATTestIDSNoFilters.class,
                FATTestIDSFailover.class,
                FATTestIDSExtremeFailover.class,
                FATTestIDSwithSSL.class,
                FATTestIDSwithSSLTrustOnly.class,
                FATTestAD.class,
                FATTestADwithSSL.class,
                FATTestADNoId.class,
                FATLDAPNameTest.class,
                FATTestPerformance.class,
                FATTestReuseConn.class,
                FATTest_SearchBase.class,
                FATTest_EntityTypeSearchFilterForGroupMembership.class,
                FederationCertificateLoginTest.class,
                TDSLDAP_SSLRef_NoSSLTest.class,
                TDSLDAP_SSLRef_BadSSLTest.class,
                LDAPRegistryNoCustomContext.class,
                LDAPRegistryContextPoolNoConfigTest.class,
                InputOutputMappingTest.class,
                URAPIs_RealmPropertyMappingTest.class,
                URAPIs_UserGroupSearchBases.class,
                ContextPoolTimeoutTest.class,
                VMMAPIs_TDSLDAPTest.class,
                OutboundSSLLDAPTest.class,
                URAPIs_PropertiesNotSupportedTest.class,
                SearchPagingTest.class,
                GetUserSecurityCustomLDAP.class,
                FATTestIDS_allIbmGroups.class,
                CustomCertificateMapperInBellTest.class,
                CustomCertificateMapperInFeatureTest.class,
                URAPIs_ADWildCardTest.class,
                LDAPRegressionTest.class,
                ReadTimeoutTest.class,
                RacfSdbmLdapTest.class,
                LdapFailoverTest.class,
                RacfSdbmLdapWithBasicTest.class,
                ADNestedGroupsWithRange.class,
                JNDIOutputTest.class,
                LDAPMemberAttributeScopeTest.class
})
public class FATSuite extends CommonLocalLDAPServerSuite {

}
