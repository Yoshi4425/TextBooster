import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * TextBoosterMain: アプリケーションのメインクラス
 * ドラッグ＆ドロップによる位置入れ替え機能を搭載
 */
public class TextBoosterMain extends JFrame {
    private static final int TOTAL = 30;
    private static final int ROWS = 10;
    private static final int COLS = 3;

    private ClipItem[] clipItems;
    private ClipButton[] buttons;
    private StringBuilder inputBuffer = new StringBuilder();
    
    // ドラッグ＆ドロップ管理用
    private ClipButton dragSourceButton = null;

    // HUD（数字入力オーバーレイ）
    private JWindow hudWindow;
    private JLabel hudLabel;
    private Timer hudTimer;

    // 外部マネジャー
 // private StorageManager storage = new StorageManager(); // 削除
    private DatabaseHelper dbHelper = new DatabaseHelper(); // 追加
    private PasteExecutor executor = new PasteExecutor();

    public TextBoosterMain() {
        initData();
        initUI();
        initHUD();
        initKeyHandler();
    }

    private void initData() {
        // DBからスロット割り当て済みの30件を取得
        clipItems = dbHelper.getItemsForSlots();
    }

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

        // タスクバーを避けて右側1/4に配置
        Rectangle winBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int width = screenWidth / 4;
        setBounds(screenWidth - width, winBounds.y, width, winBounds.height);

        JPanel panel = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        buttons = new ClipButton[TOTAL];

