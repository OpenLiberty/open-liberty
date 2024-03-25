/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.application.manager.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.application.manager.test.SharedLibTestUtils.ContainerAction;
import componenttest.application.manager.test.SharedLibTestUtils.OrderedAction;
import componenttest.custom.junit.runner.FATRunner;

//@formatter:off
@RunWith(FATRunner.class)
public class SharedLibParseTest {

    public static void main(String[] args) {
        SharedLibParseTest tester = new SharedLibParseTest();
        tester.parseTest();
    }

    public static void fail(String message) {
        SharedLibTestUtils.fail(message);
    }

    public static final String[] SAMPLE_UNIX = {
        "[3/2/24, 17:54:10:176 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 1 ] [ 7 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:178 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test2.jar ] [ 1 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@e071c466 ]",
        "[3/2/24, 17:54:10:179 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 2 ] [ 6 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:179 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test2.jar ] [ 2 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@e071c466 ]",
        "[3/2/24, 17:54:10:182 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 3 ] [ 5 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:182 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test1.jar ] [ 1 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@8b509485 ]",
        "[3/2/24, 17:54:10:217 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 4 ] [ 4 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:217 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test2.jar ] [ 3 ] [ 1 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@e071c466 ]",
        "[3/2/24, 17:54:10:231 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 5 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:231 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test1.jar ] [ 2 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@8b509485 ]",
        "[3/2/24, 17:54:10:239 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 6 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:239 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ /sharedLibConfigServer/snoopLib/test2.jar ] [ 4 ] [ 0 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@e071c466 ]",
        "[3/2/24, 17:54:10:261 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 7 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:262 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test1.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@8b509485 ]",
        "[3/2/24, 17:54:10:268 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 8 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:268 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 1 ] [ 1 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:306 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 9 ] [ 5 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:306 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 2 ] [ 2 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:307 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 10 ] [ 6 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:309 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:314 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 11 ] [ 7 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:314 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 4 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:342 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 12 ] [ 8 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:344 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 5 ] [ 5 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:372 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 13 ] [ 9 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:373 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 6 ] [ 6 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:373 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 14 ] [ 10 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:374 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test1.jar ] [ 4 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@8b509485 ]",
        "[3/2/24, 17:54:10:383 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 15 ] [ 11 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:385 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 7 ] [ 7 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:426 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 16 ] [ 12 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:429 PST] 0000004a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 8 ] [ 8 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:446 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 17 ] [ 13 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:447 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 9 ] [ 9 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:456 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 18 ] [ 14 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:456 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 19 ] [ 15 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:457 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 10 ] [ 10 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:457 PST] 0000003f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 11 ] [ 11 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]",
        "[3/2/24, 17:54:10:503 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 20 ] [ 16 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
        "[3/2/24, 17:54:10:503 PST] 0000002c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ /sharedLibConfigServer/snoopLib/test3.jar ] [ 12 ] [ 12 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@c3bac697 ]"
    };

