JC = javac
J = java

default: Client.class Main.class Wifi.class Server.class VideoStream.class RTPpacket.class RTCPpacket.class

Wifi.class: Wifi.java
	$(JC) $(JFLAGS) Wifi.java
Main.class: Main.java
	$(JC) $(JFLAGS) Main.java
Client.class: Client.java
	$(JC) $(JFLAGS) Client.java
Server.class: Server.java
	$(JC) $(JFLAGS) Server.java 
VideoStream.class: VideoStream.java
	$(JC) $(JFLAGS) VideoStream.java 
RTPpacket.class: RTPpacket.java
	$(JC) $(JFLAGS) RTPpacket.java 
RTCPpacket.class: RTCPpacket.java
	$(JC) $(JFLAGS) RTCPpacket.java 
clean:
	rm -f *.class
