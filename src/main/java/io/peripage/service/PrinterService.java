package io.peripage.service;

import com.google.common.primitives.Bytes;
import io.peripage.domain.Device;
import io.peripage.domain.PrinterType;
import io.peripage.helper.ByteHelper;
import io.peripage.helper.ImageHelper;
import net.glxn.qrgen.QRCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.peripage.helper.ByteHelper.hexStringToByteArray;
import static io.peripage.helper.ByteHelper.intToBigEndianBytes;

/**
 * This class defines the Peripage interface utility.
 * Currently, there is no thermal overheat protection opcodes found, so
 * use printing carefully and avoid overheating of the printer which
 * may result in hardware break.
 * Currently, there is no stop codes found, so you can not stop printing.
 */
public class PrinterService {

    public static final int DEFAULT_SLEEP_TIMEOUT = 250;
    private final Device device;
    private final TransportService transportService;
    private final PrinterType printerType;

    private String printBuffer = "";

    /**
     * Create a new PeripagePrinterService instance.
     * @param mac MAC address of the printer in format `xx:xx:xx:xx:xx:xx`
     * @param printerType Printer type
     */
    public PrinterService(String mac, PrinterType printerType) {
        this.printerType = printerType;
        this.transportService = new TransportService(mac);
        this.device = new Device(mac, printerType, transportService);
    }

    /**
     * Connect to the printer.
     * @throws IOException If the printer is not found
     * @throws InterruptedException If timeout could not be completed
     */
    public void connect() throws IOException, InterruptedException {
        this.transportService.connect();
    }

    /**
     * Disconnect from the printer.
     * @throws IOException If the printer is not found
     * @throws InterruptedException If timeout could not be completed
     */
    public void disconnect() throws IOException, InterruptedException {
        this.transportService.disconnect();
    }

    /**
     * Get the device information.
     * @return The device information
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Ask printer to print out a break of fixed size.
     * Printer allows user to feed out some paper to wipe away tears of this module developer.
     * Request: `1b4a+bytes[1]:big_endian`.
     * @param size break size in range `(0, 0xff)`
     * @throws IOException
     */
    public void printBreak(int size) throws IOException {
        size = Math.min(0xff, Math.max(0x01, size));
        byte[] request = Bytes.concat(hexStringToByteArray("1b4a"), intToBigEndianBytes(size));
        this.transportService.tellPrinter(request);
    }

    /**
     * Safe to use printing method that relies on in-class buffer for wrapping
     * text. The input is filtered with `Printer.filter_ascii` in order to
     * exclude all non-safe-ascii characters and later split into multiple
     * chunks over `\\n` in order to prevent freeze caused by twice-newline in
     * printer buffer. This function is equal to  normal `println` in C and
     * semi-equal to `print(text + '\\n')`. This method relies on in-class
     * buffer to track printed data and keeping sync with in-printer buffer.
     *
     * @param text text to be printed, automatically filtered with filterAscii
     */
    public void printASCII(String text) throws IOException, InterruptedException {
        text = filterAscii(text);

        // Check for empty and print out newline
        text = printBuffer + text;
        printBuffer = "";
        if (text.isEmpty()) {
            return;
        }

        // Special case: \n only, causes duplicating newlines (white-only string)
        if (text.isBlank()) {
            for (char s : text.toCharArray()) {
                if (s == '\n') {
                    printBreak(30);
                    Thread.sleep(DEFAULT_SLEEP_TIMEOUT);
                }
            }
            return;
        }

        // Iterate over lines
        String[] lines = text.split("\\n");
        for (String l : lines) {

            // Flush previous incomplete line
            if (!printBuffer.isEmpty()) {
                this.transportService.tellPrinter(printBuffer.getBytes(StandardCharsets.US_ASCII));
                this.transportService.tellPrinter("\n".getBytes(StandardCharsets.US_ASCII));
                printBuffer = "";
                Thread.sleep(DEFAULT_SLEEP_TIMEOUT);
            }

            // Flush if white-empty, because it is newline
            else if (l.isBlank()) {
                printBreak(30);
                Thread.sleep(DEFAULT_SLEEP_TIMEOUT);
            } else {
                // Wrap line
                List<String> parts = new ArrayList<>();
                for (int i = 0; i < l.length(); i += this.device.getPrinterType().getRowCharacters()) {
                    parts.add(l.substring(i, Math.min(i + this.device.getPrinterType().getRowCharacters(), l.length())));
                }

                for (String p : parts) {
                    // Print full line
                    if (p.length() == this.device.getPrinterType().getRowCharacters()) {
                        this.transportService.tellPrinter(p.getBytes(StandardCharsets.US_ASCII));
                        this.transportService.tellPrinter("\n".getBytes(StandardCharsets.US_ASCII));
                        Thread.sleep(DEFAULT_SLEEP_TIMEOUT);
                    // Partial, write to buffer
                    } else {
                        printBuffer = p;
                    }
                }
            }
        }
    }

