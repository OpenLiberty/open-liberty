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
package com.ibm.ws.filetransfer.routing.archiveExpander;

import java.io.File;
import java.io.IOException;

public class ChmodUnixModeHelper implements UnixModeHelper {

  @Override
  public void setPermissions(File f, int unixMode) throws IOException {
    int owner = 0;
    int group = 0;
    int other = 0;
    
    if ((unixMode ^ 1) == 1) {
      other += 1;
    }
    
    if ((unixMode ^ 2) == 2) {
      other += 2;
    } 
    
    if ((unixMode ^ 4) == 4) {
      other += 4;
    } 
    
    if ((unixMode ^ 8) == 8) {
      group += 1;
    }
    
    if ((unixMode ^ 16) == 16) {
      group += 2;
    }
    
    if ((unixMode ^ 32) == 32) {
      group += 4;
    }
    
    if ((unixMode ^ 64) == 64) {
      owner += 1;
    } 
    
    if ((unixMode ^ 128) == 128) {
      owner += 2;
    }
    
    if ((unixMode ^ 256) == 256) {
      owner += 4;
    }
    
    String perms = "" + owner + group + other;
    
    ProcessBuilder builder = new ProcessBuilder("chmod", perms, f.getAbsolutePath());
    try {
      builder.start().waitFor();
    } catch (IOException ioe) {
      // ignore
    } catch (InterruptedException e) {
      // ignore
    }
  }

}
