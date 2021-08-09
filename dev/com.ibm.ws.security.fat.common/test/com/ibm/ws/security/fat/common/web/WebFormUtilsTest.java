/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class WebFormUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final HtmlPage htmlPage = mockery.mock(HtmlPage.class);
    private final HtmlForm form = mockery.mock(HtmlForm.class);
    private final HtmlForm form2 = mockery.mock(HtmlForm.class, "form2");
    private final HtmlForm form3 = mockery.mock(HtmlForm.class, "form3");
    private final HtmlInput htmlInput = mockery.mock(HtmlInput.class, "htmlInput");
    private final HtmlInput submitButton = mockery.mock(HtmlInput.class, "submitButton");
    private final Page page = mockery.mock(Page.class);

    WebFormUtils utils = new WebFormUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new WebFormUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** getAndSubmitLoginForm **************************************/

    /**
     * Tests:
     * - HtmlPage: null
     * Expects:
     * - Exception should be thrown saying the HtmlPage object is null
     */
    @Test
    public void test_getAndSubmitLoginForm_nullLoginPage() {
        try {
            String username = null;
            String password = null;
            try {
                utils.getAndSubmitLoginForm(null, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "Cannot submit.+object is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Action attribute in form is not valid
     * Expects:
     * - Exception should be thrown saying the action attribute is null or doesn't match expected action value
     */
    @Test
    public void test_getAndSubmitLoginForm_invalidForm() {
        try {
            String username = "testuser";
            String password = "testuserpwd";
            final List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            mockery.checking(new Expectations() {
                {
                    one(htmlPage).getForms();
                    will(returnValue(forms));
                    one(form).getActionAttribute();
                }
            });
            try {
                utils.getAndSubmitLoginForm(htmlPage, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "action attribute.+null or was not.+" + Constants.J_SECURITY_CHECK);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Filling and submitting form goes successfully
     * Expects:
     * - No exceptions should be thrown, and submitted page should be returned
     */
    @Test
    public void test_getAndSubmitLoginForm_succeeds() {
        try {
            String username = "testuser";
            String password = "testuserpwd";
            final List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            mockery.checking(new Expectations() {
                {
                    one(htmlPage).getForms();
                    will(returnValue(forms));
                    one(form).getActionAttribute();
                    will(returnValue(Constants.J_SECURITY_CHECK));
                }
            });
            setInputValueExpectations(Constants.J_USERNAME, username);
            setInputValueExpectations(Constants.J_PASSWORD, password);
            getButtonAndSubmitExpectations(WebFormUtils.DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE);

            Page result = utils.getAndSubmitLoginForm(htmlPage, username, password);
            assertEquals("Resulting page did not match expected object.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAndValidateLoginForm **************************************/

    /**
     * Tests:
     * - Form list: null
     * Expects:
     * - Exception should be thrown saying no forms were found
     */
    @Test
    public void test_getAndValidateLoginForm_noForms() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(htmlPage).getForms();
                    will(returnValue(null));
                }
            });
            try {
                utils.getAndValidateLoginForm(htmlPage);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "no forms found");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form list: Non-empty
     * - Form action: Invalid
     * Expects:
     * - Exception should be thrown saying the action attribute is null or doesn't match expected action value
     */
    @Test
    public void test_getAndValidateLoginForm_invalidFormAction() {
        try {
            final List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            mockery.checking(new Expectations() {
                {
                    one(htmlPage).getForms();
                    will(returnValue(forms));
                    one(form).getActionAttribute();
                }
            });
            try {
                utils.getAndValidateLoginForm(htmlPage);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "action attribute.+null or was not.+" + Constants.J_SECURITY_CHECK);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form list: Non-empty
     * - Form action: Valid
     * Expects:
     * - Should return appropriate form
     */
    @Test
    public void test_getAndValidateLoginForm_validFormAction() {
        try {
            final List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            forms.add(form2);
            forms.add(form3);
            mockery.checking(new Expectations() {
                {
                    one(htmlPage).getForms();
                    will(returnValue(forms));
                    one(form).getActionAttribute();
                    will(returnValue(Constants.J_SECURITY_CHECK));
                }
            });
            HtmlForm result = utils.getAndValidateLoginForm(htmlPage);
            assertEquals("HtmlForm result did not point to the expected form object.", form, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** assertPageContainsAtLeastOneForm **************************************/

    /**
     * Tests:
     * - Form list: null
     * Expects:
     * - Exception should be thrown saying no forms were found
     */
    @Test
    public void test_assertPageContainsAtLeastOneForm_nullList() {
        try {
            try {
                utils.assertPageContainsAtLeastOneForm(null);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "no forms found");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form list: Empty
     * Expects:
     * - Exception should be thrown saying no forms were found
     */
    @Test
    public void test_assertPageContainsAtLeastOneForm_emptyList() {
        try {
            List<HtmlForm> forms = new ArrayList<HtmlForm>();
            try {
                utils.assertPageContainsAtLeastOneForm(forms);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "no forms found");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form list: Single entry
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertPageContainsAtLeastOneForm_singleEntry() {
        try {
            List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            utils.assertPageContainsAtLeastOneForm(forms);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form list: Multiple entries
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertPageContainsAtLeastOneForm_multipleEntries() {
        try {
            List<HtmlForm> forms = new ArrayList<HtmlForm>();
            forms.add(form);
            forms.add(form2);
            forms.add(form3);
            utils.assertPageContainsAtLeastOneForm(forms);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateLoginPageFormAction **************************************/

    /**
     * Tests:
     * - Form action attribute: null
     * Expects:
     * - Exception should be thrown saying the action attribute is null or doesn't match expected action value
     */
    @Test
    public void test_validateLoginPageFormAction_nullActionAttribute() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(form).getActionAttribute();
                    will(returnValue(null));
                }
            });
            try {
                utils.validateLoginPageFormAction(form);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "action attribute.+null or was not.+" + Constants.J_SECURITY_CHECK);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form action attribute: Empty
     * Expects:
     * - Exception should be thrown saying the action attribute is null or doesn't match expected action value
     */
    @Test
    public void test_validateLoginPageFormAction_emptyActionAttribute() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(form).getActionAttribute();
                    will(returnValue(""));
                }
            });
            try {
                utils.validateLoginPageFormAction(form);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "action attribute.+null or was not.+" + Constants.J_SECURITY_CHECK);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form action attribute: Superstring of expected action value
     * Expects:
     * - Exception should be thrown saying the action attribute is null or doesn't match expected action value
     */
    @Test
    public void test_validateLoginPageFormAction_actionAttributeIsSuperstring() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(form).getActionAttribute();
                    will(returnValue(Constants.J_SECURITY_CHECK + "more"));
                }
            });
            try {
                utils.validateLoginPageFormAction(form);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "action attribute.+null or was not.+" + Constants.J_SECURITY_CHECK);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Form action attribute: Matches expected action value
     * Expects:
     * - Validation should succeed
     */
    @Test
    public void test_validateLoginPageFormAction_validActionAttribute() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(form).getActionAttribute();
                    will(returnValue(Constants.J_SECURITY_CHECK));
                }
            });
            utils.validateLoginPageFormAction(form);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** fillAndSubmitCredentialForm **************************************/

    /**
     * Tests:
     * - Username: null
     * - Password: null
     * Expects:
     * - Input fields should be successfully set and form should be submitted
     */
    @Test
    public void test_fillAndSubmitCredentialForm_nullUsername_nullPassword() {
        try {
            String username = null;
            String password = null;
            setInputValueExpectations(Constants.J_USERNAME, username);
            setInputValueExpectations(Constants.J_PASSWORD, password);
            getButtonAndSubmitExpectations(WebFormUtils.DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE);

            utils.fillAndSubmitCredentialForm(form, username, password);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Username: Non-empty
     * - Password: Non-empty
     * - Submit button is missing
     * Expects:
     * - ElementNotFoundException should be thrown as-is
     */
    @Test
    public void test_fillAndSubmitCredentialForm_formMissingElement() {
        try {
            String username = "user";
            String password = "password";
            setInputValueExpectations(Constants.J_USERNAME, username);
            setInputValueExpectations(Constants.J_PASSWORD, password);
            mockery.checking(new Expectations() {
                {
                    one(form).getInputByValue(WebFormUtils.DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE);
                    will(throwException(new ElementNotFoundException(WebFormUtils.DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE, "attributeName", "attributeValue")));
                }
            });
            try {
                utils.fillAndSubmitCredentialForm(form, username, password);
                fail("Should have thrown an exception but did not.");
            } catch (ElementNotFoundException e) {
                // Expected - should not wrap or re-throw the exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAndSetUsernameField **************************************/

    /**
     * Tests:
     * - Username: null
     * Expects:
     * - Input field should be successfully set
     */
    @Test
    public void test_getAndSetUsernameField_nullUsername() {
        try {
            String username = null;
            setInputValueExpectations(Constants.J_USERNAME, username);

            utils.getAndSetUsernameField(form, username);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Username: Non-empty
     * - Input with the specified name does not exist
     * Expects:
     * - ElementNotFoundException should be thrown as-is
     */
    @Test
    public void test_getAndSetUsernameField_usernameInputElementNotFound() {
        try {
            final String username = "some value";
            mockery.checking(new Expectations() {
                {
                    one(form).getInputsByName(Constants.J_USERNAME);
                    will(throwException(new ElementNotFoundException(Constants.J_USERNAME, "attributeName", username)));
                }
            });
            try {
                utils.getAndSetUsernameField(form, username);
                fail("Should have thrown an exception but did not.");
            } catch (ElementNotFoundException e) {
                // Expected - should not wrap or re-throw the exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAndSetPasswordField **************************************/

    /**
     * Tests:
     * - Password: null
     * Expects:
     * - Input field should be successfully set
     */
    @Test
    public void test_getAndSetPasswordField_nullPassword() {
        try {
            String password = null;
            setInputValueExpectations(Constants.J_PASSWORD, password);

            utils.getAndSetPasswordField(form, password);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Password: Non-empty
     * - Input with the specified name does not exist
     * Expects:
     * - ElementNotFoundException should be thrown as-is
     */
    @Test
    public void test_getAndSetPasswordField_passwordInputElementNotFound() {
        try {
            final String password = "some value";
            mockery.checking(new Expectations() {
                {
                    one(form).getInputsByName(Constants.J_PASSWORD);
                    will(throwException(new ElementNotFoundException(Constants.J_PASSWORD, "attributeName", password)));
                }
            });
            try {
                utils.getAndSetPasswordField(form, password);
                fail("Should have thrown an exception but did not.");
            } catch (ElementNotFoundException e) {
                // Expected - should not wrap or re-throw the exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getAndSetInputField **************************************/

    /**
     * Tests:
     * - Input name: null
     * - Input value: null
     * Expects:
     * - Exception should be thrown saying the input name is null
     */
    @Test
    public void test_getAndSetInputField_nullInputName_nullValue() {
        try {
            String inputName = null;
            String value = null;
            try {
                utils.getAndSetInputField(form, inputName, value);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "input name is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Input name: Empty string
     * - Input value: null
     * Expects:
     * - Input field should be successfully set
     */
    @Test
    public void test_getAndSetInputField_emptyInputName_nullValue() {
        try {
            String inputName = "";
            String value = null;
            setInputValueExpectations(inputName, value);

            utils.getAndSetInputField(form, inputName, value);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Input name: Non-empty
     * - Input value: Non-empty
     * - Input with the specified name does not exist
     * Expects:
     * - ElementNotFoundException should be thrown as-is
     */
    @Test
    public void test_getAndSetInputField_elementNotFound() {
        try {
            final String inputName = "does not exist";
            final String value = "some value";
            mockery.checking(new Expectations() {
                {
                    one(form).getInputsByName(inputName);
                    will(throwException(new ElementNotFoundException(inputName, "attributeName", value)));
                }
            });
            try {
                utils.getAndSetInputField(form, inputName, value);
                fail("Should have thrown an exception but did not.");
            } catch (ElementNotFoundException e) {
                // Expected - should not wrap or re-throw the exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** submitForm **************************************/

    /**
     * Tests:
     * - Submit button value: null
     * Expects:
     * - Exception should be thrown saying the submit button value is null
     */
    @Test
    public void test_submitForm_nullSubmitButtonValue() {
        try {
            String submitButtonValue = null;
            try {
                Page result = utils.submitForm(form, submitButtonValue);
                fail("Should have thrown an exception but did not. Got result [" + ((result == null) ? null : result.getWebResponse().getContentAsString()));
            } catch (Exception e) {
                verifyException(e, "Cannot submit.+form.+submit button value is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Submit button value: Empty
     * Expects:
     * - Submission should succeed
     */
    @Test
    public void test_submitForm_emptySubmitButtonValue() {
        try {
            final String submitButtonValue = "";
            mockery.checking(new Expectations() {
                {
                    one(form).getInputByValue(submitButtonValue);
                    will(returnValue(submitButton));
                    one(submitButton).click();
                    will(returnValue(page));
                }
            });
            Page result = utils.submitForm(form, submitButtonValue);
            assertEquals("Page returned did not match the mocked value.", page, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Submit button value: Non-empty
     * - Submit button with the specified value does not exist
     * Expects:
     * - ElementNotFoundException should be thrown as-is
     */
    @Test
    public void test_submitForm_elementNotFound() {
        try {
            final String submitButtonValue = "mySubmitButton";
            mockery.checking(new Expectations() {
                {
                    one(form).getInputByValue(submitButtonValue);
                    will(throwException(new ElementNotFoundException(submitButtonValue, "attributeName", "attributeValue")));
                }
            });
            try {
                Page result = utils.submitForm(form, submitButtonValue);
                fail("Should have thrown an exception but did not. Got result [" + ((result == null) ? null : result.getWebResponse().getContentAsString()));
            } catch (ElementNotFoundException e) {
                // Expected - should not wrap or re-throw the exception
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    void setInputValueExpectations(final String inputName, final String value) {
        final List<HtmlInput> htmlInputs = new ArrayList<HtmlInput>();
        htmlInputs.add(htmlInput);
        mockery.checking(new Expectations() {
            {
                one(form).getInputsByName(inputName);
                will(returnValue(htmlInputs));
                one(htmlInput).setValueAttribute(value);
            }
        });
    }

    void getButtonAndSubmitExpectations(final String submitButtonValue) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(form).getInputByValue(submitButtonValue);
                will(returnValue(submitButton));
                one(submitButton).click();
                will(returnValue(page));
            }
        });
    }

}
