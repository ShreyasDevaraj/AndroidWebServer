package lift.com.androidwebserver;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;

import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import fi.iki.elonen.NanoHTTPD;

import static lift.com.androidwebserver.HttpsService.MIME;

/**
 * Created by sdevaraj on 6/1/2018.
 */

public class HttpsService extends Service {
    private MyHTTPD server;
    private static final int PORT = 8765;
    List<String> mimeType = new LinkedList<>();
    public static String MIME = null;

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
        Method method = session.getMethod();
        Map<String, String> files = new HashMap<>();

        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);

            } catch (IOException ioe) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), NanoHTTPD.MIME_PLAINTEXT, re.getMessage());
            }
        }

            Map<String, String> currentHeader = session.getHeaders();
            switch (session.getUri()) {
            case "/test":

                Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
                r.addHeader("Location", "http://127.0.0.1:4980");
                return r;
            default:

                String url;
                Uri.Builder buildUri =  Uri.parse("http://127.0.0.1:4980").buildUpon().appendEncodedPath(session.getUri());
                Map<String, String> parameterMap = session.getParms();
                if(parameterMap != null && !parameterMap.isEmpty() && session.getMethod().equals(Method.GET)){
                    for(String key : parameterMap.keySet()){
                        buildUri.appendQueryParameter(key, parameterMap.get(key));
                    }
                }

                try {

                    Uri uri1 = buildUri.build();
                    url = uri1.toString();
                    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

                    ListenableFuture<InputStream> futureInputStream = Futures.withTimeout(makeContentUpdateRestCallNew(url, currentHeader, session.getMethod(), files, parameterMap), 10, TimeUnit.SECONDS, executorService);
                    InputStream res = futureInputStream.get();
                    if(MIME == null){
                        Response response = NanoHTTPD.newChunkedResponse(Response.Status.OK, MIME_HTML, res);
                        return response;
                    }else{
                        Response response = NanoHTTPD.newChunkedResponse(Response.Status.OK, MIME, res);
                        return response;
                    }

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return super.serve(session);


        }
    }

    private ListenableFuture<InputStream> makeContentUpdateRestCallNew(final String Url, final Map<String, String> currentHeader, Method method, Map<String, String> files, Map<String, String> parameterMap){
        final SettableFuture<InputStream> future = SettableFuture.create();
        org.apache.commons.httpclient.HttpClient hc = new org.apache.commons.httpclient.HttpClient();
                if(method.equals(Method.GET)) {
                    GetMethod get = new GetMethod(Url);
                    for (String key : currentHeader.keySet()) {
                        get.setRequestHeader(key, currentHeader.get(key));
                    }
                    int code = 0;
                    try {
                        code = hc.executeMethod(get);
                        if (code == 200 || code == 401) {
                            MIME = get.getResponseHeader("Content-Type").getValue();
                            future.set(get.getResponseBodyAsStream());
                        } else {
                            future.set(null);
                        }

                    } catch (IOException e) {
                        future.set(null);
                        e.printStackTrace();
                    }
                }else if(method.equals(Method.POST)){
                    if(!files.isEmpty()) {
                        String originalFileName = parameterMap.get("upfile1");
                        String filename = files.get("upfile1");
                        filename = filename.substring(filename.lastIndexOf("/") + 1);
                        File cDir = WebServerApplication.getInstance().getApplicationContext().getCacheDir();
                        File tempFile = new File(cDir.getPath() + "/" + filename);

                        try {
                            String charset = "UTF-8";
                            String param = "value";
                            String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
                            String CRLF = "\r\n"; // Line separator required by multipart/form-data.

                            URLConnection connection = new URL(Url).openConnection();
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                            try (
                                    OutputStream output = connection.getOutputStream();
                                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
                            ) {

                                // Send text file.
                                writer.append("--" + boundary).append(CRLF);
                                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" +originalFileName + "\"").append(CRLF);
                                writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                                writer.append(CRLF).flush();
                                IOUtils.copy(new FileInputStream(tempFile),output);
                                output.flush();
                                writer.append(CRLF).flush();
                                writer.append("--" + boundary + "--").append(CRLF).flush();
                            }

                            // Request is lazily fired whenever you need to obtain information about response.
                            int responseCode = ((HttpURLConnection) connection).getResponseCode();

                            if (responseCode == 200) {
                                MIME = ((HttpURLConnection) connection).getHeaderField("Content-Type");
                                Log.e("Response header ", "" + MIME);
                                future.set(connection.getInputStream());
                            } else {
                                future.set(null);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                         }
                    }


                }

        return future;
    }


}
