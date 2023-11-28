package io.peripage.service;

import io.peripage.helper.ByteHelper;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class TransportService {

    private static final int WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS = 250;

    private final String mac;

    private StreamConnection sock;
    private OutputStream os;
    private InputStream is;

    public TransportService(String mac) {
        this.mac = mac.replaceAll(":", "");
    }

    /**
     * Open a new connection to the printer without checking for existing
     * connection. In case of malfunction and/or twice connecting to the same
     * printer, socket descriptor becomes unoperateable.
     *
     * @throws IOException
     */
    public void connect() throws IOException, InterruptedException {
        this.sock = (StreamConnection) Connector.open("btspp://" + this.mac + ":1;authenticate=false;encrypt=false;master=false;");
        Thread.sleep(WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS);
        this.os = this.sock.openOutputStream();
        this.is = this.sock.openInputStream();
        this.reset();
        Thread.sleep(WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS);
    }

    /**
     * Check if printer is connected (socket alive)
     */
    public boolean isConnected() {
        try {
            this.os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reconnect to the printer with existing connection check.
     *
     * @throws IOException
     */
    public void reconnect() throws IOException, InterruptedException {
        if (isConnected()) {
            sock.close();
        }
        connect();
    }

    /**
     * Disconnect from the printer.
     * @throws IOException
     */
    public void disconnect() throws IOException, InterruptedException {
        if (isConnected()) {
            Thread.sleep(WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS);
            sock.close();
        }
    }

    /**
     * Send reset request, required for initial printer initialization after
     * connect/reconnect. Without this operation, printer will not print nor
     * return any data.
     * Request: `10fffe01+000000000000000000000000`.
     * @throws IOException
     */
    public void reset() throws IOException {
        tellPrinterFromHex("10fffe01000000000000000000000000");
    }

    /**
     * Send bytes to the printer without response.
     * @param byteseq bytes data
     * @throws IOException
     */
    public void tellPrinter(byte[] byteseq) throws IOException {
        os.write(byteseq);
        os.flush();
    }

    /**
     * Send bytes to the printer without response.
     * @param hexData bytes data
     * @throws IOException
     */
    public void tellPrinterFromHex(String hexData) throws IOException {
        os.write(ByteHelper.hexStringToByteArray(hexData));
        os.flush();
    }

    /**
     * Send bytes to the printer with response.
     * @param byteseq
     * @return
     * @throws IOException
     */
    public byte[] askPrinter(byte[] byteseq) throws IOException, InterruptedException {
        tellPrinter(byteseq);
        Thread.sleep(WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS);
        return listenPrinter();
    }

    /**
     * Send bytes to the printer with response.
     * @param hexData
     * @return
     * @throws IOException
     */
    public String askPrinterFromHexToString(String hexData) throws IOException, InterruptedException {
        return ByteHelper.toStringAscii(askPrinter(ByteHelper.hexStringToByteArray(hexData)));
    }

    /**
     * Receive data from printer.
     * @return
     * @throws IOException
     */
    public byte[] listenPrinter() throws IOException {
        if (is.available()>=0) return is.readNBytes(is.available());
        else return new byte[0];
    }

    /**
     * Send list of bytes to the printer without response.
     * @param byteseq
     * @throws IOException
     */
    private void tellPrinterSeq(List<byte[]> byteseq) throws IOException {
        for (byte[] s : byteseq) {
            os.write(s);
        }
        os.flush();
    }

    /**
     * Send list of bytes to the printer with response.
     * @param byteseq
     * @return
     * @throws IOException
     */
    private byte[] askPrinterSeq(List<byte[]> byteseq) throws IOException, InterruptedException {
        for (byte[] s : byteseq) {
            os.write(s);
        }
        os.flush();
        Thread.sleep(WAIT_BETWEEN_REQUEST_AND_RESPONSE_MS);
        return listenPrinter();
    }

}
