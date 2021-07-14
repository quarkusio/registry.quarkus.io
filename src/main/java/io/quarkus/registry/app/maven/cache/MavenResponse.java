package io.quarkus.registry.app.maven.cache;

import java.util.Map;
import java.util.HashMap;

/**
 * the response from a maven call
 */
public class MavenResponse {
    private int status;
    private String response;
    private Map<String,String> headers = new HashMap<>();
    
    public MavenResponse(){
        
    }
    
    public MavenResponse(int status, String response) {
        this.status = status;
        this.response = response;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String value){
        headers.put(key, value);
    }
}
