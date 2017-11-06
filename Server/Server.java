/* ------------------
   Server
   usage: java Server [RTSP listening port]
---------------------- */
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
//extends JFrame
public class Server  implements ActionListener, Runnable{

    //RTP variables:
    //----------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames

    InetAddress ClientIPAddr;   //Client IP address
    int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP Client)
    int RTSP_dest_port = 0;

    //GUI:
    //----------------
    //Jimin_GUI
    //JLabel label;

    //Video variables:
    //----------------
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    static int MJPEG_TYPE = 96; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 5000; //length of the video in frames

    Timer timer;    //timer used to send the images at the video frame rate
    byte[] buf;     //buffer used to store the images to send to the client
    byte[] buff;

    int sendDelay;  //the delay to send images over the wire. Ideally should be
    //equal to the frame rate of the video file, but may be
    //adjusted when congestion is detected.

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int DESCRIBE = 7;
    final static int WIFI = 8;
    final static int FILELIST = 9;
    final static int DOWNLOAD = 10;
    int fileLenght = 0;
    int fileIndex = 1;
    String downFileList = "";

    static int state; //RTSP Server state == INIT or READY or PLAY
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file requested from the client
    static String RTSPid = UUID.randomUUID().toString(); //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session


    //RTCP variables
    //----------------
    static int RTCP_RCV_PORT = 19001; //port where the client will receive the RTP packets
    static int RTCP_PERIOD = 400;     //How often to check for control events
    DatagramSocket RTCPsocket;
    RtcpReceiver rtcpReceiver;
    int congestionLevel;

    //Performance optimization and Congestion control
