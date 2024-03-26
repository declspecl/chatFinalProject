package edu.oakland.gavin;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    public int port;
    private ServerSocket socket;

    public Server(int port) throws IOException
    {
        this.port = port;
        this.socket = new ServerSocket(port);
    }

    public ServerSocket getSocket()
    {
        return socket;
    }

    public void spawnThreadedConnection(Socket client) throws IOException, InterruptedException
    {
        System.out.println("[*] Spawning server connection thread");

        new Thread(() -> {
            try (
                DataInputStream inputStream = new DataInputStream(client.getInputStream());
            )
            {
                client.setKeepAlive(true);

                byte[] buffer = new byte[1000];

                while (!client.isClosed())
                {
                    int bytesRead = inputStream.read(buffer);

                    if (bytesRead == 0) continue;
                    if (bytesRead == -1) return;

                    String message = new String(buffer);

                    System.out.printf("[*] Read %d bytes from the user: \"%s\"\n", bytesRead, message);
                }
            }
            catch (IOException e)
            {
                System.out.printf("[!] Fatal error: %s\n", e.toString());

                throw new RuntimeException(e);
            }
            finally {
                try
                {
                    client.close();

                    System.out.println("[+] Closed connection");
                }
                catch (IOException e)
                {
                    System.out.printf("[!] Fuck: %s\n", e.toString());

                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}