    public static final String[] SAMPLE_WINDOWS = {
        "[3/4/24, 21:17:16:211 EST] 0000016f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 1 ] [ 7 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:211 EST] 0000016f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test2.jar ] [ 1 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@53b335c8 ]",
        "[3/4/24, 21:17:16:213 EST] 00000167 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 2 ] [ 6 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:213 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 3 ] [ 5 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:213 EST] 00000167 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test1.jar ] [ 1 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@876989c8 ]",
        "[3/4/24, 21:17:16:213 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test2.jar ] [ 2 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@53b335c8 ]",
        "[3/4/24, 21:17:16:261 EST] 00000168 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 4 ] [ 4 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:261 EST] 00000168 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test2.jar ] [ 2 ] [ 1 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@53b335c8 ]",
        "[3/4/24, 21:17:16:263 EST] 0000016c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 5 ] [ 3 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:263 EST] 0000016c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test1.jar ] [ 2 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@876989c8 ]",
        "[3/4/24, 21:17:16:271 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 6 ] [ 2 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:271 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].release: [ \\sharedLibConfigServer\\snoopLib\\test2.jar ] [ 2 ] [ 0 ]: [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@53b335c8 ]",
        "[3/4/24, 21:17:16:348 EST] 0000016a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 1 ] [ 1 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:349 EST] 00000169 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 2 ] [ 2 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:355 EST] 00000168 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:357 EST] 0000016f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 4 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:366 EST] 0000016c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 5 ] [ 5 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:366 EST] 00000168 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 7 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:367 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 6 ] [ 6 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:367 EST] 0000016f id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 8 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:369 EST] 0000016a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 9 ] [ 5 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:370 EST] 0000016c id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 10 ] [ 6 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:371 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 11 ] [ 7 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:374 EST] 00000169 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 12 ] [ 8 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:376 EST] 0000016e id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 7 ] [ 7 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:377 EST] 00000167 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 8 ] [ 8 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:378 EST] 0000016d id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 9 ] [ 9 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:379 EST] 0000016e id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 13 ] [ 9 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:380 EST] 00000167 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 14 ] [ 10 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:383 EST] 0000016d id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 15 ] [ 11 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:401 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 10 ] [ 10 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:401 EST] 00000171 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 11 ] [ 11 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:403 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 16 ] [ 12 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:408 EST] 00000171 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 17 ] [ 13 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:591 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 18 ] [ 14 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:595 EST] 00000172 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test1.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@876989c8 ]",
        "[3/4/24, 21:17:16:648 EST] 0000016a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test3.jar ] [ 12 ] [ 12 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@2c937585 ]",
        "[3/4/24, 21:17:16:648 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 19 ] [ 15 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]",
        "[3/4/24, 21:17:16:651 EST] 00000170 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test1.jar ] [ 4 ] [ 4 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@876989c8 ]",
        "[3/4/24, 21:17:16:651 EST] 0000016a id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
            "[container].capture: [ \\sharedLibConfigServer\\snoopLib\\test0.jar ] [ 20 ] [ 16 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@f682bcb1 ]"
    };

    public static List<ContainerAction> parseActions(String[] actionText) {
        List<ContainerAction> actions = new ArrayList<>(actionText.length);
        for ( int actionNo = 0; actionNo < actionText.length; actionNo++ ) {
            actions.add( new ContainerAction(actionNo, actionText[actionNo]) );
        }
        return actions;
    }

    public static ContainerAction[] parseActions(OrderedAction[] orderedActions) {
        ContainerAction[] actions = new ContainerAction[ orderedActions.length ];
        for ( int actionNo = 0; actionNo < orderedActions.length; actionNo++ ) {
            OrderedAction orderedAction = orderedActions[actionNo];
            actions[actionNo] = new ContainerAction(orderedAction.offset, orderedAction.actionText);
        }
        return actions;
    }

    @Test
    public void parseTest() {
        OrderedAction[] orderedUnix = SharedLibTestUtils.order(SAMPLE_UNIX);
        ContainerAction[] unixActions = parseActions(orderedUnix);
        display(unixActions, "Unix sample data");

        OrderedAction[] orderedWin = SharedLibTestUtils.order(SAMPLE_WINDOWS);
        ContainerAction[] winActions = parseActions(orderedWin);
        display(winActions, "Unix sample data");

        compare(unixActions, "Unix sample data", winActions, "Windows sample data");
    }

    public void compare(ContainerAction[] actions0, String title0, ContainerAction[] actions1, String title1) {
        List<String> failures = new ArrayList<>();

        int num0 = actions0.length;
        int num1 = actions1.length;
        if ( num0 != num1 ) {
            failures.add("Length error: " + title0 + " [ " + num0 + " ]; " + title1 + " [ " + num1 + " ]");
        }

        int minLength = (num0 < num1) ? num0 : num1;

        for ( int actionNo = 0; actionNo < minLength; actionNo++ ) {
            ContainerAction action0 = actions0[actionNo];
            ContainerAction action1 = actions1[actionNo];

            boolean sameAs = action0.sameAs(action1, failures, !ContainerAction.COMPARE_INSTANCES);
        }

        if ( failures.isEmpty() ) {
            System.out.println("Verified [ " + num0 + " ] actions");
        } else {
            System.out.println("Verification failures [ " + failures.size() + " ]");
            for ( String failure : failures ) {
                System.out.println("  [ " + failure + " ]");
            }
            fail("Verification failures [ " + failures.size() + " ]");
        }
    }

