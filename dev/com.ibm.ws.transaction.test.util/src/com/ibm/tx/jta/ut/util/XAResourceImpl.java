/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.impl.LocalTIDTable;
import com.ibm.ws.Transaction.JTA.Util;

public class XAResourceImpl implements XAResource, Serializable {
    static final long serialVersionUID = -2141508727147091254L;

    protected static ConcurrentHashMap<Integer, XAResourceData> _resources = new ConcurrentHashMap<Integer, XAResourceData>();

    protected static StateKeeper stateKeeper;

    static class StateKeeperImpl implements StateKeeper {

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.tx.jta.ut.util.StateKeeper#dumpState()
         */
        @Override
        public void dumpState() {
            printState();
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            try {
                System.out.println("Dumping state to " + STATE_FILE);
                fos = new FileOutputStream(STATE_FILE);
                if (DEBUG_OUTPUT)
                    System.out.println("Dumping state to: "
                                       + System.getProperty("user.dir"));
                oos = new ObjectOutputStream(fos);

                for (XAResourceData xares : _resources.values()) {
                    if (DEBUG_OUTPUT)
                        System.out.println("Dump Object: " + xares + ", with key: "
                                           + xares.key + ", with xid: " + xares.getXid());
                    oos.writeObject(xares);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (oos != null) {
                        oos.flush();
                        oos.close();
                    }
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.tx.jta.ut.util.StateKeeper#loadState()
         */
        @Override
        public int loadState() {
            new Throwable("com.ibm.tx.jta.ut.util.StateKeeper#loadState()").printStackTrace(System.out);;
            int resourceCount = 0;
            _commitSequence.set(0);
            ObjectInputStream ois = null;
            int resKey = 0;
            FileInputStream fos = null;

            try {
                fos = new FileInputStream(STATE_FILE);

                ois = new ObjectInputStream(fos);

                while (true) {
                    final XAResourceData xares = (XAResourceData) ois.readObject();
                    resKey = xares.key;
                    _resources.put(resKey, xares);
                    if (resKey >= _nextKey.get()) {
                        _nextKey.set(resKey + 1);
                    }
                    resourceCount++;
                }
            } catch (EOFException e) {
                System.out.println("Loaded " + resourceCount + " resources");
            } catch (FileNotFoundException e) {
                System.out.println("Loaded " + resourceCount + " resources");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ois != null)
                        ois.close();
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return resourceCount;
        }
    }

    static {
        stateKeeper = new StateKeeperImpl();
    }

    private static List<XAEvent> _XAEvents = Collections.synchronizedList(new ArrayList<XAEvent>());

    protected Integer _key;

    private static AtomicInteger _commitSequence = new AtomicInteger(0);

    public static AtomicInteger _nextKey = new AtomicInteger(0);

    public static final int RUNTIME_EXCEPTION = -1000;
    public static final int DIE = -2000;
    public static final int SLEEP_COMMIT = -3000;
    public static final int SLEEP_ROLLBACK = -4000;
    public static final int RETURN_TRUE = -5000;
    public static final int RETURN_FALSE = -6000;

    public static final int NOT_STARTED = 1;
    public static final int COMMITTED = 2;
    public static final int ENDED = 4;
    public static final int STARTED = 8;
    public static final int PREPARED = 16;
    public static final int ROLLEDBACK = 32;
    public static final int FORGOTTEN = 64;
    public static final int RECOVERED = 128;
    public static final int COMMITTED_ONE_PHASE = 256;

    protected static String STATE_FILE = "XAResourceData.dat";

    // This variable may be set to true to allow more chatty output.
    protected static boolean DEBUG_OUTPUT = true;

    private static boolean _stateLoaded;

    public static final int DIRECTION_COMMIT = 0, DIRECTION_ROLLBACK = 1, DIRECTION_EITHER = 2;

    public int getExpectedDirection() {
        return self().getExpectedDirection();
    }

    public XAResourceImpl setExpectedDirection(int expectedDirection) {
//        self().setExpectedDirection(expectedDirection);

        return this;
    }

    public static synchronized void setStateFile(String path) {
        System.out.println("setStateFile: " + path);
        STATE_FILE = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof XAResourceImpl) {
            if (_key.equals(((XAResourceImpl) o)._key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return _key.hashCode();
    }

    public static int transactionCount() {
        final Object[] trans = LocalTIDTable.getAllTransactions();

        if (trans == null) {
            return 0;
        } else {
            return trans.length;
        }
    }

    public static int committedCount() {
        int committedCount = 0;
        for (XAResourceData res : _resources.values()) {

            if (res.inState(COMMITTED)) {
                System.out.println("commitedCount: Resource " + res.key + " is in state COMMITTED");
                committedCount++;
            } else {
                System.out.println("commitedCount: Resource " + res.key + " is not in state COMMITTED");
            }
        }
        return committedCount;
    }

    public class XAResourceData implements Serializable {
        private static final long serialVersionUID = -253646295143893358L;

        public final int key;
        private UUID RM;
        private int prepareAction = XAResource.XA_OK;
        private int rollbackAction = XAResource.XA_OK;
        private int commitAction = XAResource.XA_OK;
        private int endAction = XAResource.XA_OK;
        private int startAction = XAResource.XA_OK;
        private int forgetAction = XAResource.XA_OK;
        private int recoverAction = XAResource.XA_OK;
        private int setTransactionTimeoutAction = RETURN_TRUE;
        private int expectedDirection = DIRECTION_EITHER;
        private int commitRepeatCount;
        private int rollbackRepeatCount;
        private int forgetRepeatCount;
        private int recoverRepeatCount;
        private int statusDuringCommit;
        private int statusDuringRollback;
        private int statusDuringPrepare;
        private int rollbackCount;
        private int forgetCount;
        private boolean heuristic;
        private int _sleepTime;
        private Xid _xid;
        private int _state = NOT_STARTED;
        private boolean busyInLongRunningQuery;
        private boolean queryAborted;
        private boolean commitSuicide = true;
        private final String stateFile;

        URL doomedServer;

        private int _commitOrder;

        public void setDoomedServer(URL url) {
            doomedServer = url;
        }

        public int getExpectedDirection() {
            return expectedDirection;
        }

        public void setExpectedDirection(int expectedDirection) {
            StringBuffer sb = new StringBuffer("setExpectedDirection(")
                            .append(_key)
                            .append(", ");
                            
            if (expectedDirection == DIRECTION_COMMIT) {
                sb.append("commit");
            } else if (expectedDirection == DIRECTION_ROLLBACK) {
                sb.append("rollback");
            } else if (expectedDirection == DIRECTION_EITHER) {
                sb.append("either");
            }
            sb.append(")");
            System.out.println(sb.toString());

            this.expectedDirection = expectedDirection;
        }

        public URL getDoomedServer() {
            return doomedServer;
        }

        public void setCommitSuicide(boolean b) {
            commitSuicide = b;
        }

        public boolean getCommitSuicide() {
            return commitSuicide;
        }

        public void setSleepTime(int sleepTime) {
            _sleepTime = sleepTime;
        }

        public int getSleepTime() {
            return _sleepTime;
        }

        public Xid getXid() {
            return _xid;
        }

        public void setXid(Xid xid) {
            System.out.println("setXid(" + _key + ", " + xid + ")");

            _xid = new TestXidImpl(xid);
        }

        public int getState() {
            return _state;
        }

        public void setState(int state) {
            _state |= state;
        }

        public XAResourceData(int i) {
            key = i;
            RM = UUID.randomUUID();
            stateFile = STATE_FILE;
        }

        public int getCommitAction() {
            return commitAction;
        }

        public void setCommitAction(int commitAction) {
            System.out.println("setCommitAction(" + _key + ", " + actionFormatter(commitAction) + ")");
            this.commitAction = commitAction;
        }

        public int getSetTransactionTimeoutAction() {
            return setTransactionTimeoutAction;
        }

        public void setSetTransactionTimeoutAction(int action) {
            setTransactionTimeoutAction = action;
        }

        public int getPrepareAction() {
            return prepareAction;
        }

        public void setPrepareAction(int prepareAction) {
            System.out.println("setPrepareAction(" + _key + ", " + actionFormatter(prepareAction) + ")");
            this.prepareAction = prepareAction;
        }

        public UUID getRM() {
            return RM;
        }

        public void setRM(UUID rm) {
            RM = rm;
        }

        public int getRollbackAction() {
            return rollbackAction;
        }

        public void setRollbackAction(int rollbackAction) {
            System.out.println("setRollbackAction(" + _key + ", " + actionFormatter(rollbackAction) + ")");
            this.rollbackAction = rollbackAction;
        }

        public int getRecoverAction() {
            return recoverAction;
        }

        public void setRecoverAction(int recoverAction) {
            System.out.println("setRecoverAction(" + _key + ", " + actionFormatter(recoverAction) + ")");
            this.recoverAction = recoverAction;
        }

        public int getStatusDuringCommit() {
            return statusDuringCommit;
        }

        public void setStatusDuringCommit(int statusDuringCommit) {
            this.statusDuringCommit = statusDuringCommit;
        }

        public int getStatusDuringRollback() {
            return statusDuringRollback;
        }

        public void setStatusDuringRollback(int statusDuringRollback) {
            this.statusDuringRollback = statusDuringRollback;
        }

        public int getStatusDuringPrepare() {
            return statusDuringPrepare;
        }

        public void setStatusDuringPrepare(int statusDuringPrepare) {
            this.statusDuringPrepare = statusDuringPrepare;
        }

        public boolean isForgotten() {
            return (_state & FORGOTTEN) != 0;
        }

        public boolean isHeuristic() {
            return heuristic;
        }

        public void setHeuristic(boolean heuristic) {
            this.heuristic = heuristic;
        }

        public int getEndAction() {
            return endAction;
        }

        public void setEndAction(int endAction) {
            this.endAction = endAction;
        }

        public int getCommitRepeatCount() {
            return commitRepeatCount;
        }

        public void setCommitRepeatCount(int commitRepeatCount) {
            this.commitRepeatCount = commitRepeatCount;
        }

        public int getRecoverRepeatCount() {
            return recoverRepeatCount;
        }

        public void setRecoverRepeatCount(int recoverRepeatCount) {
            System.out.println("setRecoverRepeatCount(" + _key + ", " + recoverRepeatCount + ")");
            this.recoverRepeatCount = recoverRepeatCount;
        }

        public int getForgetRepeatCount() {
            return forgetRepeatCount;
        }

        public void setForgetRepeatCount(int forgetRepeatCount) {
            this.forgetRepeatCount = forgetRepeatCount;
        }

        public int getForgetAction() {
            return forgetAction;
        }

        public void setForgetAction(int forgetAction) {
            this.forgetAction = forgetAction;
        }

        public int getRollbackCount() {
            return rollbackCount;
        }

        public void setRollbackCount(int rollbackCount) {
            this.rollbackCount = rollbackCount;
        }

        public int getRollbackRepeatCount() {
            return rollbackRepeatCount;
        }

        public void setRollbackRepeatCount(int rollbackRepeatCount) {
            this.rollbackRepeatCount = rollbackRepeatCount;
        }

        public int getStartAction() {
            return startAction;
        }

        public void setStartAction(int startAction) {
            this.startAction = startAction;
        }

        public int getForgetCount() {
            return forgetCount;
        }

        public void setForgetCount(int forgetCount) {
            this.forgetCount = forgetCount;
        }

        public boolean inState(int state) {
            return (_state & state) != 0;
        }

        public void setAmBusyInLongRunningQuery(boolean busy) {
            this.busyInLongRunningQuery = busy;
        }

        public boolean isBusyInLongRunningQuery() {
            return this.busyInLongRunningQuery;
        }

        public void setQueryAborted() {
            this.queryAborted = true;
        }

        public boolean isQueryAborted() {
            return this.queryAborted;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("Resource: " + key + "\n");
            sb.append("State: " + stateFormatter(_state));
            sb.append("\nXid: " + _xid);
            sb.append("\nCommit order: " + _commitOrder);

/*
 * private UUID RM;
 * private int prepareAction = XAResource.XA_OK;
 * private int rollbackAction = XAResource.XA_OK;
 * private int commitAction = XAResource.XA_OK;
 * private int endAction = XAResource.XA_OK;
 * private int startAction = XAResource.XA_OK;
 * private int forgetAction = XAResource.XA_OK;
 * private int recoverAction = XAResource.XA_OK;
 * private int setTransactionTimeoutAction = RETURN_TRUE;
 * private int commitRepeatCount;
 * private int rollbackRepeatCount;
 * private int forgetRepeatCount;
 * private int recoverRepeatCount;
 * private int statusDuringCommit;
 * private int statusDuringRollback;
 * private int statusDuringPrepare;
 * private int rollbackCount;
 * private int forgetCount;
 * private boolean heuristic;
 * private int _sleepTime;
 * private Xid _xid;
 * private boolean busyInLongRunningQuery;
 * private boolean queryAborted;
 * private boolean commitSuicide = true;
 */
            return sb.toString();
        }

        private String stateFormatter(int state) {
            StringBuffer sb = new StringBuffer("NOT STARTED");

            if (inState(STARTED)) {
                sb.append(", STARTED");
            }
            if (inState(ENDED)) {
                sb.append(", ENDED");
            }
            if (inState(PREPARED)) {
                sb.append(", PREPARED");
            }
            if (inState(COMMITTED)) {
                sb.append(", COMMITTED");
            }
            if (inState(ROLLEDBACK)) {
                sb.append(", ROLLEDBACK");
            }
            if (inState(FORGOTTEN)) {
                sb.append(", FORGOTTEN");
            }
            if (inState(RECOVERED)) {
                sb.append(", RECOVERED");
            }
            if (inState(COMMITTED_ONE_PHASE)) {
                sb.append(", COMMITTED_ONE_PHASE");
            }
            
            return sb.toString();
        }

        class TestXidImpl implements Xid, Serializable {

            /**
            	 *
            	 */
            private static final long serialVersionUID = 1747768416841464440L;

            //
            // The format identifier for the Xid. A value of -1 indicates
            // that the NULL Xid
            //
            final protected int _formatId;

            //
            // Holds the Global Transaction ID for this Xid
            //
            final protected byte[] _gtrid;

            //
            // Holds the Branch Qualifier for this Xid
            //
            final protected byte[] _bqual;

            /**
             * Xid copy constructor that is used during recovery
             * and only used for Xids with our formatId.
             */
            public TestXidImpl(Xid xid) {
                this._formatId = xid.getFormatId();
                this._gtrid = Util.duplicateByteArray(xid.getGlobalTransactionId());
                this._bqual = Util.duplicateByteArray(xid.getBranchQualifier());
            }

            @Override
            public byte[] getBranchQualifier() {
                return _bqual;
            }

            @Override
            public int getFormatId() {
                return _formatId;
            }

            @Override
            public byte[] getGlobalTransactionId() {
                return _gtrid;
            }

            @Override
            public String toString() {
                return "Format id: " + Integer.toHexString(_formatId) + "\n" +
                       "\tGtrid: " + Util.toHexString(_gtrid) + "\n" +
                       "\tBqual: " + Util.toHexString(_bqual);
            }
        }

        public int getCommitOrder() {
            return _commitOrder;
        }

        /**
         * @param commitOrder
         */
        public void setCommitOrder(int commitOrder) {
            _commitOrder = commitOrder;
        }
    }

    public XAResourceImpl() {
        synchronized (_resources) {
            _key = _nextKey.getAndIncrement();
            System.out.println("Constructing XAResourceImpl: " + _key);
            _resources.put(_key, new XAResourceData(_key));
        }
    }

    public XAResourceImpl(int i) {
        synchronized (_resources) {
            _key = i;
            if (!_resources.containsKey(i)) {
                System.out.println("Constructing XAResourceImpl: " + _key);
                _resources.put(_key, new XAResourceData(_key));
                if (i >= _nextKey.get()) {
                    _nextKey.set(i + 1);
                }
            } else {
                System.out.println("XAResourceImpl exists already: " + _key);
            }
        }
    }

    public static XAResourceImpl getXAResourceImpl(int key) {
        return new XAResourceImpl(key);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        System.out.println("commit(" + _key + ", " + xid + ", " + onePhase
                           + ")");

        self().setCommitOrder(_commitSequence.incrementAndGet());

        System.out.println("Commit order is " + self().getCommitOrder());

        _XAEvents.add(new XAEvent(XAEventCode.COMMIT, _key));

        try {
            if (DEBUG_OUTPUT) {
                System.out.println("commit working against " + self());
                System.out.println("commit using TM "
                                   + TransactionManagerFactory.getTransactionManager());
            }
            self().setStatusDuringCommit(
                                         TransactionManagerFactory.getTransactionManager()
                                                         .getStatus());
        } catch (SystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final int commitAction = self().getCommitAction();
        if (commitAction != XAResource.XA_OK) {
            final int repeatCount = self().getCommitRepeatCount();
            self().setCommitRepeatCount(repeatCount - 1);
            if (repeatCount >= 0) {
                switch (commitAction) {
                    case RUNTIME_EXCEPTION:
                        throw new RuntimeException();

                    case DIE:
                        System.out.println("Calling DIE on commit(" + _key + ", "
                                           + xid + ", " + onePhase + ")");
                        try {
                            killDoomedServers(true);
                        } catch (SecurityException se) {
                            System.out.println("Caught Security Exc: " + se
                                               + " on commit(" + _key + ", " + xid + ", "
                                               + onePhase + ")");
                        }

                    case SLEEP_COMMIT:
                        try {
                            Thread.sleep(self().getSleepTime());
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;

                    default:
                        throw new XAException(commitAction);
                }
            }
        }

        if (self().getExpectedDirection() != DIRECTION_COMMIT && self().getExpectedDirection() != DIRECTION_EITHER) {
            System.out.println("Commit is not the expected direction! Test failed.");
            throw new XAException("Test failed because of wrong direction. Commit is not the expected direction.");
        }

        setState(COMMITTED);
        if (onePhase) {
            setState(COMMITTED_ONE_PHASE);
        }
        System.out.println("committed(" + _key + ", " + xid + ", " + onePhase
                           + ")");
    }

    private void killDoomedServers(boolean dumpState) {
        URL doomedServer = self().getDoomedServer();
        if (doomedServer != null) {
            // Kill the other guy
            System.out.println("killDoomedServers: " + doomedServer.toString());
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) doomedServer.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                // Get a connection to the server & retrieve the output stream
                conn.connect();
                //OutputStream out = conn.getOutputStream();
                int response_code = conn.getResponseCode();
                if (response_code == HttpURLConnection.HTTP_OK) {
                    conn.getInputStream();
                    System.out.println("Should not reach here.");
                }
            } catch (Exception e) {
                //we want to get here!!
                e.printStackTrace();
                if (conn != null)
                    conn.disconnect();
                System.out.println("Get expected exception in killServer " + e.toString());
            }

        }

        if (self().getCommitSuicide()) {
//			Log.info(getClass(), "killDoomedServers", "Uh oh");
            if (dumpState) {
                dumpState();
            }
            Runtime.getRuntime().halt(DIE);
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        System.out.println("end(" + _key + ", " + xid + ", " + flags + ")");

        _XAEvents.add(new XAEvent(XAEventCode.END, _key));

        final int endAction = self().getEndAction();
        if (endAction != XAResource.XA_OK) {
            switch (endAction) {
                case RUNTIME_EXCEPTION:
                    throw new RuntimeException();

                case DIE:
                    killDoomedServers(false);

                default:
                    throw new XAException(endAction);
            }
        }

        setState(ENDED);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        System.out.println("forget(" + _key + ", " + xid + ")");

        _XAEvents.add(new XAEvent(XAEventCode.FORGET, _key));

        final int forgetAction = self().getForgetAction();
        if (forgetAction != XAResource.XA_OK) {
            final int repeatCount = self().getForgetRepeatCount();
            self().setForgetRepeatCount(repeatCount - 1);
            if (repeatCount >= 0) {
                switch (forgetAction) {
                    case RUNTIME_EXCEPTION:
                        throw new RuntimeException();

                    case DIE:
                        killDoomedServers(false);

                    default:
                        throw new XAException(forgetAction);
                }
            }
        }

        setState(FORGOTTEN);
        self().setForgetCount(self().getForgetCount() + 1);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        if (xares instanceof XAResourceImpl) {
            // System.out.println("isSameRM(" + self().getRM() + ", " +
            // ((XAResourceImpl)xares).getRM() + ")");
            return (self().getRM().equals(((XAResourceImpl) xares).getRM()));
        }

        return false;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        System.out.println("prepare(" + _key + ", " + xid + ") = "
                           + actionFormatter(self().getPrepareAction()));

        _XAEvents.add(new XAEvent(XAEventCode.PREPARE, _key));

        try {
            self().setStatusDuringPrepare(
                                          TransactionManagerFactory.getTransactionManager()
                                                          .getStatus());
        } catch (SystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        switch (self().getPrepareAction()) {
            case RUNTIME_EXCEPTION:
                throw new RuntimeException();

            case XAResource.XA_OK:
            case XAResource.XA_RDONLY:
                setState(PREPARED);
                return self().getPrepareAction();

            case DIE:
                killDoomedServers(true);

            case SLEEP_COMMIT:
                try {
                    Thread.sleep(self().getSleepTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }

                setState(PREPARED);
                return XAResource.XA_OK;

            case SLEEP_ROLLBACK:
                try {
                    Thread.sleep(self().getSleepTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }

                setState(ROLLEDBACK);
                throw new XAException(XAException.XA_RBROLLBACK);

            default:
                throw new XAException(self().getPrepareAction());
        }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        System.out.println("recover(" + _key + ", " + flag + ")");

        if (self() == null) {
            if (DEBUG_OUTPUT)
                System.out
                                .println("No XARecoveryData - returning null xid array");
            return null;
        }
        
        System.out.println("XAResource state in recover(): "+self());

        _XAEvents.add(new XAEvent(XAEventCode.RECOVER, _key));

        final int recoverAction = self().getRecoverAction();
        if (recoverAction != XAResource.XA_OK) {
            final int repeatCount = self().getRecoverRepeatCount();
            self().setRecoverRepeatCount(repeatCount - 1);
            if (repeatCount > 0) {
                switch (recoverAction) {
                    case RUNTIME_EXCEPTION:
                        throw new RuntimeException();

                    case DIE:
                        killDoomedServers(true);

                    default:
                        throw new XAException(recoverAction);
                }
            }
        }

        setState(RECOVERED);

        if (self().inState(PREPARED) && !self().inState(COMMITTED)
            && !self().inState(ROLLEDBACK)) {
            return new Xid[] { getXid() };
        }

        return null;
    }

    private Xid getXid() {
        return self().getXid();
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        System.out.println("rollback(" + _key + ", " + xid + ")");

        if (self().getExpectedDirection() != DIRECTION_ROLLBACK && self().getExpectedDirection() != DIRECTION_EITHER) {
            System.out.println("Rollback is not the expected direction! Test failed.");
            throw new XAException("Test failed because of wrong direction. Rollback is not the expected direction.");
        }

        _XAEvents.add(new XAEvent(XAEventCode.ROLLBACK, _key));

        try {
            self().setStatusDuringRollback(
                                           TransactionManagerFactory.getTransactionManager()
                                                           .getStatus());
        } catch (SystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        self().setRollbackCount(self().getRollbackCount() + 1);

        if (self().getRollbackAction() != XAResource.XA_OK) {
            final int repeatCount = self().getRollbackRepeatCount();
            self().setRollbackRepeatCount(repeatCount - 1);
            if (repeatCount >= 0) {
                final int rollbackAction = self().getRollbackAction();

                switch (rollbackAction) {
                    case RUNTIME_EXCEPTION:
                        throw new RuntimeException();

                    case DIE:
                        killDoomedServers(true);

                    case SLEEP_ROLLBACK:
                        try {
                            Thread.sleep(self().getSleepTime());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        throw new XAException(rollbackAction);
                }
            }
        }

        setState(ROLLEDBACK);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        _XAEvents.add(new XAEvent(XAEventCode.SETTRANSACTIONTIMEOUT, _key));

        final int setTransactionTimeoutAction = self()
                        .getSetTransactionTimeoutAction();
        switch (setTransactionTimeoutAction) {
            case RETURN_TRUE:
                System.out.println(this.getClass().getCanonicalName()
                                   + ".setTransactionTimeout(" + seconds + "): TRUE");
                return true;

            case RETURN_FALSE:
                System.out.println(this.getClass().getCanonicalName()
                                   + ".setTransactionTimeout(" + seconds + "): FALSE");
                return false;

            default:
                final XAException e = new XAException(setTransactionTimeoutAction);
                System.out.println(this.getClass().getCanonicalName()
                                   + ".setTransactionTimeout(" + seconds + "): "
                                   + e.toString());
                throw e;
        }
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        System.out.println("start(" + _key + ", " + xid + ", " + flags + ")");
        _XAEvents.add(new XAEvent(XAEventCode.START, _key));

        setState(STARTED);
        self().setXid(xid);

        final int startAction = self().getStartAction();
        if (startAction != XAResource.XA_OK) {
            switch (startAction) {
                case RUNTIME_EXCEPTION:
                    throw new RuntimeException();

                case DIE:
                    killDoomedServers(false);

                default:
                    throw new XAException(startAction);
            }
        }
    }

    private void setState(int state) {
        self().setState(state);
    }

    public XAResourceImpl setPrepareAction(int action) {
        self().setPrepareAction(action);

        switch (action) {
            case XAException.XA_HEURCOM:
            case XAException.XA_HEURHAZ:
            case XAException.XA_HEURMIX:
            case XAException.XA_HEURRB:
                self().setHeuristic(true);
                break;

            default:
                break;
        }

        return this;
    }

    public XAResourceImpl setRollbackAction(int action) {
        self().setRollbackAction(action);

        switch (action) {
            case XAException.XA_HEURCOM:
            case XAException.XA_HEURHAZ:
            case XAException.XA_HEURMIX:
            case XAException.XA_HEURRB:
                self().setHeuristic(true);
                break;

            default:
                break;
        }

        return this;
    }

    public XAResourceImpl setEndAction(int action) {
        self().setEndAction(action);

        return this;
    }

    public XAResourceImpl setStartAction(int action) {
        self().setStartAction(action);

        return this;
    }

    public XAResourceImpl setCommitSuicide(boolean b) {
        self().setCommitSuicide(b);

        return this;
    }

    public XAResourceImpl setForgetAction(int action) {
        self().setForgetAction(action);

        return this;
    }

    public XAResourceImpl setRecoverAction(int action) {
        self().setRecoverAction(action);

        return this;
    }

    public XAResourceImpl setCommitAction(int action) {
        self().setCommitAction(action);

        switch (action) {
            case XAException.XA_HEURCOM:
            case XAException.XA_HEURHAZ:
            case XAException.XA_HEURMIX:
            case XAException.XA_HEURRB:
                self().setHeuristic(true);
                break;

            default:
                break;
        }

        return this;
    }

    public XAResourceImpl setSetTransactionTimeoutAction(int action) {
        self().setSetTransactionTimeoutAction(action);

        return this;
    }

    public XAResourceImpl setCommitRepeatCount(int repeat) {
        self().setCommitRepeatCount(repeat);
        return this;
    }

    public int getCommitRepeatCount() {
        return self().getCommitRepeatCount();
    }

    public XAResourceImpl setRecoverRepeatCount(int repeat) {
        self().setRecoverRepeatCount(repeat);
        return this;
    }

    public int getRecoverRepeatCount() {
        return self().getRecoverRepeatCount();
    }

    public XAResourceImpl setRollbackRepeatCount(int repeat) {
        self().setRollbackRepeatCount(repeat);
        return this;
    }

    public int getRollbackCount() {
        return self().getRollbackCount();
    }

    public int getForgetCount() {
        return self().getForgetCount();
    }

    public XAResourceImpl setForgetRepeatCount(int repeat) {
        self().setForgetRepeatCount(repeat);
        return this;
    }

    public int getForgetRepeatCount() {
        return self().getForgetRepeatCount();
    }

    public int getStatusDuringPrepare() {
        return self().getStatusDuringPrepare();
    }

    public int getStatusDuringRollback() {
        return self().getStatusDuringRollback();
    }

    public int getStatusDuringCommit() {
        return self().getStatusDuringCommit();
    }

    public UUID getRM() {
        return self().getRM();
    }

    public void setRM(UUID rm) {
        self().setRM(rm);
    }

    public boolean inState(int state) {
        return self().inState(state);
    }

    public int getCommitOrder() {
        return self().getCommitOrder();
    }

    /********* NEW METHODS HERE *********/

    public void setAmBusyInLongRunningQuery(boolean busy) {
        self().setAmBusyInLongRunningQuery(busy);
    }

    public boolean isBusyInLongRunningQuery() {
        boolean isBusy = self().isBusyInLongRunningQuery();
        return isBusy;
    }

    public void setQueryAborted() {
        self().setQueryAborted();
    }

    public boolean isQueryAborted() {
        boolean aborted = self().isQueryAborted();
        return aborted;
    }

    public void simulateLongRunningQuery(int queryLength) {
        if (DEBUG_OUTPUT)
            System.out.println("simulateLongRunningQuery(" + _key + ", "
                               + queryLength + ")");

        // DateFormat df = DateFormat.getDateInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy:HH:mm:ss");

        // Wait on state change in XAResource
        setAmBusyInLongRunningQuery(true);

        LongSQLQuerySim sqlSim = new LongSQLQuerySim(queryLength * 1000);
        sqlSim.start();

        Date date = new Date();
        if (DEBUG_OUTPUT)
            System.out
                            .println("simulateLongRunningQuery: Back from call to simulate a long duration SQL query with object, "
                                     + sqlSim + ", at: " + df.format(date));

        synchronized (sqlSim) {
            try {
                date = new Date();
                if (sqlSim.isBusy()) {
                    if (DEBUG_OUTPUT)
                        System.out
                                        .println("Servlet: Start waiting for simulated long query to complete its work at: "
                                                 + df.format(date));
                    sqlSim.wait();
                } else {
                    if (DEBUG_OUTPUT)
                        System.out
                                        .println("Servlet: Long SQL query has already finished at: "
                                                 + df.format(date));
                }
            } catch (InterruptedException iex) {
                date = new Date();
                if (DEBUG_OUTPUT)
                    System.out
                                    .println("Servlet: InterruptedException: simulated long query interrupted at: "
                                             + df.format(date));
            }
        }
    }

    /********* EOF NEW METHODS HERE *********/

    public static boolean allInState(int state) {
        for (XAResourceData res : _resources.values()) {
            if (!res.inState(state)) {
                return false;
            }
        }

        return true;
    }

    public static void printState() {

        StringBuffer sb = new StringBuffer("Resources:\n");
        for (int i = 0; i < _resources.size(); i++) {
            sb.append(_resources.get(i)).append("\n");
        }

        sb.append("XA History: ");

        boolean needComma = false;
        synchronized (_XAEvents) {
            for (XAEvent event : _XAEvents) {
                if (needComma) {
                    sb.append(", ");
                } else {
                    needComma = true;
                }

                sb.append(event);
            }
        }

        System.out.println(sb.toString());
    }

    public static String checkAtomicity() {
        int committed = 0;
        int prepared = 0;
        int rolledback = 0;

        for (XAResourceData res : _resources.values()) {
            if (res.inState(COMMITTED)) {
                committed++;
            } else if (res.inState(ROLLEDBACK)) {
                rolledback++;
            } else if (res.inState(PREPARED)) {
                prepared++;
            } else {
                rolledback++;
            }
        }

        if (committed > 0) {
            if (committed != _resources.size()) {
                return "Unatomic";
            }

            return "allCommitted";
        }

        if (rolledback > 0) {
            if (rolledback != _resources.size()) {
                return "Unatomic";
            }

            return "allRollback";
        }

        if (prepared > 0) {
            return "Unatomic";
        }

        return "allRollback";
    }

    public static boolean checkForgotten() {
        for (XAResourceData res : _resources.values()) {
            if (res.isHeuristic() && !res.isForgotten()) {
                return false;
            }
        }

        return true;
    }

    public void setHeuristic() {
        self().setHeuristic(true);
    }

    public XAResourceImpl setSleepTime(int sleepTime) {
        self().setSleepTime(sleepTime);

        return this;
    }

    private XAResourceData self() {
        final XAResourceData xard = _resources.get(_key);

        if (xard == null) {
            System.out.println("self() is about to return null: " + _key);
            for (Integer key : _resources.keySet()) {
                System.out.println("Key: " + _key);
                System.out.println("Value: " + _resources.get(key));
            }
        }

        return xard;
    }

    static synchronized public int resourceCount() {
        if (!_stateLoaded) {
            loadState();
        }
        return _resources.size();
    }

    public static synchronized void clear() {
        _XAEvents.clear();
        _resources.clear();
        _nextKey.set(0);

        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

			@Override
			public Boolean run() {
				return new File(STATE_FILE).delete();
			}
        });
    }

    public static synchronized void dumpState() {
        stateKeeper.dumpState();

        // Defect 168553 - this string needs to be written in order for the
        // test infrastructure to see that the
        // server has been halted.
        System.out.println("Dump State: " + _resources.values().size());
    }

    public synchronized static int loadState() {
        final int numResources = stateKeeper.loadState();
        _stateLoaded = true;
        printState();
        return numResources;
    }

    public static void destroy(XAResourceImpl xaRes) {
        System.out.println("Destroying: " + xaRes._key);
    }

    public static List<XAEvent> getXAEvents() {
        return _XAEvents;
    }

    public enum XAEventCode {
        START, END, PREPARE, COMMIT, ROLLBACK, RECOVER, FORGET, SETTRANSACTIONTIMEOUT
    };

    public class XAEvent {
        private final XAEventCode _event;
        private final int _key;

        public XAEvent(XAEventCode event, int key) {
            _event = event;
            _key = key;
        }

        public boolean isSameAs(XAEventCode event, int key) {
            return event == _event && key == _key;
        }

        @Override
        public String toString() {
            return _event + " " + _key;
        }
    }

    public class LongSQLQuerySim extends Thread {
        private final int _busyperiod;

        public LongSQLQuerySim(int busyperiod) {
            _busyperiod = busyperiod;
        }

        public boolean isBusy() {
            boolean isBusy = isBusyInLongRunningQuery();
            return isBusy;
        }

        @Override
        public void run() {
            Date date = null;
            long startTime = 0;
            long endTime = 0;
            long timeDiff = 0;

            synchronized (this) {
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy:HH:mm:ss");
                try {
                    date = new Date();

                    startTime = date.getTime();
                    long count = _busyperiod / 250;
                    if (DEBUG_OUTPUT)
                        System.out.println("Simulated long query started at: "
                                           + df.format(date) + ", looping over " + count);
                    long counter = 0;
                    while (isBusyInLongRunningQuery() && counter < count) {
                        Thread.sleep(250);
                        counter++;
                    }
                }

                catch (InterruptedException e) {
                    if (DEBUG_OUTPUT)
                        System.out.println("Simulated long query interrupted");
                    e.printStackTrace();
                } finally {
                    date = new Date();
                    endTime = date.getTime();
                    timeDiff = (endTime - startTime) / 1000;
                    String elapsedString = "" + timeDiff;
                    if (DEBUG_OUTPUT)
                        System.out.println("Simulated long query finished at: "
                                           + df.format(date) + "after: " + elapsedString);

                    // Ensure that the parent object knows that the query is
                    // over
                    setAmBusyInLongRunningQuery(false);
                    // And notify any waiters
                    notify();
                }
            }
        }
    }

    private String actionFormatter(int action) {
        switch (action) {
            case XAException.XA_RBROLLBACK:
                return "ROLLBACK";
            case XAException.XA_RDONLY:
                return "READONLY";
            case RUNTIME_EXCEPTION:
                return "RUNTIME_EXCEPTION";
            case DIE:
                return "DIE";
            case SLEEP_COMMIT:
                return "SLEEP_COMMIT";
            case SLEEP_ROLLBACK:
                return "SLEEP_ROLLBACK";
            case RETURN_TRUE:
                return "RETURN_TRUE";
            case RETURN_FALSE:
                return "RETURN_FALSE";
            default:
                return "INVALID ACTION " + action;
        }
    }

    public void setDoomedServer(URL url) {
        self().setDoomedServer(url);
    }

    /**
     * @param stateFile
     */
    public static synchronized void loadState(String stateFile) {
        new Throwable("loadState: " + STATE_FILE).printStackTrace(System.out);
        setStateFile(stateFile);
        loadState();
    }
}
