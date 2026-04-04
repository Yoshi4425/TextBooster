import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EditDialog extends JDialog {
    private JTextField titleField;
    private JTextArea contentArea;
    private boolean saved = false;

    public EditDialog(Frame owner) {
        super(owner, "項目の編集 (Ctrl + Enter で保存)", true);
        setLayout(new BorderLayout(10, 10));

        // --- 1. 入力領域のデザイン（コード入力を想定） ---
        titleField = new JTextField(20);
        titleField.setFont(new Font("Meiryo", Font.PLAIN, 14));

        // 本文を20行、50列に拡大。フォントを等幅（Monospaced）にしてコードを見やすく
        contentArea = new JTextArea(20, 50);
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        contentArea.setLineWrap(false); // コード用なので自動折り返しはOFF（横スクロール）
        
        JScrollPane scrollPane = new JScrollPane(contentArea);
        // スクロールパネルの好ましいサイズを明示的に設定（大きく）
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 入力パネルの組み立て (GridBagLayoutで綺麗に配置)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // タイトルラベル
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        inputPanel.add(new JLabel(" 表示タイトル:"), gbc);
        // タイトル入力
        gbc.gridx = 1; gbc.weightx = 1.0;
        inputPanel.add(titleField, gbc);

        // 本文ラベル
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTH;
        inputPanel.add(new JLabel(" 貼り付け本文:"), gbc);
        // 本文入力（スクロール付き）
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        inputPanel.add(scrollPane, gbc);

        add(inputPanel, BorderLayout.CENTER);

        // --- 2. ボタンパネル（右寄せ） ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("保存 (Ctrl+Enter)");
        saveBtn.addActionListener(e -> performSave());
        buttonPanel.add(saveBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- 3. [Ctrl + Enter] キーバインドの設定 ---
        // ダイアログのルート（最上位）コンポーネントに設定することで、どこにフォーカスがあっても検知
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // キー操作（Ctrl + Enter）を定義
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        // 操作に名前を付ける
        inputMap.put(ctrlEnter, "saveAction");
        // 名前と実際の処理（performSaveメソッドの呼び出し）を結びつける
        actionMap.put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSave();
            }
        });

        // --- 4. ダイアログのサイズと配置 ---
        pack(); // コンポーネントに合わせてサイズ計算
        // メインパネル（右端）に縛られず、画面の中央に配置
        setLocationRelativeTo(null); 
    }

    /**
     * 保存処理を実行し、ダイアログを閉じる
     */
    private void performSave() {
        saved = true;
        setVisible(false); // ダイアログを閉じる（呼び出し元に制御が戻る）
    }

    /**
     * メインクラスから呼ばれる、ダイアログ表示メソッド
     */
    public boolean show(ClipItem item) {
        // フラグをリセット
        saved = false;
        titleField.setText(item.getTitle());
        contentArea.setText(item.getContent());
        
        // ダイアログを表示（モーダルなので、閉じるまでここで止まる）
        setVisible(true);

        // 戻ってきたときにsavedフラグをチェック
        if (saved) {
            item.setTitle(titleField.getText());
            item.setContent(contentArea.getText());
        }
        return saved;
    }
}