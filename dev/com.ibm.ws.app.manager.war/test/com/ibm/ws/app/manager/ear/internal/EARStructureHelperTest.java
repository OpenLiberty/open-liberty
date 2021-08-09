/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ear.internal;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.wsspi.artifact.ArtifactContainer;

public class EARStructureHelperTest {
    private final StructureHelper sh = EARStructureHelper.getUnknownRootInstance();
    private final StructureHelper shExplicit = EARStructureHelper.create(Arrays.asList("/explicit", "/dir/explicit"));
    private final Mockery mockery = new Mockery();

    private final ArtifactContainer root = mockArtifactContainer("/", null, null);
    private final ArtifactContainer file = mockArtifactContainer("/file", null, null);

    private final ArtifactContainer war = mockArtifactContainer("/x.war", root, null);
    private final ArtifactContainer warWebInf = mockArtifactContainer("/x.war/WEB-INF", root, null);
    private final ArtifactContainer warWebInfLib = mockArtifactContainer("/x.war/WEB-INF/lib", root, null);
    private final ArtifactContainer warWebInfLibJar = mockArtifactContainer("/x.war/WEB-INF/x.jar", root, null);
    private final ArtifactContainer warJar = mockArtifactContainer("/x.war/x.jar", root, null);
    private final ArtifactContainer warJarFile = mockArtifactContainer("/x.war/x.jar/file", root, null);

    private final ArtifactContainer explicit = mockArtifactContainer("/explicit", root, null);
    private final ArtifactContainer explicitWebInf = mockArtifactContainer("/explicit/WEB-INF", root, null);
    private final ArtifactContainer explicitWebInfLib = mockArtifactContainer("/explicit/WEB-INF/lib", root, null);
    private final ArtifactContainer explicitWebInfLibJar = mockArtifactContainer("/explicit/WEB-INF/x.jar", root, null);
    private final ArtifactContainer explicitJar = mockArtifactContainer("/explicit/x.jar", root, null);
    private final ArtifactContainer explicitJarFile = mockArtifactContainer("/explicit/x.jar/file", root, null);

    private final ArtifactContainer dir = mockArtifactContainer("/dir", root, null);
    private final ArtifactContainer dirWar = mockArtifactContainer("/dir/x.war", root, null);
    private final ArtifactContainer dirWarWebInf = mockArtifactContainer("/dir/x.war/WEB-INF", root, null);
    private final ArtifactContainer dirWarWebInfLib = mockArtifactContainer("/dir/x.war/WEB-INF/lib", root, null);
    private final ArtifactContainer dirWarWebInfLibJar = mockArtifactContainer("/dir/x.war/WEB-INF/x.jar", root, null);
    private final ArtifactContainer dirWarDirjar = mockArtifactContainer("/dir/x.war/x.jar", root, null);
    private final ArtifactContainer dirWarDirjarFile = mockArtifactContainer("/dir/x.war/x.jar/file", root, null);

    private final ArtifactContainer dirExplicit = mockArtifactContainer("/dir/explicit", root, null);
    private final ArtifactContainer dirExplicitWebInf = mockArtifactContainer("/dir/explicit/WEB-INF", root, null);
    private final ArtifactContainer dirExplicitWebInfLib = mockArtifactContainer("/dir/explicit/WEB-INF/lib", root, null);
    private final ArtifactContainer dirExplicitWebInfLibJar = mockArtifactContainer("/dir/explicit/WEB-INF/x.jar", root, null);
    private final ArtifactContainer dirExplicitDirjar = mockArtifactContainer("/dir/explicit/x.jar", root, null);
    private final ArtifactContainer dirExplicitDirjarFile = mockArtifactContainer("/dir/explicit/x.jar/file", root, null);

    private final ArtifactContainer dirjar = mockArtifactContainer("/x.jar", root, null);
    private final ArtifactContainer dirjarFile = mockArtifactContainer("/x.jar/file", root, null);

    private final ArtifactContainer dirDirjar = mockArtifactContainer("/dir/x.jar", root, null);
    private final ArtifactContainer dirDirjarFile = mockArtifactContainer("/dir/x.jar/file", root, null);

    private final ArtifactContainer innerRoot = mockArtifactContainer("/", null, root);
    private final ArtifactContainer innerWar = mockArtifactContainer("/x.war", null, root);
    private final ArtifactContainer innerWarFile = mockArtifactContainer("/x.war/file", null, root);

