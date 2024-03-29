package edu.oakland.gavin;

import java.io.IOException;
import java.net.Socket;

public class App
{
    public static void main(String[] args)
    {
        // using a "try-with-resources" statement to automatically close the MultiThreadedServer
        try (MultiThreadedServer server = new MultiThreadedServer(9999))
        {
            Logger.info(String.format("Listening for connections on port %d", server.getPort()));

            while (!server.getSocket().isClosed())
            {
                try
                {
                    Socket client = server.getSocket().accept();

                    Logger.info(String.format("New client connected: %s", client));

                    server.handleConnection(client);
                }
                catch (IOException e)
                {
                    Logger.error(String.format("Error when accepting connection to server: %s", e));
                }
            }
        }
        catch (IOException e)
        {
            Logger.error(String.format("Fatal error in attempting to create server socket: %s", e));
        }
    }
}
