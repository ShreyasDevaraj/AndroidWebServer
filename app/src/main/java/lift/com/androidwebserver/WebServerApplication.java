package lift.com.androidwebserver;

import android.app.Application;
import android.util.Log;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by sdevaraj on 6/15/2018.
 */

public class WebServerApplication extends Application {

    private static WebServerApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }

    public static WebServerApplication getInstance(){
        return instance;
    }


}
