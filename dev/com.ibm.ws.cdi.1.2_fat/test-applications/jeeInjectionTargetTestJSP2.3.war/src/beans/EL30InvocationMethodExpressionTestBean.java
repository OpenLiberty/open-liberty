/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beans;

/**
 * This is a simple bean to test invocation of method expressions
 */
public class EL30InvocationMethodExpressionTestBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String parent;
    private final Child myChild;

    public EL30InvocationMethodExpressionTestBean() {
        myChild = new Child();
        parent = null;
    }

    public void setParentName(String parent) {
        this.parent = parent;
    }

    public String getParentName() {
        return parent;
    }

    public Child getChild() {
        return myChild;
    }

    @Override
    public String toString() {
        return "toString method of object with current parent name " + parent;
    }

    /**
     * Child class created to know the child of the parent
     */
    public class Child {

        private String childName;

        public Child() {
            this.childName = null;
        }

        public void setChildName(String name) {
            this.childName = name;
        }

        public String getChildName() {
            return childName;
        }
    }
}
