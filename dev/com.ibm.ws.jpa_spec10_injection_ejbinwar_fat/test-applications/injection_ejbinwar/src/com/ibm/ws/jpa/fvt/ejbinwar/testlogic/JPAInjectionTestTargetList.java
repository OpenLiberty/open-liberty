/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ejbinwar.testlogic;

import java.util.ArrayList;
import java.util.List;

public class JPAInjectionTestTargetList {
    public static final List<JPAInjectionTestTarget> EJB_NOOVERRIDE;
    static {
        EJB_NOOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EJB_NOOVERRIDE_arr = {
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, true),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EJB_NOOVERRIDE_arr) {
            EJB_NOOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> EJB_YESOVERRIDE;
    static {
        EJB_YESOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EJB_YESOVERRIDE_arr = {
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, true),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, true),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EJB_YESOVERRIDE_arr) {
            EJB_YESOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> WEB_NOOVERRIDE;
    static {
        WEB_NOOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] WEB_NOOVERRIDE_arr = {
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, true),
                                                        new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : WEB_NOOVERRIDE_arr) {
            WEB_NOOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> WEB_YESOVERRIDE;
    static {
        WEB_YESOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] WEB_YESOVERRIDE_arr = {
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, true),
                                                         new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, true)
        };
        for (JPAInjectionTestTarget target : WEB_YESOVERRIDE_arr) {
            WEB_YESOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> EARLIB_NOOVERRIDE;
    static {
        EARLIB_NOOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EARLIB_NOOVERRIDE_arr = {
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, true),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                           new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EARLIB_NOOVERRIDE_arr) {
            EARLIB_NOOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> EARLIB_YESOVERRIDE;
    static {
        EARLIB_YESOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EARLIB_YESOVERRIDE_arr = {
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, true),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, true),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EARLIB_YESOVERRIDE_arr) {
            EARLIB_YESOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> EARROOT_NOOVERRIDE;
    static {
        EARROOT_NOOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EARROOT_NOOVERRIDE_arr = {
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, true),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                            new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EARROOT_NOOVERRIDE_arr) {
            EARROOT_NOOVERRIDE.add(target);
        }
    }

    public static final List<JPAInjectionTestTarget> EARROOT_YESOVERRIDE;
    static {
        EARROOT_YESOVERRIDE = new ArrayList<JPAInjectionTestTarget>();
        JPAInjectionTestTarget[] EARROOT_YESOVERRIDE_arr = {
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.CoreInjectionEntity, true),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityA, false),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARLIBEntityB, false),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityA, true),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EARROOTEntityB, true),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityA, false),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.EJBEntityB, false),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityA, false),
                                                             new JPAInjectionTestTarget(JPAInjectionEntityEnum.WAREntityB, false)
        };
        for (JPAInjectionTestTarget target : EARROOT_YESOVERRIDE_arr) {
            EARROOT_YESOVERRIDE.add(target);
        }
    }
}
