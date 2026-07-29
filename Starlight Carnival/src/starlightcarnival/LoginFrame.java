package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static starlightcarnival.EntityCrudPanel.*;

public class LoginFrame extends JFrame {

    private JTextField tfUsername;
    private JPasswordField tfPassword;
    private JButton btnLogin;
    private JLabel statusLabel;
    private AnimatedPanel animPanel;

    public LoginFrame() {
        setTitle("ParkPal - Starlight Carnival");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        setLayout(new BorderLayout());
        animPanel = new AnimatedPanel();
        animPanel.setLayout(new BorderLayout());
        add(animPanel, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        animPanel.add(centerWrapper, BorderLayout.CENTER);
        centerWrapper.add(buildCard());

        animPanel.start();
    }

    //  Login Card

    private JPanel buildCard() {
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 15, 30, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 160, 0, 55));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(380, 500));
        card.setBorder(new EmptyBorder(32, 40, 28, 40));

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel logoLbl = new JLabel("SC", SwingConstants.CENTER);
        logoLbl.setForeground(ACCENT);
        logoLbl.setFont(logoLbl.getFont().deriveFont(Font.BOLD, 42f));
        logoLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("Starlight Carnival", SwingConstants.CENTER);
        titleLbl.setForeground(ACCENT);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 20f));
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Use your MySQL database account", SwingConstants.CENTER);
        subLbl.setForeground(FG_MUTED);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.ITALIC, 11f));
        subLbl.setAlignmentX(CENTER_ALIGNMENT);

        header.add(logoLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(titleLbl);
        header.add(Box.createVerticalStrut(3));
        header.add(subLbl);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        tfUsername = styledField("Username");
        tfPassword = styledPasswordField("Password");

        gc.gridx = 0; gc.gridy = 0;
        form.add(labelFor("MySQL Username"), gc);
        gc.gridy = 1;
        form.add(tfUsername, gc);
        gc.gridy = 2; gc.insets = new Insets(12, 0, 6, 0);
        form.add(labelFor("Password"), gc);
        gc.gridy = 3; gc.insets = new Insets(6, 0, 6, 0);
        form.add(tfPassword, gc);

        // Buttons
        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 6));
        btns.setOpaque(false);

        btnLogin = actionButton("Sign In", ACCENT);
        JButton btnExit = actionButton("Exit", new Color(160, 50, 50));

        btnLogin.addActionListener(e -> doLogin());
        tfPassword.addActionListener(e -> doLogin());
        tfUsername.addActionListener(e -> tfPassword.requestFocus());
        btnExit.addActionListener(e -> System.exit(0));

        btns.add(btnLogin);
        btns.add(btnExit);

        // Status
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(ACCENT2);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(btns);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(statusLabel);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    //  Login Action

    private void doLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(tfPassword.getPassword());

        if (username.isEmpty()) { setStatus("Enter your username.", ACCENT2); return; }

        setStatus("Signing in...", FG_MUTED);
        btnLogin.setEnabled(false);

        SwingWorker<String[], Void> w = new SwingWorker<>() {
            @Override protected String[] doInBackground() throws Exception {
                try {
                    DatabaseManager.getInstance().connect(username, password);
                    String role = DatabaseManager.getInstance().getConnectedRole();
                    String displayName = DatabaseManager.getInstance().getConnectedUser();
                    return new String[]{displayName, role};
                } catch (Exception ex) {
                    return new String[]{"ERROR", ex.getMessage()};
                }
            }
            @Override protected void done() {
                btnLogin.setEnabled(true);
                try {
                    String[] result = get();
                    if ("ERROR".equals(result[0])) {
                        setStatus("DB Error: " + result[1], ACCENT2);
                        return;
                    }
                    String displayName = result[0];
                    String role        = result[1];
                    setStatus("Welcome, " + displayName + "!", new Color(80, 220, 120));
                    animPanel.stop();
                    Timer t = new Timer(350, ev -> {
                        dispose();
                        openFrameForRole(role, displayName);
                    });
                    t.setRepeats(false); t.start();
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage(), ACCENT2);
                }
            }
        };
        w.execute();
    }

    private void openFrameForRole(String role, String displayName) {
        switch (role.toUpperCase()) {
            case "ADMIN"       -> new MainFrame(displayName, false).setVisible(true);
            case "GUEST"       -> new MainFrame(displayName, true).setVisible(true);
            case "SALES"       -> new SalesFrame(displayName).setVisible(true);
            case "MAINTENANCE" -> new MaintenanceFrame(displayName).setVisible(true);
            default            -> new MainFrame(displayName, true).setVisible(true);
        }
    }

    //  UI Helpers

    private JLabel labelFor(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG_MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
        return l;
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField(16);
        styleInput(tf);
        return tf;
    }

    private JPasswordField styledPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField(16);
        styleInput(pf);
        return pf;
    }

    private void styleInput(JComponent c) {
        c.setBackground(new Color(22, 27, 45));
        c.setForeground(FG);
        if (c instanceof JTextField) ((JTextField) c).setCaretColor(ACCENT);
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        c.setFont(c.getFont().deriveFont(Font.PLAIN, 13f));
    }

    private JButton actionButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(new Color(28, 33, 52));
        b.setForeground(color);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 1, true),
            new EmptyBorder(10, 20, 10, 20)
        ));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker().darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(new Color(28, 33, 52)); }
        });
        return b;
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    //  Animated Background

    static class AnimatedPanel extends JPanel {
        private final List<Star> stars = new ArrayList<>();
        private final List<Spark> sparks = new ArrayList<>();
        private final Random rng = new Random();
        private Timer timer;
        private float hueShift = 0;

        AnimatedPanel() {
            setBackground(new Color(6, 8, 18));
            for (int i = 0; i < 180; i++) stars.add(new Star(rng));
            for (int i = 0; i < 18; i++) sparks.add(new Spark(rng, 900, 560));
        }

        void start() {
            timer = new Timer(16, e -> {
                hueShift += 0.002f;
                stars.forEach(Star::tick);
                sparks.forEach(s -> { s.tick(); if (s.isDead()) s.reset(rng, getWidth(), getHeight()); });
                repaint();
            });
            timer.start();
        }

        void stop() { if (timer != null) timer.stop(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            GradientPaint bg = new GradientPaint(0, 0, new Color(6, 8, 22), w, h, new Color(12, 6, 30));
            g2.setPaint(bg); g2.fillRect(0, 0, w, h);

            float hue = hueShift % 1f;
            RadialGradientPaint n1 = new RadialGradientPaint(w*.3f, h*.4f, w*.6f,
                new float[]{0f,1f}, new Color[]{Color.getHSBColor(hue,.6f,.15f), new Color(0,0,0,0)});
            g2.setPaint(n1); g2.fillRect(0, 0, w, h);

            RadialGradientPaint n2 = new RadialGradientPaint(w*.75f, h*.6f, w*.5f,
                new float[]{0f,1f}, new Color[]{Color.getHSBColor((hue+.5f)%1f,.5f,.10f), new Color(0,0,0,0)});
            g2.setPaint(n2); g2.fillRect(0, 0, w, h);

            stars.forEach(s -> s.draw(g2, w, h));
            sparks.forEach(s -> s.draw(g2));
            g2.dispose();
        }

        static class Star {
            float x, y, size, alpha, alphaDir, speed;
            Star(Random rng) { reset(rng); }
            void reset(Random rng) {
                x=rng.nextFloat(); y=rng.nextFloat();
                size=.6f+rng.nextFloat()*2.2f; alpha=rng.nextFloat();
                alphaDir=rng.nextBoolean()?.008f:-.008f; speed=.004f+rng.nextFloat()*.008f;
            }
            void tick() {
                alpha+=alphaDir*speed*10;
                if(alpha>1f){alpha=1f;alphaDir=-alphaDir;}
                if(alpha<.05f){alpha=.05f;alphaDir=-alphaDir;}
            }
            void draw(Graphics2D g2, int w, int h) {
                int px=(int)(x*w),py=(int)(y*h);
                g2.setColor(new Color(1f,1f,1f,alpha));
                g2.fillOval(px,py,(int)size,(int)size);
                if(size>2f){
                    g2.setColor(new Color(1f,1f,1f,alpha*.3f));
                    g2.setStroke(new BasicStroke(.5f));
                    g2.drawLine(px-(int)(size*2),py,px+(int)(size*2),py);
                    g2.drawLine(px,py-(int)(size*2),px,py+(int)(size*2));
                }
            }
        }

        static class Spark {
            float x,y,vx,vy,life,maxLife,size; Color color;
            static final Color[] PALETTE = {
                new Color(255,160,0),new Color(255,80,120),
                new Color(80,200,255),new Color(180,80,255),new Color(255,240,80)
            };
            Spark(Random rng,int w,int h){reset(rng,w,h);life=rng.nextFloat()*maxLife;}
            void reset(Random rng,int w,int h){
                x=rng.nextFloat()*w; y=rng.nextFloat()*h;
                vx=(rng.nextFloat()-.5f)*.5f; vy=-.2f-rng.nextFloat()*.6f;
                maxLife=120+rng.nextFloat()*180; life=0;
                size=2f+rng.nextFloat()*4f; color=PALETTE[rng.nextInt(PALETTE.length)];
            }
            boolean isDead(){return life>=maxLife;}
            void tick(){x+=vx;y+=vy;life++;}
            void draw(Graphics2D g2){
                float a=1f-(life/maxLife);
                g2.setColor(new Color(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,a*.8f));
                g2.fillOval((int)x,(int)y,(int)size,(int)size);
                g2.setColor(new Color(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,a*.15f));
                int gs=(int)(size*3);
                g2.fillOval((int)x-gs/2,(int)y-gs/2,gs,gs);
            }
        }
    }
}
