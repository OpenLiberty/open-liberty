/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.common;

import java.rmi.Remote;

import org.omg.CosNaming.NameComponent;

public interface CosNamingChecker extends Remote {
    void checkNameServiceIsAvailable() throws Exception;

    String getNameServiceListingFromServer() throws Exception;

    void bindResolvable(NameComponent[] name) throws Exception;

    void bindResolvableThatThrows(RuntimeException exceptionToThrow, NameComponent[] name) throws Exception;
}
