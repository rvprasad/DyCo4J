/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

public class CLIInitializerInstrumentationTestSubject {
    public static void main(String[] s) {
        new X("Hi");
    }

    static class X extends Y {
        X(String s) {
            super(new Y(s));
        }
    }

    static class Y {
        Y(Object o) {
        }
    }
}

