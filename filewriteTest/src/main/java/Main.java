import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.text.DecimalFormat;

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
        System.out.println("Decode 수행시간 : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " 초");*/

        Path newFolderPath = Paths.get(copyFolder);

        if (Files.isDirectory(newFolderPath)) {
            for (int i = 1; i <= 10; i++) {
                Files.deleteIfExists(Paths.get(copyFolder + "\\test" + i + ".txt"));
            }
        }
        Files.createDirectories(newFolderPath);

        final int bufferMaxSize = 1024 * 1024;
        Path sourceFilePath = Paths.get(sourceFile);

        long start = System.currentTimeMillis();

        for (int i = 1; i <= 10; i++) {
            try (FileChannel sourceChannel = FileChannel.open(sourceFilePath, StandardOpenOption.READ);
                 FileChannel resultChannel = FileChannel.open(Paths.get(copyFolder + "\\test" + i + ".txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
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
        long end = System.currentTimeMillis();
        System.out.println("Decode 수행시간 : " + new DecimalFormat("###.0").format((end - start) / 1000.0) + " 초");
    }
}