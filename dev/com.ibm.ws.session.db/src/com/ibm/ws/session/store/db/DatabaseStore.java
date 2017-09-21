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
 *  @(#) 1.5 SERV1/ws/code/session.store/src/com/ibm/ws/session/store/db/DatabaseStore.java, WAS.session, WASX.SERV1, ff1146.05 3/12/08 09:22:18 [11/21/11 18:33:09]
 *
 * @(#)file   DatabaseStore.java
 * @(#)version   1.5
 * @(#)date      3/12/08
 *
 *COPYRIGHT_END*************************************************************/

package com.ibm.ws.session.store.db;

import java.util.Vector;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.store.common.BackedHashMap;
import com.ibm.ws.session.store.common.BackedSession;
import com.ibm.ws.session.store.common.BackedStore;
import com.ibm.ws.session.store.common.LoggingUtil;

public class DatabaseStore extends BackedStore {

    //----------------------------------------
    // Private members.
    //----------------------------------------

    /*
     * For logging the CMVC file version once.
     */
    private static boolean _loggedVersion = false;
    private static final String methodClassName = "DatabaseStore";

    private static final int REMOTE_INVALIDATE = 0;
    private static final String methodNames[] = { "remoteInvalidate" };

    //----------------------------------------
    // Public constructor
    //----------------------------------------

    public DatabaseStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, DatabaseStoreService databaseStoreService) {
        super(smc, storeId, sc, storeHelper);
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            if (!_loggedVersion) {
                LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "", "CMVC Version 1.5 3/12/08 09:22:18");
                _loggedVersion = true;
            }
        }
        if (_smc.isUsingMultirow()) {
            _sessions = new DatabaseHashMapMR(this, smc, databaseStoreService);
        } else {
            _sessions = new DatabaseHashMap(this, smc, databaseStoreService);
        }
    }

    public DatabaseStore(SessionManagerConfig smc, String storeId, ServletContext sc, MemoryStoreHelper storeHelper, boolean isApplicationSessionStore, DatabaseStoreService databaseStoreService) {
        this(smc, storeId, sc, storeHelper, databaseStoreService);
        _isApplicationSessionStore = isApplicationSessionStore;
        ((BackedHashMap) _sessions).setIsApplicationSessionHashMap(isApplicationSessionStore);
    }

    //----------------------------------------
    // Public Methods
    //----------------------------------------

    // Set max inactive time to 0 in database so invalidator will will do inval
    // Then remove the session from cache
    public void remoteInvalidate(String sessionId, boolean backendUpdate) {
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodNames[REMOTE_INVALIDATE], "for app " + getId() + " id " + sessionId + " backendUpdate " + backendUpdate);
        }

        remoteInvalReceived = true;
        if (backendUpdate) {
            ((DatabaseHashMap) _sessions).setMaxInactToZero(sessionId, getId());
        }

        // now clean this session out of cache -- we do this even if not doing db inval
        Vector v = new Vector(1);
        v.add(sessionId);
        ((BackedHashMap) _sessions).handleDiscardedCacheItems(v.elements());

        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodNames[REMOTE_INVALIDATE], "for app " + getId() + " id " + sessionId);
        }
    }

    /*
     * @see com.ibm.ws.session.store.common.BackedStore#createSessionObject(java.lang.String)
     */
    public BackedSession createSessionObject(String sessionId) {
        return new DatabaseSession((DatabaseHashMap) _sessions, sessionId, _storeCallback);
    }

}
