package edu.oakland.gavin;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class App
{
    public static void main(String[] args)
    {
        // try-with-resource syntax to automatically close the server socket
        try (Socket server = new Socket())
        {
            // spawn socket connection to server on port 9999
            server.connect(new InetSocketAddress("127.0.0.1", 9999));
            Logger.success("Connected to server");

            // attempt to get data input stream from server, quitting if unsuccessful
            final DataInputStream dataInputStream;
            try
            {
                dataInputStream = new DataInputStream(server.getInputStream());
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to get data input stream from server: %s", e));
                return;
            }

            // attempt to get data input stream from server, quitting if unsuccessful
            final DataOutputStream dataOutputStream;
            try
            {
                dataOutputStream = new DataOutputStream(server.getOutputStream());
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to get data output stream from serveer: %s", e));
                return;
            }

            ArrayList<Integer> activeRoomNumbers = new ArrayList<>();

            // keep reading room numbers from the server until we get a -1 (meaning the server is done sending them)
            int recentRoomNumber = 0;
            while (recentRoomNumber != -1)
            {
                try
                {
                    recentRoomNumber = dataInputStream.readInt();

                    if (recentRoomNumber != -1)
                    {
                        activeRoomNumbers.add(recentRoomNumber);
                    }
                }
                catch (IOException e)
                {
                    Logger.error("Failed to read room number from server");
                    return;
                }
            }

            // print out the active room numbers to the user, if there are any
            if (activeRoomNumbers.isEmpty())
            {
                Logger.info("There are currently no rooms with any active users");
            }
            else
            {
                Logger.info("There are active users in the following rooms:");

                // iterate through each active room and print it
                for (int roomNumber : activeRoomNumbers)
                {
                    System.out.printf("- %d\n", roomNumber);
                }
            }

            // reading the room number they want to join from the user
            Scanner scanner = new Scanner(System.in);

            // keep asking the user which room to join until they give a valid room number
            int targetRoom = -1;
            while (targetRoom == -1)
            {
                System.out.print("Enter any room number to join: ");

                try
                {
                    targetRoom = Integer.parseInt(scanner.nextLine());
                }
                catch (Exception e)
                {
                    Logger.error("Please input a valid room number");
                    continue;
                }
            }

            // try sending the room number to the server
            try
            {
                dataOutputStream.writeInt(targetRoom);
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to send the room number to server: %d", targetRoom));
                return;
            }

            Logger.success(String.format("Joined room %d\n", targetRoom));

            // spawning thread to simultaneously receive messages as well as send them
            new Thread(() -> {
                try
                {
                    while (!server.isClosed())
                    {
                        char[] characters = new char[1000];

                        // read characters from the server
                        int bytesRead = charInputStream.read(characters);

                        // if nothing was read, try again
                        if (bytesRead == 0) continue;

                        // if -1 was read, the stream ended
                        if (bytesRead == -1)
                        {
                            Logger.warning(String.format("Connection with %s was unexpectedly closed", server));
                            break;
                        }

                        // if execution has gotten here, it means a message was received
                        String message = new String(characters);
                        Logger.info(String.format("Received message: \"%s\"\n", message));
                    }
                }
                catch (Exception e)
                {
                    Logger.error(String.format("Error occurred in handling server: %s", e));
                }
            }).start();

            // getting input from user until the connection with the server closes
            while (!server.isClosed())
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

            // closing all streams, connections, and buffers
            try
            {
                dataInputStream.close();
                dataOutputStream.close();
                server.close();
            }
            catch (IOException e)
            {
                Logger.error("Failed to close one or more streams, connections, and/or buffers");
            }
        }
        catch (IOException e)
        {
            Logger.error("Failed to connect to server");
        }
    }
}
