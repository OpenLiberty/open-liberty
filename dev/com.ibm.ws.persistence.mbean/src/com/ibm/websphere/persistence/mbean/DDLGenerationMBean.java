/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.persistence.mbean;

import java.io.Serializable;
import java.util.Map;

/**
 * The DDLGenerationMBean provides an interface which can be used to generate
 * DDL for WebSphere Liberty Profile features which use the persistence
 * service.
 */
public interface DDLGenerationMBean {
    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=persistence,type=DDLGenerationMBean,name=DDLGenerationMBean";

    /**
     * Key returned by the generateDDL function, which describes the
     * path in the file system where the DDL files were written. The
     * value of this key is a java.lang.String.
     */
    String OUTPUT_DIRECTORY = "output.directory";

    /**
     * Key returned by the generateDDL function, which describes the
     * number of DDL files written by the MBean. A value of zero
     * indicates that nothing was written. The value of this key is
     * a java.lang.Integer.
     */
    String FILE_COUNT = "file.count";

    /**
     * Key returned by the generateDDL function, which describes the
     * overall success of the function. The value of this key is a
     * java.lang.Boolean. A value of TRUE indicates that the function
     * completed successfully. A value of FALSE indicates that the
     * function did not complete successfully. Exceptions or FFDC
     * entries may be found in the server's logs.
     */
    String SUCCESS = "success";

    /**
     * Generate DDL for all features which use the persistence service to
     * persist data to a database.
     * 
     * One file will be written for each user of the persistence service.
     * The files will be written to a directory in the file system
     * named ${server.output.dir}/ddl. The name of each file will be
     * the config.displayId for the service generating the DDL.
     * 
     * @return A map of key/value pairs describing the results of the
     *         command. The map will include the following keys:
     *         DDLGenerationMBean.OUTPUT_DIRECTORY
     *         DDLGenerationMBean.SUCCESS
     */
    Map<String, Serializable> generateDDL();
}
