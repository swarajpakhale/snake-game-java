import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

// 1. Abstraction: abstract base class for every object drawn on the board
abstract class GameObject {
    protected int x, y;
    protected static final int SIZE = 25;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }
    // 2. Polymorphism: abstract method, each subclass draws itself differently
    public abstract void draw(Graphics g);
}

// 3. Inheritance: SnakeSegment extends GameObject
class SnakeSegment extends GameObject {
    private boolean head;

    public SnakeSegment(int x, int y, boolean head) {
        super(x, y);
        this.head = head;
    }

    public void setHead(boolean head) {
        this.head = head;
    }

    // Method Overriding
    @Override
    public void draw(Graphics g) {
        g.setColor(head ? new Color(0, 110, 0) : new Color(50, 205, 50));
        g.fillRoundRect(x, y, SIZE, SIZE, 8, 8);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, SIZE, SIZE, 8, 8);
        if (head) {                       // eyes are drawn only on the head
            g.setColor(Color.WHITE);
            g.fillOval(x + 5, y + 6, 6, 6);
            g.fillOval(x + 14, y + 6, 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval(x + 7, y + 8, 3, 3);
            g.fillOval(x + 16, y + 8, 3, 3);
        }
    }
}

// 3. Inheritance: Food extends GameObject
class Food extends GameObject {
    public Food(int x, int y) {
        super(x, y);
    }

    public void relocate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(x + 2, y + 4, SIZE - 4, SIZE - 5);
        g.setColor(new Color(0, 150, 0));
        g.fillRect(x + 11, y, 3, 6);
    }
}

