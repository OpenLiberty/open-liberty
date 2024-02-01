<!--
    Copyright (c) 2023 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
 -->
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>jsp:plugin</title>
    </head>
    <body>

        <p> Verify jsp:fallback throws an exception </p>

        <!-- 
            Not exactly correct since jsp:params should be nested within jsp:plugin
            But this verifies an exception is thrown nonetheless.
        -->
        <jsp:fallback> </jsp:fallback>
        
    </body>
</html>
