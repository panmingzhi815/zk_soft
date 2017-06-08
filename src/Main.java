import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        try {
            new Main().start();
        } catch (Exception e) {
            System.out.println("启动服务器失败");
            e.printStackTrace();
        }

    }

    private void start() throws IOException {
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(808), 0);
        HttpHandler httpHandler = e -> {
            System.out.println("开始接收考勤机信息");
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            String requestMethod = e.getRequestMethod();
            String url = e.getRequestURI().toString();
            String attributeStr = url.substring(url.indexOf("?") + 1).trim();

            System.out.println("requestMethod:" + requestMethod);
            System.out.println("url:" + url);
            System.out.println("attributeStr:" + attributeStr);

            if (url.startsWith("/iclock/cdata") && requestMethod.equals("GET")) {
                responseParameter(e, parse(attributeStr));
                return;
            } else if (url.startsWith("/iclock/cdata") && requestMethod.equals("POST")) {
                responseOperate(e, parse(attributeStr));
                return;
            } else if (url.startsWith("/iclock/fdata") && requestMethod.equals("POST")) {
                responsePhoto(e, parse(attributeStr));
                return;
            }
            writeSuccess(e, "OK");
        };
        httpServer.createContext("/", httpHandler);
        httpServer.start();
        System.out.println("启动服务器成功");
    }

    /**
     * 接收考勤图片
     * @param e
     * @param parse
     * @throws IOException
     */
    private void responsePhoto(HttpExchange e, Map<String, String> parse) throws IOException {
        try {
            byte[] buffer = readInputStream(e.getRequestBody());
            System.out.println("收到考勤图片");
            String content = new String(buffer, "GBK");
            String fileName = content.substring(content.indexOf("PIN=") + 4, content.indexOf(".jpg") + 4).trim();

            int photostartindex = content.indexOf('\0');

            byte[] photosbyte = Arrays.copyOfRange(buffer,photostartindex + 1,buffer.length);

            try (OutputStream out = new FileOutputStream(fileName)) {
                out.write(photosbyte);
                out.flush();
                System.out.println("保存考勤照片成功");
            } catch (Exception ex) {
                System.out.println("保存考勤照片失败");
                ex.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            writeSuccess(e, "OK");
        }
    }

    /**
     * 请求初始化参数
     * @param e
     * @param map
     */
    private void responseParameter(HttpExchange e, Map<String, String> map) {
        StringBuffer stringBuffer = new StringBuffer();
        println(stringBuffer, "GET OPTION FROM: " + map.get("SN"));
        println(stringBuffer, "Stamp=0");
        println(stringBuffer, "OpStamp=0");
        println(stringBuffer, "PhotoStamp=0");
        println(stringBuffer, "TransFlag=1111111111");
        println(stringBuffer, "ErrorDelay=10");
        println(stringBuffer, "Delay=1");
        println(stringBuffer, "TransInterval=5");
        println(stringBuffer, "Realtime=1");
        println(stringBuffer, "Encrypt=" + "0");
        writeSuccess(e, stringBuffer.toString());
    }

    /**
     * 接收考勤记录与指纹人脸操作
     * @param e
     * @param parse
     * @throws IOException
     */
    private void responseOperate(HttpExchange e, Map<String, String> parse) throws IOException {
        byte[] bytes = readInputStream(e.getRequestBody());

        String gbk = new String(bytes, "gbk");
        System.out.println(gbk);

        if (parse.get("table").equals("ATTLOG")) {
            System.out.println("收到考勤记录上传");
        }

        writeSuccess(e, "OK");
    }

    private byte[] readInputStream(InputStream requestBody) throws IOException {
        byte[] bytes = new byte[requestBody.available()];
        requestBody.read(bytes);
        return bytes;
    }


    private void println(StringBuffer sb, String str) {
        sb.append(str).append("\r\n");
    }

    private Map<String, String> parse(String url) {
        HashMap<String, String> map = new HashMap<>();
        String[] split = url.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            map.put(split1[0], split1[1]);
        }
        return map;
    }

    /**
     * 返回消息
     * @param e
     * @param msg
     */
    private void writeSuccess(HttpExchange e, String msg) {
        try {
            msg += "\r\n";
            byte[] bytes = msg.getBytes(Charset.forName("GBK"));
            Headers responseHeaders = e.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");
            e.sendResponseHeaders(200, bytes.length);

            OutputStream responseBody = e.getResponseBody();
            responseBody.write(bytes);
            responseBody.flush();
            TimeUnit.MILLISECONDS.sleep(200);
            System.out.println("服务器返回:" + msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            e.close();
        }

    }
}
