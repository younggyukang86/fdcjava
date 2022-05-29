import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("GraalVM TEST File writing!!");
        File file = new File("C:\\test\\test1.zip");
        File newFile = new File("C:\\test\\test2.zip");
        long start = System.nanoTime();
        Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        long end = System.nanoTime();
        System.out.println("수행시간 : " + String.valueOf(end - start) + " ns");
    }
}