package edu.oakland.gavin;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;

public class App
{
    public static void main(String[] args)
    {
        try (
            Socket socket = new Socket();
        )
        {
            socket.connect(new InetSocketAddress("127.0.0.1", 9999));
            System.out.println("[+] Connected successfully");

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            Scanner scanner = new Scanner(System.in);

            System.out.println("[*] Enter message to send:");
            String message = scanner.nextLine();

            outputStream.writeChars(message);

            System.out.println("[+] Successfully sent message");

            outputStream.close();
        }
        catch (Exception e)
        {
            System.out.printf("[!] Fatal error: %s\n", e.toString());
        }
    }
}
