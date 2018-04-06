// IBM Confidential OCO Source Material
// 5724-J08, 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 2018
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
package com.ibm.ws.cdi.impl.inject;

/**
 * According to the CDI spec a @Dependent scoped producer method may return null.
 * However that is the only time CDI can inject a null.
 * 
 * Therefore InjectInjectionObjectFactory will return this pusdo class to distinguish
 * between a null that came from @Dependent scoped producer method and any other null.
 *
 * After we have gone through the checks to ensure CDI is not trying to inject a null,
 * InjectableNull objects will be converted back into regular nulls.
 * 
 */
public class InjectableNull {
//intentionally empty.
}

