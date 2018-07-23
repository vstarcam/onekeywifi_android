package com.mediatek.demo.smartconnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import voice.encoder.VoicePlayer;
import voice.encoder.DataEncoder;
import android.widget.Toast;
import android.net.wifi.ScanResult;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private VoicePlayer player = new VoicePlayer();
    private Button mStartButton;
    private Button mStopButton;
    private ProgressBar mProgressBar;
    
    private EditText mNameEdit;
    private EditText mPswEdit;
    private EditText mCustomInfoEdit;
    private EditText mEncryptionKeyEdit;
    
    private EditText mV1V4IntervalEdit;
    private EditText mV5IntervalEdit;

    private TextView mWatcher;
    
    private CheckBox mV1CheckBox;
    private CheckBox mV4CheckBox;
    private CheckBox mV5CheckBox;
    
    private Context mContext;
    private String sendMac = null;
    private String wifiName;
    private String currentBssid;

    private JniLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mStartButton = (Button)this.findViewById(R.id.btn_start);
        mStopButton = (Button)this.findViewById(R.id.btn_stop);
        mProgressBar = (ProgressBar)this.findViewById(R.id.sending_indicator);
        
        mNameEdit = (EditText)this.findViewById(R.id.et_name);    //name
        mPswEdit = (EditText)this.findViewById(R.id.et_psw);  //pwd
        mCustomInfoEdit = (EditText)this.findViewById(R.id.et_custom_info); //custom
        mEncryptionKeyEdit = (EditText)this.findViewById(R.id.et_encryption_key); //encryption
        
        mV1V4IntervalEdit = (EditText)this.findViewById(R.id.et_v1v4_interval);
        mV5IntervalEdit = (EditText)this.findViewById(R.id.et_v5_interval);
        mWatcher = (TextView)this.findViewById(R.id.watcher);
        mV1CheckBox = (CheckBox)this.findViewById(R.id.checkbox_v1);
        mV4CheckBox = (CheckBox)this.findViewById(R.id.checkbox_v4);
        mV5CheckBox = (CheckBox)this.findViewById(R.id.checkbox_v5);

        mContext = this;
        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);
        getWifi();

        boolean res = JniLoader.LoadLib();
        Log.e("SmartConnection", "Load Smart Connection Library Result ：" + res);
        loader = new JniLoader();
        int proV = loader.GetProtoVersion();
        Log.e("SmartConnection", "proV ：" + proV);
        int libV = loader.GetLibVersion();
        Log.e("SmartConnection", "libV ：" + libV);
        String version = "V" + proV + "." + libV;
        
        this.getActionBar().setTitle("SmartConnection (" + version + ")");

        mV1CheckBox.setChecked(false);
        mV4CheckBox.setChecked(false);
        mV5CheckBox.setChecked(false);
        
        mNameEdit.addTextChangedListener(new EditChangeHandler(1));
        mPswEdit.addTextChangedListener(new EditChangeHandler(2));
        mCustomInfoEdit.addTextChangedListener(new EditChangeHandler(3));
        
        WifiManager wManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wManager.getConnectionInfo();
        Log.e("SmartConnection", "start info=" + info.getSSID());
        if (info.getSSID().isEmpty() == false) {
            String name = info.getSSID();
            String ssid = name.substring(1, name.length() - 1);
            mNameEdit.setText(ssid);
            mNameEdit.setSelection(mNameEdit.getText().length());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int sendV1 = 1;
                int sendV4 = 1;
                int sendV5 = 1;
                
                float oI = 0.0f;
                float nI = 0.0f;
                if (mV1V4IntervalEdit.getText().toString().isEmpty() == false) {
                    oI = Float.parseFloat(mV1V4IntervalEdit.getText().toString());
                    Log.e("SmartConnection", "start oI=" + oI);
                }
                if (mV5IntervalEdit.getText().toString().isEmpty() == false) {
                    nI = Float.parseFloat(mV5IntervalEdit.getText().toString());
                    Log.e("SmartConnection", "start nI=" + nI);
                }
                
                if (sendV1 == 0 && sendV4 == 0 && sendV5 == 0) {
                    showWarningDialog(R.string.error_select_leat_1_version);
                    return;
                }
                
                int retValue = JniLoader.ERROR_CODE_OK;
                String key = mEncryptionKeyEdit.getText().toString();
                String mac="0xff 0xff 0xff 0xff 0xff 0xff";
                String mac1="";
                if(key==null){
                    Log.e("SmartConnection", "init Smart key is null");
                }else{
                    Log.e("SmartConnection", "init Smart key-len="+key.length()+", key-emp="+key.isEmpty());
                }
                Log.e("SmartConnection", "init Smart key=" + key+", sendV1="+sendV1+", sendV4="+sendV4+", sendV5="+sendV5);
                retValue = loader.InitSmartConnection(key,mac,sendV1, sendV4, sendV5);
                Log.e("SmartConnection", "init return retValue=" + retValue);
                if (retValue != JniLoader.ERROR_CODE_OK) {
                    showWarningDialog(R.string.init_failed);
                    return;
                }

                Log.e("SmartConnection", "Send Smart oI=" + oI+", nI="+nI);
                loader.SetSendInterval(oI, nI);

                String SSID = mNameEdit.getText().toString();
                String Password = mPswEdit.getText().toString();
                String Custom = mCustomInfoEdit.getText().toString();
                if(Custom==null){
                    Log.e("SmartConnection", "Start Smart Custom is null");
                }else{
                    Log.e("SmartConnection", "Start Smart Custom-len="+Custom.length()+", Custom-emp="+Custom.isEmpty());
                }
                Log.e("SmartConnection", "Start Smart SSID=" + SSID + ", Password=" + Password + ", Custom=" + Custom);
                //retValue = loader.StartSmartConnection(SSID, Password, Custom);
                sendSonic(sendMac,Password.toString());
                Log.e("SmartConnection", "start return retValue=" + retValue);
                if (retValue != JniLoader.ERROR_CODE_OK) {
                    showWarningDialog(R.string.start_failed);
                    return;
                }
                
                updateUIWhileSending();
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                
                int retValue = loader.StopSmartConnection();
                Log.e("SmartConnection", "Stop return failed : " + retValue);
                if (retValue != JniLoader.ERROR_CODE_OK) {
                    showWarningDialog(R.string.stop_failed);
                }
                updateUIWhileStopped();
                player.stop();
            }
        });
    }

    private static byte uniteBytes(byte src0, byte src1)
    {
        byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 })).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 })).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    private static byte[] HexString2Bytes(String src)
    {
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < src.length() / 2; i++)
        {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    private static void printHexString(byte[] b) {
        // System.out.print(hint);
        for (int i = 0; i < b.length; i++)
        {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            System.out.print("aaa" + hex.toUpperCase() + " ");
        }
        System.out.println("");
    }


    private void getWifi()
    {
        WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        wifiName = wifiInfo.getSSID().toString();
        if (wifiName.length() > 2 && wifiName.charAt(0) == '"'
                && wifiName.charAt(wifiName.length() - 1) == '"') {
            wifiName = wifiName.substring(1, wifiName.length() - 1);
        }

        List<ScanResult> wifiList = wifiMan.getScanResults();
        ArrayList<String> mList = new ArrayList<String>();
        mList.clear();

        for (int i = 0; i < wifiList.size(); i++)
        {
            mList.add((wifiList.get(i).BSSID).toString());

        }

        currentBssid = wifiInfo.getBSSID();
        if (currentBssid == null)
        {
            for (int i = 0; i < wifiList.size(); i++) {
                if ((wifiList.get(i).SSID).toString().equals(wifiName))
                {
                    currentBssid = (wifiList.get(i).BSSID).toString();
                    break;
                }
            }
        }
        else {
            if (currentBssid.equals("00:00:00:00:00:00")
                    || currentBssid.equals("")) {
                for (int i = 0; i < wifiList.size(); i++)
                {
                    if ((wifiList.get(i).SSID).toString().equals(wifiName)) {
                        currentBssid = (wifiList.get(i).BSSID).toString();
                        break;
                    }
                }
            }
        }
        if (currentBssid == null)
        {
            finish();
        }

        String tomacaddress[] = currentBssid.split(":");
        int currentLen = currentBssid.split(":").length;

        for (int m = currentLen - 1; m > -1; m--)
        {
            for (int j = mList.size() - 1; j > -1; j--)
            {
                if (!currentBssid.equals(mList.get(j)))
                {
                    String array[] = mList.get(j).split(":");
                    if (!tomacaddress[m].equals(array[m])) {
                        mList.remove(j);//
                    }
                }
            }
            if (mList.size() == 1 || mList.size() == 0) {
                if (m == 5) {
                    sendMac = tomacaddress[m].toString();
                } else if (m == 4) {
                    sendMac = tomacaddress[m].toString()
                            + tomacaddress[m + 1].toString();
                } else {
                    sendMac = tomacaddress[5].toString()
                            + tomacaddress[4].toString()
                            + tomacaddress[3].toString();
                }
                break;
            }
        }
    }

    private  void  sendSonic(String mac, final String wifi)
    {
        byte[] midbytes = null;

        try {
            midbytes = HexString2Bytes(mac);
            printHexString(midbytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (midbytes.length > 6)
        {
            Toast.makeText(MainActivity.this, "no support",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] b = null;
        int num = 0;
        if (midbytes.length == 2) {
            b = new byte[] { midbytes[0], midbytes[1] };
            num = 2;
        } else if (midbytes.length == 3) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2] };
            num = 3;
        } else if (midbytes.length == 4) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2], midbytes[3] };
            num = 4;
        } else if (midbytes.length == 5) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                    midbytes[3], midbytes[4] };
            num = 5;
        } else if (midbytes.length == 6) {
            b = new byte[] { midbytes[0], midbytes[1], midbytes[2],
                    midbytes[3], midbytes[4], midbytes[5] };
            num = 6;
        } else if (midbytes.length == 1) {
            b = new byte[] { midbytes[0] };
            num = 1;
        }

        int a[] = new int[19];
        a[0] = 6500;
        int i, j;
        for (i = 0; i < 18; i++)
        {
            a[i + 1] = a[i] + 200;
        }

        player.setFreqs(a);

        player.play(DataEncoder.encodeMacWiFi(b, wifi.trim()), 5, 1000);

    }

    @Override
    protected void onStop() {

        super.onStop();
        if (player != null)
        {
            player.stop();
        }
    }
    
    private void updateUIWhileSending() {
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(true);
        mProgressBar.setVisibility(View.VISIBLE);
        
        mV1CheckBox.setEnabled(false);
        mV4CheckBox.setEnabled(false);
        mV5CheckBox.setEnabled(false);
        
        mNameEdit.setEnabled(false);
        mPswEdit.setEnabled(false);
        
        this.mV1V4IntervalEdit.setEnabled(false);
        this.mV5IntervalEdit.setEnabled(false);
        mCustomInfoEdit.setEnabled(false);
    }
    
    private void updateUIWhileStopped() {
        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);
        mProgressBar.setVisibility(View.INVISIBLE);
        
        mV1CheckBox.setEnabled(true);
        mV4CheckBox.setEnabled(true);
        mV5CheckBox.setEnabled(true);
        
        mNameEdit.setEnabled(true);
        mPswEdit.setEnabled(true);
        this.mV1V4IntervalEdit.setEnabled(true);
        this.mV5IntervalEdit.setEnabled(true);
        mCustomInfoEdit.setEnabled(true);
    }
    
    private void showWarningDialog(final int stringID) {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.warning);
                builder.setMessage(stringID);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });
                builder.show();
            }
            
        });
    }

    private class EditChangeHandler implements TextWatcher {

        private int which;
        
        public EditChangeHandler(int which) {
            this.which = which;
        }
        
        @Override
        public void afterTextChanged(Editable arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                int arg3) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                int arg3) {
            // TODO Auto-generated method stub
            String str = arg0.toString();
            boolean hid = false;
            
            switch(which) {
            case 1:
                if (str.getBytes().length > 32) {
                    mWatcher.setText("SSID Exceed Max Length (32)");
                } else {
                    hid = true;
                }
                break;
            case 2:
                if (str.getBytes().length > 64) {
                    mWatcher.setText("Password Exceed Max Length (64)");
                } else {
                    hid = true;
                }
                break;
            case 3:
                if (str.getBytes().length > 640) {
                    mWatcher.setText("CustomInfo Exceed Max Length (640)");
                } else {
                    hid = true;
                }
                break;
            }
            if (hid == true) {
                mWatcher.setVisibility(View.GONE);
            } else {
                mWatcher.setVisibility(View.VISIBLE);
            }
        }
        
    }
    
}
