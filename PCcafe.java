package PCRoom;

import javax.swing.*;

public class PCcafe extends JFrame {
    private UserStatus US;
    private FoodOrder FO;

    public PCcafe() {
        setTitle("PC Cafe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 400);
        setLocationRelativeTo(null);

        US = new UserStatus();
        FO = new FoodOrder(US);

        // JSplitPane을 사용하여 유저 스테이터스와 푸드 오더를 좌우로 나눔
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, FO, US);
        splitPane.setDividerLocation(1000);  // 분할 위치 지정 (기준은 픽셀)

        add(splitPane);


        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PCcafe());
    }
}

