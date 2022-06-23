package com.samsung.ds.hbase.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HbaseTest {

    public void test(int numberOfThreads, int rowKeySetCount) throws Exception {
        System.out.println("numberOfThreads : " + numberOfThreads);
        System.out.println("rowKeySetCount : " + rowKeySetCount);

        ConfigProperties configProperties = ConfigProperties.getInstance();
        String outputPath = configProperties.getValue("hbase.trace.output-path");

        long totalCount = 0L;

        long start = System.currentTimeMillis();
        Timestamp totalStart = new Timestamp(System.currentTimeMillis());

        long rowKeyStart = System.currentTimeMillis();
        // get rowkeys from mes
        List<String> rowKeys = getRowKeys();
        System.out.println("Native Row Key Select : " + new DecimalFormat("###.0").format((System.currentTimeMillis() - rowKeyStart) / 1000.0) + " seconds");

        long partKeyStart = System.nanoTime();
        List<String> tmpRowKeys = new ArrayList<>();
        for (int i = 0; i < rowKeySetCount; i++) {
            tmpRowKeys.addAll(rowKeys);
        }
        List<List<String>> partRowKeys = new ArrayList<>();
        int idx1 = (int) Math.ceil((double) tmpRowKeys.size() / numberOfThreads);
        System.out.printf("idx1 : %d%n", idx1);
        for (int i = 1; i <= numberOfThreads; i++) {
            List<String> result = new ArrayList<>();
            int idx2 = idx1 * i > tmpRowKeys.size() ? tmpRowKeys.size() : idx1 * i;
            System.out.printf("idx2 : %d%n", idx2);
            for (int j = (i-1) * idx1; j < idx2; j++) {
                result.add(tmpRowKeys.get(j));
            }
            partRowKeys.add(result);
        }
        System.out.printf("Native Part Row Key Set : (%d ns)%n", System.nanoTime() - partKeyStart);

        long executorStart = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Object>> callableTasks = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            int idx = i;
            Callable<Object> callableTask = () -> {
                return decompressTest(partRowKeys.get(idx));
            };
            callableTasks.add(callableTask);
        }

        try {
            List<Future<Object>> futures = executor.invokeAll(callableTasks);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (Future<Object> future : futures) {
                List<String> datas = (List<String>) future.get();
                for (String data : datas) {
                    baos.write(data.getBytes());
                }
            }
            System.out.println("Native Executor Service decompress : " + new DecimalFormat("###.0").format((System.currentTimeMillis() - executorStart) / 1000.0) + " seconds");

            long fileStart = System.currentTimeMillis();
            // write csv file
            File file = new File(outputPath, String.format("trace_data_write_%d.csv", System.currentTimeMillis()));
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            Timestamp writeStart = new Timestamp(System.currentTimeMillis());
            try {
                IOUtils.copy(is, new FileOutputStream(file));
                is.close();
                System.out.println("[CSV FILE] create csv file!!!!");
            } catch (Exception e) {
                // TODO
                throw new RuntimeException("CSV 파일 생성을 실패하였습니다." + e);
            }
            Timestamp writeEnd = new Timestamp(System.currentTimeMillis());
            System.out.printf("[WRITE] START TIME : %s%n", writeStart.toString());
            System.out.printf("[WRITE] END TIME : %s%n", writeEnd.toString());
            System.out.println("Native File Write : " + new DecimalFormat("###.0").format((System.currentTimeMillis() - fileStart) / 1000.0) + " seconds");
        } catch (Exception e) {
            System.out.printf("[ERROR] EXECUTOR ERROR : %s%n", e.toString());
        } finally {
            executor.shutdown();
            HbaseClient.getInstance().connection.close();
        }
        System.out.printf("[TOTAL] START TIME : %s%n", totalStart.toString());
        System.out.printf("[TOTAL] END TIME : %s%n", new Timestamp(System.currentTimeMillis()).toString());

        long end = System.currentTimeMillis();
        System.out.println("Native Total Time : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " seconds");
    }

    private List<String> decompressTest(List<String> rowKeys) throws IOException {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String refValueQualifier = configProperties.getValue("hbase.trace.ref-value");

        long tableStart = System.currentTimeMillis();
        Table table = getHbaseTable();
        System.out.println("Native Table Set : " + new DecimalFormat("###.0").format((System.currentTimeMillis() - tableStart) / 1000.0) + " seconds");

        long functionStart = System.nanoTime();
        long start = System.nanoTime();
        long forStart = System.currentTimeMillis();

        List<String> traceDatas = new ArrayList<>();
        long rowNum = 0;

        for (String rowKey : rowKeys) {
            //System.out.printf("[DECOMPRESS] ROW KEY : %s%n", rowKey);

            Result r = getResultFromHbase(rowKey, table);
            for (byte[] columnFamily : r.getMap().keySet()) {
                for (byte[] qualifier : r.getMap().get(columnFamily).keySet()) {
                    String strColumnFamily = new String(columnFamily);
                    String strQualifier = new String(qualifier);
                    //System.out.printf("[DECOMPRESS] COLUMN FAMILY : %s, QUALIFIER : %s%n", strColumnFamily, strQualifier);

                    String refValue = new String(r.getValue(columnFamily, refValueQualifier.getBytes()));
                    //System.out.printf("[DECOMPRESS] REF VALUE : %s%n", refValue);

                    if (!strQualifier.equals(refValueQualifier)) {
                        // uncompressed trace data
                        String value = new String(r.getValue(columnFamily, qualifier));
                        //System.out.printf("raw data : %s%n", value);

                        functionStart = System.nanoTime();
                        start = System.nanoTime();
                        //String data = new String(Snappy.uncompress(Base64.decodeBase64(value)));
                        byte[] base64DecodeData = Base64.decodeBase64(value);
                        //System.out.printf(rowNum + "No : Native Apache Base 64 Decode : (%d ns)%n", System.nanoTime() - start);

                        start = System.nanoTime();
                        String data = Snappy.uncompressString(base64DecodeData);
                        //System.out.printf(rowNum + "No : Native Snappy Uncompress : (%d ns)%n", System.nanoTime() - start);
                        //System.out.printf(rowNum + "No : Native Snappy Uncompress Function Call Time : (%d ns)%n", System.nanoTime() - functionStart);

                        //System.out.printf("decomressed data : %s%n", data);

                        // unnest trace data
                        String[] datas = data.split(",");
                        List<String> c3 = splitData(datas[2]);
                        List<String> c4 = splitData(datas[3]);
                        List<String> c5 = splitData(datas[4]);
                        List<String> c6 = splitData(datas[5]);
                        List<String> c7 = splitData(datas[6]);
                        List<String> c8 = splitData(datas[7]);

                        for (int i = 0; i < c3.size(); i++) {
                            String traceData = String.format("%s,%s,%s,%s,%s,%s,%s,%s\n", datas[0], datas[1], c3.get(i), c4.get(i), c5.get(i), c6.get(i), c7.get(i), c8.get(i));
                            traceDatas.add(String.format("%d,%s,%s,%s", rowNum, refValue, qualifier, traceData));
                            rowNum++;
                        }

                    }
                }
            }
        }

        System.out.println("Native For Total Time : " + new DecimalFormat("###.0").format((System.currentTimeMillis() - forStart) / 1000.0) + " seconds");

        return traceDatas;
    }

    private List<String> getRowKeys() throws Exception {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String sampleQuery = configProperties.getValue("hbase.trace.sample-query-2");

        List<Map<String, Object>> rowKeys = JdbcTemplate.getInstance().queryForList(sampleQuery, null);
        List<String> result = new ArrayList<>();

        for (Map<String, Object> rowKey : rowKeys) {
            result.add(rowKey.get("record_key").toString());
        }

        return result;
    }

    private Table getHbaseTable() throws IOException {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String hbaseTableName = configProperties.getValue("hbase.trace.table-name");

        TableName tableName = TableName.valueOf(hbaseTableName);
        return HbaseClient.getInstance().connection.getTable(tableName);
    }

    private Result getResultFromHbase(String rowKey, Table table) throws IOException {
        Get g = new Get(Bytes.toBytes(rowKey));
        return table.get(g);
    }

    private List<String> splitData(String data) {
        return Arrays.asList(data.split("\\|"));
    }
}
