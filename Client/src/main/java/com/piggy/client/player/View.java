package com.piggy.client.player;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.*;

public class View extends JPanel{
	 //GUI
    //----
    JFrame f;
    JButton setupButton;
    JButton playButton;
    JButton pauseButton;
    JButton exitButton;
    JButton saveButton;
    JPanel buttonPanel = new JPanel();
    JPanel listPanel = new JPanel();
    List left;
	List right;
	JButton addButton;
	JButton deleteButton;
	JButton downLoadButton;
	JLabel listLabel = new JLabel();
    JLabel statLabel1 = new JLabel();
    JLabel statLabel2 = new JLabel();
    Image img,title_icon = null;
    JLabel statLabel3 = new JLabel();
    String downloadList = "";
    final JProgressBar progress_bar = new JProgressBar();
	final int MAX = 100;
	int currentValue = 0;
	float totalSize = 0;
	public PlayerFrame displayPanel;
    JScrollPane logPane; //= new JScrollPane(txtLog);
    JTextArea txtLog;
    
	public View( ) {
		this.txtLog = new JTextArea();
        this.logPane = new JScrollPane(this.txtLog);
        
        this.f = new JFrame("DrugStore");
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();   //Dimension占쏙옙 占쏙옙占쏙옙占� 占쏙옙퓨占쏙옙 화占쏙옙占쏙옙 占쏙옙占쏙옙 占쏙옙占쏙옙占쏙옙占쏙옙 클占쏙옙占쏙옙
        f.setPreferredSize(new Dimension(1200,750));   //占쏙옙占쏙옙占쏙옙 크占쏙옙 占쏙옙占쏙옙
        int f_xpos = (int)(screen.getWidth() / 2 - 1200 / 2);   //占쏙옙크占쏙옙占쏙옙 占쏙옙占쏙옙, 占쏙옙占쏙옙 占쌨아울옙占쏙옙 占쏙옙占쏙옙占�
        int f_ypos = (int)(screen.getHeight() / 2 - 750 / 2);   //占쏙옙크占쏙옙 占쏙옙占쏙옙占쏘데占쏙옙 창占쏙옙 占쏙옙理듸옙占� 占쏙옙
        f.setLocation(f_xpos, f_ypos);//占쏙옙占쏙옙占쏙옙占쏙옙 화占썽에 占쏙옙치
        Container con = f.getContentPane();   //占쏙옙占쏙옙占쏙옙占쏙옙 占쏙옙占� 占쏙옙占쏙옙占싱놂옙 占쏙옙占쏙옙
        
        img = kit.getImage("./img/bg_img3.png");   //占쏙옙占싫�占쏙옙 占쏙옙占쏙옙
        title_icon = Toolkit.getDefaultToolkit().getImage("./img/icon.png");
        setLayout(null);
        
        displayPanel = new PlayerFrame();
        displayPanel.setSize(640, 480);
        displayPanel.setLocation(15,45);
        f.add(displayPanel);
        
        setupButton = new JButton(new ImageIcon("./img/setup_icon.png"));
        playButton = new JButton(new ImageIcon("./img/play_icon.png"));
        pauseButton = new JButton(new ImageIcon("./img/pause_icon.png"));
        exitButton = new JButton(new ImageIcon("./img/stop_icon.png"));
        saveButton = new JButton(new ImageIcon("./img/save_icon.png"));
        addButton = new JButton(new ImageIcon("./img/add_button.png"));
		deleteButton = new JButton(new ImageIcon("./img/delete_button.png"));
        
        setupButton.setBounds(78,618,60,60);
        playButton.setBounds(225,620,60,60);
        pauseButton.setBounds(375,620,60,60 );
        exitButton.setBounds(525,620,60,60);
        saveButton.setBounds(1080, 620, 60,60);
        logPane.setBounds(665,38, 200, 508);

        setupButton.setBorder(null);
        playButton.setBorder(null);
        pauseButton.setBorder(null );
        exitButton.setBorder(null);
        saveButton.setBorder(null);
        
        add(setupButton);
        add(playButton);
        add(pauseButton);
        add(exitButton);
        add(saveButton);
        add(logPane);
        
        //Statistics
        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Packets Lost: 0");
        statLabel3.setText("Data Rate (bytes/sec): 0");
        listLabel.setText("[ WIFI OFF FILES ]");
		
		buttonPanel.setLayout(new GridLayout(1,0));
		buttonPanel.add(addButton);
		buttonPanel.add(deleteButton);
//		buttonPanel.add(dwn);
		
		left = new List(20, true);
		right = new List(20, true);
		//left.add("ssss");

		int x=879, y=78, w=267, h= 170, bt=50, bl=5;
		listLabel.setBounds(x+60, y-20, w, 10);
		left.setBounds(x, y, w, h);
		buttonPanel.setBounds(x,y+h+bl,w,bt);
		right.setBounds(x, y+h+bt+bl+bl, w, h);
		
		progress_bar.setMinimum(0);
		progress_bar.setMaximum(MAX);
		progress_bar.setStringPainted(true);
		progress_bar.setBounds(x, y+h+bt+bl+bl+h+20, 265, 30);
		
		add(listLabel);
		add(left);
		add(buttonPanel);
		add(right);
		add(progress_bar);
		
        f.add(this);
        f.add(statLabel1);
        f.add(statLabel2);
        f.add(statLabel3);
        
        
        statLabel1.setBounds(800,615,380,20);
        statLabel2.setBounds(800,635, 380, 20);
        statLabel3.setBounds(800,655, 380, 20);
       
        statLabel1.setForeground(Color.WHITE);
        statLabel2.setForeground(Color.WHITE);
        statLabel3.setForeground(Color.WHITE);
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                String[] selectItems = left.getSelectedItems();
                for(int i = 0; i < selectItems.length;i++) {
                    //addLog("");
                    right.add(selectItems[i]);
                }
                int[] selectIndexes = left.getSelectedIndexes();
                for(int i=0;i<selectIndexes.length;i++) {
                    left.remove(selectIndexes[selectIndexes.length-i-1]);
                }
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String[] selectItems = right.getSelectedItems();
                for(int i=0;i<selectItems.length;i++) {
                    left.add(selectItems[i]);
                }
                int[] selectIndexes = right.getSelectedIndexes();
                for(int i=0;i<selectIndexes.length;i++) {
                    right.remove(selectIndexes[selectIndexes.length-i-1]);
                }
            }
        });
       
        f.setIconImage(title_icon);
        con.add(this);
        f.pack();
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //占쌥깍옙창
        f.setVisible(true);

	}
	public void addLog(String log) {
        txtLog.append(log + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }
	 protected void paintComponent(Graphics g){   //占쌓뤄옙占싫븝옙占쏙옙 g占쏙옙 占싱뱄옙占쏙옙占쌍곤옙 占쌓몌옙占쏙옙
	        g.drawImage(img, 0, 0, 1185, 710, f);    
	     } 
}