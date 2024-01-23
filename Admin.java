package PCRoom;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.sql.*;
import java.time.*;

class Admin extends JFrame implements ListSelectionListener {
    private String[] function = {"고객 정보 검색", "현재 좌석", "매출","음식 주문 확인","시간 추가"};
    private JList<String> jlst = new JList<>(function);
    private JLabel jlname = new JLabel();
    private JPanel seatPanel = new JPanel();
    private JPanel centerPanel = new JPanel(new BorderLayout());

    private boolean isLoggedIn = false;

    public Admin() {
        login();
    }

    private void login() {
        JPanel loginPanel = new JPanel(new GridLayout(3, 2));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        loginPanel.add(new JLabel("아이디 : "));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("비밀번호 : "));
        loginPanel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, loginPanel,
                "로그인", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (username.equals("admin") && password.equals("1234")) {
                isLoggedIn = true;
                adminGUI();
            } else {
                JOptionPane.showMessageDialog(null, "로그인 실패: 잘못된 사용자 이름 또는 비밀번호");
                login();
            }
        } else {
            System.exit(0); 
        }
    }

    private void adminGUI() {
        Container ct = getContentPane();
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(jlst), BorderLayout.CENTER);

        centerPanel.add(jlname, BorderLayout.NORTH);
        centerPanel.add(seatPanel, BorderLayout.CENTER);

        ct.add(leftPanel, BorderLayout.WEST);
        ct.add(centerPanel, BorderLayout.CENTER);

        jlst.addListSelectionListener(this);
        setTitle("PC방 관리 프로그램");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void valueChanged(ListSelectionEvent lse) {
        if (!isLoggedIn) {
            JOptionPane.showMessageDialog(null, "먼저 로그인이 필요합니다.");
            login();
            return;
        }

        int[] indices = jlst.getSelectedIndices();

        if (indices.length > 0) {
            String s = "";
            for (int j = 0; j < indices.length; j++) {
                s = s + function[indices[j]] + " ";
            }
            jlname.setText(s);

            if (function[indices[0]].equals("고객 정보 검색")) {
                clearPanel();
                showCustomer();
            } else if (function[indices[0]].equals("현재 좌석")) {
                clearPanel();
                showSeat();
            } else if (function[indices[0]].equals("매출")) {
                clearPanel();
                showSales();
            } else if (function[indices[0]].equals("음식 주문 확인")) {
                clearPanel();
                showFoodOrders();
            } else if (function[indices[0]].equals("시간 추가")) {
            	clearPanel();
            	addTime();
            }
        }
    }
    private void showCustomer() {
        JTextArea displayArea = new JTextArea(10, 30);
        displayArea.setEditable(false);

        String URL = "jdbc:mysql://localhost:3306/pcroom";
        String USER = "root";
        String PASSWORD = "1234";

        String customerName = JOptionPane.showInputDialog(null, "고객의 이름을 입력하세요:", "이름 입력", JOptionPane.QUESTION_MESSAGE);

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM customer WHERE name = ?")) {

            preparedStatement.setString(1, customerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                displayArea.append(resultSet.getString("name") + " 의 정보\n");
                displayArea.append("아이디 : " + resultSet.getString("id") + "\n");
                displayArea.append("휴대폰 번호 : " + resultSet.getString("phone_num") + "\n");
                displayArea.append("잔여 시간 : " + resultSet.getInt("time") + "\n");
                displayArea.append("현재 이용 좌석 : " + resultSet.getInt("seat") + "\n");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        seatPanel.add(new JScrollPane(displayArea));
        seatPanel.revalidate();

    }

    private long timeToSeconds(Time time) {
        if (time == null) {
            return 0;
        }
        LocalTime localTime = time.toLocalTime();
        return localTime.toSecondOfDay();
    }

    private long nowToSeconds() {
        LocalTime now = LocalTime.now();
        return now.toSecondOfDay();
    }

    private void showSeat() {
        String URL = "jdbc:mysql://localhost:3306/pcroom";
        String USER = "root";
        String PASSWORD = "1234";
        
        String[] customerNames = new String[16];
        Time[] customerTimes = new Time[16];
        Time[] customerLoginTimes = new Time[16];
        for (int i = 0; i < 16; i++) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM customer WHERE seat = ?")) {

                preparedStatement.setInt(1, i + 1);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    customerNames[i] = resultSet.getString("name");
                    Time time = resultSet.getTime("time");
                    Time loginTime = resultSet.getTime("login_time");

                    if (time != null && loginTime != null) {
                        customerTimes[i] = time;
                        customerLoginTimes[i] = loginTime;
                    } else {
                        customerTimes[i] = null;
                        customerLoginTimes[i] = null;
                    }
                } else {
                    customerNames[i] = null;
                    customerTimes[i] = null;
                    customerLoginTimes[i] = null;
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        seatPanel.removeAll();
        seatPanel.setLayout(new GridLayout(5, 4, 5, 5));

        for (int j = 0; j < 16; j++) {
            JButton seatButton = new JButton("좌석 " + (j + 1));
            int seatIndex = j;

            seatButton.addActionListener(e -> {
                if (customerNames[seatIndex] != null && customerTimes[seatIndex] != null && customerLoginTimes[seatIndex] != null) {
                    
                	long timeInSeconds = timeToSeconds(customerTimes[seatIndex]);
                    long loginTimeInSeconds = timeToSeconds(customerLoginTimes[seatIndex]);
                    long nowInSeconds = nowToSeconds();

                    if (loginTimeInSeconds > nowInSeconds)
                    {
                    	long usedTime = 86400 - loginTimeInSeconds + nowInSeconds;
                    	long remainingTime = timeInSeconds - usedTime;
                    	if (remainingTime > 0)
                    	{
                    	  	long remainingHours = remainingTime / 3600;
                        	long remainingMinutes = (remainingTime % 3600) / 60;
                        	JOptionPane.showMessageDialog(null,
                            		"사용자 이름 : " + customerNames[seatIndex] +
                                    "\n선불 시간 : " + remainingHours + " 시간 " + remainingMinutes + " 분",
                                    "좌석 정보", JOptionPane.INFORMATION_MESSAGE);
                        }
                    	else {
                    		long postPaidTime = Math.abs(remainingTime);
                    		long postPaidHours = postPaidTime / 3600;
                        	long postPaidMinutes = (postPaidTime % 3600) / 60;
                        	
                        	JOptionPane.showMessageDialog(null,
                            		"사용자 이름 : " + customerNames[seatIndex] +
                                    "\n후불 시간 : " + postPaidHours + " 시간 " + postPaidMinutes + " 분",
                                    "좌석 정보", JOptionPane.INFORMATION_MESSAGE);
                        }
                              
                    }
                    else {
                    	long usedTime = nowInSeconds - loginTimeInSeconds;
                    	long remainingTime = timeInSeconds - usedTime;
                    	if (remainingTime > 0)
                    	{
                    		long remainingHours = remainingTime / 3600;
                            long remainingMinutes = (remainingTime % 3600) / 60;
                            JOptionPane.showMessageDialog(null,
                            		"사용자 이름 : " + customerNames[seatIndex] +
                                    "\n선불 시간 : " + remainingHours + " 시간 " + remainingMinutes + " 분",
                                    "좌석 정보", JOptionPane.INFORMATION_MESSAGE);
                        }
                    	else {
                    		long postPaidTime = Math.abs(remainingTime);
                    		long postPaidHours = postPaidTime / 3600;
                        	long postPaidMinutes = (postPaidTime % 3600) / 60;
                      		JOptionPane.showMessageDialog(null,
                            		"사용자 이름 : " + customerNames[seatIndex] +
                                    "\n후불 시간 : " + postPaidHours + " 시간 " + postPaidMinutes + " 분",
                                    "좌석 정보", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                   
                } else {
                    JOptionPane.showMessageDialog(null, "비어있는 좌석입니다.", "좌석 정보", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            

            if (customerNames[j] == null) {
                seatButton.setBackground(Color.GRAY);
            }
            
            seatPanel.add(seatButton);
        }
        JButton postPaidButton = new JButton("후불 좌석");
        
        postPaidButton.addActionListener(e -> {
        	StringBuilder postPaidSeatNumbers = new StringBuilder();
        	for (int i = 0; i < customerNames.length; i++) {
        		if (customerNames[i] != null) {
        			long timeInSeconds = timeToSeconds(customerTimes[i]);
                    long loginTimeInSeconds = timeToSeconds(customerLoginTimes[i]);
                    long nowInSeconds = nowToSeconds();

                    if (loginTimeInSeconds > nowInSeconds)
                    {
                    	long usedTime = 86400 - loginTimeInSeconds + nowInSeconds;
                    	long remainingTime = timeInSeconds - usedTime;
                    	if (remainingTime > 0)
                    	{
                    		continue;
                    	}
                    	else {
                    		postPaidSeatNumbers.append((i + 1)).append(", ");   
                            }
                    }
                    else {
                    	long usedTime = nowInSeconds - loginTimeInSeconds;
                    	long remainingTime = timeInSeconds - usedTime;
                    	if (remainingTime > 0)
                    	{
                    		continue;
                    	}
                    	else {
                    		postPaidSeatNumbers.append((i + 1)).append(", ");                             
                        }
                    }
            	}
        		else {
        			continue;
        		}
        	}
        	if (postPaidSeatNumbers.length() > 0) {
                JOptionPane.showMessageDialog(null, "후불 좌석 : " + postPaidSeatNumbers.toString(), "후불 좌석 정보", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "후불 좌석이 없습니다.", "후불 좌석 정보", JOptionPane.INFORMATION_MESSAGE);
            }
          }
        );
        seatPanel.add(postPaidButton);
        seatPanel.revalidate();
    }
    
   
    


    private void showSales() {
        JTextArea displayArea = new JTextArea(10, 30);
        displayArea.setEditable(false);

        String URL = "jdbc:mysql://localhost:3306/pcroom";
        String USER = "root";
        String PASSWORD = "1234";

        String date_num = JOptionPane.showInputDialog(null, "매출을 확인할 날짜를 입력하세요 (예: 2023-12-07): ", "매출", JOptionPane.QUESTION_MESSAGE);

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM sales WHERE `day` = ?")) {

            preparedStatement.setString(1, date_num);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String day_num = resultSet.getString("day");
                String select_day = day_num.substring(0, 10);
                displayArea.append("날짜   : " + select_day + "\n");
                displayArea.append("총 매출 : " + resultSet.getInt("day_sales") + "\n");
            }

        } catch (SQLException ex) {
            
        ex.printStackTrace();
        }

        seatPanel.add(new JScrollPane(displayArea));
        seatPanel.revalidate();

    }
    private void showFoodOrders() {
    	JTextArea displayArea = new JTextArea(10, 30);
        displayArea.setEditable(false);

        String URL = "jdbc:mysql://localhost:3306/pcroom";
        String USER = "root";
        String PASSWORD = "1234";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT * FROM food_order");

            while (resultSet.next()) {
                int orderId = resultSet.getInt("order_id");
                int seatNum = resultSet.getInt("seat_num");
                String foodName = resultSet.getString("food_name");
                
                displayArea.append("------------------------------\n");
                displayArea.append("\n주문 번호  : "+orderId + "\n");
                displayArea.append("주문 음식 : " + foodName + "\n");
                displayArea.append("좌석 번호 : " + seatNum + "번 좌석\n\n");
            }
            

            seatPanel.add(new JScrollPane(displayArea));
            seatPanel.revalidate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    private void addTime() {
        String URL = "jdbc:mysql://localhost:3306/pcroom";
        String USER = "root";
        String PASSWORD = "1234";
    	String customerName = JOptionPane.showInputDialog(null, "고객의 이름을 입력하세요:", "이름 입력", JOptionPane.QUESTION_MESSAGE);
        int[] additionalHours = {1, 3, 5, 7, 10, 12};
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 50, 100)); 

        for (int i = 0; i < 6; i++) {
            int hours = additionalHours[i];
            JButton addTimeButton = new JButton(hours + "시간 추가");
            addTimeButton.addActionListener(e -> {
                try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement preparedStatement = connection.prepareStatement(
                             "UPDATE customer SET time = ADDTIME(time, ?) WHERE name = ?")) {

                    preparedStatement.setString(1, String.format("%02d:00:00", hours));
                    preparedStatement.setString(2, customerName);

                    int rowsAffected = preparedStatement.executeUpdate();

                    if (rowsAffected > 0) {
                        JOptionPane.showMessageDialog(null, customerName + "님의 시간이 " + hours + "시간 추가되었습니다.", "시간 추가 완료", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "시간 추가에 실패했습니다. 사용자를 찾을 수 없습니다.", "시간 추가 실패", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (SQLException | NumberFormatException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "시간 추가 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            });

            buttonPanel.add(addTimeButton); 
            seatPanel.removeAll();
            seatPanel.setLayout(new BorderLayout()); 
            seatPanel.add(buttonPanel, BorderLayout.CENTER);

            seatPanel.revalidate(); 
        }
    }

    private void clearPanel() {
        seatPanel.removeAll();
        seatPanel.revalidate();
        seatPanel.repaint();
    }

    public static void main(String[] args) {
        new Admin();
    }
}
