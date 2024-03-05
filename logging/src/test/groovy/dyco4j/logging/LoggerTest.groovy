/*
 * Copyright (c) 2024, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */
package dyco4j.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

final class LoggerTest {
    private StringWriter logStore

    @BeforeEach
    void setUp() {
        logStore = new StringWriter()
        Logger.initialize(new PrintWriter(logStore))
    }

    @AfterEach
    void tearDown() {
        logStore.close()
    }

    @Test
    void testLogArgument() {
        final _msg = "test message"
        final byte _idx = 1
        Logger.logArgument(_idx, _msg)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_ARG_TAG},$_idx,$_msg"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogGetArray() {
        final String[] _array = ["array"]
        final _value = "value"
        final _idx = 1
        final _action = Logger.ArrayAction.GETA
        Logger.logArray(_array, _idx, _value, _action)

        final _expected = "${getCurrThreadId()},$_action,$_idx,${Logger.toString(_array)},$_value"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogPutArray() {
        final String[] _array = ["array"]
        final _value = "value"
        final _idx = 1
        final _action = Logger.ArrayAction.PUTA
        Logger.logArray(_array, _idx, _value, _action)

        final _expected = "${getCurrThreadId()},$_action,$_idx,${Logger.toString(_array)},$_value"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogException() {
        final _tmp = new RuntimeException()
        Logger.logException(_tmp)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_EXCEPTION_TAG},${Logger.toString(_tmp)},${_tmp.getClass().getName()}"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogFieldForInstanceField() {
        final _object = [:]
        final _fieldValue = "test"
        final _fieldName = "message"
        final _action = Logger.FieldAction.GETF
        Logger.logField(_object, _fieldValue, _fieldName, _action)

        final _expected = "${getCurrThreadId()},$_action,$_fieldName,${Logger.toString(_object)},$_fieldValue"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogFieldForStaticField() {
        final _fieldValue = "test"
        final _fieldName = "message"
        final _action = Logger.FieldAction.PUTF
        Logger.logField(null, _fieldValue, _fieldName, _action)

        final _expected = "${getCurrThreadId()},$_action,$_fieldName,,$_fieldValue"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogMethodCall() {
        final _msg = "test message"
        Logger.logMethodCall(_msg)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_CALL_TAG},$_msg"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogMethodEntry() {
        final _msg = "test message"
        Logger.logMethodEntry(_msg)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_ENTRY_TAG},$_msg"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogMethodExitNormal() {
        final _msg = "test message"
        Logger.logMethodExit(_msg, "N")

        final _expected = "${getCurrThreadId()},${Logger.METHOD_EXIT_TAG},$_msg,N"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogMethodExitExceptional() {
        final _msg = "test message"
        Logger.logMethodExit(_msg, "E")

        final _expected = "${getCurrThreadId()},${Logger.METHOD_EXIT_TAG},$_msg,E"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogReturnForNonVoidValue() {
        final _msg = "test message"
        Logger.logReturn(_msg)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_RETURN_TAG},$_msg"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogReturnForVoidValue() {
        Logger.logReturn(null)

        final _expected = "${getCurrThreadId()},${Logger.METHOD_RETURN_TAG}"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogStringForMultipleLogStmts() {
        final _msg1 = "test message 1"
        final _msg2 = "test message 2"
        Logger.log(_msg1)
        Logger.log(_msg2)

        final _tmp = getContent()
        final _expected1 = "${getCurrThreadId()},$_msg1"
        assert _expected1 == _tmp[1]

        final _expected2 = "${getCurrThreadId()},$_msg2"
        assert _expected2 == _tmp[2]
    }

    @Test
    void testLogStringForOneLogStmt() {
        final _msg = "test message"
        Logger.log(_msg)

        final _expected = "${getCurrThreadId()},$_msg"
        assert _expected == getContent()[1]
    }

    @Test
    void testLogStringForIdenticalLogStmts() {
        final _msg1 = "test message 1"
        Logger.log(_msg1)
        Logger.log(_msg1)
        Logger.log(_msg1)
        final _msg2 = "test message 2"
        Logger.log(_msg2)
        Logger.log(_msg2)
        Logger.cleanupForTest()

        final _tmp1 = getContent()
        final _expected1 = "${getCurrThreadId()},$_msg1"
        assert _expected1 == _tmp1[1]
        final _expected2 = "${getCurrThreadId()},$_msg1,2"
        assert _expected2 == _tmp1[2]
        final _expected3 = "${getCurrThreadId()},$_msg2"
        assert _expected3 == _tmp1[3]
        final _expected4 = "${getCurrThreadId()},$_msg2,1"
        assert _expected4 == _tmp1[4]
    }

    @Test
    void testLogVarArgsForMultipleLogStmts() {
        final String[] _msg1 = ["test", "message", "1"]
        Logger.log(_msg1)

        final String[] _msg2 = ["test", "message", "2"]
        Logger.log(_msg2)

        final _tmp = getContent()
        final _expected1 = "${getCurrThreadId()},${_msg1.join(',')}"
        assert _expected1 == _tmp[1]

        final _expected2 = "${getCurrThreadId()},${_msg2.join(',')}"
        assert _expected2 == _tmp[2]
    }

    @Test
    void testLogVarArgsForOneLogStmt() {
        final String[] _msg = ["test", "message"]
        Logger.log(_msg)

        final _expected = "${getCurrThreadId()},${_msg.join(',')}"
        assert _expected == getContent()[1]
    }

    @Test
    void testToStringWithBoolean() {
        assert Logger.toString(true) ==~ /^${Logger.TRUE_VALUE}/
        assert Logger.toString(false) ==~ /^${Logger.FALSE_VALUE}/
    }

    @Test
    void testToStringWithByte() {
        final byte _tmp = 10
        assert Logger.toString(_tmp) ==~ /^${Logger.BYTE_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString(20)
    }

    @Test
    void testToStringWithChar() {
        final char _tmp = 'c'
        assert Logger.toString(_tmp) ==~ /^${Logger.CHAR_TYPE_TAG}${Character.hashCode(_tmp)}$/
        assert Logger.toString(_tmp) != Logger.toString('d')
    }

    @Test
    void testToStringWithDouble() {
        final double _tmp = 10.3d
        assert Logger.toString(_tmp) ==~ /^${Logger.DOUBLE_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString(10.4d)
    }

    @Test
    void testToStringWithFloat() {
        final float _tmp = 10.3f
        assert Logger.toString(_tmp) ==~ /^${Logger.FLOAT_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString(10.4f)
    }

    @Test
    void testToStringWithInt() {
        final int _tmp = 10
        assert Logger.toString(_tmp) ==~ /^${Logger.INT_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString(20)
    }

    @Test
    void testToStringWithLong() {
        final long _tmp = 10L
        assert Logger.toString(_tmp) ==~ /^${Logger.LONG_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString(20L)
    }

    @Test
    void testToStringWithObjectForArray() {
        final String[] _tmp = ["array"]
        assert Logger.toString(_tmp) ==~ /^${Logger.ARRAY_TYPE_TAG}\d+$/
        assert Logger.toString(_tmp) != Logger.toString(["array"].toArray())
    }

    @Test
    void testToStringWithObjectForNull() {
        assert Logger.NULL_VALUE == Logger.toString(null)
    }

    @Test
    void testToStringWithObjectForObject() {
        final _tmp = Logger.class
        assert Logger.toString(_tmp) ==~ /^${Logger.OBJECT_TYPE_TAG}\d+$/
        assert Logger.toString(_tmp) != Logger.toString(Integer.valueOf(4))
    }

    @Test
    void testToStringWithObjectForString() {
        final _tmp = "string"
        assert Logger.toString(_tmp) ==~ /^${Logger.STRING_TYPE_TAG}\d+$/
        assert Logger.toString(_tmp) != Logger.toString("str")
    }

    @Test
    void testToStringWithObjectForThrowable() {
        final _tmp = new RuntimeException()
        assert Logger.toString(_tmp) ==~ /^${Logger.THROWABLE_TYPE_TAG}\d+/
        assert Logger.toString(_tmp) != Logger.toString(new IllegalStateException())
    }

    @Test
    void testToStringWithShort() {
        final short _tmp = 10
        assert Logger.toString(_tmp) ==~ /^${Logger.SHORT_TYPE_TAG}$_tmp$/
        assert Logger.toString(_tmp) != Logger.toString((short) 20)
    }

    private String[] getContent() {
        return logStore.toString().split(System.lineSeparator())
    }

    private static long getCurrThreadId() {
        return Thread.currentThread().getId()
    }
}
