JC = javac
J = java

default: Main.class Wifi.class Server.class VideoStream.class RTPpacket.class FileServer.class

Wifi.class: Wifi.java
	$(JC) $(JFLAGS) Wifi.java
Main.class: Main.java
	$(JC) $(JFLAGS) Main.java
Server.class: Server.java
	$(JC) $(JFLAGS) Server.java 
VideoStream.class: VideoStream.java
	$(JC) $(JFLAGS) VideoStream.java 
RTPpacket.class: RTPpacket.java
	$(JC) $(JFLAGS) RTPpacket.java 
FileServer.class: FileServer.java
	$(JC) $(JFLAGS) FileServer.java
clean:
	rm -f *.class
