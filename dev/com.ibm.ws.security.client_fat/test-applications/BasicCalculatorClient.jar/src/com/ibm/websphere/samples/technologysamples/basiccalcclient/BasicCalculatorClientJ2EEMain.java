/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.samples.technologysamples.basiccalcclient;


import java.util.*;
import java.io.File;
import java.text.*;

import com.ibm.websphere.samples.technologysamples.basiccalcclient.common.*;

public class BasicCalculatorClientJ2EEMain {

    /**
     * Displays a message to the user
     */
    private void printMessage(String message) {
        System.out.println(message);
    }

    /**
     * Displays standard program usage syntax.
     */
    private void printUsageSyntax() {
        System.out.println("Hello Application Client");
        //printUsageSyntax(null);
    }

    /**
     * Displays standard program usage syntax with a user message.
     */
    private void printUsageSyntax(String message) {
        if(message == null) {
            message = "Incorrect syntax.";
        }
        
        printMessage(message);
        printMessage("  Correct syntax: <Launch Arguments> <Operation> Operand1 Operand2");
        printMessage("     <Operation>: add | subtract | multiply | divide");
        printMessage("  Example: launchClient.bat/sh TechnologySamples.ear -CCjar=BasicCalculatorClient.jar -CCBootstrapHost=MyServer add 2 3");
    }

    /**
     * Handles all of the user-interaction for the BasicCalculator EJB.
     * Takes an array of strings in the order {<Operation>,<Operand1>,<Operand2>}
     * and invokes the BasicCalculatorClient.calculate() method to perform the 
     * actual calculation.  The results of the calculation are returned in a 
     * BasicCalculatorClientResultBean object and are displayed to the user.
     */
    public void doCalculation(String[] args) {
        String operation;
        double operand1;
        double operand2;
        int waitTime = 0;
        String keyLocation = null;
        //get the number format for the default local
        NumberFormat numberFormat = NumberFormat.getNumberInstance( Locale.getDefault() );

        // Validate the arguments
        // Arguments are in the form:  Operation, Operand1, Operand2 < waitTime, kenLocation>
        if ((args == null) || ((args.length != 3) && (args.length != 5))) {
            printUsageSyntax();
            return;
        }
             
        operation = args[0];
        printMessage("operation is " + operation);
        try {

            //retrieve operands respective of the locales
            operand1 = numberFormat.parse(args[1]).doubleValue();
            operand2 = numberFormat.parse(args[2]).doubleValue();
            printMessage("operand1 is " + operand1);
            printMessage("operand2 is " + operand2);
            if (args.length == 5) {
                waitTime = Integer.parseInt(args[3]);
                keyLocation = args[4];
            }
        }
        catch(ParseException pex)
        {
            printUsageSyntax("Parse exception on number input. Please enter a valid number.");
            return;
        }

        catch(NumberFormatException nfex)
        {
            printUsageSyntax("HEIDI Invalid number entered.  Please enter a valid number.");
            return;
        }

        // Create the BasicCalculatorClient object and the object to hold the result
        BasicCalculatorClientJ2EE bcc = new BasicCalculatorClientJ2EE();
        BasicCalculatorClientResultBean calcResult = null;
        //operation = "add";
        //operand1 = 1;
        //operand2 = 5;
        if (waitTime > 0) {
            waitForKeyFile(waitTime, keyLocation);
        }
        try {
            // Initialize the BasicCalculatorClient object
            // This will find and create home interface
            bcc.init();

            // Call the calculate method
            calcResult = bcc.calculate(operation, (double)operand1, (double)operand2);

            // Print the result
            printMessage("Result: " + numberFormat.format( new Double (calcResult.getOperand1())) + calcResult.getOperation() + numberFormat.format(new Double(calcResult.getOperand2())) + " = " + numberFormat.format(new Double (calcResult.getResult())) );
            printMessage("The call to the EJB was successful");

        }
        catch(javax.naming.NamingException ne) {
            printMessage("Unable to initialize the BasicCalculatorClient object.");
            printMessage("The server may not be setup correctly.");
            ne.printStackTrace();
        }
        catch(BasicCalculatorClient.CalcException_DivideByZero dz) {
            printMessage("Division by zero is not a valid operation.");
        }
        catch(BasicCalculatorClient.CalcException_InvalidOperation io) {
            printUsageSyntax("Operation: " + operation + " is not a valid operation.");
        }
        catch(Throwable e) {
            printMessage("Unable to initialize the BasicCalculatorClient.  Refer to the following Stack Trace for details.");
            e.printStackTrace();
        }
    }


   
    /**
     * Entry point to the program used by the J2EE Application Client Container
     */
    public static void main(String[] args) {
        
        BasicCalculatorClientJ2EEMain calcclient = new BasicCalculatorClientJ2EEMain();
        calcclient.doCalculation(args);
        // System.exit(0);
    }

    /**
      * check the existence of the specified file for every two seconds up to the specified wait time.
      * return if the file exist, or the specified wait time has passed.
      */
    private void waitForKeyFile(int waitTime, String keyLocation) {
        if (waitTime <= 0 || keyLocation == null) {
            // this is an error condition, do nothing.
            return;
        }
        int maxCount = waitTime/2;
        File f = new File(keyLocation);
        for (int i = 0; i < maxCount; i++) {
            if(f.exists()) {
                System.out.println("The file exists. Resuming the operation after 1 second delay");
                // this delay is for z/OS system. For some reason, if the code runs immediately after creating
                // the keystore, the naming code throws a NameNotFoundException.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();  // do nothing
                }
                
                return;
            }
            System.out.println("Wait for 2 seconds for a file creation of " + keyLocation);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();  // do nothing
            }
        }
        System.out.println("The wait time " + waitTime + " seconds has expired. Resuming the operation.");
        return;
    }


}
