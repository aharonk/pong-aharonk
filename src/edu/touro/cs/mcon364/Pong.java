package edu.touro.cs.mcon364;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Pong extends JFrame {
    public static void main(String[] args) {
        new Pong();
    }

    private static final int WIDTH = 400, HEIGHT = 600, BALL_DIAMETER = 25, NUM_SCORES = 3, PADDLE_WIDTH = 20,
            PADDLE_HEIGHT = 100, EDGE_MARGIN = 10;
    private static final Path LEADERBOARD_STORAGE = Paths.get("./scores.txt");
    private final LinkedList<Standing> LEADERBOARD;
    private final Timer GAME_CLOCK;

    private Point ball, paddle;
    private int ballDx, ballDy, score;

    private Pong() {
        setTitle("Pong");
        setSize(WIDTH, HEIGHT);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                GAME_CLOCK.stop();
                submitScore("Nul");
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        GAME_CLOCK = new Timer(25, new GameTick());
        LEADERBOARD = loadStandings();

        JTextArea text = getStandings();
        text.setText(text.getText() + "\nAre you ready to play?");
        setVisible(true);

        int play = JOptionPane.showConfirmDialog(this, text);
        if (play == JOptionPane.YES_OPTION) {
            newGame();
            pack();
        } else {
            System.exit(0);
        }
    }

    private LinkedList<Standing> loadStandings() {
        LinkedList<Standing> scores = new LinkedList<>();

        try {
            List<String> scoreLines = Files.readAllLines(LEADERBOARD_STORAGE);
            for (int i = 0; i < NUM_SCORES; i++) {
                String[] data = scoreLines.get(i).split("\t");
                scores.add(new Standing(data[0], Integer.parseInt(data[1])));
            }
        } catch (IOException e) {
            e.printStackTrace();
            for (int i = 0; i < NUM_SCORES; i++) {
                scores.add(Standing.NULL);
            }
        }

        return scores;
    }

    private JTextArea getStandings() {
        JTextArea out = new JTextArea();
        out.setBorder(null);
        out.setOpaque(false);
        out.setFont(UIManager.getFont("Label.font"));

        StringBuilder scores = new StringBuilder("Hi Scores:\n");
        for (Standing s : LEADERBOARD) {
            scores.append(s).append("\n");
        }

        out.setText(scores.toString());
        return out;
    }

    private void newGame() {
        score = 0;

        ball = new Point(WIDTH / 2 - BALL_DIAMETER / 2, HEIGHT / 2 - BALL_DIAMETER / 2);
        paddle = new Point(WIDTH - EDGE_MARGIN - PADDLE_WIDTH, HEIGHT / 2 - PADDLE_HEIGHT / 2);

        JPanel game = new GamePanel();
        add(game, BorderLayout.CENTER);
        game.setFocusable(true);
        game.requestFocusInWindow();
    }

    private void lose() {
        GAME_CLOCK.stop();

        submitScore(null);

        JTextArea text = getStandings();
        text.setText(text.getText() + "\nWould you like to play again?");

        int response = JOptionPane.showConfirmDialog(this, text,
                "Play again?", JOptionPane.YES_NO_OPTION);
        if (response == 0) {
            newGame();
        } else {
            System.exit(0);
        }
    }

    private void submitScore(String name) {
        int index = 0;
        while (index < NUM_SCORES && LEADERBOARD.get(index).SCORE > score) {
            index++;
        }

        if (index < NUM_SCORES) {
            if (name == null) {
                name = JOptionPane.showInputDialog(this, "Enter your name for the leaderboard.");

                if (name == null || name.isEmpty()) {
                    name = "Nul";
                }
            }
            LEADERBOARD.add(index, new Standing(name, score));
            LEADERBOARD.removeLast();
        }
        saveScores();
    }

    private void saveScores() {
        LinkedList<String> toWrite = new LinkedList<>();
        for (Standing s : LEADERBOARD) {
            toWrite.add(s.toString());
        }

        try {
            Files.write(LEADERBOARD_STORAGE, toWrite);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save scores.", "Saving Failed!",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class Standing {
        public static final Standing NULL = new Standing("   ", 0);

        public final String NAME;
        public final int SCORE;

        public Standing(String name, int score) {
            this.NAME = name.substring(0, 3);
            this.SCORE = score;
        }

        @Override
        public String toString() {
            return NAME + "\t" + SCORE;
        }
    }

    private class GamePanel extends JPanel {
        private static final int PADDLE_SPEED = 10;

        private GamePanel() {
            super(new BorderLayout());
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(Pong.WIDTH, Pong.HEIGHT));

            GAME_CLOCK.start();

            Random rand = new Random();
            ballDx = rand.nextInt() % 2 == 0 ? 4 : -4;
            ballDy = rand.nextInt() % 2 == 0 ? 4 : -4;

            addMouseWheelListener(e -> movePaddle(PADDLE_SPEED * e.getWheelRotation()));

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        movePaddle(-PADDLE_SPEED);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        movePaddle(PADDLE_SPEED);
                    }
                }
            });
        }

        private void movePaddle(int dY) {
            int newY = paddle.y + dY;
            if (newY <= 0) {
                paddle.y = 0;
            } else if (paddle.y + PADDLE_HEIGHT > Pong.HEIGHT) {
                paddle.y = Pong.HEIGHT - PADDLE_HEIGHT;
            } else {
                paddle.translate(0, dY);
            }
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0xd3d3d3));
            g.fillOval(ball.x, ball.y, BALL_DIAMETER, BALL_DIAMETER);
            g.fillRect(paddle.x, paddle.y, PADDLE_WIDTH, PADDLE_HEIGHT);
            //score
            g.setFont(g.getFont().deriveFont(50f));
            g.drawString(Integer.toString(score), 10, 50);
        }
    }

    private class GameTick implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ball.translate(ballDx, ballDy);

            if (ball.x <= 0) {
                ballDx = -ballDx;
            } else if (ball.x + BALL_DIAMETER >= WIDTH) {
                lose();
            } else if (ball.x + BALL_DIAMETER >= WIDTH - PADDLE_WIDTH - EDGE_MARGIN
                    && ball.y + BALL_DIAMETER >= paddle.y && ball.y <= paddle.y + PADDLE_HEIGHT && ballDx > 0) {
                score++;
                if (ball.y + BALL_DIAMETER <= paddle.y - 5 || ball.y >= paddle.y + PADDLE_HEIGHT - 5) {
                    ballDy = -ballDy;
                } else {
                    ballDx = -ballDx;
                }
            }

            if (ball.y + BALL_DIAMETER >= HEIGHT || ball.y <= 0) {
                ballDy = -ballDy;
            }
            repaint();
        }
    }
}
