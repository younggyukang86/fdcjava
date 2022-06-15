import org.apache.commons.io.IOUtils;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Main {

    private static String filePath = "c:\\test\\testSample1.txt";

    public static void main(String[] args) throws Exception {
        System.out.println("GraalVM TEST Snappy unnest!!");
        long start = System.currentTimeMillis();

        getTableDistributeFile("2");

        long end = System.currentTimeMillis();
        System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " 초");
    }

    private static void getTableDistributeFile(String num) throws Exception {
        Timestamp totalStart = new Timestamp(System.currentTimeMillis());
        int numberOfThreads = Integer.valueOf(num);
        List<String> rowKeys = getRowKeys();

        List<List<String>> partRowKeys = new ArrayList<>();
        int idx1 = (int) Math.ceil((double) rowKeys.size() / numberOfThreads);
        for (int i = 1; i <= numberOfThreads; i++) {
            List<String> result = new ArrayList<>();
            int idx2 = idx1 * i > rowKeys.size() ? rowKeys.size() : idx1 * i;
            for (int j = (i-1) * idx1; j < idx2; j++) {
                result.add(rowKeys.get(j));
            }
            partRowKeys.add(result);
        }

        List<String> tableDatas = getTableDatas();

        //decompressTestDistributeFile(rowKeys, 1, tableDatas);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Object>> callableTasks = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            int idx = i;
            Callable<Object> callableTask = () -> {
                return decompressTestDistributeFile(partRowKeys.get(idx), idx, tableDatas);
            };
            callableTasks.add(callableTask);
        }
        try {
            List<Future<Object>> futures = executor.invokeAll(callableTasks);
        } catch (Exception e) {
            System.out.println("[ERROR] EXECUTOR ERROR : " + e);
        } finally {
            executor.shutdown();
        }
        System.out.println("[TOTAL] START TIME : " + totalStart);
        System.out.println("[TOTAL] END TIME : " + new Timestamp(System.currentTimeMillis()));
    }

    private static long decompressTestDistributeFile(List<String> rowKeys, int threadNum, List<String> tableDatas) throws Exception {
        String folder = "c:\\test\\jsm_trace_test\\";
        //String fileName = String.format("trace_data_write_%d.csv", System.currentTimeMillis());
        String fileName = String.format("trace_data_write_%d.csv", System.nanoTime());
        System.out.println(fileName);

        // 1. 기존 샘플 소스
        /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long rowNum = 0;
        for (String rowKey : rowKeys) {
            String rowData = getResultFromHbase(rowKey, tableDatas);
            List<String> colums = Arrays.asList(rowData.split("\\$"));
            String refValue = colums.get(colums.size() - 1).split("#")[1];
            String rowKwyColumn = colums.get(0);

            for (int i = 1; i < colums.size() - 1; i++) {
                String header = colums.get(i).split("#")[0];
                String qualifier = hbaseQualifierReplace(header.split("@")[0]);
                String columnFamily = header.split("@")[1];

                // uncompressed trace data
                String data = new String(Snappy.uncompress(decode(colums.get(i).split("#")[1])));
                // unnest trace data
                String[] datas = data.split(",");
                List<String> c3 = splitData(datas[2]);
                List<String> c4 = splitData(datas[3]);
                List<String> c5 = splitData(datas[4]);
                List<String> c6 = splitData(datas[5]);
                List<String> c7 = splitData(datas[6]);
                List<String> c8 = splitData(datas[7]);

                for (int j = 0; j < c3.size(); j++) {
                    String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s \n", datas[0], datas[1], c3.get(j), c4.get(j), c5.get(j), c6.get(j), c7.get(j), c8.get(j));
                    String completeTraceData = String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData);
                    baos.write(completeTraceData.getBytes());
                    rowNum++;
                }
            }
        }*/

        // 기존 샘플 소스 io 전체 쓰기
        /*File file = new File(folder, fileName);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            IOUtils.copy(is, new FileOutputStream(file));
            is.close();
            System.out.println("[CSV FILE] create csv file!!!!");
        } catch(Exception e) {
            throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
        }*/

        // 2. NIO 전체 쓰기
        /*final int bufferMaxSize = 1024 * 1024;
        try (FileChannel resultChannel = FileChannel.open(Paths.get(folder + fileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferMaxSize);
            buffer = ByteBuffer.wrap(baos.toByteArray());
            resultChannel.write(buffer);
            resultChannel.close();
            System.out.println("[CSV FILE] create csv file!!!!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
        }*/

        // 3. for 문돌면서 NIO 조금씩 쓰기
        /*long rowNum = 0;
        final int bufferMaxSize = 1024 * 1024;
        try (FileChannel resultChannel = FileChannel.open(Paths.get(folder + fileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (String rowKey : rowKeys) {
                String rowData = getResultFromHbase(rowKey, tableDatas);
                List<String> colums = Arrays.asList(rowData.split("\\$"));
                String refValue = colums.get(colums.size() - 1).split("#")[1];
                String rowKwyColumn = colums.get(0);

                for (int i = 1; i < colums.size() - 1; i++) {
                    String header = colums.get(i).split("#")[0];
                    String qualifier = hbaseQualifierReplace(header.split("@")[0]);
                    String columnFamily = header.split("@")[1];

                    // uncompressed trace data
                    String data = new String(Snappy.uncompress(decode(colums.get(i).split("#")[1])));
                    // unnest trace data
                    String[] datas = data.split(",");
                    List<String> c3 = splitData(datas[2]);
                    List<String> c4 = splitData(datas[3]);
                    List<String> c5 = splitData(datas[4]);
                    List<String> c6 = splitData(datas[5]);
                    List<String> c7 = splitData(datas[6]);
                    List<String> c8 = splitData(datas[7]);

                    for (int j = 0; j < c3.size(); j++) {
                        String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s \n", datas[0], datas[1], c3.get(j), c4.get(j), c5.get(j), c6.get(j), c7.get(j), c8.get(j));
                        String completeTraceData = String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData);

                        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferMaxSize);
                        Charset charset = Charset.defaultCharset();
                        buffer = charset.encode(completeTraceData);
                        resultChannel.write(buffer);

                        rowNum++;
                    }
                }
            }

            resultChannel.close();
            System.out.println("[CSV FILE] create csv file!!!!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
        }*/

        // 4. for 문돌면서 IO 조금씩 쓰기

        File file = new File(folder, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }

        long rowNum = 0;
        try (FileOutputStream fos = new FileOutputStream(folder + fileName, true)) {

            for (String rowKey : rowKeys) {
                String rowData = getResultFromHbase(rowKey, tableDatas);
                List<String> colums = Arrays.asList(rowData.split("\\$"));
                String refValue = colums.get(colums.size() - 1).split("#")[1];
                String rowKwyColumn = colums.get(0);

                for (int i = 1; i < colums.size() - 1; i++) {
                    String header = colums.get(i).split("#")[0];
                    String qualifier = hbaseQualifierReplace(header.split("@")[0]);
                    String columnFamily = header.split("@")[1];

                    // uncompressed trace data
                    String data = new String(Snappy.uncompress(decode(colums.get(i).split("#")[1])));
                    // unnest trace data
                    String[] datas = data.split(",");
                    List<String> c3 = splitData(datas[2]);
                    List<String> c4 = splitData(datas[3]);
                    List<String> c5 = splitData(datas[4]);
                    List<String> c6 = splitData(datas[5]);
                    List<String> c7 = splitData(datas[6]);
                    List<String> c8 = splitData(datas[7]);

                    for (int j = 0; j < c3.size(); j++) {
                        String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s \n", datas[0], datas[1], c3.get(j), c4.get(j), c5.get(j), c6.get(j), c7.get(j), c8.get(j));
                        String completeTraceData = String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData);
                        fos.write(completeTraceData.getBytes(StandardCharsets.UTF_8));
                        rowNum++;
                    }
                }
            }

            System.out.println("[CSV FILE] create csv file!!!!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
        }

        return rowNum;
    }

    private static List<String> splitData(String data) {
        return Arrays.asList(data.split("\\|"));
    }

    private static String getResultFromHbase(String rowKey, List<String> tableDatas) throws IOException {
        return tableDatas.get(Integer.valueOf(rowKey) - 1);
    }

    private static String hbaseQualifierReplace(String qualifier) {
        return qualifier.replace("^", ",");
    }

    private static List<String> getRowKeys() {
        List<String> result = new ArrayList<>();
        Path path = Paths.get(filePath);
        try {
            long lineCount = Files.lines(path).count();

            System.out.println(lineCount);

            for (int i = 1; i <= lineCount; i++) {
                result.add(String.valueOf(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static List<String> getTableDatas() {
        List<String> result = new ArrayList<>();

        Path path = Paths.get(filePath);
        Charset cs = StandardCharsets.UTF_8;
        try {
            result = Files.readAllLines(path, cs);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] decode(String base64) throws Exception {
        return Base64.getDecoder().decode(base64);
    }
}
