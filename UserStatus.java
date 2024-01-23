package PCRoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static PCRoom.FoodOrder.addSale;

public class UserStatus extends JPanel {

    private JTextField IdField;
    private JPasswordField PasswordField;
    private JLabel seatNumberLabel;
    private JLabel loginTimeLabel;
    private JLabel feeLabel;
    private JPanel loginPanel;
    private JPanel logoutPanel;
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/pcroom";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";
    private String loggedInUser;
    private int seatNumber;
    private int elapsedTimeInSeconds; // 초로 표현된 경과 시간
    private Timer timer; // 타이머 객체
    private int baseFee = 1000; // 초기 요금 (1시간당)
    private int feeIncrement = 1500; // 추가로 올라가는 요금 (1시간당)
    GridBagConstraints gbc = new GridBagConstraints();
    public UserStatus() {
        setSize(400, 200);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
        JPanel panel = new JPanel(new GridBagLayout());
        loginPanel = new JPanel(new GridBagLayout());
        JLabel idLabel = new JLabel("사용자 이름:");
        JLabel passwordLabel = new JLabel("비밀번호:");
        IdField = new JTextField();
        PasswordField = new JPasswordField();
        JButton loginButton = new JButton("로그인");
        JButton signupButton = new JButton("회원가입");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = IdField.getText();
                char[] passwordChars = PasswordField.getPassword();
                String password = new String(passwordChars);
                checkLogin(id, password);

            }
        });

        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSignupDialog();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        loginPanel.add(idLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.ipadx = 100;
        loginPanel.add(IdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.ipadx = 0;
        loginPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.ipadx = 100;
        loginPanel.add(PasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        loginPanel.add(loginButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        loginPanel.add(signupButton, gbc);

        logoutPanel = new JPanel(new GridBagLayout());
        logoutPanel.setVisible(false);

        seatNumberLabel = new JLabel();
        loginTimeLabel = new JLabel();
        feeLabel = new JLabel();

        JButton logoutButton = new JButton("로그아웃");



        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
                stopTimer();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        logoutPanel.add(seatNumberLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        logoutPanel.add(loginTimeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        logoutPanel.add(feeLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        logoutPanel.add(logoutButton, gbc);

        panel.add(loginPanel);
        panel.add(logoutPanel);
        add(panel);

        setVisible(true);
    }
    private void recordLoginTime(Connection connection, String username) throws SQLException {
        String updateQuery = "UPDATE customer SET login_time = CURRENT_TIME() WHERE id=?";
        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setString(1, username);
            updateStatement.executeUpdate();
        }
    }

    private boolean checkLogin(String name, String password) {
        String id = IdField.getText();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT * FROM customer WHERE id=? AND pw=?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, name);
                statement.setString(2, password);
                try (ResultSet resultSet = statement.executeQuery()) {

                    if (resultSet.next()) {
                        loggedInUser = name;
                        seatNumber = assignSeatNumber();
                        elapsedTimeInSeconds = 0;

                        if (seatNumber != -1) {
                            elapsedTimeInSeconds = 0;
                            recordLoginTime(connection, name);
                            recordSeatNumber(connection, name, seatNumber);

                            addSale(connection, 1000);

                            JOptionPane.showMessageDialog(UserStatus.this, id + "님, 로그인 성공!");
                            updateUI();

                            loginPanel.setVisible(false);
                            logoutPanel.setVisible(true);

                            startTimer();

                            return true;

                        } else if(seatNumber == -1){
                        // 모든 좌석이 사용 중이라 로그인 실패
                            return false;
                        }
                    } else {
                        JOptionPane.showMessageDialog(UserStatus.this, "로그인 실패. 올바른 사용자 이름과 비밀번호를 입력하세요.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


    private int assignSeatNumber() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT seat FROM customer WHERE seat >= 1 AND seat <= 15";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    Set<Integer> occupiedSeats = new HashSet<>();
                    while (resultSet.next()) {
                        occupiedSeats.add(resultSet.getInt("seat"));
                    }

                    for (int i = 1; i <= 15; i++) {
                        if (!occupiedSeats.contains(i)) {
                            return i;
                        }
                    }

                    // 만약 여기까지 왔다면 모든 좌석이 사용 중임
                    JOptionPane.showMessageDialog(this, "모든 좌석이 사용 중입니다. 나중에 다시 시도하세요.", "알람", JOptionPane.WARNING_MESSAGE);
                    return -1; // 실패 시 -1을 반환
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 1;
    }
    private void recordSeatNumber(Connection connection, String username, int seatNumber) throws SQLException {
        String updateQuery = "UPDATE customer SET seat = ? WHERE id = ?";
        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setInt(1, seatNumber);
            updateStatement.setString(2, username);
            updateStatement.executeUpdate();
        }
    }
    private void recordLogoutTime(Connection connection, String username) throws SQLException {
        String updateQuery = "UPDATE customer SET logout_time = CURRENT_TIME() WHERE id=?";
        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            updateStatement.setString(1, username);
            updateStatement.executeUpdate();
        }
    }
    private void showSignupDialog() {

        JTextField phoneField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField idField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        Object[] message = {
                "전화번호(-제외):", phoneField,
                "이름:", nameField,
                "아이디:", idField,
                "비밀번호:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "회원가입", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String pn = phoneField.getText();
            String nm = nameField.getText();
            String id = idField.getText();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            if (registerUser(pn, nm, id, password)) {
                JOptionPane.showMessageDialog(this, "회원가입이 완료되었습니다!");
            } else {
                JOptionPane.showMessageDialog(this, "회원가입에 실패했습니다. 이미 존재하는 아이디일 수 있습니다.");
            }
        }
    }

    private boolean registerUser(String pn,String name, String id, String password) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO pcroom.customer (phone_num,name, id, pw) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, pn);
                statement.setString(2, name);
                statement.setString(3, id);
                statement.setString(4, password);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateUI() {
        try {
            if (seatNumberLabel != null) {
                seatNumberLabel.setText("좌석 번호: " + seatNumber);
            } else {
                System.out.println("seatNumberLabel is null");
            }

            if (loginTimeLabel != null) {
                loginTimeLabel.setText("로그인 시간: " + formatElapsedTime());
            } else {
                System.out.println("loginTimeLabel is null");
            }

            if (feeLabel != null) {
                feeLabel.setText("요금: " + calculateFee());
            } else {
                System.out.println("feeLabel is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        loginPanel.setVisible(true);
        logoutPanel.setVisible(false);

        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            recordLogoutTime(connection, loggedInUser);

            deleteFoodOrderForSeat(seatNumber);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        stopTimer();
    }

    private void deleteFoodOrderForSeat(int seatNumber) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
            String deleteQuery = "DELETE FROM food_order WHERE seat_num = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                deleteStatement.setInt(1, seatNumber);
                deleteStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private String formatElapsedTime() {
        int hours = elapsedTimeInSeconds / 3600;
        int minutes = (elapsedTimeInSeconds % 3600) / 60;
        int seconds = elapsedTimeInSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void startTimer() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elapsedTimeInSeconds++;
                updateUI();
            }
        });
        timer.start();
    }

    private void stopTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    private int calculateFee() {
        int elapsedHours = elapsedTimeInSeconds / 3600;
        return baseFee + elapsedHours * feeIncrement;
    }
    public void addToFee(int additionalFee) {
        baseFee += additionalFee;
        updateUI();
    }
    public int getSeatNumber() {
        return seatNumber;
    }
}