    /**
     * This Java code takes a BufferedImage, converts it to black and white 1-bit image, resizes it to fit the printer.
     * It then extracts the byte data from the image, divides it into rows, and prints each row in the center of the paper.
     * @param img The image to print
     * @throws IOException If the image could not be read
     */
    public void printPaddedImage(BufferedImage img) throws IOException, InterruptedException {
        img = ImageHelper.convertToReversedBlackAndWhite(img, this.getRowWidth());
        img = ImageHelper.centerPadImage(img, this.getRowWidth());
        byte[] imgBytes = ImageHelper.getRawImageData(img);
        printImageBytes(imgBytes);
    }

    /**
     * Print a QR code.
     * @param text The text to encode
     * @throws IOException
     * @throws InterruptedException
     */
    public void printQR(String text) throws IOException, InterruptedException {
        this.printQR(text, 500);
    }

    /**
     * Print a QR code.
     * @param text The text to encode
     * @param size The size of the qrcocde
     * @throws IOException
     * @throws InterruptedException
     */
    public void printQR(String text, int size) throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        QRCode.from(text).withSize(size, size).writeTo(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        this.printPaddedImage(ImageIO.read(bais));
    }

    /**
     * Safe to use printing method that relies on in-class buffer for wrapping
     * text. The input is filtered with {@link #filterAscii(String)} filterAscii} in order to
     * exclude all non-safe-ascii characters and later splitted into multiple
     * chunks over `\\n` in order to prevent freeze caused by twice-newline in
     * printer buffer. This function is equal to  normal `println` in C and
     * semi-equal to `print(text + '\\n')`. This method relies on in-class
     * buffer to track printed data and keeping sync with in-printer buffer.
     *
     * @param text text to be printed, automatically filtered with {@link #filterAscii(String)} filterAscii}
     * @throws IOException
     */
    public void printlnASCII(String text) throws IOException, InterruptedException {
        printASCII(text + "\n");
    }

    /**
     * Force print out buffer if it is not empty.
     * @throws IOException
     */
    public void flushASCII() throws IOException, InterruptedException {
        if (!printBuffer.isEmpty()) {
            this.transportService.tellPrinter(printBuffer.getBytes(StandardCharsets.US_ASCII));
            this.transportService.tellPrinter("\n".getBytes());
            printBuffer = "";
            Thread.sleep(DEFAULT_SLEEP_TIMEOUT);
        }
    }

    /**
     * Send bytes representing a single image row in binary black/white mode.
     * If amount of bytes exceeds the `Printer.getRowBytes()` constant, input
     * is truncated. If size of input is under the `Printer.getRowBytes()`, it will be padded with zeros.
     *
     * Request: `1d763000+bytes[2]:big_endian+0100+bytes[Printer.getRowBytes()*1]`.
     *
     * Note: In case of A6+, preamble is `1d76300048000100` that can be viewed
     * as `[ 1d7630, 0030, 0001 ]`, where `1d7630` is printing operation
     * request, `0030` is big endian bytes per row, `0001` is big endian input
     * height.
     *
     * @param rowBytes bytes representing image pixels, 8 pixels per byte,
     */
    protected void printRow(byte[] rowBytes) throws IOException, InterruptedException {
        int expectedLen = this.getRowBytes();

        byte[] paddedRowBytes = ByteHelper.padRowBytes(expectedLen, rowBytes);

        this.transportService.reset();

        byte[] request = Bytes.concat(hexStringToByteArray("1d763000"), intToBigEndianBytes(expectedLen), hexStringToByteArray("000100"), paddedRowBytes);

        this.transportService.tellPrinter(request);
        Thread.sleep(10);
    }

