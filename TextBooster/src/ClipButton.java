import javax.swing.*;
import java.awt.*;

public class ClipButton extends JButton {
    private int index;
    private String originalTitle = "";

    public ClipButton(int index) {
        this.index = index;
        setFocusable(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setMargin(new Insets(15, 5, 5, 5));
        
        // ドラッグ操作を許可するための設定
        setTransferHandler(new TransferHandler("text")); 
    }

    // indexを外部から取得できるようにゲッターを追加
    public int getIndex() {
        return index;
    }

    public void setClipTitle(String title) {
        this.originalTitle = title;
        updateDisplaySelection();
    }

    /**
     * タイトルの省略ロジックを適用
     * 2行以内に収め、超える場合は "..." を付与
     */
    private void updateDisplaySelection() {
        if (originalTitle == null || originalTitle.isEmpty()) {
            setText("");
            return;
        }

        // ボタンの有効幅（左右の余白を考慮）
        int width = getWidth() > 0 ? getWidth() - 15 : 100;
        FontMetrics fm = getFontMetrics(getFont());
        
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        String[] chars = originalTitle.split(""); 

        int i = 0;
        // 1行目の計算
        for (; i < chars.length; i++) {
            if (fm.stringWidth(line1.toString() + chars[i]) < width) {
                line1.append(chars[i]);
            } else {
                break;
            }
        }

        // 2行目の計算
        boolean truncated = false;
        for (; i < chars.length; i++) {
            // "..." を加えた状態で幅に収まるかチェック
            if (fm.stringWidth(line2.toString() + chars[i] + "...") < width) {
                line2.append(chars[i]);
            } else {
                truncated = true;
                break;
            }
        }

        // HTML形式でテキストを設定
        String result;
        if (truncated) {
            result = "<html><center>" + line1 + "<br>" + line2 + "...</center></html>";
        } else if (line2.length() > 0) {
            result = "<html><center>" + line1 + "<br>" + line2 + "</center></html>";
        } else {
            result = "<html><center>" + line1 + "</center></html>";
        }
        
        setText(result);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // アンチエイリアス（文字を滑らかにする）
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. 背景の塗りつぶし
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. 左上の数字（1〜30）の描画
        // 番号は少し薄い色にするか、文字色(Foreground)を少し透明にして描くとオシャレです
        g2.setColor(new Color(100, 100, 100, 255)); // グレーで少し透明
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10)); // 小さめのフォント
        // 左上に配置（余白 3px, 10px）
        g2.drawString(String.valueOf(this.index), 3, 10); 

        // 3. 中央のタイトルの描画
        g2.setColor(getForeground());
        g2.setFont(getFont());
        
        if (this.originalTitle != null) {
            FontMetrics fm = g2.getFontMetrics();
            // 中央揃えの計算
            int x = (getWidth() - fm.stringWidth(this.originalTitle)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 2;
            g2.drawString(this.originalTitle, x, y);
        }

        g2.dispose();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // ウィンドウサイズが確定したタイミングで、再度タイトルの省略を計算する
        updateDisplaySelection();
    }
}