//    CongestionController cc;

    File outputFile = new File("video.h264");
    FileOutputStream fos1 = null;

    File saveFile = new File("hahaha.h264");
    FileOutputStream fos2 = null;
    final static String CRLF = "\r\n";
    SharedArea sharedArea;

    Wifi wifi;
    //--------------------------------
    //Constructor
    //--------------------------------
    public Server(VideoStream videoStream, SharedArea sharedArea, Wifi wifi) {

        //init Frame
        //super("RTSP Server");

        //init RTP sending Timer
        sendDelay = FRAME_PERIOD;
        timer = new Timer(sendDelay, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //init congestion controller
//        cc = new CongestionController(600);

        //allocate memory for the sending buffer
        buf = new byte[20000];
        buff = new byte[20000];

        //Handler to close the main window
        //Jimin_GUI
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                //stop the timer and exit
//                System.out.println("1111");
//                timer.stop();
//                rtcpReceiver.stopRcv();
//                System.exit(0);
//           }});

        //init the RTCP packet receiver
        rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);

        //Jimin_GUI:
        //label = new JLabel("Send frame #        ", JLabel.CENTER);
        //getContentPane().add(label, BorderLayout.CENTER);

        //Video encoding and quality

        try {
            fos1 = new FileOutputStream(outputFile,true);
            fos2 = new FileOutputStream(saveFile,true);
        } catch(Exception e) {
            //System.out.println("e");
        }

        this.video = videoStream;
        this.sharedArea = sharedArea;
        this.wifi = wifi;
    }



    @Override
    public void run() {
        //create a Server object

        //Jimin_GUI show GUI:
        //pack();
        //setVisible(true);
        //setSize(new Dimension(400, 200));

        //get RTSP socket port from the command line
        int RTSPport = 1052;//Integer.parseInt(argv[0]);
        RTSP_dest_port = RTSPport;

        //Initiate TCP connection with the client for the RTSP session
        ServerSocket listenSocket = null;
        try {
            listenSocket = new ServerSocket(RTSPport);
            RTSPsocket = listenSocket.accept();
            listenSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Get Client IP address
        ClientIPAddr = RTSPsocket.getInetAddress();
        sharedArea.clientIP = ClientIPAddr.getHostAddress();
        //Initiate RTSPstate
        state = INIT;

        //Set input and output stream filters:
        try {
            RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()) );
            RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()) );
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Wait for the SETUP message from the client
        int request_type;
        boolean done = false;
        while(!done) {
            request_type = parseRequest(); //blocking

            if (request_type == SETUP) {
                done = true;

                //update RTSP state
                state = READY;
                System.out.println("New RTSP state: READY");

                //Send response
                sendResponse();

                //init the VideoStream object:
                //video = new VideoStream();

                //init RTP and RTCP sockets
                try {
                    RTPsocket = new DatagramSocket();
                    RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }

        //loop to handle RTSP requests
        while(true) {
            //parse the request
            request_type = parseRequest(); //blocking

            if ((request_type == PLAY) && (state == READY)) {
                //send back response
                sendResponse();
                //start timer
                try {
                    video.getStarted("240","320");
                    sharedArea.start_flag = true;
                }catch(Exception e) {
                    System.out.println("error from getStarted()");
                }
                timer.start();
                rtcpReceiver.startRcv();
                //update state
                state = PLAYING;

                System.out.println("New RTSP state: PLAYING");
            }
            else if ((request_type == PAUSE) && (state == PLAYING)) {
                //send back response
                sendResponse();
                //stop timer

                //System.out.println("222");
                timer.stop();
                rtcpReceiver.stopRcv();
                try {
                   video.stopVideo();
                } catch (Exception e5) {
                   System.out.println("Exception e5: " + e5.toString());
                }

                //update state
                state = READY;
                System.out.println("New RTSP state: READY");
            }
            else if (request_type == TEARDOWN) {
                //send back response
                sendResponse();
                //stop timer

                timer.stop();
                rtcpReceiver.stopRcv();
                //close sockets
                try {
                    RTSPsocket.close();
                    RTPsocket.close();
                    fos1.close();
                    fos2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
            else if (request_type == DESCRIBE) {
                System.out.println("Received DESCRIBE request");
                sendDescribe();
            }
            else if (request_type == WIFI) {
                int option = wifi.wifiHandler();
                if(option == -1){
                    sendChange("300");
                } else if ( option == -5){
                    sendChange("500");
                } else {
                    sendChange("400");
                }
                //System.out.println("out!!!!!!!!!!!!!!!!");
            } else if (request_type == FILELIST) {
                //System.out.println("Give me FileList come on");
                String fileList = "";
                File savedDir = new File("./saved/");
                if(savedDir.isDirectory()) {
                    //System.out.println("this is dir"); 
                    for(File file : savedDir.listFiles()) {
                        if(file.isFile() && (file.getName()).startsWith("video_")) {
                            fileList += (file.getName() + "#");
                            System.out.println(fileList);
                        }
                    }
                }
                //Jimin_here 
                //sendFileList("------  file list array =--------- ");
                sendFileList(fileList);    
            }
        }
    }




    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {
        byte[] frame;
        int image_length = 0 ;


//        if (checkResult == DISCON) {
//            try {
//                image_length = video.getnextframe(buf); // ÀÌ¹ÌÁö µ¥ÀÌÅÍ ¹ÞŸÆµé¿©Œ­ buf¿¡ ÀúÀå
//                fos2.write(buf, 0, image_length); // fos2¿¡ writeÇÏŽÂµ¥
//            } catch (Exception e4) {
//                System.out.println("File Write Fail!");
//            }
//            // save_video();
//            return;
//        }

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH) {
            //update current imagenb
            imagenb++;

            try{
                //get next frame to send from the video, as well as its size
                image_length = video.getnextframe(buf);
                if(image_length < 0) {
                    //System.out.println("image_lenght < 0" );
                    return;
                }
                //Jimin: fos1.write(buf, 0, image_length);
                //adjust quality of the image if there is congestion detected
                if (congestionLevel > 0) {
                    //Fixme : Jimin
                    //image_length = frame.length;
                    //System.arraycopy(frame, 0, buf, 0, image_length);
                }

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];

                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);

                RTPsocket.send(senddp);

                //System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");
                //print the header bitstream
                //Jimin rtp_packet.printheader();

                //Jimin_GUI: update GUI
                //label.setText("Send frame #" + imagenb);
            } catch (Exception ex) {
                System.out.println("Exception caught5: " + ex.toString());
                //System.exit(0);
            }
        } else {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
            rtcpReceiver.stopRcv();
        }
    }

    //------------------------
    //Controls RTP sending rate based on traffic
    //------------------------