    private ArtifactContainer mockArtifactContainer(final String path, final ArtifactContainer root, final ArtifactContainer enclosing) {
        final ArtifactContainer ac = mockery.mock(ArtifactContainer.class,
                                                  "AC(" + (enclosing == null ? path : path + " -> " + enclosing) + ")");
        mockery.checking(new Expectations() {
            {
                allowing(ac).isRoot();
                will(returnValue(path.equals("/")));
                allowing(ac).getRoot();
                will(returnValue(root == null ? ac : root));
                allowing(ac).getPath();
                will(returnValue(path));
                allowing(ac).getEnclosingContainer();
                will(returnValue(enclosing));
            }
        });
        return ac;
    }

    @Test
    public void testIsRoot() {
        Assert.assertFalse(sh.isRoot(root));
        Assert.assertFalse(sh.isRoot(file));

        Assert.assertFalse(shExplicit.isRoot(root));
        Assert.assertFalse(shExplicit.isRoot(file));

        Assert.assertTrue(sh.isRoot(war));
        Assert.assertFalse(sh.isRoot(warWebInf));
        Assert.assertFalse(sh.isRoot(warWebInfLib));
        Assert.assertFalse(sh.isRoot(warWebInfLibJar));
        Assert.assertFalse(sh.isRoot(warJar));
        Assert.assertFalse(sh.isRoot(warJarFile));

        Assert.assertTrue(shExplicit.isRoot(explicit));
        Assert.assertFalse(shExplicit.isRoot(explicitWebInf));
        Assert.assertFalse(shExplicit.isRoot(explicitWebInfLib));
        Assert.assertFalse(shExplicit.isRoot(explicitWebInfLibJar));
        Assert.assertFalse(shExplicit.isRoot(explicitJar));
        Assert.assertFalse(shExplicit.isRoot(explicitJarFile));

        Assert.assertTrue(sh.isRoot(dirWar));
        Assert.assertFalse(sh.isRoot(dirWarWebInf));
        Assert.assertFalse(sh.isRoot(dirWarWebInfLib));
        Assert.assertFalse(sh.isRoot(dirWarWebInfLibJar));
        Assert.assertFalse(sh.isRoot(dirWarDirjar));
        Assert.assertFalse(sh.isRoot(dirWarDirjarFile));

        Assert.assertTrue(shExplicit.isRoot(dirExplicit));
        Assert.assertFalse(shExplicit.isRoot(dirExplicitWebInf));
        Assert.assertFalse(shExplicit.isRoot(dirExplicitWebInfLib));
        Assert.assertFalse(shExplicit.isRoot(dirExplicitWebInfLibJar));
        Assert.assertFalse(shExplicit.isRoot(dirExplicitDirjar));
        Assert.assertFalse(shExplicit.isRoot(dirExplicitDirjarFile));

        Assert.assertTrue(sh.isRoot(dirjar));
        Assert.assertFalse(sh.isRoot(dirjarFile));

        Assert.assertTrue(sh.isRoot(dirDirjar));
        Assert.assertFalse(sh.isRoot(dirDirjarFile));

        Assert.assertFalse(sh.isRoot(innerRoot));
        Assert.assertFalse(sh.isRoot(innerWar));
        Assert.assertFalse(sh.isRoot(innerWarFile));
    }

