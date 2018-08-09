package lift.com.androidwebserver;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

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
    private static String url = "http://10.10.100.38:4980";
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
     public Socket socket = null;

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



        switch (session.getUri()) {

            //http://10.10.100.149:4980/
            case "/test":

                Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
                r.addHeader("Location", "http://10.10.100.38:4980");
                return r;
            default:

                String url = "http://10.10.100.38:4980/" +session.getUri();
               String html;
                try {
                    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
                    ListenableFuture<String> future = Futures.withTimeout(makeContentUpdateRestCall(url), 10, TimeUnit.SECONDS, executorService);
                    html = future.get();
                    InputStream targetStream = new ByteArrayInputStream(html.getBytes());
                    //return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_HTML, targetStream, html.getBytes().length);
                    return NanoHTTPD.newChunkedResponse(Response.Status.OK, MIME_HTML, targetStream);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;


        }
    }

     private ListenableFuture<String> makeContentUpdateRestCall(final String url) {
         final SettableFuture<String> future = SettableFuture.create();
         final ExecutorService executorService = Executors.newSingleThreadExecutor();
         executorService.submit(new Runnable() {
             @Override
             public void run() {

                 StringRequest sr = new StringRequest(Request.Method.GET, url, new com.android.volley.Response.Listener<String>() {
                     @Override
                     public void onResponse(String response) {

                          future.set(response);

                     }
                 }, new com.android.volley.Response.ErrorListener() {
                     @Override
                     public void onErrorResponse(VolleyError error) {
                        future.set(null);
                     }
                 }){
                     @Override
                     public Map<String, String> getHeaders() throws AuthFailureError {
                         HashMap<String, String> params = new HashMap<String, String>();
                         String creds = String.format("%s:%s","admin","P4Ns!nAg3");
                         String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                         params.put("Authorization", auth);
                         return params;
                     }
                 };
                 WebServerApplication.getInstance().getRequestQueue().add(sr);
             }
         });

         return future;
     }
    public String makeRestCall() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() throws InterruptedException {
                final String[] responseHandler = {null};
                StringRequest sr = new StringRequest(Request.Method.GET, "http://10.10.100.38:4980", new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        responseHandler[0] = response;

                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
                WebServerApplication.getInstance().getRequestQueue().add(sr);
              return responseHandler[0];
            }
        };
        Future<String> future = executor.submit(callable);
        // future.get() returns 2 or raises an exception if the thread dies, so safer
        executor.shutdown();
        return future.get();
    }

     /**
      * StringRequest sr = new StringRequest(Request.Method.GET, "http://10.10.100.149:4980", new Response.Listener<String>() {
     @Override
     public void onResponse(String response) {
     //StratacacheLog.e(TAG, response);
     Log.e("TAG", response);
     }
     }, new Response.ErrorListener() {
     @Override
     public void onErrorResponse(VolleyError error) {
     Log.e("TAG", "Error " + error.toString());
     }
     });
      mRequestQueue.add(sr);
      */


}
