package org.beynet.model.store;

import org.beynet.model.Config;

import java.nio.file.Paths;

/**
 * Created by beynet on 16/10/2014.
 */
public class RootTest {
    static {
        Config.initConfig("password", Paths.get(System.getProperty("java.io.tmpdir")),"jptest.dat");
    }
}
