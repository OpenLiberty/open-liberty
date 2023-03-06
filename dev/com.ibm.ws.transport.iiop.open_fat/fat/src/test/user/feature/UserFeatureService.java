/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.user.feature;

import java.io.Serializable;

public class UserFeatureService implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String message;

    public UserFeatureService(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof UserFeatureService) {
            UserFeatureService other = (UserFeatureService) o;
            equal = (this.getMessage().equals(other.getMessage()));
        }
        return equal;
    }

}