//    class CongestionController implements ActionListener {
//        private Timer ccTimer;
//        int interval;   //interval to check traffic stats
//        int prevLevel;  //previously sampled congestion level
//
//        public CongestionController(int interval) {
//            this.interval = interval;
//            ccTimer = new Timer(interval, this);
//            ccTimer.start();
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            //adjust the send rate
//            if (prevLevel != congestionLevel) {
//                sendDelay = FRAME_PERIOD + congestionLevel * (int)(FRAME_PERIOD * 0.1);
//                timer.setDelay(sendDelay);
//                prevLevel = congestionLevel;
//                System.out.println("Send delay changed to: " + sendDelay);
//            }
//        }
//    }

    //------------------------
    //Listener for RTCP packets sent from client
    //------------------------
    class RtcpReceiver implements ActionListener {
        private Timer rtcpTimer;
        private byte[] rtcpBuf;
        int interval;

        public RtcpReceiver(int interval) {
            //set timer with interval for receiving packets
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);

            //allocate buffer for receiving RTCP packets
            rtcpBuf = new byte[512];
        }

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
            float fractionLost;

            try {
                //Jimin
                //RTCPsocket.setSoTimeout(3000);
                RTCPsocket.receive(dp);   // Blocking
                RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(), dp.getLength());
                //System.out.println("Jimin Jimin [RTCP] " + rtcpPkt);

                //set congestion level between 0 to 4
                fractionLost = rtcpPkt.fractionLost;
                if (fractionLost >= 0 && fractionLost <= 0.01) {
                    congestionLevel = 0;    //less than 0.01 assume negligible
                }
                else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                    congestionLevel = 1;
                }
                else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                    congestionLevel = 2;
                }
                else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                    congestionLevel = 3;
                }
                else {
                    congestionLevel = 4;
                }
            }
            catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught6: "+ioe);
            }
        }

        public void startRcv() {
            rtcpTimer.start();
        }

        public void stopRcv() {
            //System.out.println("5555");
            rtcpTimer.stop();
        }
    }


    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private int parseRequest() {
        int request_type = -1;
        try {
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            //System.out.println("RTSP Server - Received from Client:");
            //System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((new String(request_type_string)).compareTo("SETUP") == 0)
                request_type = SETUP;
            else if ((new String(request_type_string)).compareTo("PLAY") == 0)
                request_type = PLAY;
            else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
                request_type = PAUSE;
            else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
                request_type = TEARDOWN;
            else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
                request_type = DESCRIBE;
            else if ((new String(request_type_string)).compareTo("WIFI") == 0)
                request_type = WIFI;
            else if ((new String(request_type_string)).compareTo("FILELIST") == 0)
                request_type = FILELIST;
            else if ((new String(request_type_string)).compareTo("DOWNLOAD") == 0)
                request_type = DOWNLOAD;

            if (request_type == SETUP) {
                //extract VideoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            //System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());

            //get LastLine
            String LastLine = RTSPBufferedReader.readLine();
            //System.out.println(LastLine);

            tokens = new StringTokenizer(LastLine);
            if (request_type == SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i=0; i<3; i++)
                    tokens.nextToken(); //skip unused stuff
                RTP_dest_port = Integer.parseInt(tokens.nextToken());
            }
            else if (request_type == DESCRIBE) {
                tokens.nextToken();
                String describeDataType = tokens.nextToken();
            }
            else if (request_type == DOWNLOAD) {
                tokens.nextToken();
                String filelist = tokens.nextToken();
                System.out.println("DOWNLOAD FILE LIST: " + filelist);
                sharedArea.filelist = filelist;
                sharedArea.file_flag  = true;
                //FileServer fServer = new FileServer(ClientIPAddr.getHostAddress(), 2222, filelist.split("#"));
                //fServer.start();
                sendResponse();
            }
            else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
                RTSPid = tokens.nextToken();
            }
        } catch(Exception ex) {
            System.out.println("Exception caught2: "+ex.toString());
            System.exit(0);
        }

        return(request_type);
    }

    // Creates a DESCRIBE response string in SDP format for current media
    private String describe() {
        StringWriter writer1 = new StringWriter();
        StringWriter writer2 = new StringWriter();

        // Write the body first so we can get the size later
        writer2.write("v=0" + CRLF);
        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF);
        writer2.write("a=control:streamid=" + RTSPid + CRLF);
        writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
        String body = writer2.toString();

        writer1.write("Content-Base: " + VideoFileName + CRLF);
        writer1.write("Content-Type: " + "application/sdp" + CRLF);
        writer1.write("Content-Length: " + body.length() + CRLF);
        writer1.write(body);

        return writer1.toString();
    }

    //——————————————————
    //Send RTSP Response
    //——————————————————
    private void sendResponse() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write("Session: "+RTSPid+CRLF);
            RTSPBufferedWriter.flush();
            //System.out.println("RTSP Server - Sent response to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught3: "+ex.toString());
            System.exit(0);
        }
    }

    private void sendChange(String str) {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 " + str +" OK"+CRLF);
            RTSPBufferedWriter.flush();
            //System.out.println("RTSP Server - Sent Change to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught3: "+ex.toString());
            System.exit(0);
        }
    }


    private void sendFileList(String file_list) {
    //Jimin_here
        try {
            RTSPBufferedWriter.write("RTSP/1.0 " + "1234" +" OK"+CRLF);
            RTSPBufferedWriter.write(file_list + CRLF);
            RTSPBufferedWriter.flush();
            //System.out.println("RTSP Server - Sent Change to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught 4: "+ex.toString());
            System.exit(0);
        }
    }


    private void sendDescribe() {
        String des = describe();
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            RTSPBufferedWriter.write(des);
            RTSPBufferedWriter.flush();
            //System.out.println("RTSP Server - Sent response to Client.");
        } catch(Exception ex) {
            System.out.println("Exception caught4: "+ex.toString());
            System.exit(0);
        }
    }
}
