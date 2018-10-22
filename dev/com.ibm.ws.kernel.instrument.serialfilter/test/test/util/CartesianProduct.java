package test.util;

import java.util.AbstractList;
import java.util.Iterator;

public class CartesianProduct extends AbstractList<Object[]> {

    private final Object[][] factors;
    private final int size;

    public static CartesianProduct of(Class<? extends Enum> e) {
        return of(e.getEnumConstants());
    }

    public static CartesianProduct of(Object[]set) {
        return new CartesianProduct(new Object[][]{set});
    }

    CartesianProduct(Object[][]sets) {
        this.factors = sets;
        int s = sets.length == 0 ? 0 : 1;
        for (Object[] set : sets) s *= set.length;
        this.size = s;
    }

    public CartesianProduct with(Class<? extends Enum> e) {
        return with(e.getEnumConstants());
    }

    public CartesianProduct with(Object[]set) {
        Object[][] sets = new Object[factors.length + 1][];
        System.arraycopy(factors, 0, sets, 0, factors.length);
        sets[factors.length] = set;
        return new CartesianProduct(sets);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] get(int index) {
        Object[] result = new Object[factors.length];
        for (int i = factors.length - 1; i >= 0; i--) {
            Object[] f = factors[i];
            result[i] = f[index%f.length];
            index /= f.length;
        }
        System.out.println("Test Index : " + index);
        return result;
    }

    @Override
    public Iterator<Object[]> iterator() {
        return new Iterator<Object[]>() {
            int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Object[] next() {
                Object[] result = new Object[factors.length];
                int r = index++;
                // fill up the array from the end
                for (int i = factors.length - 1; i >= 0; i--) {
                    Object[] f = factors[i];
                    result[i] = f[r%f.length];
                    r /= f.length;
                }
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public String toString() {
        if (isEmpty()) return "{}";
        String result = "{";
        for (Object[] arr : this) {
            result += "(";
            if (arr.length>0) {
                result += arr[0];
                for (int i = 1; i < arr.length; i++) result += "," + arr[i];
            }
            result += "), ";
        }
        result = result.substring(0, result.length() - 2) + "}";
        return result;
    }
}
