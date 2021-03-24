import socket
import json
import time
import numpy as np 


PORT = 8080
SERVER = "192.168.1.62"  # IP del server
ADDR = (SERVER, PORT)
FORMAT = "utf-8"
DISCONNECT_MESSAGE = "!DISCONNECT"


def send_msg(msg, client):

    message = msg.encode(FORMAT)  # codificarlo in binario per la trasmissione
    client.send(message)  # mando msg
    # print(client.recv(2048).decode(FORMAT))# decodifico risposta e la printo
    client.close()

    


def main():

    with open('rpy.JSON') as j:
        data = json.load(j)

    wordset = np.genfromtxt(fname='navigation_target.txt')  
    
    #for i in range(400):
    #    x = wordset[i][0]
    #    y = wordset[i][2]
    while True:
        data['target_x'] = 1
        data['target_y'] = 0
        
        msg = json.dumps(data)

        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # creo il client
        client.connect(ADDR)  # indirizzo del server a cui devo connettermi
        
        print("Sanding the jason msg...")
        send_msg(msg, client)
        time.sleep(0.3)
        
        


if __name__ == "__main__":
    main()
