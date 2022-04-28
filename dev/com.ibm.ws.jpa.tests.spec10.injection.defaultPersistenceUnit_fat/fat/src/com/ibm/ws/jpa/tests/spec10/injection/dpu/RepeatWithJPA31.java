/**
 *
 */
package com.ibm.ws.jpa.tests.spec10.injection.dpu;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatWithJPA31 extends JakartaEE10Action implements RepeatTestAction {
    public static final String ID = "JPA31";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "JPA 3.1";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        FATSuite.repeatPhase = "jpa31.xml";
    }
}
