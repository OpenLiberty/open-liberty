/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.io.Serializable;
import java.util.Hashtable;

import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;

public class XAResourceFactoryImpl implements XAResourceFactory {
    public static final int RETURN_NULL = 0;
    private static XAResourceFactoryImpl _instance = new XAResourceFactoryImpl();
    private static boolean _stateLoaded;

    public XAResourceFactoryImpl() {
//        if (!_stateLoaded) {
//            _stateLoaded = true;
//            try {
//                XAResourceImpl.loadState();
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
//                e.printStackTrace();
//            }
//        }
    }

    public static XAResourceFactoryImpl instance() {
        return _instance;
    }

    private static final Hashtable<XAResourceInfoImpl, Object> _getActions = new Hashtable<XAResourceInfoImpl, Object>();
    private static final Hashtable<XAResourceInfoImpl, Integer> _getActionRepeatCounts = new Hashtable<XAResourceInfoImpl, Integer>();
    private static final Hashtable<XAResourceImpl, Object> _destroyActions = new Hashtable<XAResourceImpl, Object>();
    private static final Hashtable<XAResourceImpl, Integer> _destroyActionRepeatCounts = new Hashtable<XAResourceImpl, Integer>();

    public static void setGetAction(XAResourceInfoImpl xaResInfo, Object getAction) {
        _getActions.put(xaResInfo, getAction);
    }

    public static void setDestroyAction(XAResourceImpl xaRes, Object destroyAction) {
        _destroyActions.put(xaRes, destroyAction);
    }

    public static void setGetActionRepeatCount(XAResourceInfoImpl xaResInfo, int i) {
        _getActionRepeatCounts.put(xaResInfo, i);
    }

    public static void setDestroyActionRepeatCount(XAResourceImpl xaRes, int i) {
        _destroyActionRepeatCounts.put(xaRes, i);
    }

    private int getGetActionRepeatCount(Serializable xaResInfo) {
        final Integer i = _getActionRepeatCounts.get(xaResInfo);

        if (i == null) {
            return 0;
        }

        return i;
    }

    private int getDestroyActionRepeatCount(XAResource xaRes) {
        final Integer i = _destroyActionRepeatCounts.get(xaRes);

        if (i == null) {
            return 0;
        }

        return i;
    }

    @Override
    public void destroyXAResource(XAResource xaRes) throws DestroyXAResourceException {
        final Object action = _destroyActions.get(xaRes);
        if (action instanceof DestroyXAResourceException) {
            int repeatCount = getDestroyActionRepeatCount(xaRes);
            _destroyActionRepeatCounts.put((XAResourceImpl) xaRes, repeatCount - 1);
            if (repeatCount >= 0) {
                throw (DestroyXAResourceException) action;
            }
        } else if (action instanceof RuntimeException) {
            int repeatCount = getDestroyActionRepeatCount(xaRes);
            _destroyActionRepeatCounts.put((XAResourceImpl) xaRes, repeatCount - 1);
            if (repeatCount >= 0) {
                throw (RuntimeException) action;
            }
        }

        XAResourceImpl.destroy((XAResourceImpl) xaRes);
    }

    @Override
    public XAResource getXAResource(Serializable xaResInfo) throws XAResourceNotAvailableException {
        if (!_stateLoaded) {
            _stateLoaded = true;
            XAResourceImpl.loadState(((XAResourceInfoImpl) xaResInfo).getStateFile());
        }
        final Object action = _getActions.get(xaResInfo);
        if (action instanceof XAResourceNotAvailableException) {
            int repeatCount = getGetActionRepeatCount(xaResInfo);
            _getActionRepeatCounts.put((XAResourceInfoImpl) xaResInfo, repeatCount - 1);
            if (repeatCount >= 0) {
                throw (XAResourceNotAvailableException) action;
            }
        } else if (action instanceof RuntimeException) {
            int repeatCount = getGetActionRepeatCount(xaResInfo);
            _getActionRepeatCounts.put((XAResourceInfoImpl) xaResInfo, repeatCount - 1);
            if (repeatCount >= 0) {
                throw (RuntimeException) action;
            }
        } else if (action instanceof Integer) {
            switch ((Integer) action) {
                case RETURN_NULL:
                    int repeatCount = getGetActionRepeatCount(xaResInfo);
                    _getActionRepeatCounts.put((XAResourceInfoImpl) xaResInfo, repeatCount - 1);
                    if (repeatCount >= 0) {
                        return null;
                    }
                default:
            }
        }

        return XAResourceImpl.getXAResourceImpl(((XAResourceInfoImpl) xaResInfo).getKey());
    }

    public XAResourceImpl getXAResourceImpl(Serializable xaresinfo) throws XAResourceNotAvailableException {
        return XAResourceImpl.getXAResourceImpl(((XAResourceInfoImpl) xaresinfo).getKey());
    }

    public AbortableXAResourceImpl getAbortableXAResourceImpl(Serializable xaresinfo) throws XAResourceNotAvailableException {
        return AbortableXAResourceImpl.getAbortableXAResourceImpl(((XAResourceInfoImpl) xaresinfo).getKey());
    }
}