// Main Game Panel - Swing component + Event Handling (Timer and Keyboard)
class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int CELL = 25;
    private static final int COLS = 24, ROWS = 24;
    private static final int BOARD_W = COLS * CELL;     // 600
    private static final int BOARD_H = ROWS * CELL;     // 600
    private static final int TOP = 40;                  // height of score bar

    private boolean running = false;
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean newRecord = false;

    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private int delay = 120;                            // timer delay in ms

    private char direction = 'R';                       // U, D, L, R
    private char nextDirection = 'R';

    // 4. Collections Framework: ArrayList stores the snake (index 0 = head)
    private ArrayList<SnakeSegment> snake;
    private Food food;
    private Timer timer;
    private Random random = new Random();

    public GamePanel() {
        setPreferredSize(new Dimension(BOARD_W, BOARD_H + TOP));
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        loadHighScore();
        timer = new Timer(delay, this);
        initGame();
        timer.start();
    }

    private void initGame() {
        snake = new ArrayList<>();
        snake.add(new SnakeSegment(300, 300, true));
        snake.add(new SnakeSegment(275, 300, false));
        snake.add(new SnakeSegment(250, 300, false));
        direction = 'R';
        nextDirection = 'R';
        score = 0;
        level = 1;
        delay = 120;
        timer.setDelay(delay);
        newRecord = false;
        spawnFood();
    }

    private void spawnFood() {
        int fx, fy;
        boolean onSnake;
        do {
            fx = random.nextInt(COLS) * CELL;
            fy = random.nextInt(ROWS) * CELL;
            onSnake = false;
            for (SnakeSegment s : snake) {
                if (s.x == fx && s.y == fy) {
                    onSnake = true;
                    break;
                }
            }
        } while (onSnake);

        if (food == null) food = new Food(fx, fy);
        else food.relocate(fx, fy);
    }

    // 5. File Handling: high score is stored in highscore.txt
    private void loadHighScore() {
        try (BufferedReader br = new BufferedReader(new FileReader("highscore.txt"))) {
            String line = br.readLine();
            if (line != null) highScore = Integer.parseInt(line.trim());
        } catch (IOException | NumberFormatException e) {
            highScore = 0;
        }
    }

    private void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            newRecord = true;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("highscore.txt"))) {
                bw.write(String.valueOf(highScore));
            } catch (IOException e) {
                System.out.println("Error saving high score.");
            }
        }
    }

    private void endGame() {
        running = false;
        gameOver = true;
        saveHighScore();
    }

    private void move() {
        direction = nextDirection;
        SnakeSegment head = snake.get(0);
        int nx = head.x, ny = head.y;

        switch (direction) {
            case 'U': ny -= CELL; break;
            case 'D': ny += CELL; break;
            case 'L': nx -= CELL; break;
            default:  nx += CELL; break;
        }

        // Wall collision
        if (nx < 0 || nx >= BOARD_W || ny < 0 || ny >= BOARD_H) {
            endGame();
            return;
        }

        boolean ate = (nx == food.x && ny == food.y);

        // Self collision (the tail cell is free unless the snake is growing)
        int limit = ate ? snake.size() : snake.size() - 1;
        for (int i = 0; i < limit; i++) {
            SnakeSegment s = snake.get(i);
            if (s.x == nx && s.y == ny) {
                endGame();
                return;
            }
        }

        head.setHead(false);
        snake.add(0, new SnakeSegment(nx, ny, true));

        if (ate) {
            score += 10;
            level = score / 50 + 1;                     // level up every 5 foods
            delay = Math.max(60, 120 - (level - 1) * 10);
            timer.setDelay(delay);
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }
    private void drawCentered(Graphics g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (BOARD_W - fm.stringWidth(text)) / 2, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Score bar
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, BOARD_W, TOP);
        g.setColor(Color.WHITE);
        g.setFont(new Font("serif", Font.BOLD, 20));
        g.drawString("High Score: " + highScore, 20, 27);
        g.drawString("Level: " + level, 265, 27);
        g.drawString("Score: " + score, 470, 27);

        g.translate(0, TOP);

        // Board background and grid
        g.setColor(Color.DARK_GRAY.darker());
        g.fillRect(0, 0, BOARD_W, BOARD_H);
        g.setColor(new Color(70, 70, 70));
        for (int i = 0; i <= COLS; i++) g.drawLine(i * CELL, 0, i * CELL, BOARD_H);
        for (int j = 0; j <= ROWS; j++) g.drawLine(0, j * CELL, BOARD_W, j * CELL);

        // Food and snake drawn through the common draw() method (Polymorphism)
        food.draw(g);
        for (SnakeSegment segment : snake) {
            segment.draw(g);
        }

        // Border
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, BOARD_W, 3);
        g.fillRect(0, 0, 3, BOARD_H);
        g.fillRect(BOARD_W - 3, 0, 3, BOARD_H);
        g.fillRect(0, BOARD_H - 3, BOARD_W, 3);

        // Messages (drawn on a semi-transparent box so the text is easy to read)
        if (gameOver || paused || !running) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect(50, 215, 500, 180, 20, 20);
        }
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("serif", Font.BOLD, 34));
            drawCentered(g, "Game Over, Score: " + score, 280);
            if (newRecord) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("serif", Font.BOLD, 22));
                drawCentered(g, "New High Score!", 320);
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font("serif", Font.BOLD, 20));
            drawCentered(g, "Press Enter to Restart", 365);
        } else if (paused) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("serif", Font.BOLD, 34));
            drawCentered(g, "PAUSED", 285);
            g.setColor(Color.WHITE);
            g.setFont(new Font("serif", Font.BOLD, 20));
            drawCentered(g, "Press P to Resume", 330);
        } else if (!running) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("serif", Font.BOLD, 28));
            drawCentered(g, "Press any Arrow key to Start", 285);
            g.setColor(Color.WHITE);
            g.setFont(new Font("serif", Font.BOLD, 18));
            drawCentered(g, "P = Pause   |   Enter = Restart after Game Over", 325);
        }

        g.translate(0, -TOP);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            move();
        }
        repaint();
    }

    private boolean isOpposite(char a, char b) {
        return (a == 'U' && b == 'D') || (a == 'D' && b == 'U')
        || (a == 'L' && b == 'R') || (a == 'R' && b == 'L');
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameOver) {
            if (key == KeyEvent.VK_ENTER) {
                initGame();
                gameOver = false;
                paused = false;
                running = true;
                repaint();
            }
            return;
        }

        if (key == KeyEvent.VK_P) {
            if (running) {
                paused = !paused;
                repaint();
            }
            return;
        }
        if (paused) return;

        char d = ' ';
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) d = 'U';
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) d = 'D';
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) d = 'L';
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) d = 'R';

        if (d != ' ') {
            running = true;                              // first key press starts the game
            if (!isOpposite(d, direction)) nextDirection = d;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}

public class SnakeGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Game using Java");
            GamePanel gamePanel = new GamePanel();

            frame.add(gamePanel);
            frame.pack();
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            gamePanel.requestFocusInWindow();
        });
    }
}
