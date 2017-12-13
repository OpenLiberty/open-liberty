/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beanvalidationcdi.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

/**
 * Simple test CDI managed bean that can be injected into other CDI managed
 * beans.
 */
@ApplicationScoped
public class TestBean {

    public String getSomething() {
        return "something";
    }

    @ValidateOnExecution(type = ExecutableType.ALL)
    public String testMethodParameterValidation(@NotNull String x) {
        return x + "addedInMethod";
    }

    @Size(max = 10)
    @ValidateOnExecution(type = ExecutableType.ALL)
    public String testMethodReturnValidation(String x) {
        return x + "modified";
    }
}