    /**
     * Send an array of bytes representing a multiple image rows in binary
     * black/white mode. If amount of bytes per row exceeds the
     * `Printer.getRowBytes()` constant, input is truncated. If size of input
     * is under the `Printer.getRowBytes()`, it will be padded with zeros.
     *
     * This printer supports pages up to `0xffff` rows, but current
     * implementation relies on chunked data with height limit of `0xff` and
     * automatically slices the input into chunks.
     *
     * Note: In case of A6+, preamble is `1d76300048000100` that can be viewed
     * as `[ 1d7630, 0030, 0001 ]`, where `1d7630` is printing operation
     * request, `0030` is big endian bytes per row, `0001` is big endian input
     * height.
     *
     * Request: chunked `1d763000+bytes[1]:big_endian+00+bytes[1]:big_endian+00+bytes[Printer.getRowBytes()*chunk_height]`.
     *
     * @param rowBytesList list of bytes defining each row of the image. If row length does not match the `Printer.getRowBytes()`, data is truncated/padded to match the size.
     * @throws IOException
     * @throws InterruptedException
     */
    public void printRowBytesList(List<byte[]> rowBytesList) throws IOException, InterruptedException {
        if (rowBytesList.isEmpty()) {
            return;
        }

        int expectedLen = this.getRowBytes();
        List<List<byte[]>> chunks = new ArrayList<>();

        for (int i = 0; i < rowBytesList.size(); i += 0xff) {
            chunks.add(rowBytesList.subList(i, Math.min(i + 0xff, rowBytesList.size())));
        }

        for (List<byte[]> chunk : chunks) {
            transportService.reset();

            byte[] request = Bytes.concat(hexStringToByteArray("1d763000"), intToBigEndianBytes(expectedLen), hexStringToByteArray("00"), intToBigEndianBytes(chunk.size()), hexStringToByteArray("00"));

            this.transportService.tellPrinter(request);

            for (byte[] rowBytes : chunk) {
                byte[] paddedRowBytes = ByteHelper.padRowBytes(expectedLen, rowBytes);
                this.transportService.tellPrinter(paddedRowBytes);
                Thread.sleep(10);
            }
        }
    }

    /**
     * Send a bytes representing single-line encoded image.
     * For example,
     * `[0xff000000, 0x00ff0000, 0x0000ff00, 0x000000ff]` is encoded as `0xff00000000ff00000000ff00000000ff`.
     *
     * Image must be validly aligned and sequence size must divide by `Printer.getRowBytes()`.
     * In the case of partial data, the rest of partial
     * data is padded with zeros.
     * Number of lines is calculated as
     * `nlines = ceil(imagebytes.length) / Printer.getRowBytes())`.
     *
     * Each row must be aligned to `Printer.getRowBytes()` in order to display properly.
     * If length of the last row do not match `Printer.getRowBytes()`, data is truncated/padded to match the size.
     *
     * @param imagebytes bytes defining concatenated rows of the image.
     * @throws IOException
     * @throws InterruptedException
     */
    protected void printImageBytes(byte[] imagebytes) throws IOException, InterruptedException {
        if (imagebytes.length == 0) {
            return;
        }

        List<byte[]> rowChunks = new ArrayList<>();
        int rowBytes = this.getRowBytes();

        for (int i = 0; i < imagebytes.length; i += rowBytes) {
            int endIndex = Math.min(i + rowBytes, imagebytes.length);
            byte[] row = Arrays.copyOfRange(imagebytes, i, endIndex);
            rowChunks.add(row);
        }

        printRowBytesList(rowChunks);
    }

    /**
     * WARNING: THIS API IS UNSAFE
     *
     * Write text into printer without internal safety-checks and filtering. If
     * you want to print text with internal checks for non-ascii or
     * unsafe-ascii characters, use `Printer.printASCII()`. If you need to use
     * this function, check you text with `Printer.is_safe_ascii` or filter
     * with `Printer.filter_ascii` and do not leave more than one sequential
     * `\\n` character.
     *
     * Request: `ascii_str`.
     *
     * Arguments:
     * * `text` - text to be printed, should be checked by user before print or
     * may malfunction and/or damage the printer. String must not contain
     * repeating `\\n` characters or printer will freeze.
     * @param text
     * @param wait
     * @throws IOException
     */
    protected void writeASCII(String text, boolean wait) throws IOException, InterruptedException {
        byte[] request = text.getBytes(StandardCharsets.US_ASCII);
        if (wait) {
            this.transportService.askPrinter(request);
        } else {
            this.transportService.tellPrinter(request);
        }
    }

    /**
     * Returns a String with all non-ascii characters removed or converted
     * @param text The text to filter
     * @return The filtered text
     */
    protected String filterAscii(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("[^\\x00-\\x7F]", "");
    }

    protected TransportService getCommunication() {
        return transportService;
    }

    private int getRowCharacters() {
        return this.printerType.getRowCharacters();
    }

    private int getRowWidth() {
        return this.printerType.getRowWidth();
    }

    private int getRowBytes() {
        return this.printerType.getRowBytes();
    }

}
