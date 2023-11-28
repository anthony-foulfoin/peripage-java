# peripage-java
### Java project for printing on Peripage printers

**This project is a java port development of the [python version](https://github.com/bitrate16/peripage-python). 
It doesn't provide a CLI but a set of utility classes that can be used in other apps.**

## Features

* Printing text of any length encoded in ASCII
* Printing Images
* Printing page breaks using paper feed
* Requesting printer details (Serial Number, Name, Battery Level, Hardware Info and an option the meaning of which i don't know)
* Configuring print concentration (temperature)
* Changing printer serial number
* Configuring printer poweroff timeout
* Supported printers:
    * Peripage A6
    * Peripage A6+
    * Peripage A40
    * Peripage A40+

## Prerequisites

* Peripage A6/A6+/A40/A40+/e.t.c printer
* Java 21

## Installation

**Install from git clone**

```
./gradlew build
```

**Launch the main class**

```
./gradlew run
```

## Identify printer Bluetooth MAC address

**On linux:**

```
user@name:~$ hcitool scan
Scanning ..
00:15:83:15:bc:5f    PeriPage+BC5F
```

**On windows:**

You may use [BluetoothCL](https://www.nirsoft.net/utils/bluetoothcl.html)

```
PS E:\E\E> .\BluetoothCL.exe
BluetoothCL v1.07
Copyright (c) 2009 - 2014 Nir Sofer
Web Site: http://www.nirsoft.net

syntax:
BluetoothCL -timeout [seconds]

-timeout is optional parameter. The default value is 15 seconds.


Scanning bluetooth devices... please wait.

00:15:83:15:bc:5f    Imaging                         PeriPage+BC5F
```

## Usage

### Setup

**Instanciate a new service with the printer mac address and connect to it**

```
PeripagePrinterService printer = new PeripagePrinterService("04:7F:0E:B0:CA:57", PrinterType.A40p);
printer.connect();
```

### Print image example

```
BufferedImage image = ImageIO.read(new File("c:\\dev\\image.png"));
printer.printPaddedImage(image);
```

### Print text example

**Print some random text followed by newline and break for 100px**
```
printer.printASCII("testABC");
printer.printBreak(100);
printer.flushASCII();
```

### Print text example

**Print some random text followed by newline and break for 100px**
```
printer.printASCII("testABC");
printer.printBreak(100);
printer.flushASCII();
```

### Disconnect after usage

```
printer.disconnect();
```

## Recommendations

* Don't forget about concentration, this can make print brighter and better visible.
* Split long images into multiple print requests with cooldown time for printer (printer may overheat during a long print and will stop printing for a while. This will result in partial print loss because the internal buffer is about 250px height). For example, when you print [looooooooooooooooooooooooooooooongcat.jpg](http://lurkmore.so/images/9/91/Loooooooooooooooooooooooooooooooooooooooooongcat.JPG), split it into at least 20 pieces with 1-2 minutes delay because you will definetly loose something without cooling. Printer gets hot very fast. Yes, it was the first that i've printed.
* Be carefull when printing lots of black or using max concentration, as i said, printer heats up very fast.
* The picture printed at maximum concentration has the longest shelf life.
* Turn printer off then long press the power button till it becomes orange. Release the button and look at the another useless feature.
* Be aware of cats, they have paws 🐾

## Credits

* [Elias Weingärtner](https://github.com/eliasweingaertner) for initial work in reverse-engineering bluetooth protocol
* [bitrate16](https://github.com/bitrate16) for additional research and python module
* [henryleonard](https://github.com/henryleonard) for specs of A40 printer
* [anthony-foulfoin](https://github.com/anthony-foulfoin) for specs of A40+ printer
