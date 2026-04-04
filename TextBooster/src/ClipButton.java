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
        super.paintComponent(g);
        
        // --- インデックス番号の固定描画（表示系統の分離） ---
        Graphics2D g2d = (Graphics2D) g.create();
        // アンチエイリアスを有効にして文字を綺麗に
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 10)); 
        g2d.setColor(new Color(100, 100, 100)); // 少し薄めのグレー
        
        // 左上端（5, 12）付近に数字を固定
        g2d.drawString(String.format("%02d", index), 5, 12);
        
        g2d.dispose();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // ウィンドウサイズが確定したタイミングで、再度タイトルの省略を計算する
        updateDisplaySelection();
    }
}