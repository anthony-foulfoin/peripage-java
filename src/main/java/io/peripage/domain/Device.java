package io.peripage.domain;

import io.peripage.domain.PrinterType;
import io.peripage.helper.ByteHelper;
import io.peripage.service.TransportService;

import java.io.IOException;
import java.util.Arrays;

public class Device {

    private final PrinterType printerType;
    private final String mac;
    private final TransportService transportService;

    public Device(String mac, PrinterType printerType, TransportService transportService) {
        this.printerType = printerType;
        this.mac = mac;
        this.transportService = transportService;
    }

    /**
     * Query Unknown Property.
     * Request: `10ff20f0`.
     * Response: `bytes` with unknown property.
     * Example: Peripage A6+ returns `IP-300`.
     * @return
     * @throws IOException
     */
    public String getIP() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff20f0");
    }

    /**
     * Query device name.
     * Request: `10ff3011`.
     * Response: `bytes` with `device_name+two_bytes_of_mac`
     * Example: Peripage A6+ returns `PeriPage+DF7A`.
     * @return
     * @throws IOException
     */
    public String getName() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff3011");
    }

    /**
     * Query serial number.
     * Request: `10ff20f2`.
     * Response: `bytes` with serial number
     * Example: Peripage A6+ returns `A6491571121`.
     * @return
     * @throws IOException
     */
    public String getSerialNumber() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff20f2");
    }

    /**
     * Query device firmware version.
     * Request: `10ff20f1`.
     * Response: `bytes` with firmware version
     * Example: Peripage A6+ returns `V2.11_304dpi`.
     * @return
     * @throws IOException
     */
    public String getFirmware() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff20f1");
    }

    /**
     * Query device battery percentage.
     * Request: `10ff50f1`.
     * Response: `bytes[2]` with percentage. `bytes[2] = { 0, percentage }`
     * Example: Peripage A6+ returns `\\x00@` (equals to `bytes[2] = { 0, 64 }`).
     * @return
     * @throws IOException
     */
    public int getBattery() throws IOException, InterruptedException {
        byte[] response = this.transportService.askPrinter(ByteHelper.hexStringToByteArray("10ff50f1"));
        return response[1];
    }

    /**
     * Query device firmware version.
     * Request: `10ff20f1`.
     * Response: `bytes` with firmware version
     * Example: Peripage A6+ returns `V2.11_304dpi`.
     * @return
     * @throws IOException
     */
    public String getHardware() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff3010");
    }

    /**
     * Query device mac from device itself.
     * Request: `10ff3012`.
     * Response: `bytes` with mac address.
     * Example: Peripage A6+ returns `\\x00\\xF5\\x73\\x25\\xAC\\x9F_\\x00\\xF5\\x73\\x25\\xAC\\x9F_`
     * (equals to `00:F5:73:25:AC:9F`).
     * @return
     * @throws IOException
     */
    public String getMAC() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff3012");
    }

    /**
     * Query full device info.
     * Request: `10ff70f100`.
     * Response: `bytes` with fill info.
     * Example: Peripage A6+ returns `PeriPage+DF7A|00:F5:73:25:AC:9F|C5:12:81:19:2C:51|V2.11_304dpi|A6491571121|84`
     * (`name+mac_slice|device_mac|client_mac|firmware|serial_number|battery_percentage`).
     * WARNING:
     * This command has a side-effect causing the printed images getting
     * corrupted by shifting horisontally and adding a █ character to the
     * in-printer ASCII buffer.
     * @return
     * @throws IOException
     */
    public String getFull() throws IOException, InterruptedException {
        return this.transportService.askPrinterFromHexToString("10ff70f100");
    }

    /**
     * Set printing concentration level.
     *
     * Printer supports multiple temperature concentration modes that allow to
     * print darker or lighter images with the price of overheating. The more
     * concentration - the longer lasting image will be.
     *
     * Request: `10ff1000+bytes[1]:big_endian`.
     *
     * Arguments:
     * `concentration` - concentration value from range `(0, 1, 2)`
     * @param concentration
     * @param wait
     * @throws IOException
     */
    public void setConcentration(int concentration, boolean wait) throws IOException, InterruptedException {
        byte[] request;
        if (concentration <= 0) {
            request = ByteHelper.hexStringToByteArray("10ff100000");
        } else if (concentration == 1) {
            request = ByteHelper.hexStringToByteArray("10ff100001");
        } else {
            request = ByteHelper.hexStringToByteArray("10ff100002");
        }

        if (wait) {
            this.transportService.askPrinter(request);
        } else {
            this.transportService.tellPrinter(request);
        }
    }

    public PrinterType getPrinterType() {
        return printerType;
    }


    /**
     * WARNING UNTESTED AND DANGEROUS TO USE
     */

    /**
     * Set device serial number.
     *
     * Set a new device serial number explicitly. `serial_number` defines the
     * new serial number for the device. This serial number must be
     * ascii-encodable string that match the requirements of
     * `Printer.is_safe_ascii()` filter in order to work. Serial number string
     * is additionally filtered with `Printer.filter_ascii` if you haven't read
     * the previous sentence.
     *
     * Request: `10ff20f4+ascii_str+00`.
     *
     * Arguments:
     * * `serial_number` - serial number string that passes the
     * `Printer.is_safe_ascii()` check.
     * @param serial_number
     * @param wait
     * @throws IOException
     */
    private void setDeviceSerialNumber(String serial_number, boolean wait) throws IOException, InterruptedException {
        byte[] request = ByteHelper.hexStringToByteArray("10ff20f4" + ByteHelper.asciiToHex(serial_number) + "00");
        if (wait) {
            transportService.askPrinter(request);
        } else {
            transportService.tellPrinter(request);
        }
    }

    /**
     * Set device poweroff timeout.
     *
     *         Device standby mode is triggered by any action made with the device. It
     *         can be either a print task, battery lever query and anything else that
     *         envolves ask-answer communication. Power timeout defines the internal
     *         auto poweroff timeout of the device in minutes, up to `0xffff` minutes.
     *
     *         Request: `10ff12+bytes[2]:big_endian`.
     *
     *         Arguments:
     *         * `timeout` - new timeout value between `0` and `0xffff`, minutes
     * @param timeout
     * @param wait
     * @throws IOException
     */
    private void setPowerTimeout(int timeout, boolean wait) throws IOException, InterruptedException {
        timeout = Math.max(Math.min(0xfff0, timeout), 0x0001);
        byte[] request = ByteHelper.hexStringToByteArray("10ff12" + Arrays.toString(ByteHelper.intToBigEndianBytes(timeout, 2)));
        if (wait) {
            transportService.askPrinter(request);
        } else {
            transportService.tellPrinter(request);
        }
    }

}
