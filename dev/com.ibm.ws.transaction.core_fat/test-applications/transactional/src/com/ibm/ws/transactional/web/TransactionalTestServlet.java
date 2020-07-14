/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionalException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.wsspi.uow.UOWManager;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Servlet implementation class TransactionalTest
 */
@WebServlet("/transactional")
@Mode(TestMode.FULL)
public class TransactionalTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private MethodAnnotatedTestBean methodAnnotatedTestBean;

    @Inject
    private ClassAnnotatedMandatoryTestBean classAnnotatedMandatoryTestBean;

    @Inject
    private ClassAnnotatedMandatoryNoListsTestBean classAnnotatedMandatoryNoListsTestBean;

    @Inject
    private ClassAnnotatedNeverTestBean classAnnotatedNeverTestBean;

    @Inject
    private ClassAnnotatedNeverNoListsTestBean classAnnotatedNeverNoListsTestBean;

    @Inject
    private ClassAnnotatedNotSupportedTestBean classAnnotatedNotSupportedTestBean;

    @Inject
    private ClassAnnotatedNotSupportedNoListsTestBean classAnnotatedNotSupportedNoListsTestBean;

    @Inject
    private ClassAnnotatedRequiredTestBean classAnnotatedRequiredTestBean;

    @Inject
    private ClassAnnotatedRequiredNoListsTestBean classAnnotatedRequiredNoListsTestBean;

    @Inject
    private ClassAnnotatedRequiredTestBeanAlternativeExceptions classAnnotatedRequiredTestBeanAlternativeExceptions;

    @Inject
    private ClassAnnotatedRequiresNewTestBean classAnnotatedRequiresNewTestBean;

    @Inject
    private ClassAnnotatedRequiresNewNoListsTestBean classAnnotatedRequiresNewNoListsTestBean;

    @Inject
    private ClassAnnotatedSupportsTestBean classAnnotatedSupportsTestBean;

    @Inject
    private ClassAnnotatedSupportsNoListsTestBean classAnnotatedSupportsNoListsTestBean;

    @Inject
    private ClassAnnotatedMandatoryStereotypeTestBean classAnnotatedMandatoryStereotypeTestBean;

    @Inject
    private ClassAnnotatedRequiredCallsRequiresNew classAnnotatedRequiredCallsRequiresNew;

    @Resource
    private UserTransaction ut;

    @Resource
    private UOWManager uowm;

    private PrintWriter writer = new PrintWriter(System.out);

    @Test
    public void testMA001a() {
        testMA001(methodAnnotatedTestBean, "MA001a");
    }

    @Test
    public void testMA001b() {
        testMA001(classAnnotatedMandatoryTestBean, "MA001b");
    }

    @Test
    public void testMA001c() {
        testMA001(classAnnotatedMandatoryStereotypeTestBean, "MA001c");
    }

    @Test
    public void testMA002a() {
        testMA002(methodAnnotatedTestBean, "MA002a");
    }

    @Test
    public void testMA002b() {
        testMA002(classAnnotatedMandatoryTestBean, "MA002b");
    }

    @Test
    public void testMA002c() {
        testMA002(classAnnotatedMandatoryStereotypeTestBean, "MA002c");
    }

    @Test
    public void testMA003a() {
        testMA003(methodAnnotatedTestBean, "MA003a");
    }

    @Test
    public void testMA003b() {
        testMA003(classAnnotatedMandatoryTestBean, "MA003b");
    }

    @Test
    public void testMA003c() {
        testMA003(classAnnotatedMandatoryStereotypeTestBean, "MA003c");
    }

    @Test
    public void testMA004a() {
        testMA004(methodAnnotatedTestBean, "MA004a");
    }

    @Test
    public void testMA004b() {
        testMA004(classAnnotatedMandatoryTestBean, "MA004b");
    }

    @Test
    public void testMA004c() {
        testMA004(classAnnotatedMandatoryStereotypeTestBean, "MA004c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA005a() {
        testMA005(methodAnnotatedTestBean, "MA005a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA005b() {
        testMA005(classAnnotatedMandatoryTestBean, "MA005b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA005c() {
        testMA005(classAnnotatedMandatoryStereotypeTestBean, "MA005c");
    }

    @Test
    public void testMA006a() {
        testMA006(methodAnnotatedTestBean, "MA006a");
    }

    @Test
    public void testMA006b() {
        testMA006(classAnnotatedMandatoryTestBean, "MA006b");
    }

    @Test
    public void testMA006c() {
        testMA006(classAnnotatedMandatoryStereotypeTestBean, "MA006c");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA007a() {
        testMA007(methodAnnotatedTestBean, "MA007a");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA007b() {
        testMA007(classAnnotatedMandatoryTestBean, "MA007b");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA007c() {
        testMA007(classAnnotatedMandatoryStereotypeTestBean, "MA007c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA008a() {
        testMA008(methodAnnotatedTestBean, "MA008a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA008b() {
        testMA008(classAnnotatedMandatoryTestBean, "MA008b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA008c() {
        testMA008(classAnnotatedMandatoryStereotypeTestBean, "MA008c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA009a() {
        testMA009(methodAnnotatedTestBean, "MA009a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA009b() {
        testMA009(classAnnotatedMandatoryTestBean, "MA009b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA009c() {
        testMA009(classAnnotatedMandatoryStereotypeTestBean, "MA009c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA010a() {
        testMA010(methodAnnotatedTestBean, "MA010a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA010b() {
        testMA010(classAnnotatedMandatoryTestBean, "MA010b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA010c() {
        testMA010(classAnnotatedMandatoryStereotypeTestBean, "MA010c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA011a() {
        testMA011(methodAnnotatedTestBean, "MA011a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA011b() {
        testMA011(classAnnotatedMandatoryTestBean, "MA011b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA011c() {
        testMA011(classAnnotatedMandatoryStereotypeTestBean, "MA011c");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA012a() {
        testMA012(methodAnnotatedTestBean, "MA012a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA012b() {
        testMA012(classAnnotatedMandatoryTestBean, "MA012b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA012c() {
        testMA012(classAnnotatedMandatoryStereotypeTestBean, "MA012c");
    }

    @Test
    public void testMA013a() {
        testMA013(methodAnnotatedTestBean, "MA013a");
    }

    @Test
    public void testMA013b() {
        testMA013(classAnnotatedMandatoryTestBean, "MA013b");
    }

    @Test
    public void testMA013c() {
        testMA013(classAnnotatedMandatoryStereotypeTestBean, "MA013c");
    }

    @Test
    public void testMA014a() {
        testMA014(methodAnnotatedTestBean, "MA014a");
    }

    @Test
    public void testMA014b() {
        testMA014(classAnnotatedMandatoryNoListsTestBean, "MA014b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA015a() {
        testMA015(methodAnnotatedTestBean, "MA015a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testMA015b() {
        testMA015(classAnnotatedMandatoryNoListsTestBean, "MA015b");
    }

    @Test
    public void testNE001a() {
        testNE001(methodAnnotatedTestBean, "NE001a");
    }

    @Test
    public void testNE001b() {
        testNE001(classAnnotatedNeverTestBean, "NE001b");
    }

    @Test
    public void testNE002a() {
        testNE002(methodAnnotatedTestBean, "NE002a");
    }

    @Test
    public void testNE002b() {
        testNE002(classAnnotatedNeverTestBean, "NE002b");
    }

    @Test
    public void testNE003a() {
        testNE003(methodAnnotatedTestBean, "NE003a");
    }

    @Test
    public void testNE003b() {
        testNE003(classAnnotatedNeverTestBean, "NE003b");
    }

    @Test
    public void testNE004a() {
        testNE004(methodAnnotatedTestBean, "NE004a");
    }

    @Test
    public void testNE004b() {
        testNE004(classAnnotatedNeverTestBean, "NE004b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNE005a() {
        testNE005(methodAnnotatedTestBean, "NE005a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNE005b() {
        testNE005(classAnnotatedNeverTestBean, "NE005b");
    }

    @Test
    public void testNE006a() {
        testNE006(methodAnnotatedTestBean, "NE006a");
    }

    @Test
    public void testNE006b() {
        testNE006(classAnnotatedNeverTestBean, "NE006b");
    }

    @Test
    public void testNE007a() {
        testNE007(methodAnnotatedTestBean, "NE007a");
    }

    @Test
    public void testNE007b() {
        testNE007(classAnnotatedNeverTestBean, "NE007b");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testNE008a() {
        testNE008(methodAnnotatedTestBean, "NE008a");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testNE008b() {
        testNE008(classAnnotatedNeverTestBean, "NE008b");
    }

    @Test
    public void testNE009a() {
        testNE009(methodAnnotatedTestBean, "NE009a");
    }

    @Test
    public void testNE009b() {
        testNE009(classAnnotatedNeverTestBean, "NE009b");
    }

    @Test
    public void testNE010a() {
        testNE010(methodAnnotatedTestBean, "NE010a");
    }

    @Test
    public void testNE010b() {
        testNE010(classAnnotatedNeverTestBean, "NE010b");
    }

    @Test
    public void testNE011a() {
        testNE011(methodAnnotatedTestBean, "NE011a");
    }

    @Test
    public void testNE011b() {
        testNE011(classAnnotatedNeverTestBean, "NE011b");
    }

    @Test
    public void testNE012a() {
        testNE012(methodAnnotatedTestBean, "NE012a");
    }

    @Test
    public void testNE012b() {
        testNE012(classAnnotatedNeverTestBean, "NE012b");
    }

    @Test
    public void testNE013a() {
        testNE013(methodAnnotatedTestBean, "NE013a");
    }

    @Test
    public void testNE013b() {
        testNE013(classAnnotatedNeverTestBean, "NE013b");
    }

    @Test
    public void testNE014a() {
        testNE014(methodAnnotatedTestBean, "NE014a");
    }

    @Test
    public void testNE014b() {
        testNE014(classAnnotatedNeverNoListsTestBean, "NE014b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNE015a() {
        testNE015(methodAnnotatedTestBean, "NE015a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNE015b() {
        testNE015(classAnnotatedNeverNoListsTestBean, "NE015b");
    }

    @Test
    public void testNS001a() {
        testNS001(methodAnnotatedTestBean, "NS001a");
    }

    @Test
    public void testNS001b() {
        testNS001(classAnnotatedNotSupportedTestBean, "NS001b");
    }

    @Test
    public void testNS002a() {
        testNS002(methodAnnotatedTestBean, "NS002a");
    }

    @Test
    public void testNS002b() {
        testNS002(classAnnotatedNotSupportedTestBean, "NS002b");
    }

    @Test
    public void testNS003a() {
        testNS003(methodAnnotatedTestBean, "NS003a");
    }

    @Test
    public void testNS003b() {
        testNS003(classAnnotatedNotSupportedTestBean, "NS003b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS004a() {
        testNS004(methodAnnotatedTestBean, "NS004a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS004b() {
        testNS004(classAnnotatedNotSupportedTestBean, "NS004b");
    }

    @Test
    public void testNS005a() {
        testNS005(methodAnnotatedTestBean, "NS005a");
    }

    @Test
    public void testNS005b() {
        testNS005(classAnnotatedNotSupportedTestBean, "NS005b");
    }

    @Test
    public void testNS006a() {
        testNS006(methodAnnotatedTestBean, "NS006a");
    }

    @Test
    public void testNS006b() {
        testNS006(classAnnotatedNotSupportedTestBean, "NS006b");
    }

    @Test
    public void testNS007a() {
        testNS007(methodAnnotatedTestBean, "NS007a");
    }

    @Test
    public void testNS007b() {
        testNS007(classAnnotatedNotSupportedTestBean, "NS007b");
    }

    @Test
    public void testNS008a() {
        testNS008(methodAnnotatedTestBean, "NS008a");
    }

    @Test
    public void testNS008b() {
        testNS008(classAnnotatedNotSupportedTestBean, "NS008b");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS009a() {
        testNS009(methodAnnotatedTestBean, "NS009a");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS009b() {
        testNS009(classAnnotatedNotSupportedTestBean, "NS009b");
    }

    @Test
    public void testNS010a() {
        testNS010(methodAnnotatedTestBean, "NS010a");
    }

    @Test
    public void testNS010b() {
        testNS010(classAnnotatedNotSupportedTestBean, "NS010b");
    }

    @Test
    public void testNS011a() {
        testNS011(methodAnnotatedTestBean, "NS011a");
    }

    @Test
    public void testNS011b() {
        testNS011(classAnnotatedNotSupportedTestBean, "NS011b");
    }

    @Test
    public void testNS012a() {
        testNS012(methodAnnotatedTestBean, "NS012a");
    }

    @Test
    public void testNS012b() {
        testNS012(classAnnotatedNotSupportedTestBean, "NS012b");
    }

    @Test
    public void testNS013a() {
        testNS013(methodAnnotatedTestBean, "NS013a");
    }

    @Test
    public void testNS013b() {
        testNS013(classAnnotatedNotSupportedTestBean, "NS013b");
    }

    @Test
    public void testNS014a() {
        testNS014(methodAnnotatedTestBean, "NS014a");
    }

    @Test
    public void testNS014b() {
        testNS014(classAnnotatedNotSupportedTestBean, "NS014b");
    }

    @Test
    public void testNS015a() {
        testNS015(methodAnnotatedTestBean, "NS015a");
    }

    @Test
    public void testNS015b() {
        testNS015(classAnnotatedNotSupportedTestBean, "NS015b");
    }

    @Test
    public void testNS016a() {
        testNS016(methodAnnotatedTestBean, "NS016a");
    }

    @Test
    public void testNS016b() {
        testNS016(classAnnotatedNotSupportedTestBean, "NS016b");
    }

    @Test
    public void testNS017a() {
        testNS017(methodAnnotatedTestBean, "NS017a");
    }

    @Test
    public void testNS017b() {
        testNS017(classAnnotatedNotSupportedTestBean, "NS017b");
    }

    @Test
    public void testNS018a() {
        testNS018(methodAnnotatedTestBean, "NS018a");
    }

    @Test
    public void testNS018b() {
        testNS018(classAnnotatedNotSupportedNoListsTestBean, "NS018b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.NullPointerException" })
    public void testNS019a() {
        testNS019(methodAnnotatedTestBean, "NS019a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.NullPointerException" })
    public void testNS019b() {
        testNS019(classAnnotatedNotSupportedNoListsTestBean, "NS019b");
    }

    @Test
    public void testNS020a() {
        testNS020(methodAnnotatedTestBean, "NS020a");
    }

    @Test
    public void testNS020b() {
        testNS020(classAnnotatedNotSupportedNoListsTestBean, "NS020b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS021a() {
        testNS021(methodAnnotatedTestBean, "NS021a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNS021b() {
        testNS021(classAnnotatedNotSupportedNoListsTestBean, "NS021b");
    }

    @Test
    public void testRE001a() {
        testRE001(methodAnnotatedTestBean, "RE001a");
    }

    @Test
    public void testRE001b() {
        testRE001(classAnnotatedRequiredTestBean, "RE001b");
    }

    @Test
    public void testRE002a() {
        testRE002(methodAnnotatedTestBean, "RE002a");
    }

    @Test
    public void testRE002b() {
        testRE002(classAnnotatedRequiredTestBean, "RE002b");
    }

    @Test
    public void testRE003a() {
        testRE003(methodAnnotatedTestBean, "RE003a");
    }

    @Test
    public void testRE003b() {
        testRE003(classAnnotatedRequiredTestBean, "RE003b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE004a() {
        testRE004(methodAnnotatedTestBean, "RE004a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE004b() {
        testRE004(classAnnotatedRequiredTestBean, "RE004b");
    }

    @Test
    public void testRE005a() {
        testRE005(methodAnnotatedTestBean, "RE005a");
    }

    @Test
    public void testRE005b() {
        testRE005(classAnnotatedRequiredTestBean, "RE005b");
    }

    @Test
    public void testRE006a() {
        testRE006(methodAnnotatedTestBean, "RE006a");
    }

    @Test
    public void testRE006b() {
        testRE006(classAnnotatedRequiredTestBean, "RE006b");
    }

    @Test
    public void testRE007a() {
        testRE007(methodAnnotatedTestBean, "RE007a");
    }

    @Test
    public void testRE007b() {
        testRE007(classAnnotatedRequiredTestBean, "RE007b");
    }

    @Test
    public void testRE008a() {
        testRE008(methodAnnotatedTestBean, "RE008a");
    }

    @Test
    public void testRE008b() {
        testRE008(classAnnotatedRequiredTestBean, "RE008b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE009a() {
        testRE009(methodAnnotatedTestBean, "RE009a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE009b() {
        testRE009(classAnnotatedRequiredTestBean, "RE009b");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRE010a() {
        testRE010(methodAnnotatedTestBean, "RE010a");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRE010b() {
        testRE010(classAnnotatedRequiredTestBean, "RE010b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE011a() {
        testRE011(methodAnnotatedTestBean, "RE011a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE011b() {
        testRE011(classAnnotatedRequiredTestBean, "RE011b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE012a() {
        testRE012(methodAnnotatedTestBean, "RE012a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE012b() {
        testRE012(classAnnotatedRequiredTestBean, "RE012b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE013a() {
        testRE013(methodAnnotatedTestBean, "RE013a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE013b() {
        testRE013(classAnnotatedRequiredTestBean, "RE013b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE014a() {
        testRE014(methodAnnotatedTestBean, "RE014a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE014b() {
        testRE014(classAnnotatedRequiredTestBean, "RE014b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE015a() {
        testRE015(methodAnnotatedTestBean, "RE015a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE015b() {
        testRE015(classAnnotatedRequiredTestBean, "RE015b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE016a() {
        testRE016(methodAnnotatedTestBean, "RE016a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE016b() {
        testRE016(classAnnotatedRequiredTestBean, "RE016b");
    }

    @Test
    public void testRE017a() {
        testRE017(methodAnnotatedTestBean, "RE017a");
    }

    @Test
    public void testRE017b() {
        testRE017(classAnnotatedRequiredTestBean, "RE017b");
    }

    @Test
    public void testRE018a() {
        testRE018(methodAnnotatedTestBean, "RE018a");
    }

    @Test
    public void testRE018b() {
        testRE018(classAnnotatedRequiredNoListsTestBean, "RE018b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE019a() {
        testRE019(methodAnnotatedTestBean, "RE019a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE019b() {
        testRE019(classAnnotatedRequiredNoListsTestBean, "RE019b");
    }

    @Test
    public void testRE020a() {
        testRE020(methodAnnotatedTestBean, "RE020a");
    }

    @Test
    public void testRE020b() {
        testRE020(classAnnotatedRequiredNoListsTestBean, "RE020b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE021a() {
        testRE021(methodAnnotatedTestBean, "RE021a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRE021b() {
        testRE021(classAnnotatedRequiredNoListsTestBean, "RE021b");
    }

    @Test
    public void testRE022a() {
        testRE022(methodAnnotatedTestBean, "RE022a");
    }

    @Test
    public void testRE022b() {
        testRE022(classAnnotatedRequiredTestBeanAlternativeExceptions, "RE022b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.RuntimeException" })
    public void testRE023a() {
        testRE023(methodAnnotatedTestBean, "RE023a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.RuntimeException" })
    public void testRE023b() {
        testRE023(classAnnotatedRequiredTestBeanAlternativeExceptions, "RE023b");
    }

    @Test
    public void testRE024a() {
        testRE024(methodAnnotatedTestBean, "RE024a");
    }

    @Test
    public void testRE024b() {
        testRE024(classAnnotatedRequiredTestBeanAlternativeExceptions, "RE024b");
    }

    @Test
    public void testRN001a() {
        testRN001(methodAnnotatedTestBean, "RN001a");
    }

    @Test
    public void testRN001b() {
        testRN001(classAnnotatedRequiresNewTestBean, "RN001b");
    }

    @Test
    public void testRN002a() {
        testRN002(methodAnnotatedTestBean, "RN002a");
    }

    @Test
    public void testRN002b() {
        testRN002(classAnnotatedRequiresNewTestBean, "RN002b");
    }

    @Test
    public void testRN003a() {
        testRN003(methodAnnotatedTestBean, "RN003a");
    }

    @Test
    public void testRN003b() {
        testRN003(classAnnotatedRequiresNewTestBean, "RN003b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN004a() {
        testRN004(methodAnnotatedTestBean, "RN004a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN004b() {
        testRN004(classAnnotatedRequiresNewTestBean, "RN004b");
    }

    @Test
    public void testRN005a() {
        testRN005(methodAnnotatedTestBean, "RN005a");
    }

    @Test
    public void testRN005b() {
        testRN005(classAnnotatedRequiresNewTestBean, "RN005b");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRN006a() {
        testRN006(methodAnnotatedTestBean, "RN006a");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testRN006b() {
        testRN006(classAnnotatedRequiresNewTestBean, "RN006b");
    }

    @Test
    public void testRN007a() {
        testRN007(methodAnnotatedTestBean, "RN007a");
    }

    @Test
    public void testRN007b() {
        testRN007(classAnnotatedRequiresNewTestBean, "RN007b");
    }

    @Test
    public void testRN008a() {
        testRN008(methodAnnotatedTestBean, "RN008a");
    }

    @Test
    public void testRN008b() {
        testRN008(classAnnotatedRequiresNewTestBean, "RN008b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN009a() {
        testRN009(methodAnnotatedTestBean, "RN009a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN009b() {
        testRN009(classAnnotatedRequiresNewTestBean, "RN009b");
    }

    @Test
    public void testRN010a() {
        testRN010(methodAnnotatedTestBean, "RN010a");
    }

    @Test
    public void testRN010b() {
        testRN010(classAnnotatedRequiresNewTestBean, "RN010b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN011a() {
        testRN011(methodAnnotatedTestBean, "RN011a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN011b() {
        testRN011(classAnnotatedRequiresNewTestBean, "RN011b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN012a() {
        testRN012(methodAnnotatedTestBean, "RN012a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN012b() {
        testRN012(classAnnotatedRequiresNewTestBean, "RN012b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN013a() {
        testRN013(methodAnnotatedTestBean, "RN013a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN013b() {
        testRN013(classAnnotatedRequiresNewTestBean, "RN013b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN014a() {
        testRN014(methodAnnotatedTestBean, "RN014a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN014b() {
        testRN014(classAnnotatedRequiresNewTestBean, "RN014b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN015a() {
        testRN015(methodAnnotatedTestBean, "RN015a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN015b() {
        testRN015(classAnnotatedRequiresNewTestBean, "RN015b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN016a() {
        testRN016(methodAnnotatedTestBean, "RN016a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN016b() {
        testRN016(classAnnotatedRequiresNewTestBean, "RN016b");
    }

    @Test
    public void testRN017a() {
        testRN017(methodAnnotatedTestBean, "RN017a");
    }

    @Test
    public void testRN017b() {
        testRN017(classAnnotatedRequiresNewTestBean, "RN017b");
    }

    @Test
    public void testRN018a() {
        testRN018(methodAnnotatedTestBean, "RN018a");
    }

    @Test
    public void testRN018b() {
        testRN018(classAnnotatedRequiresNewNoListsTestBean, "RN018b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN019a() {
        testRN019(methodAnnotatedTestBean, "RN019a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN019b() {
        testRN019(classAnnotatedRequiresNewNoListsTestBean, "RN019b");
    }

    @Test
    public void testRN020a() {
        testRN020(methodAnnotatedTestBean, "RN020a");
    }

    @Test
    public void testRN020b() {
        testRN020(classAnnotatedRequiresNewNoListsTestBean, "RN020b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN021a() {
        testRN021(methodAnnotatedTestBean, "RN021a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testRN021b() {
        testRN021(classAnnotatedRequiresNewNoListsTestBean, "RN021b");
    }

    @Test
    public void testSU001a() {
        testSU001(methodAnnotatedTestBean, "SU001a");
    }

    @Test
    public void testSU001b() {
        testSU001(classAnnotatedSupportsTestBean, "SU001b");
    }

    @Test
    public void testSU002a() {
        testSU002(methodAnnotatedTestBean, "SU002a");
    }

    @Test
    public void testSU002b() {
        testSU002(classAnnotatedSupportsTestBean, "SU002b");
    }

    @Test
    public void testSU003a() {
        testSU003(methodAnnotatedTestBean, "SU003a");
    }

    @Test
    public void testSU003b() {
        testSU003(classAnnotatedSupportsTestBean, "SU003b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU004a() {
        testSU004(methodAnnotatedTestBean, "SU004a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU004b() {
        testSU004(classAnnotatedSupportsTestBean, "SU004b");
    }

    @Test
    public void testSU005a() {
        testSU005(methodAnnotatedTestBean, "SU005a");
    }

    @Test
    public void testSU005b() {
        testSU005(classAnnotatedSupportsTestBean, "SU005b");
    }

    @Test
    public void testSU006a() {
        testSU006(methodAnnotatedTestBean, "SU006a");
    }

    @Test
    public void testSU006b() {
        testSU006(classAnnotatedSupportsTestBean, "SU006b");
    }

    @Test
    public void testSU007a() {
        testSU007(methodAnnotatedTestBean, "SU007a");
    }

    @Test
    public void testSU007b() {
        testSU007(classAnnotatedSupportsTestBean, "SU007b");
    }

    @Test
    public void testSU008a() {
        testSU008(methodAnnotatedTestBean, "SU008a");
    }

    @Test
    public void testSU008b() {
        testSU008(classAnnotatedSupportsTestBean, "SU008b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU009a() {
        testSU009(methodAnnotatedTestBean, "SU009a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU009b() {
        testSU009(classAnnotatedSupportsTestBean, "SU009b");
    }

    @Test
    public void testSU010a() {
        testSU010(methodAnnotatedTestBean, "SU010a");
    }

    @Test
    public void testSU010b() {
        testSU010(classAnnotatedSupportsTestBean, "SU010b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU011a() {
        testSU011(methodAnnotatedTestBean, "SU011a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU011b() {
        testSU011(classAnnotatedSupportsTestBean, "SU011b");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU012a() {
        testSU012(methodAnnotatedTestBean, "SU012a");
    }

    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU012b() {
        testSU012(classAnnotatedSupportsTestBean, "SU012b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU013a() {
        testSU013(methodAnnotatedTestBean, "SU013a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU013b() {
        testSU013(classAnnotatedSupportsTestBean, "SU013b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU014a() {
        testSU014(methodAnnotatedTestBean, "SU014a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU014b() {
        testSU014(classAnnotatedSupportsTestBean, "SU014b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU015a() {
        testSU015(methodAnnotatedTestBean, "SU015a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU015b() {
        testSU015(classAnnotatedSupportsTestBean, "SU015b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU016a() {
        testSU016(methodAnnotatedTestBean, "SU016a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU016b() {
        testSU016(classAnnotatedSupportsTestBean, "SU016b");
    }

    @Test
    public void testSU017a() {
        testSU017(methodAnnotatedTestBean, "SU017a");
    }

    @Test
    public void testSU017b() {
        testSU017(classAnnotatedSupportsTestBean, "SU017b");
    }

    @Test
    public void testSU018a() {
        testSU018(methodAnnotatedTestBean, "SU018a");
    }

    @Test
    public void testSU018b() {
        testSU018(classAnnotatedSupportsNoListsTestBean, "SU018b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU019a() {
        testSU019(methodAnnotatedTestBean, "SU019a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU019b() {
        testSU019(classAnnotatedSupportsNoListsTestBean, "SU019b");
    }

    @Test
    public void testSU020a() {
        testSU020(methodAnnotatedTestBean, "SU020a");
    }

    @Test
    public void testSU020b() {
        testSU020(classAnnotatedSupportsNoListsTestBean, "SU020b");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU021a() {
        testSU021(methodAnnotatedTestBean, "SU021a");
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testSU021b() {
        testSU021(classAnnotatedSupportsNoListsTestBean, "SU021b");
    }

    /**
     * Checks that it's impossible to access UserTransaction methods
     * in 'nested' @Transactional calls.
     */
    @Test
    @Mode(TestMode.LITE)
    @ExpectedFFDC(value = { "java.lang.IllegalStateException" })
    public void testNestedUTAccess() throws Throwable {
        final TestContext tc = new TestContext("NestedUTAccess");
        try {
            classAnnotatedRequiredCallsRequiresNew.tryGetStatus(classAnnotatedRequiresNewTestBean, tc);
            tc.setFailed(new Exception());
        } catch (IllegalStateException e) {
            tc.setPassed();
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute MANDATORY method outside a tran
     * Expect TransactionalException with TransactionRequiredException cause
     */
    private void testMA001(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicMandatory(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (TransactionalException e) {
            if (e.getCause() instanceof TransactionRequiredException) {
                tc.setPassed();
            } else {
                tc.setFailed(e);
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Expect commit to succeed
     */
    private void testMA002(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatory(tc, null);
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testMA003(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatory(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Exception e) {
                // fail
                tc.setFailed(e);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect commit to fail
     */
    private void testMA004(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatory(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("1"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect commit to fail
     */
    private void testMA005(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatory(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("0"));
            } catch (Exception e) {
                tc.setPassed();
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testMA006(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicMandatory(tc, new NullPointerException());
        } catch (NullPointerException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.begin()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA007(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTBegin(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.commit()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA008(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTCommit(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.getStatus()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA009(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTGetStatus(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.rollback()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA010(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTRollback(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.setRollbackOnly()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA011(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTSetRollbackOnly(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UserTransaction.setTransactionTimeout()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testMA012(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithUTSetTransactionTimeout(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Executes UOWManager.runUnderUOW()
     * Expect different UOW id in test context
     * Expect inner tran to have committed
     * Expect commit to succeed
     */
    private void testMA013(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.mandatoryWithRunUnderUOW(tc, null);
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (uowm.getLocalUOWId() == tc.getUOWId()) {
                    tc.setFailed(new Exception("0"));
                } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testMA014(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatoryNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Exception e) {
                // fail
                tc.setFailed(e);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute MANDATORY method inside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect commit to fail
     */
    private void testMA015(IMandatory testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicMandatoryNoLists(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("0"));
            } catch (Exception e) {
                tc.setPassed();
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NEVER method inside a tran
     * Expect to catch TransactionalException with InvalidTransactionException cause
     * Expect commit to succeed
     */
    private void testNE001(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNever(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (TransactionalException e) {
            if (!(e.getCause() instanceof InvalidTransactionException)) {
                tc.setFailed(e);
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NEVER method outside a tran
     */
    private void testNE002(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNever(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     */
    private void testNE003(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNever(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     */
    private void testNE004(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNever(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     */
    private void testNE005(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNever(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     */
    private void testNE006(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNever(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.begin()
     */
    private void testNE007(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTBegin(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.commit()
     */
    private void testNE008(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTCommit(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.getStatus()
     */
    private void testNE009(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTGetStatus(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.rollback()
     */
    private void testNE010(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTRollback(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.setRollbackOnly()
     */
    private void testNE011(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTSetRollbackOnly(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UserTransaction.setTransactionTimeout()
     */
    private void testNE012(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.neverWithUTSetTransactionTimeout(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Executes UOWManager.runUnderUOW()
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testNE013(INever testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.neverWithRunUnderUOW(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     */
    private void testNE014(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNeverNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NEVER method outside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     */
    private void testNE015(INever testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNeverNoLists(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     */
    private void testNS001(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupported(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     */
    private void testNS002(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupported(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     */
    private void testNS003(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupported(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     */
    private void testNS004(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupported(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     */
    private void testNS005(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupported(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Expect commit to succeed
     */
    private void testNS006(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupported(tc, null);
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS007(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupported(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS008(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupported(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS009(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupported(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS010(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            testBean.basicNotSupported(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.begin()
     */
    private void testNS011(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTBegin(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.commit()
     */
    private void testNS012(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTCommit(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.getStatus()
     */
    private void testNS013(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTGetStatus(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.rollback()
     */
    private void testNS014(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTRollback(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.setRollbackOnly()
     */
    private void testNS015(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTSetRollbackOnly(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UserTransaction.setTransactionTimeout()
     */
    private void testNS016(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.notSupportedWithUTSetTransactionTimeout(tc, null);
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Executes UOWManager.runUnderUOW()
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testNS017(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.notSupportedWithRunUnderUOW(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     */
    private void testNS018(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupportedNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method outside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     */
    private void testNS019(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicNotSupportedNoLists(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS020(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupportedNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute NOT_SUPPORTED method inside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect commit to succeed
     */
    private void testNS021(INotSupported testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.basicNotSupportedNoLists(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRE001(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequired(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRE002(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequired(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRE003(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequired(tc, new IllegalAccessException());
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRE004(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequired(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRE005(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequired(tc, new NullPointerException());
        } catch (NullPointerException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to succeed
     */
    private void testRE006(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            final long originalTran = uowm.getLocalUOWId();

            testBean.basicRequired(tc, null);

            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("0"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to succeed
     */
    private void testRE007(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequired(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to fail
     */
    private void testRE008(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequired(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to fail
     */
    private void testRE009(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequired(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to succeed
     */
    private void testRE010(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequired(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.begin()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE011(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTBegin(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.commit()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE012(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTCommit(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.getStatus()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE013(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTGetStatus(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.rollback()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE014(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTRollback(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.setRollbackOnly()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE015(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTSetRollbackOnly(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Executes UserTransaction.setTransactionTimeout()
     * Expect to catch IllegalStateException
     * Expect tran to be marked rollback
     * Expect commit to fail
     */
    private void testRE016(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            ut.begin();
            testBean.requiredWithUTSetTransactionTimeout(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                if (ut.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tc.setFailed(new Exception("1"));
                }

                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Execute UOWManager.runUnderUOW()
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have committed
     * Expect outer tran commit to succeed
     */
    private void testRE017(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            final long originalTran = uowm.getLocalUOWId();

            testBean.requiredWithRunUnderUOW(tc, null);

            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRE018(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicRequiredNoLists(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRE019(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);
        try {
            testBean.basicRequiredNoLists(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to succeed
     */
    private void testRE020(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiredNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method inside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect commit tran to fail
     */
    private void testRE021(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiredNoLists(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("2"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws subclass of checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRE022(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiredAlternativeExceptions(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws superclass of runtime exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRE023(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiredAlternativeExceptions(tc, new RuntimeException("Expected"));
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("Expected")) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("2"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRED method outside a tran
     * Throws superclass of runtime exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRE024(IRequired testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiredAlternativeExceptions(tc, new NumberFormatException());
        } catch (RuntimeException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRN001(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNew(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRN002(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNew(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN003(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNew(tc, new IllegalAccessException());
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN004(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNew(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRN005(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNew(tc, new NullPointerException());
        } catch (NullPointerException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have committed
     * Expect outer tran commit to succeed
     */
    private void testRN006(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            final long originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNew(tc, null);

            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have committed
     * Expect outer tran commit to succeed
     */
    private void testRN007(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNew(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have rolled back
     * Expect outer tran commit to succeed
     */
    private void testRN008(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNew(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have rolled back
     * Expect outer tran commit to succeed
     */
    private void testRN009(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNew(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have committed
     * Expect outer tran commit to succeed
     */
    private void testRN010(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNew(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            }
            if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.begin()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN011(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTBegin(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.commit()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN012(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTCommit(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.getStatus()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN013(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTGetStatus(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.rollback()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN014(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTRollback(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.setRollbackOnly()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN015(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTSetRollbackOnly(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Executes UserTransaction.setTransactionTimeout()
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN016(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithUTSetTransactionTimeout(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Execute UOWManager.runUnderUOW()
     * Expect to find UOW Id in the test context
     * Expect inner tran to have committed
     */
    private void testRN017(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.requiresNewWithRunUnderUOW(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testRN018(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNewNoLists(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method outside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have rolled back
     */
    private void testRN019(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicRequiresNewNoLists(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have committed
     * Expect outer tran commit to succeed
     */
    private void testRN020(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNewNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute REQUIRES_NEW method inside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be different from original tran
     * Expect inner tran to have rolled back
     * Expect outer tran commit to succeed
     */
    private void testRN021(IRequiresNew testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicRequiresNewNoLists(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() == originalTran) {
                tc.setFailed(new Exception("1"));
            } else if (tc.getStatus() != Status.STATUS_ROLLEDBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Expect no UOW Id in the test context
     */
    private void testSU001(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupports(tc, null);

            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect no UOW Id in the test context
     */
    private void testSU002(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupports(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect no UOW Id in the test context
     */
    private void testSU003(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupports(tc, new IllegalAccessException());
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect to no UOW Id in the test context
     */
    private void testSU004(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupports(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect no UOW Id in the test context
     */
    private void testSU005(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupports(tc, new NullPointerException());
        } catch (NullPointerException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Expect UOW Id in the test context to be same as original tran
     * Expect tran commit to succeed
     */
    private void testSU006(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            ut.begin();
            final long originalTran = uowm.getLocalUOWId();

            testBean.basicSupports(tc, null);

            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("0"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws checked exception not in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect tran commit to succeed
     */
    private void testSU007(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupports(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws checked exception in the rollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect UOW to be marked rollback only
     * Expect tran commit to fail
     */
    private void testSU008(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupports(tc, new IllegalAccessException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalAccessException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }

            int status = Status.STATUS_UNKNOWN;
            try {
                status = ut.getStatus();
            } catch (Throwable t) {
                tc.setFailed(t);
            }

            if (status != Status.STATUS_MARKED_ROLLBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("3"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws unchecked exception not in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect UOW to be marked rollback only
     * Expect tran commit to fail
     */
    private void testSU009(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupports(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }

            int status = Status.STATUS_UNKNOWN;
            try {
                status = ut.getStatus();
            } catch (Throwable t) {
                tc.setFailed(t);
            }

            if (status != Status.STATUS_MARKED_ROLLBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("3"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws unchecked exception in the dontRollbackOn set
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect tran commit to succeed
     */
    private void testSU010(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupports(tc, new NullPointerException());
            tc.setFailed(new Exception("0"));
        } catch (NullPointerException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.begin()
     * Expect to catch exception
     */
    private void testSU011(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTBegin(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.commit()
     * Expect to catch exception
     */
    private void testSU012(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTCommit(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.getStatus()
     * Expect to catch exception
     */
    private void testSU013(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTGetStatus(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.rollback()
     * Expect to catch exception
     */
    private void testSU014(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTRollback(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.setRollbackOnly()
     * Expect to catch exception
     */
    private void testSU015(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTSetRollbackOnly(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Executes UserTransaction.setTransactionTimeout()
     * Expect to catch exception
     */
    private void testSU016(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithUTSetTransactionTimeout(tc, null);
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            tc.setPassed();
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Execute UOWManager.runUnderUOW()
     * Expect to find a UOW Id in the test context
     * Expect the UOW to have committed
     */
    private void testSU017(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.supportsWithRunUnderUOW(tc, null);

            if (tc.getUOWId() < 0) {
                tc.setFailed(new Exception("0"));
            } else if (tc.getStatus() != Status.STATUS_COMMITTED) {
                tc.setFailed(new Exception("1"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws checked exception with no rollback lists
     * Expect to catch exception
     * Expect no UOW Id in the test context
     */
    private void testSU018(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupportsNoLists(tc, new InstantiationException());
        } catch (InstantiationException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method outside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect to no UOW Id in the test context
     */
    private void testSU019(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        try {
            testBean.basicSupportsNoLists(tc, new IllegalStateException());
        } catch (IllegalStateException e) {
            if (tc.getUOWId() >= 0) {
                tc.setFailed(new Exception("0"));
            } else {
                tc.setPassed();
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            writer.println(tc);
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws checked exception with no lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect tran commit to succeed
     */
    private void testSU020(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupportsNoLists(tc, new InstantiationException());
            tc.setFailed(new Exception("0"));
        } catch (InstantiationException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }

    /**
     * Execute SUPPORTS method inside a tran
     * Throws unchecked exception with no rollback lists
     * Expect to catch exception
     * Expect UOW Id in the test context to be same as original tran
     * Expect UOW to be marked rollback only
     * Expect tran commit to fail
     */
    private void testSU021(ISupports testBean, String test) {
        final TestContext tc = new TestContext(test);

        long originalTran = -1;

        try {
            ut.begin();
            originalTran = uowm.getLocalUOWId();

            testBean.basicSupportsNoLists(tc, new IllegalStateException());
            tc.setFailed(new Exception("0"));
        } catch (IllegalStateException e) {
            if (tc.getUOWId() != originalTran) {
                tc.setFailed(new Exception("1"));
            }

            int status = Status.STATUS_UNKNOWN;
            try {
                status = ut.getStatus();
            } catch (Throwable t) {
                tc.setFailed(t);
            }

            if (status != Status.STATUS_MARKED_ROLLBACK) {
                tc.setFailed(new Exception("2"));
            }
        } catch (Throwable t) {
            tc.setFailed(t);
        } finally {
            try {
                ut.commit();
                tc.setFailed(new Exception("3"));
            } catch (RollbackException e) {
                tc.setPassed();
            } catch (Throwable t) {
                tc.setFailed(t);
            } finally {
                writer.println(tc);
            }
        }
    }
}