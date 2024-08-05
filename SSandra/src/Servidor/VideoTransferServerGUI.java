package Servidor;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoTransferServerGUI {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int PROGRESS_BAR_WIDTH = 20;

    private JFrame frame;
    private JPanel panel;

    public VideoTransferServerGUI() {
        SwingUtilities.invokeLater(this::initializeGUI);
        startServer();
    }

    private void initializeGUI() {
        frame = new JFrame("Servidor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane);

        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void startServer() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor esperando conexiones...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());

                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            ) {
                while (true) {
                    try {
                        String fileName = (String) inputStream.readObject();
                        if (fileName == null) {
                            break; 
                        }

                        long fileSize = inputStream.readLong();

                        File outputFile = new File("videos", fileName);
                        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        long totalBytesRead = 0; 
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead; 
                            final int progress = (int) (((double) totalBytesRead / fileSize) * 100);
                            SwingUtilities.invokeLater(() -> updateProgressBar(fileName, progress));
                        }

                        fileOutputStream.close();

                        System.out.println("Archivo recibido: " + outputFile.getAbsolutePath());
                    } catch (EOFException e) {
                        System.out.println("El cliente ha cerrado la conexión.");
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void updateProgressBar(String fileName, int progress) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JPanel) {
            	
                JPanel progressPanel = (JPanel) component;
                JLabel fileNameLabel = (JLabel) progressPanel.getComponent(0);
                if (fileNameLabel.getText().equals(fileName)) {
                    JProgressBar progressBar = (JProgressBar) progressPanel.getComponent(1);
                    progressBar.setValue(progress);
                    return; 
                }
            }
        }

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); 
        JProgressBar progressBar = new JProgressBar(0, 100);
       progressBar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH,10)); 
        progressBar.setValue(progress);
        progressBar.setStringPainted(true);

        JLabel fileNameLabel = new JLabel(fileName);

        progressPanel.add(fileNameLabel, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        panel.add(progressPanel);
        frame.revalidate();
        frame.repaint();
    }

    public static void main(String[] args) {
        new VideoTransferServerGUI();
    }
}
