package com.example.thermalprintertest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.example.thermalprintertest.USB_PERMISSION";
    private static final String TAG = "MainActivity";
    private TextView StatusTV;
    UsbDevice device = null;
    UsbManager usbManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getUsbManager();
        getPrinterDevice();
        setContentView(R.layout.activity_main);
        StatusTV = findViewById(R.id.statusTV);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
                                         new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver,filter);
        usbManager.requestPermission(device,permissionIntent);
    }


    @Override
    protected void onStart() {
        super.onStart();
        TextView tv = findViewById(R.id.deviceTextView);
        displayPrinters(tv);

    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver(){

        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)){
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,false)){
                        if (device != null){
                            Log.d(TAG, "Permission Succeed & Device Exist");
                            StatusTV.setText("Permission Succeed & Device Exist");
                        } else {
                            Log.d(TAG, "Permission Succeed & Device doesnt Exist");
                            StatusTV.setText("Permission Succeed & Device doesn't Exist");
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                        StatusTV.setText("Permission denied");
                    }
                }
            }
        }
    };
    /**
     * displayPrinters
     * @param tv
     */
    void displayPrinters(TextView tv) {
        if (device == null){
            tv.setText("No Device found");
        } else {
            tv.setText("ProductName" + device.getProductName());
        }
    }

    public void getPrinterDevice(){
        Boolean found = false;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext() && !found){
            device = deviceIterator.next();
            String deviceProduct = device.getProductName().toLowerCase();
            if (deviceProduct.contains("printer")){
                found = true;
            }
        }
    }

    public void getUsbManager(){
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    public void printExample(View view) throws UnsupportedEncodingException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date  = new Date();
        byte[] bytes;
        bytes = new byte[]{0x1b,0x40,0x48,0x65,0x6c,0x6c,0x6f,0x20,0x77,0x6f,0x72,0x6c,0x64,0x0a,0x1d,0x56,0x41,0x03,0x10};
        Printer usbPrinter = new Printer(usbManager,device);
//        for(int i=0; i<9; i++){
//            usbPrinter.printText("Hello World",null,null,null,0);
//        }
        usbPrinter.printFormattedText(
                "[C]<img>"+ PrinterTextParserImg.bitmapToHexadecimalString(usbPrinter,
                        this.getApplicationContext().getResources().getDrawableForDensity(
                                R.drawable.talittalogo, DisplayMetrics.DENSITY_MEDIUM))+"</img>\n" +
                "[C]<font size='small'>Mutiara Citra Graha I1/2,Candi,Sidoarjo</font>" +
                "[L]\n"+
                "[C]<font size='small'>41271</font>"+
                "[L]\n"+
                "[L]\n"+
                "[L]" + formatter.format(date) +
                "[L]\n"+
                "[L]Cashier : Rani" +
                "[L]\n"+
                "[C]================================" +
                "[L]\n"+
                "[L]\n" +
                "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99e\n" +
                "[L]  + Size : S\n" +
                "[L]\n" +
                "[L]<b>AWESOME HAT</b>[R]24.99e\n" +
                "[L]  + Size : 57/58\n" +
                "[L]\n" +
                "[C]--------------------------------\n" +
                "[L][R]TOTAL PRICE :[R]34.98e\n" +
                "[L][R]TAX :[R]4.23e\n" +
                "[L]\n" +
                "[C]================================\n" +
                "[L]\n");
    }
}
