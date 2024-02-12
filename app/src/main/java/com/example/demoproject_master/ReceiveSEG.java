package com.example.demoproject_master;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.example.demoproject.Seg.ImageData;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// SEG 소켓 통신
// Device2 socket (SEG)
public class ReceiveSEG implements Runnable {

    private final Handler uiHandler;
    private final TextView device2_state;
    private final int serverPort;
    public static final int MESSAGE_SEG_DATA = 2;
    ExecutorService executorService = Executors.newFixedThreadPool(5);

    public ReceiveSEG(int serverPort, Handler uiHandler, TextView device2_state) {
        this.serverPort = serverPort;
        this.uiHandler = uiHandler;
        this.device2_state = device2_state;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            while (!Thread.interrupted()) {
                Socket clientSocket = serverSocket.accept();

                executorService.submit(() -> {
                    try (BufferedInputStream inFromClient = new BufferedInputStream(clientSocket.getInputStream());
                         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        byte[] data = new byte[1];
                        int bytesRead;

                        while ((bytesRead = inFromClient.read(data)) != -1) {
                            byteArrayOutputStream.write(data, 0, bytesRead);
                        }
                        byte[] receivedData = byteArrayOutputStream.toByteArray();

                        // Parse the protobuf message
                        ImageData imageDataProto = ImageData.parseFrom(receivedData);

                        // Get the image data from the protobuf message
                        byte[] imageBytes = imageDataProto.getImageData().toByteArray();
                        boolean run = imageDataProto.getRun();
                        Bitmap receiveBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        uiHandler.post(() -> {
                            if (run)
                                device2_state.setText("on");
                            else
                                device2_state.setText("off");
                        });

                        Message message = uiHandler.obtainMessage(MESSAGE_SEG_DATA, receiveBitmap);
                        uiHandler.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdownNow();
        }
    }
}
