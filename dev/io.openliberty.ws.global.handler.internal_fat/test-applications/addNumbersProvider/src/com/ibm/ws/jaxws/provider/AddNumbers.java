//
// @(#) 1.1 autoFVT/src/faultbean/wsfvt/server/AddNumbers.java, WAS.websvcs.fvt, WAS85.FVT, cf011231.01 6/18/10 12:10:42 [8/8/12 06:58:49]
//
// IBM Confidential OCO Source Material
// (C) COPYRIGHT International Business Machines Corp. 2009
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Change History:
// Date       UserId      Defect          Description
// ----------------------------------------------------------------------------
// 06/18/2010 jtnguyen    657385          New File

package com.ibm.ws.jaxws.provider;

@javax.jws.WebService(serviceName = "AddNumbers")
public class AddNumbers {

    public String addNumbers(int arg0, int arg1) throws AddNumbersException {
        if (arg0 + arg1 < 0) {
            throw new AddNumbersException("Sum is less than 0.");
        }
        return "Result = " + String.valueOf(arg0 + arg1);
    }

    public String addNegatives(int arg0, int arg1) throws AddNegativesException {
        // expect 2 negative numbers
        if (arg0 > 0 || arg1 > 0) {
            throw new AddNegativesException("Expected all negative numbers.");
        }
        return "Result = " + String.valueOf(arg0 + arg1);
    }

}
