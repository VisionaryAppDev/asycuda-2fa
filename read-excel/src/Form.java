import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.Hashtable;

public class Form {
    ///
    private static final int TWO_FA_PANEL_X = 500;
    private static final int TWO_FA_PANEL_Y = 150;
    private static final int TWO_FA_PANEL_WIDTH = 250;
    private static final int TWO_FA_PANEL_HEIGHT = 250;
    private static final int QR_CODE_SIZE = 250;


    private final Socket mSocket = IO.socket("http://localhost:9092");
    private final JLabel lblQrcode = new JLabel();
    private final JLabel lblFingerprint = new JLabel();
    private final JPanel otpPanel = new JPanel();
    private final JPanel fingerprintPanel = new JPanel();
    private final JPanel qrPanel = new JPanel();


    ///
    private JFrame mainFrame;
    private JDialog twoFADialog;


    public Form() throws URISyntaxException {
        init();
        mSocket.connect();
    }


   private void init() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth() - 600;
        int height = gd.getDisplayMode().getHeight() - 600;


        mainFrame = new JFrame();
        mainFrame.setSize(width, height);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.getContentPane().setLayout(null);
        mainFrame.setLocationRelativeTo(null);


        twoFADialog = new JDialog(mainFrame, "Two Factor Authentication", Dialog.ModalityType.APPLICATION_MODAL);
        twoFADialog.setSize(width, height);
        twoFADialog.setLayout(null);
        twoFADialog.setLocationRelativeTo(null);

        /// Exit the application if user close the dialog
        twoFADialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });


        /// 2FA Options
        JButton btnOTP = new JButton("OTP Code");
        JButton btnFingerprint = new JButton("Fingerprint");
        JButton btnQr = new JButton("QR Code");

        /// 2FA Options Location and Size
        btnOTP.setBounds(410, 100, 125, 40);
        btnFingerprint.setBounds(545, 100, 125, 40);
        btnQr.setBounds(680, 100, 125, 40);

        /// Add to Frame
        twoFADialog.getContentPane().add(btnOTP);
        twoFADialog.getContentPane().add(btnFingerprint);
        twoFADialog.getContentPane().add(btnQr);
        twoFADialog.dispose();

        btnOTP.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                toggleVisibility(otpPanel);
            }
        });


        btnFingerprint.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                toggleVisibility(fingerprintPanel);
            }
        });


        btnQr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                toggleVisibility(qrPanel);

                if (!mSocket.connected()) {
                    mSocket.connect();
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", "Test-001");
                jsonObject.put("userId", "4c19dad0-d3a1-429b-a8bc-d41760a442c4");
                mSocket.emit("on_2fa_qr_clicked", jsonObject);
            }
        });


        mSocket.on("on_2fa_qr_clicked", new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                if (objects != null) {
                    try {
                        String qrCodeText = (String) ((JSONObject) objects[0]).get("url");
                        Image image = createQRImage(qrCodeText);
                        ImageIcon icon = new ImageIcon(image);
                        lblQrcode.setSize(QR_CODE_SIZE, QR_CODE_SIZE);
                        lblQrcode.setIcon(icon);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).on("on_2fa_qr_refresh", new Emitter.Listener() {
            @Override
            public void call(Object... objects) {

            }
        }).on("on_2fa_qr_successfully_scanned", new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                mainFrame.setVisible(true);
                twoFADialog.dispose();
            }
        });

        prepareOtpFrame();
        prepareFingerprintPanel();
        prepareQrPanel();

        twoFADialog.getContentPane().add(otpPanel);
        twoFADialog.getContentPane().add(fingerprintPanel);
        twoFADialog.getContentPane().add(qrPanel);
        twoFADialog.setVisible(true);
    }

    private ImageIcon getFingerprintLabel() {
        Image iconImg = new ImageIcon("fingerprint-icon.png").getImage();
        Image newIconImg = iconImg.getScaledInstance(QR_CODE_SIZE, QR_CODE_SIZE, java.awt.Image.SCALE_AREA_AVERAGING);

        return new ImageIcon(newIconImg);
    }

    private Image createQRImage(String qrCodeText) throws WriterException {
        // Create the ByteMatrix for the QR-Code that encodes the given String
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hintMap);
        // Make the BufferedImage that are to hold the QRCode
        int matrixWidth = byteMatrix.getHeight();
        int matrixHeight = byteMatrix.getHeight();
        BufferedImage image = new BufferedImage(matrixWidth, matrixHeight, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, matrixWidth, matrixHeight);
        // Paint and save the image using the ByteMatrix
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixHeight; j++) {
                if (byteMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }

        return image;
    }



    private void prepareOtpFrame() {
        otpPanel.setLayout(null);
        otpPanel.setBounds(TWO_FA_PANEL_X, TWO_FA_PANEL_Y, TWO_FA_PANEL_WIDTH, TWO_FA_PANEL_HEIGHT);


        /// Input OTP
        JTextArea txtOTP = new JTextArea(1, 6);
        txtOTP.setBounds(10, 10, 75, 25);

        /// Button Request OTP
        JButton btnOtpGetCode = new JButton("Get Code");
        btnOtpGetCode.setBounds(90, 10, 100, 25);

        /// Button Okay
        JButton btnOtpOk = new JButton("OK");
        btnOtpOk.setBounds(10, 100, 175, 25);


        /// Event
        /// Get OTP Code Clicked
        btnOtpGetCode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                toggleVisibility(otpPanel);

                HttpRequest<String> httpRequest = new HttpRequest<>();
                String out = httpRequest.get("http://localhost:8080/2fa/otp/1/1", String.class);
            }
        });

        /// Verify OTP Code
        btnOtpOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    String otp = txtOTP.getText();
                    HttpRequest<String> httpRequest = new HttpRequest<>();
                    httpRequest.get("http://localhost:8080/2fa/otp/verify/1/" + otp, String.class);
                    JOptionPane.showMessageDialog(new JFrame(), "Successful! ", "OTP", JOptionPane.PLAIN_MESSAGE);
                    mainFrame.setVisible(true);
                    twoFADialog.dispose();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(new JFrame(), "Input OTP wasn't matched", "OTP", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        /// Add to Panel
        otpPanel.add(txtOTP);
        otpPanel.add(btnOtpGetCode);
        otpPanel.add(btnOtpOk);
        otpPanel.setVisible(true);
    }

    private void prepareFingerprintPanel() {
        fingerprintPanel.setLayout(null);
        fingerprintPanel.setBounds(TWO_FA_PANEL_X, TWO_FA_PANEL_Y, TWO_FA_PANEL_WIDTH, TWO_FA_PANEL_HEIGHT);

        /// Add to Panel
        lblFingerprint.setBounds(0, 0, 225, 225);
        lblFingerprint.setIcon(getFingerprintLabel());
        fingerprintPanel.add(lblFingerprint);
        fingerprintPanel.setVisible(false);
    }


    private void prepareQrPanel() {
        qrPanel.setLayout(null);
        qrPanel.setBounds(TWO_FA_PANEL_X, TWO_FA_PANEL_Y, TWO_FA_PANEL_WIDTH, TWO_FA_PANEL_HEIGHT);


        lblFingerprint.setBounds(0, 0, 225, 225);
        lblFingerprint.setIcon(getFingerprintLabel());
        qrPanel.add(lblQrcode);
        qrPanel.setVisible(false);
    }


    private void toggleVisibility(JPanel panel) {
        qrPanel.setVisible(false);
        fingerprintPanel.setVisible(false);
        otpPanel.setVisible(false);

        panel.setVisible(true);
    }
}