        for (int i = 0; i < TOTAL; i++) {
            final int index = i;
            buttons[i] = new ClipButton(i + 1);
            
            // すべてのボタンに共通の設定（透明化を防ぐ）
            buttons[i].setOpaque(true);
            buttons[i].setContentAreaFilled(true);

            if (index == TOTAL - 1) { // 30番目（設定ボタン）
                buttons[i].setClipTitle("⚙ 設定画面"); 
                buttons[i].setBackground(new Color(220, 220, 220)); // 薄グレー
                buttons[i].setForeground(Color.BLACK);
                buttons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                buttons[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            } else {
                // 1〜29番目のボタン
                buttons[i].setClipTitle(clipItems[i].getTitle());
                
                // --- ここを追加：標準のボタン色を設定 ---
                buttons[i].setBackground(Color.WHITE); // または以前使っていた Color(245, 250, 255) など
                buttons[i].setForeground(Color.BLACK); // 文字色も念のため指定
                buttons[i].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            }
            
            setupInteraction(buttons[i], index);
            panel.add(buttons[i]);
        }
        add(panel);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshAllButtons();
            }
        });
    }

    /**
     * DBから最新情報を読み込み、ボタンを再描画する
     */
    private void refreshAllButtons() {
        clipItems = dbHelper.getItemsForSlots();
        for (int i = 0; i < TOTAL; i++) {
            // 30番（設定ボタン）はDBのデータで上書きしない
            if (i == TOTAL - 1) continue; 
            
            buttons[i].setClipTitle(clipItems[i].getTitle());
        }
    }
    
    /**
     * ボタンに対するマウス操作（クリック・右クリック・ドラッグ）を統合管理する
     */
    private void setupInteraction(ClipButton btn, int index) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragSourceButton = (ClipButton) e.getSource();
                }
                /*if (SwingUtilities.isRightMouseButton(e)) {
                    editItem(index);
                }*/
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragSourceButton != null && SwingUtilities.isLeftMouseButton(e)) {
                    // ドロップ先のコンポーネントを特定
                    Point p = e.getLocationOnScreen();
                    SwingUtilities.convertPointFromScreen(p, getContentPane());
                    Component target = SwingUtilities.getDeepestComponentAt(getContentPane(), p.x, p.y);

                    if (target instanceof ClipButton && target != dragSourceButton) {
                        // 他のボタン上で離されたら入れ替え
                        swapItems(dragSourceButton.getIndex() - 1, ((ClipButton) target).getIndex() - 1);
                    } else if (target == dragSourceButton) {
                        // ドラッグせずにその場で離されたら実行
                        executeAction(index);
                    }
                }
                dragSourceButton = null;
                setCursor(Cursor.getDefaultCursor());
            }
        });

        btn.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // ドラッグ中であることを示すカーソルに変更
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
        });
    }

    /**
     * データの入れ替えとUIの更新
     */
    private void swapItems(int srcIdx, int destIdx) {
        // 30番（設定ボタン）は入れ替え対象から除外
        if (srcIdx == TOTAL - 1 || destIdx == TOTAL - 1) return;
        // 1. メモリ上のデータを入れ替え
        ClipItem srcItem = clipItems[srcIdx];
        ClipItem destItem = clipItems[destIdx];

        clipItems[srcIdx] = destItem;
        clipItems[destIdx] = srcItem;

        // 2. DBのスロット番号を更新（IDが有効な場合のみ）
        // srcItem だったものは destIdx(スロット) へ、destItem だったものは srcIdx へ
        if (srcItem.getId() != -1) dbHelper.assignSlot(srcItem.getId(), destIdx);
        if (destItem.getId() != -1) dbHelper.assignSlot(destItem.getId(), srcIdx);

        // 3. UI表示の更新
        buttons[srcIdx].setClipTitle(clipItems[srcIdx].getTitle());
        buttons[destIdx].setClipTitle(clipItems[destIdx].getTitle());

        buttons[destIdx].requestFocusInWindow();
    }

    private void executeAction(int index) {
        // 30番（index 29）が呼ばれた場合は設定画面を開く
        if (index == TOTAL - 1) {
            openDatabaseWindow();
            return;
        }

        // 通常の貼り付け処理
        buttons[index].requestFocusInWindow();
        buttons[index].doClick(100);
        executor.copyAndPaste(clipItems[index].getContent());
    }

    /**
     * データベース編集画面を、メインパネルの左側に隣接させて開く
     */
    private void openDatabaseWindow() {
        SwingUtilities.invokeLater(() -> {
            // 1. 設定画面のインスタンスを生成
            ClipDatabaseWindow dbWindow = new ClipDatabaseWindow();
            
            // 2. メインパネル（自分自身）の現在のスクリーン座標を取得
            Point mainPos = this.getLocation();
            
            // 3. 設定画面の横幅を取得
            // ※ClipDatabaseWindowのコンストラクタでサイズ（画面の3/4等）が設定されている前提です
            int dbWidth = dbWindow.getWidth();
            
            // 4. 配置座標の計算
            // X座標: メインパネルの左端(mainPos.x)から、設定画面の幅(dbWidth)を引く
            // Y座標: メインパネルの高さ(mainPos.y)に合わせる
            int x = mainPos.x - dbWidth;
            int y = mainPos.y;

            // 5. 計算した位置をセット（画面左端より外に行かないよう Math.max で 0 以上を保証）
            dbWindow.setLocation(Math.max(0, x), y);
            
            // 6. 画面を表示
            dbWindow.setVisible(true);
            
            // 管理画面が閉じられた際にメインパネルのボタン表示を最新にするリスナー
            dbWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    refreshAllButtons();
                }
            });
        });
    }
    /*
    private void editItem(int index) {
        EditDialog dialog = new EditDialog(this);
        if (dialog.show(clipItems[index])) {
            buttons[index].setClipTitle(clipItems[index].getTitle());
            storage.save(clipItems);
        }
    }*/

    // --- HUD制御ロジック ---
    /**
     * HUD（数字オーバーレイ）を初期化
     */
    private void initHUD() {
        hudWindow = new JWindow(this);
        hudLabel = new JLabel("", SwingConstants.CENTER);
        
        // 等幅フォントで位置を固定
        hudLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 100)); 
        hudLabel.setForeground(Color.WHITE);
        
        // 背景設定：半透明（Alpha 180）
        hudLabel.setOpaque(true);
        hudLabel.setBackground(new Color(0, 0, 0, 180));
        hudLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));

        hudWindow.add(hudLabel);
        hudWindow.setSize(320, 180);
        
        // HUDのタイマー（2秒で非表示）
        hudTimer = new Timer(2000, e -> hideHUD());
        hudTimer.setRepeats(false);
    }
     /**
     * メインウィンドウの位置に合わせてHUDの位置を更新
     */
    private void updateHUDPosition() {
        Point p = this.getLocation();
        // 3. 幅320に合わせて、中心位置がずれないよう調整 (x座標を -160 する)
        hudWindow.setLocation(p.x - 160, p.y + (this.getHeight() / 2) - 90);
    }

    private void initKeyHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) handleKeyPress(e);
            return false;
        });
    }

    private void handleKeyPress(KeyEvent e) {
        if (Character.isDigit(e.getKeyChar())) {
            inputBuffer.append(e.getKeyChar());
            showHUD(inputBuffer.toString());
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && inputBuffer.length() > 0) {
            try {
                int num = Integer.parseInt(inputBuffer.toString());
                if (num >= 1 && num <= TOTAL) executeAction(num - 1);
            } catch (NumberFormatException ex) {}
            hideHUD();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            hideHUD();
        }
    }

    /**
     * 【重要】最新のHUDのみをクリーンに表示する
     */
    private void showHUD(String text) {
        // 1. 一旦ウィンドウを非表示にする（これでOS側に残像をクリアさせる）
        hudWindow.setVisible(false);
        
        // 2. ラベルを空にしてから新しいテキストをセットする
        hudLabel.setText(""); 
        hudLabel.setText(text);
        
        // 3. 最新の座標を計算
        updateHUDPosition();
        
        // 4. 再表示する
        hudWindow.setVisible(true);
        hudWindow.setAlwaysOnTop(true);
        
        // 5. タイマーを再スタート
        hudTimer.restart();
    }

    /**
     * HUDを隠し、入力をリセットする
     */
    private void hideHUD() {
        inputBuffer.setLength(0); // 入力バッファをクリア
        hudWindow.setVisible(false);
        hudTimer.stop();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextBoosterMain().setVisible(true));
    }
    
    
}