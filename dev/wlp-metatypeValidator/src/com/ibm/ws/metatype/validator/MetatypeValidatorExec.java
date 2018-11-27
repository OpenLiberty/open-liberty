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
package com.ibm.ws.metatype.validator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.ibm.ws.metatype.validator.MetatypeValidator.Project;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidationEntry;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;

public class MetatypeValidatorExec {
    /**
     * Entry point for calling the metatype validator code from an ANT task.
     * 
     * @param args should contain one element: the directory containing the
     *            project(s) to validate
     * @throws IOException
     * @throws JAXBException
     */
    public static void main(String[] args) throws IOException, JAXBException {
        if (args.length != 1)
            throw new IOException("A project directory path must be specified.");

        File projectDir = new File(args[0]);
        File outputDir;

        boolean failOnExit = true;

        if (projectDir.getName().equals("ant_build")) {
            // we are wanting to validate ALL projects, so we need to go up one level.
            System.out.println("Validating all projects...");
            projectDir = projectDir.getParentFile();
            outputDir = new File(projectDir.getAbsolutePath() + "/build.image/output/metatypeValidator/");
            projectDir = new File(projectDir, "build.image");
            projectDir = new File(projectDir, "wlp");
            projectDir = new File(projectDir, "lib");

            failOnExit = false;
        } else {
            outputDir = new File(projectDir.getAbsolutePath() + "/build/reports/metatypeValidator/");
            projectDir = new File(projectDir, "build");
            projectDir = new File(projectDir, "lib");
        }
        if (!outputDir.exists() && !outputDir.mkdirs())
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());

        List<Project> projects = new MetatypeValidator(projectDir, outputDir).validate(!failOnExit);
        int rCode = -1;

        if (projects.isEmpty()) {
            System.out.println("uhh...something bad happened and no projects were returned...");
        } else {
            for (Project project : projects) {
                List<ValidationEntry> validationEntries = project.validationEntries;

                for (ValidationEntry validationEntry : validationEntries) {
                    if (validationEntry.validity == ValidityState.Pass) {
                        if (rCode == -1)
                            rCode = 0;
                    } else if (validationEntry.validity == ValidityState.Warning) {
                        if (rCode < 1)
                            rCode = 1;
                    } else if (validationEntry.validity == ValidityState.Failure) {
                        rCode = 2;
                    } else {
                        if (validationEntry.validity == ValidityState.MetatypeNotFound)
                            rCode = 3;
                        else if (validationEntry.validity == ValidityState.NotValidated)
                            rCode = 4;

                    }
                }
            }

            if (failOnExit) {
                if (rCode == 0)
                    System.out.println("Metatype validation passed.");
                else if (rCode == 1)
                    System.out.println("Metatype validation passed with warnings.");
                else if (rCode == 2) {
                    System.out.println("Metatype validation FAILED!");
                    System.exit(2);
                } else if (rCode == 3 || rCode == -1)
                    System.out.println("No metatype file(s) found.");
                else if (rCode == 4)
                    System.out.println("Could not validate metatype file(s).");
                else
                    System.out.println("return code " + rCode);
            }
        }
    }
}
