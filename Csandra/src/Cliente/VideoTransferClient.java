package Cliente;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class VideoTransferClient {
    private JFrame frame;
    private JPanel buttonPanel;
    private JButton addVideoButton;
    private JButton sendVideosButton;
    private ArrayList<File> selectedVideos;
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public VideoTransferClient() {
        selectedVideos = new ArrayList<>();
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Video Transfer Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        addVideoButton = new JButton("Agregar más videos");
        addVideoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addVideoButton();
            }
        });

        sendVideosButton = new JButton("Enviar Videos");
        sendVideosButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarVideos();
            }
        });

        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(addVideoButton, BorderLayout.NORTH);
        frame.add(sendVideosButton, BorderLayout.SOUTH);

        frame.setSize(400, 250);
        frame.setVisible(true);
    }

    private void addVideoButton() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            selectedVideos.add(selectedFile);
            JButton newButton = new JButton(selectedFile.getName());
            buttonPanel.add(newButton);
            frame.revalidate();
        }
    }

    private void enviarVideos() {
        // Creando un hilo para enviar cada archivo
        for (File file : selectedVideos) {
            Thread thread = new Thread(new FileSender(file));
            thread.start();
        }
    }

    private class FileSender implements Runnable {
        private File file;

        public FileSender(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                 FileInputStream fileInputStream = new FileInputStream(file)) {

                // Enviando el nombre del archivo y doo lo demas
                outputStream.writeObject(file.getName());
                outputStream.flush();

                outputStream.writeLong(file.length());
                outputStream.flush();

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

                System.out.println("Archivo enviado: " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VideoTransferClient();
            }
        });
    }
}