    public static void display(ContainerAction[] actions, String title) {
        System.out.println(title);
        System.out.println("==========");
        for ( ContainerAction action : actions ) {
            System.out.println("  [ " + action + " ]");
        }
        System.out.println("==========");
    }

    // Container verification is disabled due to problems of event ordering.
    // Logging does not guarantee that log events from different threads will
    // be written in the order in which logging was requested.

//  public static final String[] IN_ORDER_UNIX = {
//  "[3/2/24, 17:51:05:690 PST] 0000002d id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 1 ] [ 1 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
//  "[3/2/24, 17:51:05:687 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 2 ] [ 2 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
//  "[3/2/24, 17:51:05:690 PST] 00000039 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]"
// };
//
// public static final String[] OUT_OF_ORDER_UNIX = {
//  "[3/2/24, 17:51:05:687 PST] 00000037 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 2 ] [ 2 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
//  "[3/2/24, 17:51:05:690 PST] 00000039 id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 3 ] [ 3 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]",
//  "[3/2/24, 17:51:05:690 PST] 0000002d id=00000000 com.ibm.ws.app.manager.module.internal.CaptureCache          3 " +
//      "[container].capture: [ /sharedLibConfigServer/snoopLib/test0.jar ] [ 1 ] [ 1 ] [ com.ibm.ws.app.manager.module.internal.CaptureCache$CaptureSupplier@12bde948 ]"
// };
//
//    private List<ContainerAction> inOrderActions;
//
//    public List<ContainerAction> getInOrderActions() {
//        if ( inOrderActions == null ) {
//            inOrderActions = parseActions(IN_ORDER_UNIX);
//        }
//        return inOrderActions;
//    }
//
//    private List<ContainerAction> outOfOrderActions;
//
//    public List<ContainerAction> getOutOfOrderActions() {
//        if ( outOfOrderActions == null ) {
//            outOfOrderActions = parseActions(OUT_OF_ORDER_UNIX);
//        }
//        return outOfOrderActions;
//    }
//
//    @Test
//    public void validateInOrderTest() {
//        List<ContainerAction> actions = getInOrderActions();
//        display(actions, "In-order actions");
//        SharedLibTestUtils.verifyContainers(actions);
//    }
//
//    @Test
//    public void validateOutOfOrderTest() {
//        List<ContainerAction> actions = getOutOfOrderActions();
//        display(actions, "Out-of-order actions");
//        SharedLibTestUtils.verifyContainers(actions);
//    }
//
//    @Test
//    public void verifyOrderDataTest() {
//        List<ContainerAction> useInOrderActions = getInOrderActions();
//        display(useInOrderActions, "In-order actions");
//        useInOrderActions.sort(ContainerAction::compareTo);
//        display(useInOrderActions, "In-order actions (sorted)");
//
//        List<ContainerAction> useOutOfOrderActions = getOutOfOrderActions();
//        display(useOutOfOrderActions, "Out-of-order actions");
//        useOutOfOrderActions.sort(ContainerAction::compareTo);
//        display(useInOrderActions, "Out-of-order actions (sorted)");
//
//        ContainerAction[] inOrder = asArray(useInOrderActions, ContainerAction.class);
//        ContainerAction[] outOfOrder = asArray(useOutOfOrderActions, ContainerAction.class);
//
//        compare(inOrder, "In-Order actions", outOfOrder, "Out-Of-Order actions");
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <T> T[] asArray(List<T> elements, Class<T> tClass) {
//        return elements.toArray( (T[]) Array.newInstance(tClass, elements.size()) );
//    }
//
//    public static void display(List<ContainerAction> actions, String title) {
//        System.out.println(title);
//        System.out.println("==========");
//        for ( ContainerAction action : actions ) {
//            System.out.println("  [ " + action + " ]");
//        }
//        System.out.println("==========");
//    }
}
//@formatter:on