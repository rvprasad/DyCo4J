/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import org.junit.Test;

public class CLITestSubject {

    public static void main(String[] s) {
        final CLITestSubject _t = new CLITestSubject();
        _t.test1();
        _t.test2();
        _t.testSuffix1();
        _t.testSuffix2();
        _t.nonTest();
    }

    void test1() {
    }

    void testSuffix1() {
    }

    @Test
    void test2() {
    }

    @Test
    void testSuffix2() {
    }

    void nonTest() {
    }

}
