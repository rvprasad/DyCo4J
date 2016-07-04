/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import java.io.IOException;
import java.util.Date;

public class CLITestSubject {
    static int staticField;
    int instanceField;

    public static void main(String[] s) {
        exerciseStaticFeatures();
        exerciseInstanceFeatures();

        int[] _tmp1 = new int[3];
        _tmp1[2] = 28;
        System.out.println(_tmp1[0]);
    }

    static void exerciseInstanceFeatures() {
        CLITestSubject _tmp1 = new CLITestSubject();
        _tmp1.instanceField = 3;
        _tmp1.publishedInstanceMethod0();
        _tmp1.publishedInstanceMethod1(9);
        _tmp1.publishedInstanceMethod2('e');
        _tmp1.publishedInstanceMethod3(323.3f);
        _tmp1.publishedInstanceMethod4(898.98);
        _tmp1.publishedInstanceMethod5(true);
        _tmp1.publishedInstanceMethod6("Random");
        _tmp1.publishedInstanceMethod7(new Date());

        /*
        try {
            _tmp1.publishedInstanceExceptionalMethod1();
        } catch (IOException e) {

        }

        try {
            _tmp1.publishedInstanceExceptionalMethod2();
        } catch (RuntimeException e) {

        }
        */
    }

    static void exerciseStaticFeatures() {
        staticField = 4;
        publishedStaticMethod0();
        publishedStaticMethod1(9);
        publishedStaticMethod2('e');
        publishedStaticMethod3(323.3f);
        publishedStaticMethod4(898.98);
        publishedStaticMethod5(true);
        publishedStaticMethod6("Random");
        publishedStaticMethod7(new Date());

        /*
        try {
            publishedStaticExceptionalMethod1();
        } catch (IOException e) {

        }
        try {
            publishedStaticExceptionalMethod2();
        } catch (RuntimeException e) {

        }
        */
    }

    private static void publishedStaticExceptionalMethod1() throws IOException {
        throw new IOException("Test String");
    }

    private static void publishedStaticExceptionalMethod2() {
        throw new IllegalStateException();
    }

    private static boolean publishedStaticMethod0() {
        return staticField == 3;
    }

    private static float publishedStaticMethod1(int i) {
        return i * 3.0f;
    }

    private static boolean publishedStaticMethod2(char i) {
        return i == 'c';
    }

    private static char publishedStaticMethod3(float i) {
        return (char) i;
    }

    private static int publishedStaticMethod4(double i) {
        return (int) i * 3;
    }

    private static void publishedStaticMethod5(boolean i) {
    }

    private static Object publishedStaticMethod6(String i) {
        return new Object();
    }

    private static String publishedStaticMethod7(Date date) {
        return date.toString();
    }

    private void publishedInstanceExceptionalMethod1() throws IOException {
        throw new IOException("Test String");
    }
    private void publishedInstanceExceptionalMethod2() {
        throw new IllegalStateException();
    }

    private boolean publishedInstanceMethod0() {
        return instanceField == 3;
    }

    private float publishedInstanceMethod1(int i) {
        return i * 3.0f;
    }

    private boolean publishedInstanceMethod2(char i) {
        return i == 'c';
    }

    private char publishedInstanceMethod3(float i) {
        return (char) i;
    }

    private int publishedInstanceMethod4(double i) {
        return (int) i * 3;
    }

    private void publishedInstanceMethod5(boolean i) {
    }

    private Object publishedInstanceMethod6(String i) {
        return new Object();
    }

    private String publishedInstanceMethod7(Date date) {
        return date.toString();
    }
}
