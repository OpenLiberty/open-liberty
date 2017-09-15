/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.util.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class Tr
{
    private static Tracer t;

    static
    {
        reinitializeTracer();
    }

    private static String getProperty(final String prop) {
        if (System.getSecurityManager() == null)
            return System.getProperty(prop);
        else
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(prop);
                }
            });
    }

    public static void reinitializeTracer()
    {
        String tracerClass = getProperty("com.ibm.tx.tracer");

        if (tracerClass == null)
        {
            tracerClass = "com.ibm.ws.tx.util.logging.WASTr";
        }

        try
        {
            t = (Tracer)Class.forName(tracerClass).newInstance();
        }
        catch(Exception e)
        {
            try
            {
                t = (Tracer)Class.forName("com.ibm.tx.jta.util.logging.TxTr").newInstance();
            }
            catch(Exception e1)
            {
                t = null;
                e1.printStackTrace();
            }
        }
        
        if (t == null)
        {       
            t = new Tracer()
            {
    
                public void audit(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void audit(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void audit(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void debug(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void debug(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void debug(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void entry(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void entry(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void entry(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void error(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void error(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void error(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void event(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void event(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void event(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void exit(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void exit(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void exit(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void fatal(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void fatal(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void fatal(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public void info(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void info(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void info(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }
    
                public TraceComponent register(Class cl, String traceGroup, String nlsFile)
                {
                    // TODO Auto-generated method stub
                    return new TraceComponent()
                    {
                        public Object getData() {
                            // TODO Auto-generated method stub
                            return null;
                        }
    
                        public boolean isDebugEnabled() {
                            // TODO Auto-generated method stub
                            return false;
                        }
    
                        public boolean isEntryEnabled() {
                            // TODO Auto-generated method stub
                            return false;
                        }
                        
                        public boolean isEventEnabled() {
                            // TODO Auto-generated method stub
                            return false;
                        }                   
                        
                        public boolean isWarningEnabled() {
                            // TODO Auto-generated method stub
                            return false;
                        }

                        public void setDebugEnabled(boolean enabled)
                        {
                            // TODO Auto-generated method stub

                        }

                        public void setEntryEnabled(boolean enabled)
                        {
                            // TODO Auto-generated method stub

                        }

                        public void setEventEnabled(boolean enabled)
                        {
                            // TODO Auto-generated method stub

                        }

                        public void setWarningEnabled(boolean enabled)
                        {
                            // TODO Auto-generated method stub

                        }                   
                    };
                }
    
                public void warning(TraceComponent tc, String s) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void warning(TraceComponent tc, String s, Object o) {
                    // TODO Auto-generated method stub
                    
                }
                
                public void warning(TraceComponent tc, String s, Object[] o) {
                    // TODO Auto-generated method stub
                    
                }

                public void initTrace()
                {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public TraceComponent register(String s, String traceGroup, String nlsFile) {
                   return new TraceComponent()
                   {
                       public Object getData() {
                           // TODO Auto-generated method stub
                           return null;
                       }
   
                       public boolean isDebugEnabled() {
                           // TODO Auto-generated method stub
                           return false;
                       }
   
                       public boolean isEntryEnabled() {
                           // TODO Auto-generated method stub
                           return false;
                       }
                       
                       public boolean isEventEnabled() {
                           // TODO Auto-generated method stub
                           return false;
                       }                   
                       
                       public boolean isWarningEnabled() {
                           // TODO Auto-generated method stub
                           return false;
                       }

                       public void setDebugEnabled(boolean enabled)
                       {
                           // TODO Auto-generated method stub

                       }

                       public void setEntryEnabled(boolean enabled)
                       {
                           // TODO Auto-generated method stub

                       }

                       public void setEventEnabled(boolean enabled)
                       {
                           // TODO Auto-generated method stub

                       }

                       public void setWarningEnabled(boolean enabled)
                       {
                           // TODO Auto-generated method stub

                       }                   
                   };
               }
                
            }; 
        }
        
        t.initTrace();
    }

    public static void audit(TraceComponent tc, String s)
    {
        t.audit(tc, s);
    }

    public static void audit(TraceComponent tc, String s, Object o)
    {
        t.audit(tc, s, o);
    }

    public static void debug(TraceComponent tc, String s)
    {
        t.debug(tc, s);
    }

    public static void debug(TraceComponent tc, String s, Object o)
    {
        t.debug(tc, s, o);
    }

    public static void entry(TraceComponent tc, String s)
    {
        t.entry(tc, s);
    }

    public static void entry(TraceComponent tc, String s, Object o)
    {
        t.entry(tc, s, o);
    }

    public static void error(TraceComponent tc, String s)
    {
        t.error(tc, s);
    }

    public static void error(TraceComponent tc, String s, Object o)
    {
        t.error(tc, s, o);
    }

    public static void event(TraceComponent tc, String s)
    {
        t.event(tc, s);
    }

    public static void event(TraceComponent tc, String s, Object o)
    {
        t.event(tc, s, o);
    }

    public static void exit(TraceComponent tc, String s)
    {
        t.exit(tc, s);
    }

    public static void exit(TraceComponent tc, String s, Object o)
    {
        t.exit(tc, s, o);
    }

    public static void fatal(TraceComponent tc, String s)
    {
        t.fatal(tc, s);
    }

    public static void fatal(TraceComponent tc, String s, Object o)
    {
        t.fatal(tc, s, o);
    }

    public static void info(TraceComponent tc, String s)
    {
        t.info(tc, s);
    }

    public static void info(TraceComponent tc, String s, Object o)
    {
        t.info(tc, s, o);
    }

    public static TraceComponent register(Class cl, String traceGroup, String nlsFile)
    {
        return t.register(cl, traceGroup, nlsFile);
    }
    
    public static TraceComponent register(String string, String summaryTraceGroup, String nlsFile) 
    {
        return t.register(string, summaryTraceGroup, nlsFile);
    }

    public static void warning(TraceComponent tc, String s)
    {
        t.warning(tc, s);
    }

    public static void warning(TraceComponent tc, String s, Object o)
    {
        t.warning(tc, s, o);
    }
}
