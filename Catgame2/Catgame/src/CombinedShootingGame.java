import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.Random;

public class CombinedShootingGame extends JFrame {
    private StartMenuPanel startMenuPanel;
    private GamePanel gamePanel;
    

    public static void main(String[] args) {
        CombinedShootingGame game = new CombinedShootingGame();
        game.setSize(1980, 1080);
        game.setVisible(true);
    }

    public CombinedShootingGame() {
        setTitle("Shooting Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // สร้างหน้าเมนูเริ่ม
        startMenuPanel = new StartMenuPanel();
        gamePanel = new GamePanel();

        // ตั้งค่าเริ่มต้นให้แสดงหน้าเมนู
        setLayout(new CardLayout());
        add(startMenuPanel, "StartMenu");
        add(gamePanel, "Game");

        // กำหนดให้แสดงหน้าเมนูเริ่มก่อน
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), "StartMenu");

        // กำหนด Action สำหรับปุ่ม Start
        startMenuPanel.startButton.addActionListener(e -> startGame());
    }

    // ฟังก์ชันเริ่มเกมเมื่อกดปุ่ม Start
    private void startGame() {
        // สลับไปยังหน้าเกม
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), "Game");
        gamePanel.requestFocusInWindow();  // ทำให้หน้าเกมโฟกัสกับคีย์บอร์ด
        
    }

    // หน้าเมนูเริ่มต้น
    class StartMenuPanel extends JPanel {
        JButton startButton;
        private Image backgroundImagestart;
        private Image catImage;

        public StartMenuPanel() {
             setLayout(new GridBagLayout());

            // โหลดภาพพื้นหลัง
            backgroundImagestart = new ImageIcon(getClass().getResource("BG start.png")).getImage();
            catImage = new ImageIcon(getClass().getResource("cat1.png")).getImage();

            startButton = new JButton("Start Game");
            startButton.setFont(new Font("Arial", Font.BOLD, 30));
           // startButton.setOpaque(false); // ทำให้ปุ่มโปร่งใส
            startButton.setContentAreaFilled(false);
            startButton.setBorderPainted(false);
            add(startButton);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImagestart, 0, 0, getWidth(), getHeight(), this); // วาดภาพเต็มจอ
            int catX = getWidth() - catImage.getWidth(this);
            int catY = getHeight() - catImage.getHeight(this);
            g.drawImage(catImage, catX, catY, this);
        }

    }
    }

    class GamePanel extends JPanel implements MouseMotionListener, KeyListener {
        private final static int BALL_RADIUS = 10;
        private final static int GUN_LENGTH = 25;

        private int normalEnemiesSpawned = 0;  // ตัวนับศัตรูธรรมดา
        private boolean allNormalEnemiesCleared = false;  // ตรวจสอบว่าเกิดศัตรูธรรมดาครบหรือไม่
        private int greenEnemiesSpawned = 0;

        private double angle;
        private int score = 0;
        
        private int health = 5;
        private int scoreThreshold = 500;
        private int gunPositionX;
        private int ammo = 300;  // จำนวนกระสุนเริ่มต้น
        private boolean ammoAvailable = true;  // สถานะของกระสุน (ว่ายิงได้หรือไม่)
        private int currentLevel = 1;
       
       

        private LinkedList<SmallBall> bullets = new LinkedList<>();
        private LinkedList<Enemy> enemies = new LinkedList<>();
        private LinkedList<AmmoItem> ammoItems = new LinkedList<>();  // สำหรับเก็บรายการไอเท็มกระสุน
        private Thread enemyThread, bulletThread;
        private Random random = new Random();
        
        // Images for background, enemies, gun, and ammo item
        private Image backgroundImage;
        private Image enemyImage;
        private Image greenEnemyImage;
        private Image gunImage;
        private Image ammoImage;
        private Image catImage;
        private Image heartImage;
        
        


        public GamePanel() {
            setFocusable(true);
            addMouseMotionListener(this);
            addKeyListener(this);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    gunPositionX = getWidth() / 2;
                }
            });

            // Load images
            backgroundImage = new ImageIcon(getClass().getResource("BG state.png")).getImage();
            enemyImage = new ImageIcon(getClass().getResource("Rat1.png")).getImage();
            greenEnemyImage = new ImageIcon(getClass().getResource("Rat2.png")).getImage();
            gunImage = new ImageIcon(getClass().getResource("Gun.png")).getImage();
            ammoImage = new ImageIcon(getClass().getResource("ammo.png")).getImage();  // ไอเท็มกระสุน
            catImage = new ImageIcon(getClass().getResource("cat1.png")).getImage();
            heartImage = new ImageIcon(getClass().getResource("heart.png")).getImage();

            enemyThread = new Thread(this::moveEnemies);
            enemyThread.start();

            bulletThread = new Thread(this::moveBullets);
            bulletThread.start();

            Timer enemyTimer = new Timer(2000, e -> spawnEnemy());
            enemyTimer.start();

            // สร้างไอเท็มกระสุนทุกๆ 20 วินาที
            Timer ammoTimer = new Timer(20000, e -> spawnAmmoItem());
            ammoTimer.start();
        }

        

        class SmallBall {
            int length;
            int angle;
            int originX;

            SmallBall(int length, int angle, int originX) {
                this.length = length;
                this.angle = angle;
                this.originX = originX;
            }

            public int getX() {
                return (int) (length * Math.cos(Math.toRadians(angle )) + originX);
            }

            public int getY(int height) {
                return (int) (height - length * Math.sin(Math.toRadians(angle + 180)));
            }

            public void move() {
                length += 5;
            }
        }

        class Enemy {
            int x, y;
            int direction;
            boolean active = true;

            Enemy(int x, int y) {
                this.x = x;
                this.y = y;
                this.direction = random.nextBoolean() ? 1 : -1;
            }

            public void move() {
                int dy = 5;
                y += dy;

                if (random.nextInt(5) == 0) {
                    direction = -direction;
                }

                x += direction * 15;
                if (x < 0 || x > getWidth() - BALL_RADIUS * 2) {
                    direction = -direction;
                }
            }
        }

        // New GreenEnemy class with 3 health
        class GreenEnemy extends Enemy {
            int health = 3;

            GreenEnemy(int x, int y) {
                super(x, y);
            }
        }

        class RedEnemy extends Enemy{
            int health = 1;

            RedEnemy(int x,int y){

                super(x,y);

                
            }
        }

        // New AmmoItem class with falling speed control using Thread
        class AmmoItem {
            int x, y;
            boolean active = true;

            AmmoItem(int x, int y) {
                this.x = x;
                this.y = y;

                // Thread for falling down the item
                Thread fallingThread = new Thread(() -> {
                    while (active && this.y < getHeight()) {
                        this.y += 5;  // ควบคุมความเร็วการตกของไอเท็ม
                        try {
                            Thread.sleep(50);  // กำหนดระยะห่างเวลาให้ไอเท็มตกเร็วขึ้นหรือลง
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        repaint();  // วาดใหม่เพื่ออัปเดตตำแหน่ง
                    }
                });
                fallingThread.start();
            }
        }
        private void spawnEnemieset2(int num) {
            enemies.clear(); // ลบศัตรูเก่าออกทั้งหมดก่อนสร้างใหม่
            for (int i = 0; i < num; i++) {
                int x = random.nextInt(getWidth()); // สุ่มตำแหน่ง x
                int y = random.nextInt(100); // สุ่มตำแหน่ง y ให้เกิดที่ด้านบนของหน้าจอ
                enemies.add(new Enemy(x, y));
            }
        }

        private void spawnEnemy() {
            int x = getWidth() / 2 - BALL_RADIUS; // กำหนดให้ศัตรูเริ่มจากกึ่งกลางหน้าจอ
            int y = 0;
        
            if (!allNormalEnemiesCleared) {
                // Spawn normal enemies until 30 have been created
                if (normalEnemiesSpawned < 30) {
                    enemies.add(new Enemy(x, y));
                    normalEnemiesSpawned++;
                } else {
                    // เมื่อเกิดครบ 30 ตัวแล้วกำหนดว่าให้เริ่ม spawn GreenEnemy แทน
                    allNormalEnemiesCleared = true;
                }
            } else if (greenEnemiesSpawned < 10) { // ตรวจสอบให้แน่ใจว่า GreenEnemy จะไม่เกิน 10 ตัว
                // Spawn GreenEnemy after all normal enemies are cleared
                enemies.add(new GreenEnemy(x, y));
                greenEnemiesSpawned++;
            }
        }
        private void spawnAmmoItem() {
            int x = random.nextInt(getWidth() - BALL_RADIUS * 2);
            int y = 0;
            ammoItems.add(new AmmoItem(x, y));
        }

        private void moveEnemies() {
            while (true) {
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy enemy = enemies.get(i);
                    enemy.move();
        
                    // Check if enemy goes beyond screen bounds
                    if (enemy.y > getHeight()+100) {
                        health--;  // Reduce health by 1 if enemy goes off-screen
                        enemies.remove(i);  // Remove enemy
                        i--;  // Adjust index
                    }
                }
                repaint();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        
        private void moveBullets() {
            while (true) {
                for (int i = 0; i < bullets.size(); i++) {
                    SmallBall bullet = bullets.get(i);
                    bullet.move();

                    int bx = bullet.getX();
                    int by = bullet.getY(getHeight());

                    if (bx > getWidth() || bx < 0 || by < 0) {
                        bullets.remove(i);
                        i--;
                        continue;
                    }

                    for (int j = 0; j < enemies.size(); j++) {
                        Enemy enemy = enemies.get(j);

                        //int hitboxRadius = BALL_RADIUS * 8;


                        if (overlaps(bx, by, BALL_RADIUS, enemy.x, enemy.y,50)) {
                            bullets.remove(i);
                            i--;

                            if (enemy instanceof GreenEnemy) {
                                GreenEnemy greenEnemy = (GreenEnemy) enemy;
                                greenEnemy.health--;
                                if (greenEnemy.health <= 0) {
                                    enemies.remove(j);
                                    score += 20;
                                }
                            } else {
                                enemies.remove(j);
                                score += 10;
                            }
                            break;
                        }
                    }
                }

                // ตรวจสอบว่าผู้เล่นเก็บไอเท็มกระสุนหรือไม่
for (int i = 0; i < ammoItems.size(); i++) {
    AmmoItem item = ammoItems.get(i);
    
    // คำนวณตำแหน่งของฐานปืนให้แม่นยำ
    int gunBaseX = gunPositionX;
    int gunBaseY = getHeight() - gunImage.getHeight(null) - 100;

    // ตรวจสอบการชนกันระหว่างฐานปืนและไอเท็มกระสุน
    if (overlaps(item.x, item.y, BALL_RADIUS, gunBaseX, gunBaseY, BALL_RADIUS)) {
        ammo += 15;  // เพิ่มจำนวนกระสุนเมื่อเก็บไอเท็มได้
        ammoItems.remove(i);
        i--;
    } else if (item.y > getHeight()) {
        ammoItems.remove(i);  // ลบไอเท็มกระสุนเมื่อหลุดจากหน้าจอ
        i--;
    }
}


                repaint();
                try {
                    Thread.sleep(7);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
@Override
public void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
   
    // ตำแหน่งของแมว
    int catX = gunPositionX - (catImage.getWidth(null) / 2); 
    int catY = getHeight() - catImage.getHeight(null);
    g.drawImage(catImage, catX, catY, this);
    

    // ตำแหน่งของปืน
    int gunY = getHeight() - gunImage.getHeight(null) - 100;
    int gunX = catX + 175; // ปรับตำแหน่ง X ของปืนตามแมว

    Graphics2D g2d = (Graphics2D) g;

    // แปลตำแหน่งไปยังจุดกลางของปืน
    g2d.translate(gunX, gunY + gunImage.getHeight(null) / 2);

    // หมุนตามมุมที่คำนวณได้
    g2d.rotate(Math.toRadians(angle));

    // วาดปืนที่จุดกึ่งกลาง
    g2d.drawImage(gunImage, -gunImage.getWidth(null) / 2, -gunImage.getHeight(null) / 2, this);

    // รีเซ็ตการหมุน
    g2d.rotate(-Math.toRadians(angle));
    g2d.translate(-gunX, -(gunY + gunImage.getHeight(null) / 2));

    for (int i = 0; i < health; i++) {
        int heartX = 10 + i * (heartImage.getWidth(null) + 5);
        g.drawImage(heartImage, heartX, 10, this);
    }

    // วาดกระสุน
    g.setColor(Color.BLACK);
    for (SmallBall bullet : bullets) {
        int bx = bullet.getX() + 40;
        int by = bullet.getY(getHeight()) - 170;
        g.fillOval(bx - BALL_RADIUS, by - BALL_RADIUS, 2 * BALL_RADIUS, 2 * BALL_RADIUS);
    }

    // วาดศัตรู
    for (Enemy enemy : enemies) {
        if (enemy instanceof GreenEnemy) {
            g.drawImage(greenEnemyImage, enemy.x, enemy.y - 200, this);
        } else {
            g.drawImage(enemyImage, enemy.x, enemy.y - 200, this);
        }
    }

    // วาดไอเท็มกระสุน
    for (AmmoItem item : ammoItems) {
        g.drawImage(ammoImage, item.x, item.y, this);
    }
    for (int i = 0; i < health; i++) {
        int heartX = 10 + i * (heartImage.getWidth(null) + 5);
        g.drawImage(heartImage, heartX, 10, this);
    }
    // แสดงคะแนน, พลังชีวิต, และกระสุน
    g.setColor(Color.BLACK);
    g.setFont(new Font("Arial", Font.BOLD, 30));
    g.drawString("Score: " + score, 10, 20);
    //g.drawString("Health: " + health, 10, 40);
    g.drawString("Level: " + currentLevel, 10, 100);
    g.drawString("Ammo: " + ammo, 10, 60);

    if (health <= 0) {
        g.drawString("Game Over", getWidth() / 2 - 40, getHeight() / 2);
    } else if (allNormalEnemiesCleared && greenEnemiesSpawned >= 10 && enemies.isEmpty()) {
        g.drawString("You Win!", getWidth() / 2 - 40, getHeight() / 2);
    }
}


        

@Override
public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
        if (ammo > 0) {
            // คำนวณจุดเริ่มต้นของลูกกระสุนที่ปลายกระบอกปืน
            int gunTipX = (int) (gunPositionX + GUN_LENGTH * Math.cos(Math.toRadians(angle)));
           
            
            // เพิ่มลูกกระสุนโดยให้จุดเริ่มต้นเป็นตำแหน่งของปลายกระบอกปืน
            bullets.add(new SmallBall(0, (int) angle, gunTipX));
            ammo--;
        }
    } else if (e.getKeyCode() == KeyEvent.VK_A) {
        gunPositionX = Math.max(gunPositionX - 10, 0);
    } else if (e.getKeyCode() == KeyEvent.VK_D) {
        gunPositionX = Math.min(gunPositionX + 10, getWidth());
    }
    repaint();
}



        @Override
        public void keyReleased(KeyEvent e) {}
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
public void mouseMoved(MouseEvent e) {
    double mouseX = e.getX();
    double mouseY = e.getY();
    
    // ตำแหน่ง Y ของฐานปืน
    int gunBaseY = getHeight() - gunImage.getHeight(null) - 100; // ตำแหน่ง Y ของฐานปืน

    // คำนวณมุมจากตำแหน่งเมาส์
    angle = Math.toDegrees(Math.atan2(mouseY - gunBaseY, mouseX - gunPositionX)); // หมุนไปตามตำแหน่งเมาส์
    repaint();
}



        @Override
        public void mouseDragged(MouseEvent e) {}

        public boolean overlaps(double x1, double y1, double radius1, double x2, double y2, double radius2) {
            return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) <= radius1 + radius2;
        }
    }

