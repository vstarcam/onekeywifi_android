package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import mediatek.android.IoTManager.IoTManagerNative;


import com.example.sounddemo.R;

import voice.encoder.DataEncoder;
import voice.encoder.VoicePlayer;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener{

	//����
	private VoicePlayer player = new VoicePlayer();
	
	//�ؼ�
	private EditText wifi_name,wifi_pwd;
	private Button sure,cancel;
	
	private String sendMac = null;
	private String wifiName;
	private String currentBssid;
	
	private IoTManagerNative IoTManager;
	private WifiManager mWifiManager;

	private String mConnectedSsid = "";
	private String mConnectedPassword;
	private String mAuthString;
	private byte mAuthMode;
	private byte AuthModeOpen = 0x00;
	private byte AuthModeShared = 0x01;
	private byte AuthModeAutoSwitch = 0x02;
	private byte AuthModeWPA = 0x03;
	private byte AuthModeWPAPSK = 0x04;
	private byte AuthModeWPANone = 0x05;
	private byte AuthModeWPA2 = 0x06;
	private byte AuthModeWPA2PSK = 0x07;
	private byte AuthModeWPA1WPA2 = 0x08;
	private byte AuthModeWPA1PSKWPA2PSK = 0x09;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWifi();
        IoTManager = new IoTManagerNative();
		IoTManager.InitSmartConnection();
        findView();
    }
    private void findView()
    {
    	wifi_name=(EditText) findViewById(R.id.wifi_name);
    	wifi_pwd=(EditText) findViewById(R.id.wifi_pwd);
    	if(wifiName!=null)
    	{
    		Log.e("wifiname", wifiName);
    		wifi_name.setText(wifiName);
    	}
    	sure=(Button) findViewById(R.id.sure);
    	cancel=(Button) findViewById(R.id.cancel);
    	sure.setOnClickListener(this);
    	cancel.setOnClickListener(this);
    }
    
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (player != null)
		{
			player.stop();
		}	
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.sure:
			if(wifi_pwd.getText().toString()==null||wifi_pwd.getText().toString().equalsIgnoreCase(""))
			{
				Toast.makeText(MainActivity.this, "������wifi����", Toast.LENGTH_LONG).show();
				return;
			}
			Log.e("��������", sendMac+"_______"+wifi_pwd.getText().toString());
			sendSonic(sendMac,wifi_pwd.getText().toString());
			setSmartLink();
			if (!mConnectedSsid.equals("")) {	
				IoTManager.StartSmartConnection(mConnectedSsid, wifi_pwd.getText().toString().trim(), "FF:FF:FF:FF:FF:FF",(byte) mAuthMode);
			}
			break;
        case R.id.cancel:
        	player.stop();
        	if (IoTManager != null) 
        	{
    			IoTManager.StopSmartConnection();
    		}
			break;
		default:
			break;
		}
	}
    //��ȡwifi
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
    
    private void setSmartLink() {
    	mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager.isWifiEnabled()) {
			WifiInfo WifiInfo = mWifiManager.getConnectionInfo();
			mConnectedSsid = WifiInfo.getSSID();
			int iLen = mConnectedSsid.length();
			if (mConnectedSsid.startsWith("\"") && mConnectedSsid.endsWith("\"")) {
				mConnectedSsid = mConnectedSsid.substring(1, iLen - 1);
			}
			List<ScanResult> ScanResultlist = mWifiManager.getScanResults();

			for (int i = 0, len = ScanResultlist.size(); i < len; i++) {
				ScanResult AccessPoint = ScanResultlist.get(i);

				if (AccessPoint.SSID.equals(mConnectedSsid)) {
					boolean WpaPsk = AccessPoint.capabilities.contains("WPA-PSK");
					boolean Wpa2Psk = AccessPoint.capabilities.contains("WPA2-PSK");
					boolean Wpa = AccessPoint.capabilities.contains("WPA-EAP");
					boolean Wpa2 = AccessPoint.capabilities.contains("WPA2-EAP");

					if (AccessPoint.capabilities.contains("WEP")) {
						mAuthString = "OPEN-WEP";
						mAuthMode = AuthModeOpen;
						break;
					}

					if (WpaPsk && Wpa2Psk) {
						mAuthString = "WPA-PSK WPA2-PSK";
						mAuthMode = AuthModeWPA1PSKWPA2PSK;
						break;
					} else if (Wpa2Psk) {
						mAuthString = "WPA2-PSK";
						mAuthMode = AuthModeWPA2PSK;
						break;
					} else if (WpaPsk) {
						mAuthString = "WPA-PSK";
						mAuthMode = AuthModeWPAPSK;
						break;
					}

					if (Wpa && Wpa2) {
						mAuthString = "WPA-EAP WPA2-EAP";
						mAuthMode = AuthModeWPA1WPA2;
						break;
					} else if (Wpa2) {
						mAuthString = "WPA2-EAP";
						mAuthMode = AuthModeWPA2;
						break;
					} else if (Wpa) {
						mAuthString = "WPA-EAP";
						mAuthMode = AuthModeWPA;
						break;
					}

					mAuthString = "OPEN";
					mAuthMode = AuthModeOpen;

				}
			}

		}
	}
    
    @Override
    public void finish() {
    	if (IoTManager != null) 
    	{
			IoTManager.StopSmartConnection();
		}
    	super.finish();
    }
    //������
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
	
}
