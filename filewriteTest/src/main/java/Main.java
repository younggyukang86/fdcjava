import java.awt.image.ImagingOpException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    private final static String sourceFile = "C:\\test\\testSample.txt";
    private final static String copyFolder = "C:\\test2";

    public static void main(String[] args) throws Exception {
        System.out.println("GraalVM TEST File writing!!");
        /*File file = new File("C:\\test\\test1.zip");
        File newFile = new File("C:\\test2\\test2.zip");
        //long start = System.nanoTime();
        long start = System.currentTimeMillis();

        Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        //long end = System.nanoTime();
        long end = System.currentTimeMillis();

        //System.out.println("수행시간 : " + String.valueOf(end - start) + " ns");
        System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " 초");*/

        Path newFolderPath = Paths.get(copyFolder);

        if (Files.isDirectory(newFolderPath)) {
            for (int i = 1; i <= 10; i++) {
                Files.deleteIfExists(Paths.get(copyFolder + "\\test" + i + ".txt"));
            }
        }
        Files.createDirectories(newFolderPath);

        /*final int bufferMaxSize = 1024 * 1024;
        Path sourceFilePath = Paths.get(sourceFile);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= 10; i++) {
            Path resultFilePath = Paths.get(copyFolder + "\\test" + i + ".txt");
            try (FileChannel sourceChannel = FileChannel.open(sourceFilePath, StandardOpenOption.READ);
                 FileChannel resultChannel = FileChannel.open(resultFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(bufferMaxSize);

                while (sourceChannel.read(buffer) >= 0) {
                    buffer.flip();
                    resultChannel.write(buffer);
                    buffer.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();*/

        Path sourceFilePath = Paths.get(sourceFile);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= 10; i++) {
            Path resultFilePath = Paths.get(copyFolder + "\\test" + i + ".txt");

            try (AsynchronousFileChannel sourceChannel = AsynchronousFileChannel.open(sourceFilePath, StandardOpenOption.READ);) {

                ByteBuffer buffer = ByteBuffer.allocate((int) sourceChannel.size());
                Future<Integer> readResult = sourceChannel.read(buffer, 0);
                readResult.get();
                if (readResult.isDone()) {

                }

                buffer.flip();
                byte[] bytes = buffer.array();

                try (AsynchronousFileChannel resultChannel = AsynchronousFileChannel.open(
                        resultFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE);) {
                    ByteBuffer writeBuffer = ByteBuffer.wrap(bytes);
                    Future<Integer> wirteResult = resultChannel.write(writeBuffer, 0);
                    if (wirteResult.isDone()) {

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                buffer.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();

        System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " 초");
    }

    public static List<String> readAllLines(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return new ArrayList<String>();

        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
             Reader in = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(in)) {

            List<String> lines = new ArrayList<String>();
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}