import java.io.*;

public class StorageManager {
    private static final String FILE_NAME = "text_booster_data.dat";

    public void save(ClipItem[] items) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(items);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ClipItem[] load() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (ClipItem[]) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}