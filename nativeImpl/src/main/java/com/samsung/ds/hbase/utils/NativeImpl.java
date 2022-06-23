package com.samsung.ds.hbase.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.Pointer;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;


public final class NativeImpl {
    @CEntryPoint(name = "Java_com_samsung_ds_hbase_Native_createIsolate", builtin=CEntryPoint.Builtin.CREATE_ISOLATE)
    public static native IsolateThread createIsolate();

    @CEntryPoint(name = "Java_com_samsung_ds_hbase_Native_Native_snappyCompress")
    public static JNIEnv.JString snappyCompress(JNIEnv jniEnv, Pointer clazz, @CEntryPoint.IsolateThreadContext long isolateId, JNIEnv.JString dataCharStr) {
        JNIEnv.JNINativeInterface fn = jniEnv.getFunctions();
        CCharPointer charPtr = fn.getGetStringUTFChars().call(jniEnv, dataCharStr, (byte) 0);
        String value  = CTypeConversion.toJavaString(charPtr);

        if (value == null || "".equals(value.trim())) {
            return getJString(jniEnv, value);
        }

        // 데이터에 한글 있을 경우 자바는 유니코드로 처리하고 C는 KSC5601 언어셋을 사용하여 문제가 되므로, 다음과 같이 문자열을 변경해주거나
        // value.getBytes("KSC5601") 다음과 같이 변환하여 넘겨줌. 다시 자바에서는 new String(value)로 유니코드로 변환처리함.
        //value = StringEscapeUtils.unescapeJava(value);

        String compress = "";
        try {
            compress = Base64.encodeBase64String(Snappy.compress(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
            compress = "";
        }

        return getJString(jniEnv, compress);
    }

    @CEntryPoint(name = "Java_com_samsung_ds_hbase_Native_Native_snappyUncompress")
    public static JNIEnv.JString snappyUncompress(JNIEnv jniEnv, Pointer clazz, @CEntryPoint.IsolateThreadContext long isolateId, JNIEnv.JString dataCharStr) {
        JNIEnv.JNINativeInterface fn = jniEnv.getFunctions();
        CCharPointer charPtr = fn.getGetStringUTFChars().call(jniEnv, dataCharStr, (byte) 0);
        String value  = CTypeConversion.toJavaString(charPtr);

        if (value == null || "".equals(value.trim())) {
            return getJString(jniEnv, value);
        }

        String uncompress = "";
        try {

            long start = System.nanoTime();
            byte[] base64DecodeData = Base64.decodeBase64(value);
            System.out.printf("Native Apache Base64 Decode : (%d ns)%n", System.nanoTime() - start);

            start = System.nanoTime();
            uncompress = Snappy.uncompressString(base64DecodeData);
            System.out.printf("Native Snappy Uncompress : (%d ns)%n", System.nanoTime() - start);

            // 데이터에 한글 있을 경우 자바는 유니코드로 처리하고 C는 KSC5601 언어셋을 사용하여 문제가 되므로, 다음과 같이 문자열을 변경해주거나
            // value.getBytes("KSC5601") 다음과 같이 변환하여 넘겨줌. 다시 자바에서는 new String(value)로 유니코드로 변환처리함.
            //uncompress = StringEscapeUtils.escapeJava(uncompress);

        } catch (Exception e) {
            e.printStackTrace();
            uncompress = "";
        }

        return getJString(jniEnv, uncompress);
    }

    private static JNIEnv.JString getJString(JNIEnv jniEnv, String value) {
        if (value == null) {
            value = "";
        }

        JNIEnv.JNINativeInterface fn = jniEnv.getFunctions();
        try (final CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(value)) {
            final CCharPointer p = holder.get();
            return fn.getNewStringUTF().call(jniEnv, p);
        } catch (Exception e) {
            e.printStackTrace();
            final CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString("");
            final CCharPointer p = holder.get();
            return fn.getNewStringUTF().call(jniEnv, p);
        }
    }

    @CEntryPoint(name = "Java_com_samsung_ds_hbase_Native_Native_decompressTest")
    public static void decompressTest(JNIEnv jniEnv, Pointer clazz, @CEntryPoint.IsolateThreadContext long isolateId, int numberOfThreads, int rowKeySetCount) throws Exception {
        HbaseTest hbaseTest = new HbaseTest();
        hbaseTest.test(numberOfThreads, rowKeySetCount);
    }

    public static void main(String[] args) throws Exception {}
}