package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
    	int contentLength = 0;
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
        	
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	if(line == null) return;
        	String[] reqInfo = line.split(" ");
        	while(line != null) {
        		if(line == null || line.equals("")) break;
        		log.debug("header : {}", line);
        		if(line.startsWith("Content-Length")) {
        			contentLength = Integer.parseInt(line.split(":")[1].trim());
        		}
        		line = br.readLine();
        	}
        	
        	byte[] body = "file not found".getBytes();
        	Path filePath = null;
        	
        	String reqUrl = reqInfo[0] +" "+ reqInfo[1];
        	String params = "";
        	if(reqUrl.startsWith("GET /user/create")) {
        		int index = reqInfo[1].indexOf("?");
        		if(index == 0) return;
        		params = reqInfo[1].substring(index+1);
        		body = createUser(params).toString().getBytes();
        	} else if(reqUrl.startsWith("POST /user/create")) {
        		if(contentLength > 0) {
        			params = IOUtils.readData(br, contentLength);
        			body = ("POST "+(createUser(params).toString())).getBytes();
        		}
        	} else {
        		filePath = new File("./webapp"+reqInfo[1]).toPath();
        		body = Files.readAllBytes(filePath);
        	}
        	
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private User createUser(String params) {
		Map<String, String> map = HttpRequestUtils.parseQueryString(params);
		User user = new User(map.get("userId"), map.get("password"), map.get("name"), map.get("email"));
		log.debug("user info : {}", user.toString());
		
		return user;
	}

	private void resonseIndex(InputStream in) {
//    	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//    	String str = "";
//    	while((str = br.readLine()) != null) {
//    		if(str.startsWith("GET ")) {
//    			
//    		}
//    	}
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
