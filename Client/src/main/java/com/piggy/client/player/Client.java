/* ------------------
   Client
   usage: java Client [Server hostname] [Server RTSP listening port] [Video file requested]
   ---------------------- */

package com.piggy.client.player;
import java.awt.event.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.StringTokenizer;


public class Client extends JPanel implements Runnable {
    boolean wifi_restart_flag = false;
    //GUI
    JFrame f;
    JButton setupButton;
    JButton playButton;
    JButton pauseButton;
    JButton exitButton;
    JButton saveButton;
    JPanel buttonPanel = new JPanel();
    List left;
	List right;
	JButton addButton;
	JButton deleteButton;
	JLabel listLabel = new JLabel();
    JLabel statLabel1 = new JLabel();
    JLabel statLabel2 = new JLabel();
    JLabel statLabel3 = new JLabel();
    Image img,title_icon = null;

    //RTP variables:
    //----------------
    DatagramPacket rcvdp;            //UDP packet received from the server
    DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
    static int RTP_RCV_PORT = 5004; //port where the client will receive the RTP packets
    
    //File outputFile = new File("sample.h264");
    //FileOutputStream fos;
    Timer timer; //timer used to receive data from the UDP socket
    byte[] buf;  //buffer used to store data received from the server 
   
    //RTSP variables
    //----------------
    //rtsp states 
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    static int state;            //RTSP state == INIT or READY or PLAYING
    Socket RTSPsocket;           //socket used to send/receive RTSP messages
    InetAddress ServerIPAddr;

    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file to request to the server
    int RTSPSeqNb = 0;           //Sequence number of RTSP messages within the session
    String RTSPid;              // ID of the RTSP session (given by the RTSP Server)

    final static String CRLF = "\r\n";
    final static String DES_FNAME = "session_info.txt";

    //RTCP variables
    //----------------
    DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    static int RTCP_RCV_PORT = 19001;   //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;       //How often to send RTCP packets
    RtcpSender rtcpSender;

    //Video constants:
    //------------------
    static int MJPEG_TYPE = 96; //RTP payload type for MJPEG video

    //Statistics variables:
    //------------------
    double statDataRate;        //Rate of video data received in bytes/s
    int statTotalBytes;         //Total number of bytes received in a session
    double statStartTime;       //Time in milliseconds when start is pressed
    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    int statCumLost;            //Number of packets lost
    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    int statHighSeqNb;          //Highest sequence number received in session

    PipedOutputStream pipedOutputStream;
    int wifi_check_cnt = 0;
    H264Player h264Player;
    boolean reconnect_flag = false;

    String downloadList = "";

