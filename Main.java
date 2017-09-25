import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * Created by Jimin on 9/25/17.
 */
public class Main {
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
    static int state; //RTSP Server state == INIT or READY or PLAY
    static int RTCP_RCV_PORT = 19001; //port where the client will receive the RTP packets

    private static BufferedReader RTSPBufferedReader;
    private static BufferedWriter RTSPBufferedWriter;

    public static void main(String argv[]) throws Exception
    {


        //get RTSP socket port from the command line
        int RTSPport = Integer.parseInt(argv[0]);

        //create a Server object
        ServerSocket listenSocket = new ServerSocket(RTSPport);
        Server server = new Server(RTSPport, listenSocket);

        //show GUI:
        server.pack();
        server.setVisible(true);
        server.setSize(new Dimension(400, 200));

        //Initiate TCP connection with the client for the RTSP session
        listenSocket.close();

        //Get Client IP address
        server.ClientIPAddr = server.RTSPsocket.getInetAddress();

        //Initiate RTSPstate
        state = INIT;

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(server.RTSPsocket.getInputStream()) );
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.RTSPsocket.getOutputStream()) );

        //Wait for the SETUP message from the client
        int request_type;
        boolean done = false;
        while(!done) {
            request_type = server.parseRequest(RTSPBufferedReader); //blocking

            if (request_type == SETUP) {
                done = true;

                //update RTSP state
                state = READY;
                System.out.println("New RTSP state: READY");

                //Send response
                server.sendResponse(RTSPBufferedWriter);

                //init the VideoStream object:
                server.video = new VideoStream();

                //init RTP and RTCP sockets
                server.RTPsocket = new DatagramSocket();
                server.RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
            }
        }

        //loop to handle RTSP requests
        while(true) {
            //parse the request
            request_type = server.parseRequest(RTSPBufferedReader); //blocking

            if ((request_type == PLAY) && (state == READY)) {
                //send back response
                server.sendResponse(RTSPBufferedWriter);
                //start timer
                try {
                    server.video.getStarted();
                }catch(Exception e) {
                    System.out.println("error from getStarted()");
                }

                server.timer.start();
                server.rtcpReceiver.startRcv();
                //update state
                state = PLAYING;

                System.out.println("New RTSP state: PLAYING");
            }
            else if ((request_type == PAUSE) && (state == PLAYING)) {
                //send back response
                server.sendResponse(RTSPBufferedWriter);
                //stop timer

                System.out.println("222");
                server.timer.stop();
                server.rtcpReceiver.stopRcv();
                //update state
                state = READY;
                System.out.println("New RTSP state: READY");
            }
            else if (request_type == TEARDOWN) {
                //send back response
                server.sendResponse(RTSPBufferedWriter);
                //stop timer

                server.timer.stop();
                server.rtcpReceiver.stopRcv();
                //close sockets
                server.RTSPsocket.close();
                server.RTPsocket.close();

                System.exit(0);
            }
            else if (request_type == DESCRIBE) {
                System.out.println("Received DESCRIBE request");
                server.sendDescribe(RTSPBufferedWriter);
            }
        }
    }
}
