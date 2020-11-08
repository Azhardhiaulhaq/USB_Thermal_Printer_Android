package com.example.thermalprintertest;

import android.graphics.Bitmap;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Printer {

    public static final float INCH_TO_MM = 25.4f;

    private UsbDevice device = null;
    private UsbManager usbManager = null;
    private UsbInterface intf;
    private UsbEndpoint outEndpoint;
    private UsbDeviceConnection connection;
    private int nbrCharactersPerLine;
    private int printerDpi;
    private float printingWidthMM;
    private int printingWidthPx;
    private int charSizeWidthPx;

    public Printer(UsbManager usbManager, UsbDevice device){
        this.device = device;
        this.usbManager = usbManager;
        this.intf = this.getInterface();
        this.outEndpoint = this.getOutEndpoint();
        this.connection = this.getConnection();
        this.nbrCharactersPerLine = 32;
        this.printerDpi = 203;
        this.printingWidthMM = 48f;
        int printingWidthPx = this.mmToPx(this.printingWidthMM);
        this.printingWidthPx = printingWidthPx + (printingWidthPx % 8);

        this.charSizeWidthPx = printingWidthPx / this.nbrCharactersPerLine;
    }

    public int mmToPx(float printingWidthMM) {
        return Math.round(printingWidthMM * ((float) this.printerDpi) / Printer.INCH_TO_MM);
    }

    public UsbInterface getInterface(){
        return device.getInterface(0);
    }

    public UsbEndpoint getOutEndpoint(){
        UsbEndpoint end = null;
        for (int i=0; i < getInterface().getEndpointCount(); i++){
            if(getInterface().getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT){
                end = getInterface().getEndpoint(i);
            }
        }
        return end;
    }

    public UsbDeviceConnection getConnection(){
        return usbManager.openDevice(device);
    }

    public void print(byte[] bytes){
        connection.claimInterface(intf,true);
        connection.bulkTransfer(getOutEndpoint(),bytes,bytes.length,0);
    }

    public void printText(String text, byte[] textSize, byte[] textBold, byte[] textUnderline, int maxlength) throws UnsupportedEncodingException {
        byte[] bytes = text.getBytes("ISO-8859-1");
        maxlength = bytes.length;
        this.print(PrinterCommands.WESTERN_EUROPE_ENCODING);
        this.print(PrinterCommands.TEXT_SIZE_NORMAL);
        this.print(PrinterCommands.TEXT_WEIGHT_NORMAL);
        this.print(PrinterCommands.TEXT_UNDERLINE_OFF);
        if(textSize != null){
            this.print(textSize);
        }
        if(textBold != null){
            this.print(textBold);
        }
        if(textUnderline != null){
            this.print(textUnderline);
        }
        this.print(bytes);
    }

    public void printImage(Bitmap bitmap){
        this.printImage(this.bitmapToBytes(bitmap));
    }

    public void printImage(byte[] image){
        if(device != null) {
            try {
                this.print(image);
                Thread.sleep(PrinterCommands.TIME_BETWEEN_TWO_PRINT * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printBarcode(int barcodeType, String barcode, int heightPx){
        Boolean valid = false;
        if(device != null){
            int barcodeLength = 0;
            switch (barcodeType) {
                case PrinterCommands.BARCODE_UPCA:
                    barcodeLength = 11;
                    break;
                case PrinterCommands.BARCODE_UPCE:
                    barcodeLength = 6;
                    break;
                case PrinterCommands.BARCODE_EAN13:
                    barcodeLength = 12;
                    break;
                case PrinterCommands.BARCODE_EAN8:
                    barcodeLength = 7;
                    break;
            }

            if (barcodeLength > 0 && barcode.length() >= barcodeLength){
                barcode = barcode.substring(0, barcodeLength);
                try {
                    valid = true;
                    switch (barcodeType) {
                        case PrinterCommands.BARCODE_UPCE:
                            String firstChar = barcode.substring(0, 1);
                            if (!firstChar.equals("0") && !firstChar.equals("1")) {
                                barcode = "0" + barcode.substring(0, 5);
                            }
                            break;
                        case PrinterCommands.BARCODE_UPCA:
                        case PrinterCommands.BARCODE_EAN13:
                        case PrinterCommands.BARCODE_EAN8:
                            int stringBarcodeLength = barcode.length(), totalBarcodeKey = 0;
                            for (int i = 0; i < stringBarcodeLength; i++) {
                                int pos = stringBarcodeLength - 1 - i,
                                        intCode = Integer.parseInt(barcode.substring(pos, pos + 1), 10);
                                if (i % 2 == 0) {
                                    intCode = 3 * intCode;
                                }
                                totalBarcodeKey += intCode;
                            }

                            String barcodeKey = String.valueOf(10 - (totalBarcodeKey % 10));
                            if (barcodeKey.length() == 2) {
                                barcodeKey = "0";
                            }
                            barcode += barcodeKey;
                            break;
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            if (valid){
                barcodeLength = barcode.length();
                byte[] barcodeCommand = new byte[barcodeLength + 4];
                System.arraycopy(new byte[]{0x1d, 0x6b, (byte) barcodeType}, 0, barcodeCommand, 0, 3);
                try {
                    for (int i = 0; i < barcodeLength; i++) {
                        barcodeCommand[i + 3] = (byte) (Integer.parseInt(barcode.substring(i, i + 1), 10) + 48);
                    }

                    this.print(new byte[]{0x1d, 0x68, (byte) heightPx});
                    this.print(barcodeCommand);
                    Thread.sleep(PrinterCommands.TIME_BETWEEN_TWO_PRINT * 2);
                }  catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void printFormattedText(String text) throws UnsupportedEncodingException {
        if(device != null){
            PrinterTextParser textParser = new PrinterTextParser(this);
            PrinterTextParserLine[] linesParsed = textParser.setFormattedText(text).parse();

            for (PrinterTextParserLine line : linesParsed){
                PrinterTextParserColumn[] columns = line.getColumns();

                for (PrinterTextParserColumn column : columns){
                    PrinterTextParserElement[] elements = column.getElements();
                    for (PrinterTextParserElement element : elements){
                        if(element instanceof PrinterTextParserString){
                            PrinterTextParserString string = (PrinterTextParserString) element;
                            this.printText(string.getText(),string.getTextSize(),string.getTextBold(),string.getTextUnderline(),0);
                        } else if (element instanceof  PrinterTextParserImg){
                            PrinterTextParserImg img = (PrinterTextParserImg) element;
                            this.printImage(img.getImage());
                        } else if (element instanceof PrinterTextParserBarcode){
                            PrinterTextParserBarcode barcode = (PrinterTextParserBarcode) element;
                            this.printBarcode(barcode.getBarcodeType(), barcode.getCode(),barcode.getHeight());
                        }
                    }
                }
            }
            connection.releaseInterface(intf);
            connection.close();
        }
    }

    public int getNbrCharactersPerLine(){
        return this.nbrCharactersPerLine;
    }

    public int getPrintingWidthPx(){
        return printingWidthPx;
    }

    public int getCharSizeWidthPx(){
        return charSizeWidthPx;
    }

    public byte[] bitmapToBytes(Bitmap bitmap) {
        boolean isSizeEdit = false;
        int bitmapWidth = bitmap.getWidth(),
                bitmapHeight = bitmap.getHeight(),
                maxWidth = this.getPrintingWidthPx(),
                maxHeight = 256;

        if (bitmapWidth > maxWidth) {
            bitmapHeight = Math.round(((float) bitmapHeight) * ((float) maxWidth) / ((float) bitmapWidth));
            bitmapWidth = maxWidth;
            isSizeEdit = true;
        }
        if (bitmapHeight > maxHeight) {
            bitmapWidth = Math.round(((float) bitmapWidth) * ((float) maxHeight) / ((float) bitmapHeight));
            bitmapHeight = maxHeight;
            isSizeEdit = true;
        }

        if (isSizeEdit) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
        }

        return this.parseBitmap(bitmap);
    }

    public byte[] parseBitmap(Bitmap bitmap){
        int bitmapWidth = bitmap.getWidth(),
                bitmapHeight = bitmap.getHeight();

        int bytesByLine = (int) Math.ceil(((float) bitmapWidth) / 8f);

        byte[] imageBytes = new byte[8 + bytesByLine * bitmapHeight];
        System.arraycopy(new byte[]{0x1D, 0x76, 0x30, 0x00, (byte) bytesByLine, 0x00, (byte) bitmapHeight, 0x00}, 0, imageBytes, 0, 8);

        int i = 8;
        for (int posY = 0; posY < bitmapHeight; posY++) {
            for (int j = 0; j < bitmapWidth; j += 8) {
                StringBuilder stringBinary = new StringBuilder();
                for (int k = 0; k < 8; k++) {
                    int posX = j + k;
                    if (posX < bitmapWidth) {
                        int color = bitmap.getPixel(posX, posY),
                                r = (color >> 16) & 0xff,
                                g = (color >> 8) & 0xff,
                                b = color & 0xff;

                        if (r > 160 && g > 160 && b > 160) {
                            stringBinary.append("0");
                        } else {
                            stringBinary.append("1");
                        }
                    } else {
                        stringBinary.append("0");
                    }
                }
                imageBytes[i++] = (byte) Integer.parseInt(stringBinary.toString(), 2);
            }
        }

        return imageBytes;
    }
}
