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
    private StorageManager storage = new StorageManager();
    private PasteExecutor executor = new PasteExecutor();

    public TextBoosterMain() {
        initData();
        initUI();
        initHUD();
        initKeyHandler();
    }

    private void initData() {
        clipItems = storage.load();
        if (clipItems == null) {
            clipItems = new ClipItem[TOTAL];
            for (int i = 0; i < TOTAL; i++) {
                clipItems[i] = new ClipItem(i + 1);
            }
        }
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
            buttons[i].setClipTitle(clipItems[i].getTitle());
            
            // ドラッグ＆ドロップとクリックを制御するリスナーを設定
            setupInteraction(buttons[i], index);
            
            panel.add(buttons[i]);
        }
        add(panel);
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
                if (SwingUtilities.isRightMouseButton(e)) {
                    editItem(index);
                }
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
        // データ本体を入れ替え
        ClipItem temp = clipItems[srcIdx];
        clipItems[srcIdx] = clipItems[destIdx];
        clipItems[destIdx] = temp;

        // UI表示（タイトル）を更新
        buttons[srcIdx].setClipTitle(clipItems[srcIdx].getTitle());
        buttons[destIdx].setClipTitle(clipItems[destIdx].getTitle());

        // 設定ファイルへ保存
        storage.save(clipItems);

        // 入れ替え先を目立たせる（フォーカス）
        buttons[destIdx].requestFocusInWindow();
    }

    private void executeAction(int index) {
        buttons[index].requestFocusInWindow();
        buttons[index].doClick(100);
        executor.copyAndPaste(clipItems[index].getContent());
    }

    private void editItem(int index) {
        EditDialog dialog = new EditDialog(this);
        if (dialog.show(clipItems[index])) {
            buttons[index].setClipTitle(clipItems[index].getTitle());
            storage.save(clipItems);
        }
    }

    // --- HUD制御ロジック ---
    private void initHUD() {
        hudWindow = new JWindow(this);
        hudLabel = new JLabel("", SwingConstants.CENTER);
        hudLabel.setFont(new Font("Arial", Font.BOLD, 80));
        hudLabel.setForeground(Color.WHITE);
        hudLabel.setOpaque(true);
        hudLabel.setBackground(new Color(0, 0, 0, 160));
        hudLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        hudWindow.add(hudLabel);
        hudWindow.setSize(220, 140);
        updateHUDPosition();
        hudTimer = new Timer(2000, e -> hideHUD());
        hudTimer.setRepeats(false);
    }

    private void updateHUDPosition() {
        Point p = this.getLocation();
        hudWindow.setLocation(p.x - 110, p.y + (this.getHeight() / 2) - 70);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextBoosterMain().setVisible(true));
    }
}