    @Test
    public void testIsValid() {
        for (ArtifactContainer ac : new ArtifactContainer[] { root, war, dirWar, dirjar, dirDirjar }) {
            Assert.assertTrue(sh.isValid(ac, "/"));
            Assert.assertTrue(sh.isValid(ac, "/file"));

            Assert.assertTrue(shExplicit.isValid(ac, "/"));
            Assert.assertTrue(shExplicit.isValid(ac, "/file"));

            Assert.assertTrue(sh.isValid(ac, "/x.war"));
            Assert.assertFalse(sh.isValid(ac, "/x.war/WEB-INF"));
            Assert.assertFalse(sh.isValid(ac, "/x.war/WEB-INF/lib"));
            Assert.assertFalse(sh.isValid(ac, "/x.war/WEB-INF/lib/x.jar"));

            Assert.assertTrue(shExplicit.isValid(ac, "/explicit"));
            Assert.assertFalse(shExplicit.isValid(ac, "/explicit/WEB-INF"));
            Assert.assertFalse(shExplicit.isValid(ac, "/explicit/WEB-INF/lib"));
            Assert.assertFalse(shExplicit.isValid(ac, "/explicit/WEB-INF/lib/x.jar"));

            Assert.assertTrue(sh.isValid(ac, "/dir"));
            Assert.assertTrue(sh.isValid(ac, "/dir/x.war"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.war/WEB-INF"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.war/WEB-INF/lib"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.war/WEB-INF/lib/x.jar"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.war/x.jar"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.war/x.jar/file"));

            Assert.assertTrue(shExplicit.isValid(ac, "/dir/explicit"));
            Assert.assertFalse(shExplicit.isValid(ac, "/dir/explicit/WEB-INF"));
            Assert.assertFalse(shExplicit.isValid(ac, "/dir/explicit/WEB-INF/lib"));
            Assert.assertFalse(shExplicit.isValid(ac, "/dir/explicit/WEB-INF/lib/x.jar"));
            Assert.assertFalse(shExplicit.isValid(ac, "/dir/explicit/x.jar"));
            Assert.assertFalse(shExplicit.isValid(ac, "/dir/explicit/x.jar/file"));

            Assert.assertTrue(sh.isValid(ac, "/dir/x.jar"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.jar/file"));
            Assert.assertTrue(sh.isValid(ac, "/dir/x.jar"));
            Assert.assertFalse(sh.isValid(ac, "/dir/x.jar/file"));
        }

        Assert.assertTrue(sh.isValid(war, "WEB-INF"));
        Assert.assertTrue(sh.isValid(war, "WEB-INF/lib"));
        Assert.assertTrue(sh.isValid(war, "WEB-INF/lib/x.jar"));
        Assert.assertTrue(sh.isValid(war, "x.jar"));
        Assert.assertTrue(sh.isValid(war, "x.jar/file"));

        Assert.assertTrue(shExplicit.isValid(explicit, "WEB-INF"));
        Assert.assertTrue(shExplicit.isValid(explicit, "WEB-INF/lib"));
        Assert.assertTrue(shExplicit.isValid(explicit, "WEB-INF/lib/x.jar"));
        Assert.assertTrue(shExplicit.isValid(explicit, "x.jar"));
        Assert.assertTrue(shExplicit.isValid(explicit, "x.jar/file"));

        Assert.assertTrue(sh.isValid(dir, "x.war"));
        Assert.assertFalse(sh.isValid(dir, "x.war/WEB-INF"));
        Assert.assertFalse(sh.isValid(dir, "x.war/WEB-INF/lib"));
        Assert.assertFalse(sh.isValid(dir, "x.war/WEB-INF/lib/x.jar"));
        Assert.assertFalse(sh.isValid(dir, "x.war/x.jar"));
        Assert.assertFalse(sh.isValid(dir, "x.war/x.jar/file"));

        Assert.assertTrue(shExplicit.isValid(dir, "explicit"));
        Assert.assertFalse(shExplicit.isValid(dir, "explicit/WEB-INF"));
        Assert.assertFalse(shExplicit.isValid(dir, "explicit/WEB-INF/lib"));
        Assert.assertFalse(shExplicit.isValid(dir, "explicit/WEB-INF/lib/x.jar"));
        Assert.assertFalse(shExplicit.isValid(dir, "explicit/x.jar"));
        Assert.assertFalse(shExplicit.isValid(dir, "explicit/x.jar/file"));

        Assert.assertTrue(sh.isValid(dirWar, "WEB-INF"));
        Assert.assertTrue(sh.isValid(dirWar, "WEB-INF/lib"));
        Assert.assertTrue(sh.isValid(dirWar, "WEB-INF/lib/x.jar"));
        Assert.assertTrue(sh.isValid(dirWar, "x.jar"));
        Assert.assertTrue(sh.isValid(dirWar, "x.jar/file"));

        Assert.assertTrue(shExplicit.isValid(dirExplicit, "WEB-INF"));
        Assert.assertTrue(shExplicit.isValid(dirExplicit, "WEB-INF/lib"));
        Assert.assertTrue(shExplicit.isValid(dirExplicit, "WEB-INF/lib/x.jar"));
        Assert.assertTrue(shExplicit.isValid(dirExplicit, "x.jar"));
        Assert.assertTrue(shExplicit.isValid(dirExplicit, "x.jar/file"));

        Assert.assertTrue(sh.isValid(dirjar, "file"));

        Assert.assertTrue(sh.isValid(dirDirjar, "file"));

        Assert.assertTrue(sh.isValid(innerRoot, "file"));
        Assert.assertTrue(sh.isValid(innerRoot, "x.war"));
        Assert.assertTrue(sh.isValid(innerRoot, "x.war/file"));
    }
}
