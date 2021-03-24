import socket
import json
import time
# ROS
import rospy
from geometry_msgs.msg import Point


class send_target_socket():
    PORT
    SERVER
    ADDR = (SERVER, PORT)
    FORMAT = "utf-8"
    DISCONNECT_MESSAGE = "!DISCONNECT"

    def __init__(self, ip_server, port):

        rospy.init_node('send_target_socket', anonymous=True)
        self.PORT = port
        self.SERVER =ip_server  # IP del server
        # mettere try 
        with open('target.JSON') as j:
            self.data = json.load(j)

        self.sub = rospy.Subscriber("/D_Control_point", Point, self.send_target_to_app, queue_size=1)

    def send_msg(self, msg, client):
        message = msg.encode(self.FORMAT)  # codificarlo in binario per la trasmissione
        client.send(message)  # mando msg
        # print(client.recv(2048).decode(FORMAT))# decodifico risposta e la printo
        client.close()

    
    def send_target_to_app(self, target):
        self.data["target_x"] = target.x
        self.date["target_y"] = traget.y
        msg = json.dumps(data)

        client = socket.socket(
        socket.AF_INET, socket.SOCK_STREAM)  # creo il client
        client.connect(self.ADDR)  # indirizzo del server a cui devo connettermi
        print("Sanding the jason msg...")
        send_msg(msg, client)
        
        time.sleep(1)


if __name__ == "__main__":
    
    ip_server = "130.251.13.144"
    port = 8080
    client = send_target_socket(ip_server, port)
    try:
        rospy.spin()
    except KeyboardInterrupt:
        print ("Shutting down ROS Image feature detector module")


