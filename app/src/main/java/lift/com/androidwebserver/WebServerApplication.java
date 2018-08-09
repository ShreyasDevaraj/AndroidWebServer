package lift.com.androidwebserver;

import android.app.Application;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by sdevaraj on 6/15/2018.
 */

public class WebServerApplication extends Application {
    RequestQueue mRequestQueue;
    private static WebServerApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mRequestQueue = Volley.newRequestQueue(this);
        Log.e("TAG", getLocalIpAddress());
    }

    public String getLocalIpAddress()
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        boolean isIPv4 = inetAddress.getHostAddress().indexOf(':')<0;

                        if (true) {
                            if (isIPv4)
                                return inetAddress.getHostAddress();
                        } else {
                            if (!isIPv4) {
                                int delim = inetAddress.getHostAddress().indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? inetAddress.getHostAddress().toUpperCase() : inetAddress.getHostAddress().substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }
    public static WebServerApplication getInstance(){
        return instance;
    }

    public RequestQueue getRequestQueue(){
        return mRequestQueue;
    }
}
