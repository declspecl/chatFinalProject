package edu.oakland.gavin;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.ArrayList;

public class App
{
    public static void main(String[] args)
    {
        // try-with-resource syntax to automatically close the server socket
        try (Socket server = new Socket("localhost", 9999))
        {
            // spawn socket connection to server on port 9999
            // server.connect(new InetSocketAddress("127.0.0.1", 9999));
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

                try
                {
                    server.close();
                }
                catch (IOException e2)
                {
                    Logger.error("Failed to close one or more streams, connections, and/or buffers");
                }

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
                Logger.error(String.format("Failed to get data output stream from server: %s", e));

                try
                {
                    dataInputStream.close();
                    server.close();
                }
                catch (IOException e2)
                {
                    Logger.error("Failed to close one or more streams, connections, and/or buffers");
                }

                return;
            }

            ArrayList<Integer> activeRoomNumbers = new ArrayList<>();
            ArrayList<Integer> roomSizes = new ArrayList<>();

            // keep reading room numbers from the server until we get a -1 (meaning the server is done sending them)
            while (true)
            {
                try
                {
                    int roomNumber = dataInputStream.readInt();

                    if (roomNumber == -1)
                    {
                        break;
                    }
                    else
                    {
                        int roomSize = dataInputStream.readInt();

                        activeRoomNumbers.add(roomNumber);
                        roomSizes.add(roomSize);
                    }
                }
                catch (IOException e)
                {
                    Logger.error("Failed to read room number and/or size of room from server");
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
                // iterate through each active room and print its room number and how many people are in it
                for (int i = 0; i < activeRoomNumbers.size(); i++)
                {
                    Logger.info(String.format("Room #%d has %d client(s) connected", activeRoomNumbers.get(i), roomSizes.get(i)));
                }
            }

            // reading the room number they want to join from the user
            Scanner scanner = new Scanner(System.in);

            // keep asking the user which room to join until they give a positive valid room number
            int targetRoom = -1;
            while (targetRoom < 0)
            {
                System.out.print("\nEnter any room number to join (positive integers only): ");

                try
                {
                    targetRoom = Integer.parseInt(scanner.nextLine());

                    if (targetRoom < 0)
                    {
                        Logger.warning("Please input a valid room number (positive integers only)");
                        continue;
                    }
                }
                catch (Exception e)
                {
                    Logger.warning("Please input a valid room number");
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

            Logger.success(String.format("\nJoined room %d\n", targetRoom));

            // spawning thread to simultaneously receive messages as well as send them
            new Thread(() -> {
                while (!server.isClosed())
                {
                    // try to read messages from the server and print them
                    String message;
                    try
                    {
                        message = dataInputStream.readUTF();
                        Logger.info(String.format("Received message: \"%s\"", message));
                    }
                    catch (IOException e)
                    {
                        Logger.error("Failed to read message from server");

                        try
                        {
                            dataInputStream.close();
                            dataOutputStream.close();
                            server.close();
                        }
                        catch (IOException e2)
                        {
                            Logger.error("Failed to close one or more streams, connections, and/or buffers");
                        }

                        return;
                    }
                }
            }).start();

            Logger.info("Type a message and press enter to send it in the room. Or type \"quit\" to quit the application");

            // getting input from user until the connection with the server closes
            while (!server.isClosed())
            {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("quit"))
                {
                    break;
                }

                try
                {
                    dataOutputStream.writeUTF(message);
                }
                catch (IOException e)
                {
                    Logger.error(String.format("Failed to send message \"%s\" to server", message));
                }

                Logger.success(String.format("Sent message: \"%s\"", message));
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
