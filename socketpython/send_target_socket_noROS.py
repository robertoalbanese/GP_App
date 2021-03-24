import socket
import json
import time




class send_target_socket():

    

    def __init__(self, ip_server, port):

        
        self.PORT = port
        self.SERVER =ip_server  # IP del server
        self.ADDR = (self.SERVER, self.PORT)
        self.FORMAT = "utf-8"
        self.DISCONNECT_MESSAGE = "!DISCONNECT"
        
        # mettere try 
        with open('target.JSON') as j:
            self.data = json.load(j)

    

    def send_msg(self, msg, client):
        message = msg.encode(self.FORMAT)  # codificarlo in binario per la trasmissione
        client.send(message)  # mando msg
        # print(client.recv(2048).decode(FORMAT))# decodifico risposta e la printo
        client.close()

    
    def send_target_to_app(self, target):
        self.data["target_x"] = 5
        self.date["target_y"] = 5
        msg = json.dumps(data)

        while True:
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
   