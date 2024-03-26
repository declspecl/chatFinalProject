package edu.oakland.gavin;

import java.io.IOException;
import java.net.Socket;

public class App
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        Server server = new Server(9999);

        System.out.println("[*] Listening for connections");

        while (true)
        {
            Socket client = server.getSocket().accept();

            server.spawnThreadedConnection(client);

            System.out.println("[*] Spawned threaded connection");
        }
    }
}
