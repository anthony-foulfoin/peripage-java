package io.peripage;

import io.peripage.domain.Device;
import io.peripage.domain.PrinterType;
import io.peripage.service.PrinterService;

import java.io.IOException;

public class Main {
    public static void main(String... args) throws IOException, InterruptedException {

        PrinterService printer = new PrinterService("04:7F:0E:B0:CA:57", PrinterType.A40p);
        printer.connect();

        Device device = printer.getDevice();

//        System.out.println(device.getIP());
//        System.out.println(device.getName());
//        System.out.println(device.getSerialNumber());
//        System.out.println(device.getFirmware());
//        System.out.println(device.getBattery());
//        System.out.println(device.getHardware());
//        System.out.println(device.getMAC());
//        System.out.println(device.getFull());

        printer.printQR("https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        printer.disconnect();
    }
}
