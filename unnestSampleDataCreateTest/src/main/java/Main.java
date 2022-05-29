import org.apache.commons.io.IOUtils;
import org.xerial.snappy.Snappy;

import java.io.*;
import java.util.Base64;

public class Main {

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        StringBuilder data = new StringBuilder();

        int row = 10000;
        int column = 50;

        // 컬럼 0,1
        StringBuilder columnData = new StringBuilder();
        columnData.append("0,371,");
        // 컬럼 2
        for (int j = 1; j <= column; j++) {
            columnData.append("20210205165036091");
            if (j != column) {
                columnData.append("|");
            }
        }
        // 컬럼 3
        columnData.append(",");
        for (int j = 1; j <= column; j++) {
            columnData.append("POSTSTEP");
            if (j != column) {
                columnData.append("|");
            }
        }
        // 컬럼 4
        columnData.append(",");
        for (int j = 1; j <= column; j++) {
            columnData.append("0.0");
            if (j != column) {
                columnData.append("|");
            }
        }
        // 컬럼 5
        columnData.append(",");
        for (int j = 1; j <= column; j++) {
            columnData.append("Nan");
            if (j != column) {
                columnData.append("|");
            }
        }
        // 컬럼 6
        columnData.append(",");
        for (int j = 1; j <= column; j++) {
            columnData.append("F");
            if (j != column) {
                columnData.append("|");
            }
        }
        // 컬럼 7
        columnData.append(",");
        for (int j = 1; j <= column; j++) {
            columnData.append("Nan");
            if (j != column) {
                columnData.append("|");
            }
        }

        byte[] compressed = Snappy.compress(columnData.toString().getBytes("UTF-8"));
        String snappyBase64ColumnData = encode(compressed);

        compressed = Snappy.compress("S1^KFBKA^METAL^ENDURA2_CGA_MTJ_SINGLE_97019^SUMM01^175298^C^MC^MC_PVD_T5_FL^3^MC_PVD_T5_FL^KE725850^KZT6^S4LP173B01-K0Z^PMH10U^ALL^NULL^13350444^2300460901^TRACE_STEP_MAX_TIME^TR^0^D^F^20210205^NPNBRN".getBytes("UTF-8"));
        String snappyBase64RefColumnData = encode(compressed);


        for (int i = 1; i <= row; i++) {
            if (i != 1) {
                data.append("\n");
            }

            StringBuilder rowData = new StringBuilder();
            rowData.append("ROW_KEY#" + i + "$LOT_ID^WAFER_NO^SLOT_NO@CF : 000#" + snappyBase64ColumnData);
            rowData.append("$NPNBR." + i + "^01^01@CF : 000#" + snappyBase64ColumnData);
            rowData.append("$NPNBR." + i + "^02^02@CF : 000#" + snappyBase64ColumnData);
            rowData.append("$NPNBR." + i + "^03^03@CF : 000#" + snappyBase64ColumnData);
            rowData.append("$ref_value@CF : 000#" + snappyBase64RefColumnData);

            data.append(rowData);
        }

        long end = System.nanoTime();
        System.out.println("테스트 TXT 파일 생성 수행시간1 : " + String.valueOf(end - start) + " ns");

        start = System.nanoTime();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data.toString().getBytes());

        File file = new File("/test/", String.format("testSample.txt", System.currentTimeMillis()));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            IOUtils.copy(is, new FileOutputStream(file));
            is.close();
            System.out.println("파일 생성");
        } catch(Exception e) {
            throw new RuntimeException("파일 생성 오류" + e);
        }

        end = System.nanoTime();
        System.out.println("테스트 TXT 파일 생성 수행시간2 : " + String.valueOf(end - start) + " ns");
    }

    public static String encode(byte[] value) throws Exception {
        return Base64.getEncoder().withoutPadding().encodeToString(value);
    }
}