package com.piggy.client.player;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class View extends JPanel {
	Image img, title_icon = null;
	JPanel listp = new JPanel();
	JPanel bp = new JPanel();
	JButton play_video;
	JButton pause_video;
	JButton stop_video;
	JButton save_video;
	JButton setup_video;
	JButton mv;
	JButton del;
	JButton dwn;
	List left;// = new List(10, true);
	List right;// = new List(10, true);

	JFrame f;

	public View(JFrame f) {
		this.f = f;
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); // Dimension�� ����� ��ǻ�� ȭ���� ���� �������� Ŭ����
		// int f_width = (int) screen.getWidth();
		// int f_height = (int) screen.getHeight();
		f.setPreferredSize(new Dimension(1200, 750)); // ������ ũ�� ����
		int f_xpos = (int) (screen.getWidth() / 2 - 1200 / 2); // ��ũ���� ����, ���� �޾ƿ��� �����
		int f_ypos = (int) (screen.getHeight() / 2 - 750 / 2); // ��ũ�� ������� â�� ��쵵�� ��
		f.setLocation(f_xpos, f_ypos);// �������� ȭ�鿡 ��ġ
		Container con = f.getContentPane(); // �������� ��� �����̳� ����

		img = kit.getImage("bg_img3.png"); // ���ȭ�� ����
		title_icon = Toolkit.getDefaultToolkit().getImage("icon.png");
		setLayout(null);
		play_video = new JButton(new ImageIcon("play_icon.png"));
		pause_video = new JButton(new ImageIcon("pause_icon.png"));
		stop_video = new JButton(new ImageIcon("stop_icon.png"));
		save_video = new JButton(new ImageIcon("save_icon.png"));
		setup_video = new JButton(new ImageIcon("setup_icon.png"));
		mv = new JButton("Move");
		del = new JButton("Delete");
		dwn = new JButton("Download");

		bp.setLayout(new GridLayout(0,1));
		bp.add(mv);
		bp.add(del);
		bp.add(dwn);

		List left = new List(10, true);
		List right = new List(10, true);

		listp.setBounds(700, 20, 300, 300);
		listp.setLayout(new GridLayout(0, 1));
		listp.add(left);
		listp.add(bp);
		listp.add(right);
		
		setup_video.setBounds(78, 618, 60, 60);
		play_video.setBounds(225, 620, 60, 60);
		pause_video.setBounds(375, 620, 60, 60);
		stop_video.setBounds(525, 620, 60, 60);
		save_video.setBounds(1080, 620, 60, 60);

		setup_video.setBorder(null);
		play_video.setBorder(null);
		pause_video.setBorder(null);
		stop_video.setBorder(null);
		save_video.setBorder(null);

		play_video.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			}
		});

		add(play_video);
		add(pause_video);
		add(stop_video);
		add(save_video);
		add(setup_video);
		// add(iconLabel);
		f.setIconImage(title_icon); // Ÿ��Ʋ ������
		con.add(this); // MapViewŬ���� ������Ʈ���� Container�� add
		f.pack(); // �̵��� �ϳ��� ����
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // �ݱ�â
		f.setVisible(true);

	}

	protected void paintComponent(Graphics g) { // �׷��Ⱥ��� g�� �̹����ְ� �׸���
		g.drawImage(img, 0, 0, 1185, 710, f);
	}

	public static void main(String[] args) {

		// System.out.println(f_width);
		// System.out.println(f_height);
		JFrame f = new JFrame();
		View m = new View(f);
	}

}