/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.utility

import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class ProgramDataTest {

    @Test
    void writeAndReadNonEmptyDataObject() {
        final _tmp1 = new ProgramData()
        _tmp1.class2superClass['a'] = 'b'
        _tmp1.fieldId2Name['98'] = 'x1'
        _tmp1.shortFieldName2Id['f2'] = '23'
        _tmp1.methodId2Name['23'] = 'm1'
        _tmp1.shortMethodName2Id['m2'] = '908'
        final _tmp2 = File.createTempFile("pre", ".json").toPath()
        ProgramData.saveData(_tmp1, _tmp2)
        final _tmp3 = ProgramData.loadData(_tmp2)
        Files.delete(_tmp2)
        assert _tmp3 == _tmp1
    }

    @Test
    void writeAndReadEmptyDataObject() {
        final _tmp1 = File.createTempFile("pre", ".json").toPath()
        final _tmp2 = new ProgramData()
        ProgramData.saveData(_tmp2, _tmp1)
        final _tmp3 = ProgramData.loadData(_tmp1)
        Files.delete(_tmp1)
        assert _tmp3 == _tmp2
    }

    @Test
    void loadFromEmptyFile() {
        final _tmp1 = File.createTempFile("pre", ".json").toPath()
        final _tmp2 = ProgramData.loadData(_tmp1)
        Files.delete(_tmp1)
        assert _tmp2 == null
    }

    @Test
    void loadFromNonExistentFile() {
        final _tmp2 = ProgramData.loadData(Paths.get("This cannot exists!"))
        assert _tmp2 == (new ProgramData())
    }

    @Test
    void writeWhenDataFileIsPresent() {
        final _dataFile = File.createTempFile("pre", ".json").toPath()
        final _bakFile = Paths.get(_dataFile.toString() + ".bak")

        final _tmp1 = new ProgramData()
        _tmp1.class2superClass['a'] = 'b'
        ProgramData.saveData(_tmp1, _dataFile)
        assert Files.exists(_bakFile)

        final _tmp3 = ProgramData.loadData(_dataFile)
        _tmp3.fieldId2Name['98'] = 'x1'
        ProgramData.saveData(_tmp3, _dataFile)
        assert Files.exists(_bakFile)
        final _tmp4 = ProgramData.loadData(_bakFile)
        assert _tmp4 == _tmp1
    }
}
