import java.io.Serializable;

public class ClipItem implements Serializable {
    private String title;
    private String content;

    public ClipItem(int index) {
        this.title = String.format("項目 %02d", index);
        this.content = "";
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}