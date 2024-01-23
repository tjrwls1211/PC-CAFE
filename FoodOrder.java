package PCRoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class FoodOrder extends JPanel {
    private UserStatus userStatus;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pcroom";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    private JTextArea textArea;
    private JLabel totalPriceLabel;

    private java.util.List<String> selectedFoods;
    private int totalPrice;

    public FoodOrder(UserStatus userStatus) {
        this.userStatus = userStatus;

        setSize(400, 200);

        selectedFoods = new ArrayList<>();
        totalPrice = 0;

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String query = "SELECT name, foodtype, price FROM food";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                JTabbedPane tabbedPane = new JTabbedPane();
                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String foodType = resultSet.getString("foodtype");
                    int price = resultSet.getInt("price");

                    JButton button = new JButton(name + " (" + price + "원)");
                    button.addActionListener(new FoodButtonActionListener(name, price, connection));

                    boolean tabExists = false;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabbedPane.getTitleAt(i).equals(foodType)) {
                            tabExists = true;
                            JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
                            JPanel panel = (JPanel) scrollPane.getViewport().getView();
                            panel.add(button);
                            break;
                        }
                    }

                    if (!tabExists) {
                        JPanel panel = new JPanel();
                        panel.setLayout(new GridLayout(0, 4)); // 그리드 레이아웃 설정
                        panel.add(button);
                        JScrollPane scrollPane = new JScrollPane(panel);
                        tabbedPane.addTab(foodType, scrollPane);
                    }
                }

                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

                textArea = new JTextArea(10, 30);
                textArea.setEditable(false);
                infoPanel.add(new JScrollPane(textArea));

                totalPriceLabel = new JLabel("Total Price: 0원");
                infoPanel.add(totalPriceLabel);

                JButton orderButton = new JButton("주문하기");
                orderButton.addActionListener(e -> {
                    if (!selectedFoods.isEmpty()) {

                        try (Connection orderConnection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                            int seatNumber = userStatus.getSeatNumber();
                            for (String foodName : selectedFoods) {
                                saveOrderToDatabase(orderConnection, seatNumber, foodName);
                            }

                            addSale(orderConnection, totalPrice);

                            JOptionPane.showMessageDialog(this, "주문이 완료되었습니다!");

                            if (userStatus != null) {
                                userStatus.addToFee(totalPrice);
                            }

                            clearSelection();

                            // 추가: UI 업데이트

                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "데이터베이스 연결에 실패했습니다.");
                            System.exit(1);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "선택된 음식이 없습니다. 음식을 선택해주세요.");
                    }
                });
                infoPanel.add(orderButton);

                // 초기화 버튼 추가
                JButton resetButton = new JButton("초기화");
                resetButton.addActionListener(e -> clearSelection());
                infoPanel.add(resetButton);

                // 패널에 컴포넌트 추가
                add(tabbedPane);
                add(infoPanel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터베이스 연결에 실패했습니다.");
            System.exit(1);
        }

        setLayout(new FlowLayout());
        setVisible(true);
    }

    static void addSale(Connection connection, int amount) throws SQLException {
        String salesQuery = "INSERT INTO sales (day, day_sales) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE day_sales = day_sales + ?";
        try (PreparedStatement salesStatement = connection.prepareStatement(salesQuery)) {
            java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
            salesStatement.setDate(1, currentDate);
            salesStatement.setInt(2, amount);
            salesStatement.setInt(3, amount);

            salesStatement.executeUpdate();
        }
    }



    // 추가: 현재 날짜를 반환하는 메서드
    private Date getCurrentDate() {
        long currentTimeMillis = System.currentTimeMillis();
        return new Date(currentTimeMillis);
    }


    private void saveOrderToDatabase(Connection connection, int seatNumber, String foodName) throws SQLException {
        // 현재까지의 최대 주문 아이디 조회
        int orderId = getMaxOrderId(connection) + 1;

        String query = "INSERT INTO food_order (order_id, seat_num, food_name) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, orderId);
            statement.setInt(2, seatNumber);
            statement.setString(3, foodName);
            statement.executeUpdate();
        }
    }

    private int getMaxOrderId(Connection connection) throws SQLException {
        String query = "SELECT MAX(order_id) FROM food_order";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0; // 주문이 없는 경우
            }
        }
    }

    private void updateInfoPanel() {
        StringBuilder selectedFoodsText = new StringBuilder("Selected Foods:\n");
        for (String food : selectedFoods) {
            selectedFoodsText.append(food).append("\n");
        }

        textArea.setText(selectedFoodsText.toString());
        totalPriceLabel.setText("Total Price: " + totalPrice + "원");
    }

    private void clearSelection() {
        selectedFoods.clear();
        totalPrice = 0;
        updateInfoPanel();
    }

    private class FoodButtonActionListener implements ActionListener {
        private String name;
        private int price;

        private Connection buttonConnection;

        public FoodButtonActionListener(String name, int price, Connection connection) {
            this.name = name;
            this.price = price;
            this.buttonConnection = connection;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedFoods.add(name);
            totalPrice += price;

            updateInfoPanel();
        }
    }

    // 추가: UI 업데이트 메서드
    public void updateUI() {
        removeAll();
        revalidate();
        repaint();
        // 여기서 UI 업데이트 로직을 추가할 수 있습니다.
    }

    public static void main(String[] args) {
        // JDBC 드라이버 로딩
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "JDBC 드라이버 로딩에 실패했습니다.");
            System.exit(1);
        }

        // UI 생성
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Food Order");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new FoodOrder(new UserStatus())); // 적절한 사용자 상태 객체를 생성하여 전달
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}