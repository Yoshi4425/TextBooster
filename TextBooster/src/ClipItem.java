import java.io.Serializable;

/**
 * ClipItem: データベースの1レコードに対応するデータモデル
 */
public class ClipItem implements Serializable {
    private int id;             // DBのプライマリキー (新規登録前は -1)
    private String title;       // ボタン表示用タイトル
    private String content;     // 貼り付け本文
    private String majorLabel;  // 大分類（例：ソースコード、Tex作成）
    private String minorLabel;  // 小分類（例：Java、数式テンプレート）
    private int slotNumber;     // パネルのスロット番号 (0-29: 配置済み, -1: ストック)

    /**
     * コンストラクタ（新規作成時などIDが未定の場合）
     */
    public ClipItem() {
        this.id = -1;
        this.title = "";
        this.content = "";
        this.majorLabel = "";
        this.minorLabel = "";
        this.slotNumber = -1;
    }

    /**
     * コンストラクタ（DBからの読み込み時などIDを指定する場合）
     */
    public ClipItem(int id) {
        this();
        this.id = id;
    }

    // --- Getter & Setter ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMajorLabel() { return majorLabel; }
    public void setMajorLabel(String majorLabel) { this.majorLabel = majorLabel; }

    public String getMinorLabel() { return minorLabel; }
    public void setMinorLabel(String minorLabel) { this.minorLabel = minorLabel; }

    public int getSlotNumber() { return slotNumber; }
    public void setSlotNumber(int slotNumber) { this.slotNumber = slotNumber; }

    /**
     * デバッグ用：オブジェクトの状態を文字列で返す
     */
    @Override
    public String toString() {
        return String.format("ID:%d | Title:%s | Slot:%d", id, title, slotNumber);
    }
}