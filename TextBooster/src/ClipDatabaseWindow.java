import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

/**
 * ClipDatabaseWindow: 検索窓に終了ボタンを追加し、Slot 1-29制限を設けた管理画面
 */
public class ClipDatabaseWindow extends JFrame {
    private DatabaseHelper dbHelper = new DatabaseHelper();
    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField searchField = new JTextField(15);
    private JTextField searchMajorField = new JTextField(10);
    private JTextField searchMinorField = new JTextField(10);

    private JTextField titleField = new JTextField();
    private JTextArea contentArea = new JTextArea(15, 40);
    private JTextField majorField = new JTextField();
    private JTextField minorField = new JTextField();
    private JTextField slotInputField = new JTextField(3);
    private int currentEditingId = -1;

    public ClipDatabaseWindow() {
        setTitle("Database Manager");

        // 配置：左側 3/4
        Rectangle winBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int dbWidth = (winBounds.width * 3) / 4;
        setBounds(0, winBounds.y + 10, dbWidth, winBounds.height - 20);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(2, 2));

        add(initSearchPanel(), BorderLayout.NORTH);
        add(initTablePanel(), BorderLayout.CENTER);
        add(initEditPanel(), BorderLayout.EAST);

        refreshTable();
    }

    /**
     * 1. 検索パネル：右端に終了ボタンを配置
     */
    private JPanel initSearchPanel() {
        JPanel mainSearchPanel = new JPanel(new BorderLayout());
        mainSearchPanel.setBorder(BorderFactory.createTitledBorder("検索・操作"));

        // 左側：検索入力
        JPanel leftSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftSearchPanel.add(new JLabel("キーワード:"));
        leftSearchPanel.add(searchField);
        leftSearchPanel.add(new JLabel("大ラベル:"));
        leftSearchPanel.add(searchMajorField);
        leftSearchPanel.add(new JLabel("小ラベル:"));
        leftSearchPanel.add(searchMinorField);

        JButton searchBtn = new JButton("検索");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        leftSearchPanel.add(searchBtn);

        // 右側：終了ボタン
        JButton exitBtn = new JButton("管理画面を閉じる");
        exitBtn.setFocusable(false);
        exitBtn.addActionListener(e -> dispose()); // ウィンドウを閉じる

        mainSearchPanel.add(leftSearchPanel, BorderLayout.WEST);
        mainSearchPanel.add(exitBtn, BorderLayout.EAST);

        return mainSearchPanel;
    }

    private JComponent initTablePanel() {
        // IDはモデルに保持し、表示からは除外
        String[] columns = {"ID", "タイトル", "大ラベル", "小ラベル", "Slot"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        
        TableColumnModel cm = table.getColumnModel();
        
        // 1. ID列を非表示にする
        TableColumn idColumn = cm.getColumn(0);
        cm.removeColumn(idColumn); 

        // 2. Slot列の幅を固定（最小限に）
        TableColumn slotCol = cm.getColumn(3); // ID削除後は3番目
        slotCol.setMinWidth(35);
        slotCol.setMaxWidth(45);
        slotCol.setPreferredWidth(35);
        
        // 3. 残りの幅を 4 : 3 : 3 の比率で配分
        // 合計1000とした時の配分: タイトル400, 大300, 小300
        cm.getColumn(0).setPreferredWidth(400); // タイトル
        cm.getColumn(1).setPreferredWidth(300); // 大ラベル
        cm.getColumn(2).setPreferredWidth(300); // 小ラベル

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedData();
        });

        return new JScrollPane(table);
    }

    private JPanel initEditPanel() {
        // 左右の余白を削るため、内側のInsetsを小さく設定
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(400, 0));
        panel.setBorder(BorderFactory.createTitledBorder("詳細編集"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4); // 余白を最小限に
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- スロット割当行 (右端を揃える) ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Slot(0-29):"), gbc);
        
        JPanel slotActionPanel = new JPanel(new BorderLayout(5, 0));
        slotActionPanel.add(slotInputField, BorderLayout.CENTER);
        JButton assignBtn = new JButton("スロット確定");
        assignBtn.addActionListener(e -> performSlotAssignment());
        slotInputField.addActionListener(e -> performSlotAssignment());
        slotActionPanel.add(assignBtn, BorderLayout.EAST);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(slotActionPanel, gbc);

        // --- タイトル ---
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("タイトル:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(titleField, gbc);

        // --- ラベル ---
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("大ラベル:"), gbc);
        gbc.gridx = 1; panel.add(majorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("小ラベル:"), gbc);
        gbc.gridx = 1; panel.add(minorField, gbc);

        // --- 本文エリア ---
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(new JLabel("本文:"), gbc);
        
        gbc.gridy = 5; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(contentArea), gbc);

        // --- ボタン ---
        gbc.gridy = 6; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 2, 0));
        JButton addBtn = new JButton("新規");
        JButton upBtn = new JButton("更新");
        JButton delBtn = new JButton("削除");
        JButton clrBtn = new JButton("クリア");
        
        addBtn.addActionListener(e -> { saveItem(true); refreshTable(); });
        upBtn.addActionListener(e -> { saveItem(false); refreshTable(); });
        delBtn.addActionListener(e -> deleteAction());
        clrBtn.addActionListener(e -> clearForm());

        btnPanel.add(addBtn); btnPanel.add(upBtn); btnPanel.add(delBtn); btnPanel.add(clrBtn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // IDを隠し持っているため、モデルから直接取得する
    private void loadSelectedData() {
        int row = table.getSelectedRow();
        if (row != -1) {
            // 表示されていなくてもモデルの0番目(ID)は取得可能
            currentEditingId = (int) tableModel.getValueAt(row, 0);
            List<ClipItem> items = dbHelper.getAllItems();
            for (ClipItem item : items) {
                if (item.getId() == currentEditingId) {
                    titleField.setText(item.getTitle());
                    contentArea.setText(item.getContent());
                    majorField.setText(item.getMajorLabel());
                    minorField.setText(item.getMinorLabel());
                    slotInputField.setText("");
                    break;
                }
            }
        }
    }

    /**
     * 3. スロット割り当ての修正（1-29制限とエラー処理）
     */
    private void performSlotAssignment() {
        if (currentEditingId == -1) {
            JOptionPane.showMessageDialog(this, "データを選択してください。");
            return;
        }

        try {
            String input = slotInputField.getText().trim();
            if (input.isEmpty()) return;

            int displaySlot = Integer.parseInt(input);

            // エラー処理: 30番（設定ボタン）は拒否
            if (displaySlot == 30) {
                JOptionPane.showMessageDialog(this, 
                    "Slot 30 は設定ボタン専用です。1〜29番を選択してください。", 
                    "入力エラー", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // エラー処理: 範囲外
            if (displaySlot < 0 || displaySlot > 29) {
                JOptionPane.showMessageDialog(this, 
                    "有効な範囲は 0(解除) または 1〜29 です。", 
                    "入力エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 内部値への変換 (0なら-1, 1-29なら0-28)
            int internalSlot = (displaySlot == 0) ? -1 : displaySlot - 1;
            dbHelper.assignSlot(currentEditingId, internalSlot);
            
            refreshTable();
            slotInputField.setText("");
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "数字を入力してください。");
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<ClipItem> items = dbHelper.getAllItems();
        for (ClipItem item : items) {
            tableModel.addRow(new Object[]{
                item.getId(), item.getTitle(), item.getMajorLabel(), item.getMinorLabel(),
                (item.getSlotNumber() == -1 ? 0 : item.getSlotNumber() + 1)
            });
        }
    }

 // --- ヘルパーメソッド ---
    private void addFormField(JPanel p, String label, JTextField tf, int y, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        p.add(tf, gbc);
    }
    
    private void performSearch() {
        tableModel.setRowCount(0);
        List<ClipItem> items = dbHelper.searchItems(searchField.getText(), searchMajorField.getText(), searchMinorField.getText());
        for (ClipItem item : items) {
            tableModel.addRow(new Object[]{
                item.getId(), item.getTitle(), item.getMajorLabel(), item.getMinorLabel(),
                (item.getSlotNumber() == -1 ? 0 : item.getSlotNumber() + 1)
            });
        }
    }

    private void saveItem(boolean isNew) {
        if (titleField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "タイトルは必須です。");
            return;
        }

        ClipItem item = new ClipItem(isNew ? -1 : currentEditingId);
        item.setTitle(titleField.getText());
        item.setContent(contentArea.getText());
        item.setMajorLabel(majorField.getText());
        item.setMinorLabel(minorField.getText());
        
        // 更新時は既存のSlot（内部値 -1, 0-29）を保持、新規時は -1
        item.setSlotNumber(isNew ? -1 : getInternalSlotFromTable());

        if (isNew) dbHelper.insertItem(item);
        else dbHelper.updateItem(item);
        
        if (isNew) clearForm();
    }

    /**
     * テーブルの表示値から内部用Slot番号を取得する
     */
    private int getInternalSlotFromTable() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int displaySlot = (int) tableModel.getValueAt(row, 4);
            return (displaySlot == 0) ? -1 : displaySlot - 1;
        }
        return -1;
    }

    private void deleteAction() {
        if(currentEditingId != -1) {
            int res = JOptionPane.showConfirmDialog(this, "本当に削除しますか？", "確認", JOptionPane.YES_NO_OPTION);
            if(res == JOptionPane.YES_OPTION) {
                dbHelper.deleteItem(currentEditingId);
                refreshTable();
                clearForm();
            }
        }
    }

    private void clearForm() {
        currentEditingId = -1;
        titleField.setText("");
        contentArea.setText("");
        majorField.setText("");
        minorField.setText("");
        table.clearSelection();
    }

    private void clearSearchFields() {
        searchField.setText("");
        searchMajorField.setText("");
        searchMinorField.setText("");
    }
}