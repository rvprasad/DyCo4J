/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.jvm.instrumentation.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import static org.junit.Assert.assertEquals;

public final class LoggerTest {
    private StringWriter logStore;

    @Before
    public void setUp() throws Exception {
        logStore = new StringWriter();
        Logger.initialize(new PrintWriter(logStore));
    }

    @After
    public void tearDown() throws Exception {
        logStore.close();
    }

    @Test
    public void testLogArgument() throws Exception {
        final String _msg = "test message";
        final byte _idx = 1;
        Logger.logArgument(_idx, _msg);

        final String _expected = MessageFormat.format("1,{0},arg,{1},{2}",
                Thread.currentThread().getId(), _idx, _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogArray() throws Exception {
        final String[] _array = new String[]{"array"};
        final String _value = "value";
        final int _idx = 1;
        final Logger.Action _action = Logger.Action.GET;
        Logger.logArray(_array, _idx, _value, _action);

        final String _expected = MessageFormat.format("1,{0},{1},{2},{3},{4}",
                Thread.currentThread().getId(), _action + "A", _idx,
                Logger.toString(_array), _value);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogException() throws Exception {
        final Exception _tmp = new RuntimeException();
        Logger.logException(_tmp);

        final String _expected = MessageFormat.format("1,{0},exception,{1},{2}",
                Thread.currentThread().getId(), Logger.toString(_tmp),
                _tmp.getClass().getName());
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogFieldForInstanceField() throws Exception {
        final Object _object = 10;
        final String _fieldValue = "test";
        final String _fieldName = "message";
        final Logger.Action _action = Logger.Action.GET;
        Logger.logField(_object, _fieldValue, _fieldName, _action);

        final String _expected = MessageFormat.format("1,{0},{1},{2},{3},{4}",
                Thread.currentThread().getId(), _action + "F", _fieldName,
                Logger.toString(_object), _fieldValue);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogFieldForStaticField() throws Exception {
        final String _fieldValue = "test";
        final String _fieldName = "message";
        final Logger.Action _action = Logger.Action.PUT;
        Logger.logField(null, _fieldValue, _fieldName, _action);

        final String _expected = MessageFormat.format("1,{0},{1},{2},,{3}",
                Thread.currentThread().getId(), _action + "F", _fieldName,
                _fieldValue);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodEntry() throws Exception {
        final String _msg = "test message";
        Logger.logMethodEntry(_msg);

        final String _expected = MessageFormat.format("1,{0},entry,{1}",
                Thread.currentThread().getId(), _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogMethodExit() throws Exception {
        final String _msg = "test message";
        Logger.logMethodExit(_msg, "N");

        final String _expected = MessageFormat.format("1,{0},exit,{1},N",
                Thread.currentThread().getId(), _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogReturnForNonVoidValue() throws Exception {
        final String _msg = "test message";
        Logger.logReturn(_msg);

        final String _expected = MessageFormat.format("1,{0},return,{1}",
                Thread.currentThread().getId(), _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogReturnForVoidValue() throws Exception {
        Logger.logReturn(null);

        final String _expected = MessageFormat.format("1,{0},return",
                Thread.currentThread().getId());
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogStringForMultipleLogStmts() throws Exception {
        final String _msg1 = "test message 1";
        final String _msg2 = "test message 2";
        Logger.log(_msg1);
        Logger.log(_msg2);

        final String[] _tmp = getContent();
        final String _expected1 = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), _msg1);
        assertEquals(_expected1, _tmp[1]);

        final String _expected2 = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), _msg2);
        assertEquals(_expected2, _tmp[2]);
    }

    @Test
    public void testLogStringForOneLogStmt() throws Exception {
        final String _msg = "test message";
        Logger.log(_msg);

        final String _expected = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), _msg);
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testLogVarArgsForMultipleLogStmts() throws Exception {
        final String[] _msg1 = new String[]{"test", "message", "1"};
        Logger.log(_msg1);

        final String[] _msg2 = new String[]{"test", "message", "2"};
        Logger.log(_msg2);

        final String[] _tmp = getContent();
        final String _expected1 = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), String.join(",", _msg1));
        assertEquals(_expected1, _tmp[1]);

        final String _expected2 = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), String.join(",", _msg2));
        assertEquals(_expected2, _tmp[2]);
    }

    @Test
    public void testLogVarArgsForOneLogStmt() throws Exception {
        final String[] _msg = new String[]{"test", "message"};
        Logger.log(_msg);

        final String _expected = MessageFormat.format("1,{0},{1}",
                Thread.currentThread().getId(), String.join(",", _msg));
        assertEquals(_expected, getContent()[1]);
    }

    @Test
    public void testToStringWithBoolean() throws Exception {
        assertEquals("p_b:1", Logger.toString(true));
        assertEquals("p_b:0", Logger.toString(false));
    }

    @Test
    public void testToStringWithByte() throws Exception {
        assertEquals("p_y:10", Logger.toString((byte) 10));
    }

    @Test
    public void testToStringWithChar() throws Exception {
        assertEquals("p_c:" + Character.hashCode('c'), Logger.toString('c'));
    }

    @Test
    public void testToStringWithDouble() throws Exception {
        assertEquals("p_d:10.3", Logger.toString(10.3d));
    }

    @Test
    public void testToStringWithFloat() throws Exception {
        assertEquals("p_f:10.3", Logger.toString(10.3f));
    }

    @Test
    public void testToStringWithInt() throws Exception {
        assertEquals("p_i:10", Logger.toString(10));
    }

    @Test
    public void testToStringWithLong() throws Exception {
        assertEquals("p_l:10", Logger.toString(10L));
    }

    @Test
    public void testToStringWithObjectForArray() throws Exception {
        final String[] _tmp = new String[]{"array"};
        assertEquals("r_a:" + System.identityHashCode(_tmp),
                Logger.toString(_tmp));
    }

    @Test
    public void testToStringWithObjectForNull() throws Exception {
        assertEquals("r_o:null", Logger.toString(null));
    }

    @Test
    public void testToStringWithObjectForObject() throws Exception {
        final Integer _tmp = 10;
        assertEquals("r_o:" + System.identityHashCode(_tmp),
                Logger.toString(_tmp));
    }

    @Test
    public void testToStringWithObjectForString() throws Exception {
        final String _tmp = "string";
        assertEquals("r_s:" + System.identityHashCode(_tmp),
                Logger.toString(_tmp));
    }

    @Test
    public void testToStringWithObjectForThrowable() throws Exception {
        final Exception _tmp = new RuntimeException();
        assertEquals("r_t:" + System.identityHashCode(_tmp),
                Logger.toString(_tmp));
    }

    @Test
    public void testToStringWithShort() throws Exception {
        assertEquals("p_s:10", Logger.toString((short) 10));
    }

    private String[] getContent() {
        return logStore.toString().split("\\n");
    }
}
