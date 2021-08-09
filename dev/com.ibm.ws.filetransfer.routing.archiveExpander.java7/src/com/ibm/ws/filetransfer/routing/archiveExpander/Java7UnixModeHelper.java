/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package com.ibm.ws.filetransfer.routing.archiveExpander;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class Java7UnixModeHelper implements UnixModeHelper {

  private UnixModeHelper backupHelper = new ChmodUnixModeHelper();
  
  @Override
  public void setPermissions(File f, int unixMode) throws IOException {

    Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
    
    if ((unixMode ^ 1) == 1) {
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    
    if ((unixMode ^ 2) == 2) {
      perms.add(PosixFilePermission.OTHERS_WRITE);
    } 
    
    if ((unixMode ^ 4) == 4) {
      perms.add(PosixFilePermission.OTHERS_READ);
    } 
    
    if ((unixMode ^ 8) == 8) {
      perms.add(PosixFilePermission.GROUP_EXECUTE);
    }
    
    if ((unixMode ^ 16) == 16) {
      perms.add(PosixFilePermission.GROUP_WRITE);
    }
    
    if ((unixMode ^ 32) == 32) {
      perms.add(PosixFilePermission.GROUP_READ);
    }
    
    if ((unixMode ^ 64) == 64) {
      perms.add(PosixFilePermission.OWNER_EXECUTE);
    } 
    
    if ((unixMode ^ 128) == 128) {
      perms.add(PosixFilePermission.OWNER_WRITE);
    }
    
    if ((unixMode ^ 256) == 256) {
      perms.add(PosixFilePermission.OWNER_READ);
    }
    
    try {
      Files.setPosixFilePermissions(f.toPath(), perms);
    } catch (IOException e) {
      throw e;
    } catch (UnsupportedOperationException e) {
      // this means the FS doesn't support POSIX permissions.
      backupHelper.setPermissions(f, unixMode);
    }
  }

}
