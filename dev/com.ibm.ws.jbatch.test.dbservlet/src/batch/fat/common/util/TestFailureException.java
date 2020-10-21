/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package batch.fat.common.util;

public class TestFailureException extends Exception {

    /**  */
    private static final long serialVersionUID = 1L;

    public TestFailureException() {
        // TODO Auto-generated constructor stub
    }

    public TestFailureException(String message) {
        super(message);
    }

    public TestFailureException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public TestFailureException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}