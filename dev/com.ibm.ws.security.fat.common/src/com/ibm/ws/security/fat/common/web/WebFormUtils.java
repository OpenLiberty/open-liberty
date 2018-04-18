package com.ibm.ws.security.fat.common.web;

import java.util.List;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;

public class WebFormUtils {
    public static Class<?> thisClass = WebFormUtils.class;

    public static final String DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE = "Login";

    /**
     * Fills out the first form found in the provided login page with the specified credentials and submits the form. An exception
     * is thrown if the provided login page does not have any forms, if the first form does not have an action of
     * "j_security_check", or if the form is missing the appropriate username/password inputs or submit button.
     */
    public Page getAndSubmitLoginForm(HtmlPage loginPage, String username, String password) throws Exception {
        if (loginPage == null) {
            throw new Exception("Cannot submit login form because the provided HtmlPage object is null.");
        }
        HtmlForm form = getAndValidateLoginForm(loginPage);
        return fillAndSubmitCredentialForm(form, username, password);
    }

    HtmlForm getAndValidateLoginForm(HtmlPage loginPage) throws Exception {
        List<HtmlForm> forms = loginPage.getForms();
        assertPageContainsAtLeastOneForm(forms);
        HtmlForm form = forms.get(0);
        validateLoginPageFormAction(form);
        return form;
    }

    void assertPageContainsAtLeastOneForm(List<HtmlForm> forms) throws Exception {
        if (forms == null || forms.isEmpty()) {
            throw new Exception("There were no forms found in the provided HTML page. We most likely didn't reach the login page. Check the page content to ensure we arrived at the expected web page.");
        }
    }

    void validateLoginPageFormAction(HtmlForm loginForm) throws Exception {
        String formAction = loginForm.getActionAttribute();
        if (formAction == null || !formAction.equals(Constants.J_SECURITY_CHECK)) {
            throw new Exception("The action attribute [" + formAction + "] of the form to use was either null or was not \"" + Constants.J_SECURITY_CHECK
                    + "\" as expected. Check the page contents to ensure we reached the correct page.");
        }
    }

    Page fillAndSubmitCredentialForm(HtmlForm form, String username, String password) throws Exception {
        getAndSetUsernameField(form, username);
        getAndSetPasswordField(form, password);
        return submitForm(form, DEFAULT_LOGIN_SUBMIT_BUTTON_VALUE);
    }

    void getAndSetUsernameField(HtmlForm form, String username) throws Exception {
        getAndSetInputField(form, Constants.J_USERNAME, username);
    }

    void getAndSetPasswordField(HtmlForm form, String password) throws Exception {
        getAndSetInputField(form, Constants.J_PASSWORD, password);
    }

    void getAndSetInputField(HtmlForm form, String inputName, String value) throws Exception {
        String thisMethod = "getAndSetInputField";
        if (inputName == null) {
            throw new Exception("Cannot set the input field because the provided input name is null.");
        }
        HtmlInput input = form.getInputByName(inputName);
        Log.info(thisClass, thisMethod, "Found input field for name \"" + inputName + "\": " + input);
        Log.info(thisClass, thisMethod, "Setting input value to: " + value);
        input.setValueAttribute(value);
    }

    Page submitForm(HtmlForm form, String submitButtonValue) throws Exception {
        if (submitButtonValue == null) {
            throw new Exception("Cannot submit HTML form because the provided submit button value is null.");
        }
        HtmlInput submitButton = form.getInputByValue(submitButtonValue);
        return submitButton.click();
    }

}
