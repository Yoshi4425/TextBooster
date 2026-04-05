import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:textbooster.db";

    public DatabaseHelper() {
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS clips (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "title TEXT NOT NULL," +
                     "content TEXT," +
                     "major_label TEXT," +
                     "minor_label TEXT," +
                     "slot_number INTEGER DEFAULT -1" +
                     ");";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertItem(ClipItem item) {
        String sql = "INSERT INTO clips(title, content, major_label, minor_label, slot_number) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getContent());
            pstmt.setString(3, item.getMajorLabel());
            pstmt.setString(4, item.getMinorLabel());
            pstmt.setInt(5, item.getSlotNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<ClipItem> getAllItems() {
        List<ClipItem> list = new ArrayList<>();
        String sql = "SELECT * FROM clips ORDER BY id DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ClipItem item = new ClipItem(rs.getInt("id"));
                item.setTitle(rs.getString("title"));
                item.setContent(rs.getString("content"));
                item.setMajorLabel(rs.getString("major_label"));
                item.setMinorLabel(rs.getString("minor_label"));
                item.setSlotNumber(rs.getInt("slot_number"));
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateItem(ClipItem item) {
        String sql = "UPDATE clips SET title=?, content=?, major_label=?, minor_label=?, slot_number=? WHERE id=?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getTitle());
            pstmt.setString(2, item.getContent());
            pstmt.setString(3, item.getMajorLabel());
            pstmt.setString(4, item.getMinorLabel());
            pstmt.setInt(5, item.getSlotNumber());
            pstmt.setInt(6, item.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteItem(int id) {
        String sql = "DELETE FROM clips WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 条件を指定してデータを検索する
     */
    public List<ClipItem> searchItems(String keyword, String major, String minor) {
        List<ClipItem> list = new ArrayList<>();
        // 動的にSQLを組み立てる
        StringBuilder sql = new StringBuilder("SELECT * FROM clips WHERE 1=1");
        if (!keyword.isEmpty()) sql.append(" AND (title LIKE ? OR content LIKE ?)");
        if (!major.isEmpty()) sql.append(" AND major_label LIKE ?");
        if (!minor.isEmpty()) sql.append(" AND minor_label LIKE ?");
        sql.append(" ORDER BY id DESC");

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (!keyword.isEmpty()) {
                pstmt.setString(paramIdx++, "%" + keyword + "%");
                pstmt.setString(paramIdx++, "%" + keyword + "%");
            }
            if (!major.isEmpty()) pstmt.setString(paramIdx++, "%" + major + "%");
            if (!minor.isEmpty()) pstmt.setString(paramIdx++, "%" + minor + "%");

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ClipItem item = new ClipItem(rs.getInt("id"));
                item.setTitle(rs.getString("title"));
                item.setContent(rs.getString("content"));
                item.setMajorLabel(rs.getString("major_label"));
                item.setMinorLabel(rs.getString("minor_label"));
                item.setSlotNumber(rs.getInt("slot_number"));
                list.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * 指定したIDのデータにスロットを割り当てる。
     * もし同じスロットを使っている別のデータがあれば、それは未割り当て(-1)にリセットする。
     */
    public void assignSlot(int clipId, int targetInternalSlot) {
        // 1. まず、移動先のスロット(targetInternalSlot)を現在使っている他のデータを -1 にリセット
        String resetSql = "UPDATE clips SET slot_number = -1 WHERE slot_number = ?";
        // 2. その後、対象のデータ(clipId)に新しいスロットを割り当てる
        String assignSql = "UPDATE clips SET slot_number = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // トランザクション開始
            try (PreparedStatement pstmt1 = conn.prepareStatement(resetSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(assignSql)) {
                
                // ステップ1: 既存の占有者を追い出す
                pstmt1.setInt(1, targetInternalSlot);
                pstmt1.executeUpdate();

                // ステップ2: 新しい主を割り当てる
                pstmt2.setInt(1, targetInternalSlot);
                pstmt2.setInt(2, clipId);
                pstmt2.executeUpdate();

                conn.commit(); // 確定
            } catch (SQLException e) {
                conn.rollback(); // 失敗したら戻す
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * スロット0-29に割り当てられているアイテムをすべて取得し、
     * インデックスに対応した配列で返す。
     */
    public ClipItem[] getItemsForSlots() {
        ClipItem[] slots = new ClipItem[30];
        // 初期化（空のスロット用）
        for (int i = 0; i < 30; i++) {
            slots[i] = new ClipItem();
            slots[i].setSlotNumber(i);
        }

        String sql = "SELECT * FROM clips WHERE slot_number BETWEEN 0 AND 29";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ClipItem item = new ClipItem(rs.getInt("id"));
                item.setTitle(rs.getString("title"));
                item.setContent(rs.getString("content"));
                item.setMajorLabel(rs.getString("major_label"));
                item.setMinorLabel(rs.getString("minor_label"));
                int slot = rs.getInt("slot_number");
                if (slot >= 0 && slot < 30) {
                    slots[slot] = item;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }
}