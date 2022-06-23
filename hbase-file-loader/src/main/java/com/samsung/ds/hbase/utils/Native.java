package com.samsung.ds.hbase.utils;

public class Native {

    public static native long createIsolate();

    public static native String snappyCompress(long isolateThread, String value);

    public static native String snappyUncompress(long isolateThread, String value);

    public static native void decompressTest(long isolateThread, int numberOfThreads, int rowKeySetCount);

}
