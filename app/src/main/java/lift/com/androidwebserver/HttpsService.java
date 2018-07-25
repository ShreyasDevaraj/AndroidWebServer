package lift.com.androidwebserver;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by sdevaraj on 6/1/2018.
 */

public class HttpsService extends Service {
    private MyHTTPD server;
    private static final int PORT = 8765;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        server = new MyHTTPD(PORT);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/keystore.bks";
        try {

            File keystoreFile = new File(path);
            SSLServerSocketFactory mySSLSocketFactory = makeSSLSocketFactory(keystoreFile, "shreyas".toCharArray());

            server.makeSecure(mySSLSocketFactory, null);
            server.start();
        } catch (IOException e) {
            Log.e("TAG", "exception " + e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    /**
     *
     * @param keystoreFile
     * @param passphrase
     * @return
     * @throws IOException
     */
    static SSLServerSocketFactory makeSSLSocketFactory(File keystoreFile, char[] passphrase) throws IOException {
        SSLServerSocketFactory res = null;
        try {
            Log.e("TAG", KeyStore.getDefaultType());
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = new FileInputStream(keystoreFile);
            keystore.load(keystoreStream, passphrase);
            // TODO: close keystoreStream??
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            res = ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return res;
    }
}

 class MyHTTPD extends NanoHTTPD {


    public MyHTTPD(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.e("TAG", session.getUri());

        final StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> kv : session.getHeaders().entrySet()) {
            buf.append(kv.getKey() + " : " + kv.getValue() + "\n");
        }

        Log.e("TAG", buf.toString());
      /*  handler.post(new Runnable() {
            @Override
            public void run() {
               // hello.setText(buf);
            }
        });*/

        switch (session.getUri()) {

            //http://10.10.100.149:4980/
            case "/test":

                Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
                r.addHeader("Location", "http://10.10.100.149:4980");
                return r;
            default:
                final String html = "<html><head><head><body><h1>Hello, Shreyas how are you??</h1></body></html>";
                InputStream targetStream = new ByteArrayInputStream(html.getBytes());
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_HTML, targetStream, html.getBytes().length);


        }
    }

}
