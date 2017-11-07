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
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames

    InetAddress ClientIPAddr;   //Client IP address
    int RTP_dest_port = 0;      //destination port for RTP packets  (given by the RTSP Client)
    int RTSP_dest_port = 0;

    //Video variables:
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
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int DESCRIBE = 7;
    final static int WIFI = 8;
    final static int FILELIST = 9;
    final static int DOWNLOAD = 10;

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
    DatagramSocket RTCPsocket;

    final static String CRLF = "\r\n";
    SharedArea sharedArea;

    Wifi wifi;
    //--------------------------------
    //Constructor
    //--------------------------------
    public Server(VideoStream videoStream, SharedArea sharedArea, Wifi wifi) {
        //init RTP sending Timer
        sendDelay = FRAME_PERIOD;
        timer = new Timer(sendDelay, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //allocate memory for the sending buffer
        buf = new byte[20000];
        buff = new byte[20000];

        this.video = videoStream;
        this.sharedArea = sharedArea;
        this.wifi = wifi;
    }

    @Override
    public void run() {
        //create a Server object

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

                File[] listFile = new File("./saved").listFiles();
                try{
                   if(listFile.length >0) {
                        for(int i=0; i<listFile.length; i++) {
                            if(listFile[i].isFile()) {
                                listFile[i].delete();
                            } 
                        }
                    }
                } catch (Exception e9) {
                    System.out.println("Execption caught9: " + e9.toString());
                }


                done = true;

                //update RTSP state
                state = READY;
                System.out.println("New RTSP state: READY");

                //Send response
                sendResponse();

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
                //update state
                state = PLAYING;

                System.out.println("New RTSP state: PLAYING");
            }
            else if ((request_type == PAUSE) && (state == PLAYING)) {
                //send back response
                sendResponse();
                //stop timer

                timer.stop();
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
                //close sockets
                try {
                    RTSPsocket.close();
                    RTPsocket.close();
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
            } else if (request_type == FILELIST) {
                //System.out.println("Give me FileList come on");
                String fileList = "";
                File savedDir = new File("./saved/");
                if(savedDir.isDirectory()) {
                    for(File file : savedDir.listFiles()) {
                        if(file.isFile() && (file.getName()).startsWith("video_")) {
                            fileList += (file.getName() + "#");
                            System.out.println(fileList);
                        }
                    }
                }
                sendFileList(fileList);
            }
        }
    }


    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {
        int image_length = 0 ;

        if(sharedArea.disconnect_flag) {
            return;
        }

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
                //adjust quality of the image if there is congestion detected

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();
                
                if(packet_length <= 0) {
                    return;
                }

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];

                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);

                RTPsocket.send(senddp);

            } catch (Exception ex) {
                //System.out.println("Exception caught5: " + ex.toString());
                System.out.println("I'm waiting");
            }
        } else {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
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
