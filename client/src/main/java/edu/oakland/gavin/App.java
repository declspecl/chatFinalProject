package edu.oakland.gavin;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Scanner;

public class App
{
    public static void main(String[] args)
    {
        try (Socket socket = new Socket())
        {
            // spawn socket connection to localhost on port 9999
            socket.connect(new InetSocketAddress("127.0.0.1", 9999));

            Logger.success("Connected to server");

            // open output channel to server
            OutputStream rawOutputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(rawOutputStream);
            OutputStreamWriter charOutputStream = new OutputStreamWriter(rawOutputStream);

            InputStreamReader charInputStream = new InputStreamReader(socket.getInputStream());

            // reading the room number from the user
            Scanner scanner = new Scanner(System.in);

            Logger.info("Enter room number to join:");

            int targetRoom = Integer.parseInt(scanner.nextLine());
            dataOutputStream.writeInt(targetRoom);

            Logger.success(String.format("Joined room %d\n", targetRoom));

            // spawning thread to simultaneously receive messages as well as send them
            new Thread(() -> {
                try
                {
                    while (!socket.isClosed())
                    {
                        char[] characters = new char[1000];

                        // read characters from the socket
                        int bytesRead = charInputStream.read(characters);

                        // if nothing was read, try again
                        if (bytesRead == 0) continue;

                        // if -1 was read, the stream ended
                        if (bytesRead == -1)
                        {
                            Logger.warning(String.format("Connection with %s was unexpectedly closed", socket));
                            break;
                        }

                        // if execution has gotten here, it means a message was received
                        String message = new String(characters);
                        Logger.info(String.format("Received message: \"%s\"\n", message));
                    }
                }
                catch (Exception e)
                {
                    Logger.error(String.format("Error occurred in handling socket: %s", e));
                }
            }).start();

            // getting input from user until the connection with the server closes
            while (!socket.isClosed())
            {
                Logger.info("Enter message to send. Or type \"quit\" to leave the room.");
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("quit"))
                    break;

                charOutputStream.write(message);
                charOutputStream.flush();

                Logger.success(String.format("Sent message: \"%s\"\n", message));
            }

            Logger.info("Disconnected from server");

            // closing all streams and buffers
            dataOutputStream.close();
            charOutputStream.close();
            rawOutputStream.close();
            charInputStream.close();
        }
        catch (NumberFormatException e)
        {
            Logger.error("Invalid room number input supplied");
        }
        catch (IOException e)
        {
            Logger.error(e.toString());
        }
    }
}
