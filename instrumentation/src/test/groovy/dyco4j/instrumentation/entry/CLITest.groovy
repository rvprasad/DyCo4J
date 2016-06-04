/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry

import dyco4j.instrumentation.logging.LoggerInitializer
import org.junit.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CLITest {
    private static final Path PROPERTY_FILE = Paths.get("build", "classes", "test", "dyco4j", "instrumentation",
            "logging", "logging.properties");
    private static final Path TRACES_FOLDER = Paths.get("build", "tmp", "traces")

    @BeforeClass
    static void setUpClass() {
        Files.createDirectories(PROPERTY_FILE.getParent())
        Files.createFile(PROPERTY_FILE).withWriter {
            it.println("traceFolder=build/tmp/traces")
        }
    }

    @AfterClass
    static void tearDownClass() {
        PROPERTY_FILE.toFile().delete()
    }

    @Before
    void setUp() {
        Files.createDirectories(TRACES_FOLDER)
    }

    @After
    void tearDown() {
        Files.find(TRACES_FOLDER, 1, {p, _ -> p.fileName.toString().matches("^trace.*gz")}).each {
            it.toFile().delete()
        }
    }

    @Test
    void test1() {
        LoggerInitializer.initialize();
    }
}
