/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.jsp.tools;

import java.util.Map;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Scott Johnson
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface JspTools {
    public boolean compileApp(String contextDir);

    public boolean compileDir(String contextDir, String directory);

    public boolean compileFile(String contextDir, String jspFile);

    public void setForceCompilation(boolean forceCompilation);

    public void setClasspath(String classpath);
    //begin 241038: add new API for setting classpath
    public void setClasspath(String serverClasspath, String appClasspath);
    //end 241038: add new API for setting classpath

    public void setClassloader(ClassLoader loader); //418518

    public void setLooseLibs(Map looseLibs);

    public void setOptions(JspToolsOptionsMap options);

    public void setTaglibs(Hashtable tagLibs);

    public void setCompilerOptions(List compilerOptions);

    public void setKeepGeneratedclassfiles(boolean b);

    public void setCreateDebugClassfiles(boolean createDebugClassfiles);

    public void setLogger(Logger loggerIn);

	// Defect 202493
	public Logger getLogger();
}
