/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class AuxiliaryDataTest {

    @Test
    void writeAndReadNonEmptyDataObject() {
        final _tmp1 = new AuxiliaryData()
        _tmp1.class2superClass['a'] = 'b'
        _tmp1.fieldId2Name['98'] = 'x1'
        _tmp1.shortFieldName2Id['f2'] = '23'
        _tmp1.methodId2Name['23'] = 'm1'
        _tmp1.shortMethodName2Id['m2'] = '908'
        final _tmp2 = File.createTempFile("pre", ".json").toPath()
        AuxiliaryData.saveData(_tmp1, _tmp2)
        final _tmp3 = AuxiliaryData.loadData(_tmp2)
        Files.delete(_tmp2)
        assert _tmp3.equals(_tmp1)
    }

    @Test
    void writeAndReadEmptyDataObject() {
        final _tmp1 = File.createTempFile("pre", ".json").toPath()
        final _tmp2 = new AuxiliaryData()
        AuxiliaryData.saveData(_tmp2, _tmp1)
        final _tmp3 = AuxiliaryData.loadData(_tmp1)
        Files.delete(_tmp1)
        assert _tmp3.equals(_tmp2)
    }

    @Test
    void loadFromEmptyFile() {
        final _tmp1 = File.createTempFile("pre", ".json").toPath()
        final _tmp2 = AuxiliaryData.loadData(_tmp1)
        Files.delete(_tmp1)
        assert _tmp2 == null
    }

    @Test
    void loadFromNonExistentFile() {
        final _tmp2 = AuxiliaryData.loadData(Paths.get("This cannot exists!"))
        assert _tmp2.equals(new AuxiliaryData())
    }

    @Test
    void writeWhenDataFileIsPresent() {
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        final _bakFile = Paths.get(_dataFile.toString() + ".bak")

        final _tmp1 = new AuxiliaryData()
        _tmp1.class2superClass['a'] = 'b'
        AuxiliaryData.saveData(_tmp1, _dataFile)
        assert Files.exists(_bakFile)

        final _tmp3 = AuxiliaryData.loadData(_dataFile)
        _tmp3.fieldId2Name['98'] = 'x1'
        AuxiliaryData.saveData(_tmp3, _dataFile)
        assert Files.exists(_bakFile)
        final _tmp4 = AuxiliaryData.loadData(_bakFile)
        assert _tmp4.equals(_tmp1)
    }
}
