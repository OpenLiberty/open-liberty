@rem ***************************************************************************
@rem Copyright (c) 2017 IBM Corporation and others.
@rem All rights reserved. This program and the accompanying materials
@rem are made available under the terms of the Eclipse Public License v1.0
@rem which accompanies this distribution, and is available at
@rem http://www.eclipse.org/legal/epl-v10.html
@rem
@rem Contributors:
@rem     IBM Corporation - initial API and implementation
@rem ***************************************************************************
set JAR=c:\java60\bin\jar

for /r %%i in (*.jar) do (
  if exist %%~ni.mf (
  echo "%%~ni" 
  mkdir "%%~ni"
  cd %%~ni
  %JAR% xvf "%%i"
  cd ..
  del "%%i"
  %JAR% cvmf "%%~ni.mf" "%%i" -C "%%~ni" .  
  del /s/q "%%~ni"
  rmdir /s/q "%%~ni"
  )
  )