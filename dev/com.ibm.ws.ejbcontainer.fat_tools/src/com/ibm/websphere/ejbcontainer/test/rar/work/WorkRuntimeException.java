// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason       Description
//  --------   -------    ------       ---------------------------------
//  06/05/03   jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.work;

/**
 * @author jitang
 * 
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates.
 *         To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class WorkRuntimeException extends RuntimeException {

    public WorkRuntimeException() {
        super();
    }

    public WorkRuntimeException(String msg) {
        super(msg);
    }

    public WorkRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }

    public WorkRuntimeException(Throwable t) {
        super(t);
    }
}
