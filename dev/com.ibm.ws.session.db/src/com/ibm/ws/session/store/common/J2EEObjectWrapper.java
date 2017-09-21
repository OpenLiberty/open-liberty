/*COPYRIGHT_START***********************************************************
 *
 * IBM Confidential OCO Source Material
 * 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5724-J08 (C) COPYRIGHT International Business Machines Corp. 1997, 2012
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 *
 *   IBM DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
 *   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *   PURPOSE. IN NO EVENT SHALL IBM BE LIABLE FOR ANY SPECIAL, INDIRECT OR
 *   CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF
 *   USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 *   OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE
 *   OR PERFORMANCE OF THIS SOFTWARE.
 *
 *  @(#) 1.1 SERV1/ws/code/session.store/src/com/ibm/ws/session/store/common/J2EEObjectWrapper.java, WAS.session, WASX.SERV1, ff1146.05 10/13/06 16:01:16 [11/21/11 18:33:09]
 *
 * @(#)file   J2EEObjectWrapper.java
 * @(#)version   1.1
 * @(#)date      10/13/06
 *
 *COPYRIGHT_END*************************************************************/
package com.ibm.ws.session.store.common;

import java.io.Serializable;

/**
 * J2EE specification states that in addition to Serializable objects
 * HttpSession has to support J2EE objects(EJB, EJBHOME, Context & UserTransction) in
 * a distributable environment. As these J2EE objects are not serializable,
 * we use the serializable information of those Objects and store it form of
 * J2EEObjectWrapper when serializing to external store and we convert the J2EE wrapper
 * back to J2EE objects when deserializing
 * 
 * @version 1.0
 * @author srinivas@planetjava
 */
public class J2EEObjectWrapper implements Serializable {

    private Object serObj = null;
    private short ejbLocalType = 0; //cmd LIDB2282.12 - distinguishes EJBLocalHome and EJBLocalObject
    private static final long serialVersionUID = 2362252033211339042L;

    /**
     * Constructor for EJBWrapper.
     */
    public J2EEObjectWrapper() {
        super();
    }

    /**
     * Argument has to be a Serializable
     */
    public J2EEObjectWrapper(Object obj) {
        this.serObj = obj;
    }

    // cmd LIDB2282.12 new constructor that includes type
    public J2EEObjectWrapper(Object obj, short type) {
        this.serObj = obj;
        this.ejbLocalType = type;
    }

    /**
     * To get at the serializable
     * object
     */
    public Object getSerializableObject() {
        return serObj;
    }

    // cmd LIDB2282.12 new method getType()
    public int getEjbLocalType() {
        return ejbLocalType;
    }
}
