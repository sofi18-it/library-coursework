package ua.edu.nupp.library.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class LibraryApiClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule());
    private final String baseUrl=System.getenv().getOrDefault("LIBRARY_API_URL","http://localhost:8080/library-server/api").replaceAll("/$","");

    public <T> List<T> getList(String path,Class<T> type)throws IOException,InterruptedException {
        JsonNode data=send("GET",path,null);List<T> rows=new ArrayList<>();for(JsonNode node:data)rows.add(json.treeToValue(node,type));return rows;
    }
    public <T>T get(String path,Class<T> type)throws IOException,InterruptedException{return json.treeToValue(send("GET",path,null),type);}
    public <T>T post(String path,Object body,Class<T> type)throws IOException,InterruptedException{return json.treeToValue(send("POST",path,body),type);}
    public <T>T put(String path,Object body,Class<T> type)throws IOException,InterruptedException{return json.treeToValue(send("PUT",path,body),type);}
    public void delete(String path)throws IOException,InterruptedException{send("DELETE",path,null);}

    private JsonNode send(String method,String path,Object body)throws IOException,InterruptedException {
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(baseUrl+path)).timeout(Duration.ofSeconds(20)).header("Accept","application/json");
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json; charset=UTF-8").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());JsonNode root=json.readTree(response.body());
        if(response.statusCode()<200||response.statusCode()>=300)throw new IOException(root.path("error").asText("HTTP "+response.statusCode()));
        return root.path("data");
    }
}
