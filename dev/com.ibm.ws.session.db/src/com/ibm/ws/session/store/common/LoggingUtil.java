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
 *  @(#) 1.2 SERV1/ws/code/web.session.core/src/com/ibm/ws/session/utils/LoggingUtil.java, WASCC.web.session.core, WASX.SERV1, o0901.11 6/4/08 10:12:52 [1/9/09 15:01:30]
 *
 * @(#)file   LoggingUtil.java
 * @(#)version   1.2
 * @(#)date      6/4/08
 *
 *COPYRIGHT_END*************************************************************/
package com.ibm.ws.session.store.common;

import java.util.logging.Logger;

public class LoggingUtil {

    /**
     * The resource bundle location
     */
    private static final String WASResourceBundleString = "com.ibm.ws.session.db.resources.WASSession";

    /**
     * The java.util.Logger for the logs coming from WAS specific files. You can specifically set this Logger by adding
     * the Session string to the end of the trace String. (ie. com.ibm.ws.session.WASSession=all)
     */
    public static final Logger SESSION_LOGGER_WAS = Logger.getLogger("com.ibm.ws.session.WASSession", WASResourceBundleString);

}
