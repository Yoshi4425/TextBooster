import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * TextBoosterMain: アプリケーションのメインウィンドウおよび制御クラス
 */
public class TextBoosterMain extends JFrame {
    private static final int TOTAL = 30;
    private static final int ROWS = 10;
    private static final int COLS = 3;

    private ClipItem[] clipItems;
    private ClipButton[] buttons;
    private StringBuilder inputBuffer = new StringBuilder();
    
    // HUD（数字入力オーバーレイ）用コンポーネント
    private JWindow hudWindow;
    private JLabel hudLabel;
    private Timer hudTimer;

    // 外部マネジャークラス
    private StorageManager storage = new StorageManager();
    private PasteExecutor executor = new PasteExecutor();

    public TextBoosterMain() {
        initData();       // データの読み込み
        initUI();         // メインGUIの構築
        initHUD();        // 入力確認用HUDの構築
        initKeyHandler(); // グローバルキーイベントの監視
    }

    /**
     * 保存ファイルからデータを読み込み、なければ初期化する
     */
    private void initData() {
        clipItems = storage.load();
        if (clipItems == null) {
            clipItems = new ClipItem[TOTAL];
            for (int i = 0; i < TOTAL; i++) {
                clipItems[i] = new ClipItem(i + 1);
            }
        }
    }

    /**
     * メインウィンドウの構築
     */
    private void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Text Booster");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);

        // --- ウィンドウ配置：右側1/4 かつ タスクバーを避ける ---
        Rectangle winBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int width = screenWidth / 4;
        int height = winBounds.height;
        int x = screenWidth - width;
        int y = winBounds.y;
        setBounds(x, y, width, height);

        // ボタン配置パネル
        JPanel panel = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        buttons = new ClipButton[TOTAL];

        for (int i = 0; i < TOTAL; i++) {
            final int index = i;
            buttons[i] = new ClipButton(i + 1);
            buttons[i].setClipTitle(clipItems[i].getTitle());
            
            // マウス操作の登録
            buttons[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        editItem(index); // 右クリック：編集
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        executeAction(index); // 左クリック：実行
                    }
                }
            });
            panel.add(buttons[i]);
        }
        add(panel);
    }

    /**
     * 数字入力時に浮かび上がるHUD（半透明ウィンドウ）を初期化
     */
    private void initHUD() {
        hudWindow = new JWindow(this);
        hudLabel = new JLabel("", SwingConstants.CENTER);
        hudLabel.setFont(new Font("Arial", Font.BOLD, 80));
        hudLabel.setForeground(Color.WHITE);
        hudLabel.setOpaque(true);
        hudLabel.setBackground(new Color(0, 0, 0, 160)); // 半透明の黒
        hudLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        hudWindow.add(hudLabel);
        hudWindow.setSize(220, 140);
        updateHUDPosition();

        // 2秒間入力がない場合に自動でバッファをクリアしてHUDを隠すタイマー
        hudTimer = new Timer(2000, e -> hideHUD());
        hudTimer.setRepeats(false);
    }

    /**
     * メインウィンドウの移動に追従してHUDの位置を更新
     */
    private void updateHUDPosition() {
        Point p = this.getLocation();
        // メインウィンドウの左側にはみ出す形で配置
        hudWindow.setLocation(p.x - 110, p.y + (this.getHeight() / 2) - 70);
    }

    /**
     * キーボード入力をアプリ全体で監視する（数字・Enter・Esc）
     */
    private void initKeyHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                handleKeyPress(e);
            }
            return false;
        });
    }

    /**
     * キー入力のロジック処理
     */
    private void handleKeyPress(KeyEvent e) {
        char c = e.getKeyChar();
        if (Character.isDigit(c)) {
            // 数字キー：バッファに追加してHUDを表示
            inputBuffer.append(c);
            showHUD(inputBuffer.toString());
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            // Enter：確定実行
            if (inputBuffer.length() > 0) {
                try {
                    int num = Integer.parseInt(inputBuffer.toString());
                    if (num >= 1 && num <= TOTAL) {
                        executeAction(num - 1);
                    }
                } catch (NumberFormatException ex) {
                    // 数値変換失敗時は無視
                }
                hideHUD();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // Esc：入力をキャンセル
            hideHUD();
        }
    }

    private void showHUD(String text) {
        hudLabel.setText(text);
        updateHUDPosition();
        hudWindow.setVisible(true);
        hudWindow.setAlwaysOnTop(true);
        hudTimer.restart();
    }

    private void hideHUD() {
        inputBuffer.setLength(0);
        hudWindow.setVisible(false);
        hudTimer.stop();
    }

    /**
     * データのコピー＆貼り付けアクションを実行
     */
    private void executeAction(int index) {
        // 視覚的フィードバック：ボタンをクリックした時と同じエフェクトを発生
        buttons[index].requestFocusInWindow();
        buttons[index].doClick(100);

        // 自動貼り付け処理の実行
        executor.copyAndPaste(clipItems[index].getContent());
    }

    /**
     * 項目の編集ダイアログを表示
     */
    private void editItem(int index) {
        EditDialog dialog = new EditDialog(this);
        if (dialog.show(clipItems[index])) {
            // 保存された場合、UIを更新してファイルへ書き出し
            buttons[index].setClipTitle(clipItems[index].getTitle());
            storage.save(clipItems);
        }
    }

    public static void main(String[] args) {
        // GUIスレッドで実行
        SwingUtilities.invokeLater(() -> {
            TextBoosterMain app = new TextBoosterMain();
            app.setVisible(true);
        });
    }
}