    SharedArea sharedArea;
    JScrollPane logPane; //= new JScrollPane(txtLog);
    JTextArea txtLog;
    //--------------------------
    //Constructor
    //--------------------------
    public Client(PipedOutputStream pipedOutputStream, JFrame frame, H264Player h264Player, SharedArea sharedArea,JTextArea txtlog ) {
        //share Thread data
        this.pipedOutputStream = pipedOutputStream;
        this.h264Player  = h264Player;
        this.sharedArea = sharedArea;
        this.txtLog = txtlog;
        this.logPane = new JScrollPane(txtlog);

        //build GUI
        this.f = frame;
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();   //Dimension�� ����� ��ǻ�� ȭ���� ���� �������� Ŭ����
        f.setPreferredSize(new Dimension(1200, 750));   //������ ũ�� ����
        int f_xpos = (int)(screen.getWidth() / 2 - 1200 / 2);   //��ũ���� ����, ���� �޾ƿ��� �����
        int f_ypos = (int)(screen.getHeight() / 2 - 750 / 2);   //��ũ�� ������� â�� ��쵵�� ��
        f.setLocation(f_xpos, f_ypos);//�������� ȭ�鿡 ��ġ
        Container con = f.getContentPane();   //�������� ��� �����̳� ����
        
        img = kit.getImage("./img/bg_img3.png");   //���ȭ�� ����
        title_icon = Toolkit.getDefaultToolkit().getImage("./img/icon.png");
        setLayout(null);
        
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
        exitButton.setBounds(525, 620, 60, 60);
        saveButton.setBounds(1080, 620, 60,60);
        logPane.setBounds(665, 38, 200, 508);

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
        
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        exitButton.addActionListener(new exitButtonListener());
        saveButton.addActionListener(new saveButtonListener());
        
        //Statistics
        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Packets Lost: 0");
        statLabel3.setText("Data Rate (bytes/sec): 0");
        listLabel.setText("[ WIFI OFF FILES ]");
		
		buttonPanel.setLayout(new GridLayout(1,0));
		buttonPanel.add(addButton);
		buttonPanel.add(deleteButton);

		left = new List(20, true);
		right = new List(20, true);
		left.add("ssss");

		int x=879, y=78, w=267, h= 200, bt=50, bl=5;
		listLabel.setBounds(x+60, y-20, w, 10);
		left.setBounds(x, y, w, h);
		buttonPanel.setBounds(x,y+h+bl,w,bt);
		right.setBounds(x, y+h+bt+bl+bl, w, h);
		
		add(listLabel);
		add(left);
		add(buttonPanel);
		add(right);
		
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
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //�ݱ�â
        f.setVisible(true);

        //init timer
        timer = new Timer(50, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //init RTCP packet sender
        rtcpSender = new RtcpSender(400);

        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[25000];    

    }
    protected void paintComponent(Graphics g){   //�׷��Ⱥ��� g�� �̹����ְ� �׸���
        g.drawImage(img, 0, 0, 1185, 710, f);    
     } 

    public void run() {
        //Run Client Thread
        try {
            //Fixme:// change so it can get port and Ip when exec.
            //get server RTSP port and IP address from the command line
            int RTSP_server_port = 1052;//Integer.parseInt(argv[1]);
            String ServerHost = "192.168.0.248";//"203.252.160.76";//"192.168.0.11";//argv[0];
            ServerIPAddr = InetAddress.getByName(ServerHost);

            //Establish a TCP connection with the server to exchange RTSP messages
            RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

            //Establish a UDP connection with the server to exchange RTCP control packets
            //Set input and output stream filters:
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));

            //init RTSP state:
            state = INIT;
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void addLog(String log) {
        txtLog.append(log + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }
    //------------------------------------
    //Handler for buttons
    //------------------------------------
    //Handler for Setup button
    class setupButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e){
            System.out.println("Setup Button pressed !");
            if (state == INIT) {
                //Init non-blocking RTPsocket that will be used to receive data
                try {
                    //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                    //UDP socket for sending QoS RTCP packets
                    RTCPsocket = new DatagramSocket();
                    //set TimeOut value of the socket to 5msec.
                    RTPsocket.setSoTimeout(1000);
                }
                catch (SocketException se)
                {
                    System.out.println("Socket exception: "+se);
                    System.exit(0);
                }
                //init RTSP sequence number
                RTSPSeqNb = 1;
                //Send SETUP message to the server
                sendRequest("SETUP");
                //Wait for the response 
                if (parseServerResponse() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    //change RTSP state and print new state 
                    state = READY;
                    System.out.println("New RTSP state: READY");
                }
                addLog("Client succeeded connecting to server ");
            }
            //else if state != INIT then do nothing
        }
    }
    class saveButtonListener implements ActionListener{
    	public void actionPerformed(ActionEvent e) {
    		String[] selectItems = right.getItems();
            for(int i=0;i<selectItems.length;i++) {
                downloadList+=selectItems[i]+"#";
            }
            System.out.println("download list : "+downloadList);
            System.out.println("Sending DOWNLOAD request");

            //increase RTSP sequence number
            RTSPSeqNb++;

            //Send DESCRIBE message to the server
            sharedArea.downloadList = downloadList;
            sharedArea.file_flag = true;
            sendRequest("DOWNLOAD");
            addLog("Client sent download request to Server");
            //FileDownloader fileDown = new FileDownloader(5522, downloadList.split("#"));
            //fileDown.start();
            System.out.println("Thread started!!!!!!!!!!!!!");

            //Wait for the response
            if (parseServerResponse() != 200) {
                System.out.println("Invalid Server Response");
            }
            else {
                System.out.println("Receiving file data from server");
            }
    	}
    }
    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //Start to save the time in stats
            statStartTime = System.currentTimeMillis();
            if (state == READY) {
                //increase RTSP sequence number
                RTSPSeqNb++;
                //Send PLAY message to the server
                sendRequest("PLAY");
                //Wait for the response 
                if (parseServerResponse() != 200) {
                    System.out.println("Invalid Server Response");
                    addLog("Error : failed playing video");
                } else {
                    //change RTSP state and print out new state
                    state = PLAYING;
                    System.out.println("New RTSP state: PLAYING");
                    //Fixme:// this part is not necessary it is only for test
                    //don't need to save file
//                    try {
//                        fos = new FileOutputStream(outputFile, true);
//                    } catch(Exception fosE) {
//                        fosE.printStackTrace();
//                    }
                    //start the timer
                    timer.start();
                    rtcpSender.startSend();
                    addLog("Client started playing live video");
                }
            }
            //else if state != READY then do nothing
        }
    }
    //Handler for Pause button
    //-----------------------
    class pauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            if (state == PLAYING) 
            {
                //increase RTSP sequence number
                RTSPSeqNb++;
                //Send PAUSE message to the server
                sendRequest("PAUSE");
                //Wait for the response 
                if (parseServerResponse() != 200) {
                    System.out.println("Invalid Server Response");
                    addLog("Error : couldn't stop video");
                } else {
                    //change RTSP state and print out new state
                    state = READY;
                    System.out.println("New RTSP state: READY");
                      
                    //stop the timer
                    timer.stop();
                    rtcpSender.stopSend();
                    try {
                        pipedOutputStream.flush();
                    } catch (Exception e1) {
                        System.out.println(e1.toString());
                    }
                    h264Player.replay();
                    addLog("Video paused");
                }
                //Fixme:// need to change, we don't need file anymore
                //Fixme:// need to stop h264Player too.
//                try {
//                    fos.close();
//                    //h264Player.stop();
//                } catch (Exception e1) {
//                    System.out.println(e1.toString());
//                }
            }
            //else if state != PLAYING then do nothing
        }
    }
    //Handler for Teardown button
    //-----------------------
    class exitButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e){
            //increase RTSP sequence number
            RTSPSeqNb++;

            //Send TEARDOWN message to the server
            sendRequest("TEARDOWN");

            //Wait for the response
            if (parseServerResponse() != 200) {
                System.out.println("Invalid Server Response");
            } else {
                //change RTSP state and print out new state
                state = INIT;
                System.out.println("New RTSP state: INIT");
                //stop the timer
                timer.stop();
                rtcpSender.stopSend();

                //exit
                System.exit(0);
            }
        }
    }

    //------------------------------------
    //Handler for timer
    //------------------------------------
    class timerListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            //Fixme: need to erase this part of the code and test wifi disconnection
            try {
                //receive the DP from the socket, save time for stats
                try {
                    RTPsocket.receive(rcvdp);
                } catch (Exception e5) {
                    System.out.println(e5.toString());
                }

                if(reconnect_flag && rcvdp != null) {
                    reconnect_flag = false;
                    sendRequest("FILELIST");
                    int option = parseServerResponse();
                    System.out.println(option);
                    if (option == 1234) {
                        // Jimin_Here
                        System.out.println("FileLIST COMEON!!!!!!!!!!!!");
                    }
                    else {
                        System.out.println("Invalid Server Response");
                    }
                }

                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime;
                statStartTime = curTime;

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                //this is the highest seq num received

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                if(payload_length > 0) {

                    byte[] payload = new byte[payload_length];
                    rtp_packet.getpayload(payload);


                    if (wifi_check_cnt == 20) {
                        sendRequest("WIFI");
                        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                        int option = parseServerResponse();
                        if (option == 400) {
                            System.out.println("New RTSP state: WIFI CHANGED");
                            try {
                                pipedOutputStream.flush();
                            } catch (IOException e1) {
                                System.out.println("Exception Jimin 3 " + e1.toString());
                            }
                            wifi_restart_flag = true;
                            //h264Player.replay();
                        } else if (option == 300) {
                            System.out.println("New RTSP state: WIFI NOT CHANGED");
                        } else if(option == 500) {
                            System.out.println("New RTSP state: WIFI Reconnected");
                            reconnect_flag = true;
                        }
                        else {
                            System.out.println("Invalid Server Response");
                        }
                        wifi_check_cnt = 0;
                    }
                    wifi_check_cnt++;




                    try {
                        pipedOutputStream.write(payload);
                    } catch (IOException e1) {
                        System.out.println("Exception caught Jimin 2" + e1.toString());
                    }


                    if(wifi_restart_flag) {
                        h264Player.replay();
                        wifi_restart_flag = false;
                    }

                    //fos.write(payload,0,payload_length);
                    //compute stats and update the label in GUI
                    statExpRtpNb++;
                    if (seqNb > statHighSeqNb) {
                        statHighSeqNb = seqNb;
                    }
                    if (statExpRtpNb != seqNb) {
                        statCumLost++;
                    }
                    statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                    statFractionLost = (float) statCumLost / statHighSeqNb;
                    statTotalBytes += payload_length;
                    updateStatsLabel();
                } else {
                    System.out.println("*************************************");
                }
            } catch (Exception ioe) {
                System.out.println("Exception caught: Jimin 1 " + ioe.toString());
            }
        }
    }
    //------------------------------------
    // Send RTCP control packets for QoS feedback
    //------------------------------------
    class RtcpSender implements ActionListener {

        private Timer rtcpTimer;
        int interval;
        // Stats variables
        private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
        private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
        private int lastHighSeqNb;      // The last highest Seq number received
        private int lastCumLost;        // The last cumulative packets lost
        private float lastFractionLost; // The last fraction lost
        Random randomGenerator;         // For testing only

        public RtcpSender(int interval) {
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);
            randomGenerator = new Random();
        }
        public void run() {
            System.out.println("RtcpSender Thread Running");
        }
        public void actionPerformed(ActionEvent e) {

            // Calculate the stats for this period
            numPktsExpected = statHighSeqNb - lastHighSeqNb;
            numPktsLost = statCumLost - lastCumLost;
            lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
            lastHighSeqNb = statHighSeqNb;
            lastCumLost = statCumLost;

            //To test lost feedback on lost packets
            // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

            RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb);
            int packet_length = rtcp_packet.getlength();
            byte[] packet_bits = new byte[packet_length];
            rtcp_packet.getpacket(packet_bits);

            try {
                DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, ServerIPAddr, RTCP_RCV_PORT);
                RTCPsocket.send(dp);
            } catch (InterruptedIOException iioe) {
                System.out.println("1 Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }

        // Start sending RTCP packets
        public void startSend() {
            rtcpTimer.start();
        }

        // Stop sending RTCP packets
        public void stopSend() {
            rtcpTimer.stop();
        }
    }

    //------------------------------------
    //Synchronize frames
    //------------------------------------
    //------------------------------------
    //Parse Server Response
    //------------------------------------
    private int parseServerResponse() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Client - Received from Server:");
            System.out.println(StatusLine);
          
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP von
            reply_code = Integer.parseInt(tokens.nextToken());

            System.out.println("reply_code: " + reply_code);
            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);
                
                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                //if state == INIT gets the Session Id from the SessionLine
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = tokens.nextToken();
                }
                else if (temp.compareTo("Content-Base:") == 0) {
                    // Get the DESCRIBE lines
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = RTSPBufferedReader.readLine();
                        System.out.println(newLine);
                    }
                }
            } else if (reply_code == 1234) {
                String fileList = RTSPBufferedReader.readLine();
                System.out.println("!!!!!!!!Filelist: " + fileList);
                for(String str : fileList.split("#")) {
                    if(str.length() > 0) {
                        left.add(str);
                    }
                }
            }
        } catch(Exception ex) {
            System.out.println("Exception caught 9 : "+ex);
            System.exit(0);
        }

        return(reply_code);
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        statLabel1.setText("Total Bytes Received: " + statTotalBytes);
        statLabel2.setText("Packet Lost Rate: " + formatter.format(statFractionLost));
        statLabel3.setText("Data Rate: " + formatter.format(statDataRate) + " bytes/s");
    }

    //------------------------------------
    //Send RTSP Request
    //------------------------------------
    private void sendRequest(String request_type) {
        try {
            //Use the RTSPBufferedWriter to write to the RTSP socket

            //write the request line:
            RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + CRLF);

            //write the CSeq line: 
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            //check if request_type is equal to "SETUP" and in this case write the 
            //Transport: line advertising to the server the port used to receive 
            //the RTP packets RTP_RCV_PORT
            if (request_type == "SETUP") {
                RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            }
            else if (request_type == "WIFI") {
                RTSPBufferedWriter.write("Check: WIFI" + CRLF);
            }
            else if (request_type == "DOWNLOAD") {
                RTSPBufferedWriter.write("DOWNLOAD: " + downloadList + CRLF); //#########
            }
            else {
                //otherwise, write the Session line from the RTSPid field
                RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
            }
            RTSPBufferedWriter.flush();
        } catch(Exception ex) {
            System.out.println("Exception caught: "+ex);
            System.exit(0);
        }
    }
}
