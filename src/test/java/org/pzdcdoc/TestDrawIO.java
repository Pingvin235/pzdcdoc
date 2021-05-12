package org.pzdcdoc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class TestDrawIO {
    @Test
    public void testExportToSVG() throws Exception {
        OkHttpClient client = new OkHttpClient();

        //MediaType XML = MediaType.get("text/xml; charset=utf-8");

        /* var body = new FormBody.Builder()
            .add("source", IOUtils.toString(new FileInputStream("src/doc/_res/test.drawio"), StandardCharsets.UTF_8))
            .add("format", "svg")
            .build(); */

        var data = Map.of(
            "source", IOUtils.toString(new FileInputStream("src/doc/_res/test.drawio"), StandardCharsets.UTF_8),
            "format", "svg"
        );

        var mapper = new ObjectMapper();

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String json = mapper.writeValueAsString(data);
        var body = RequestBody.create(JSON, json);
        
        var request = new Request.Builder()
            .url("http://bgerp.org:5000/convert")
            .post(body)
            .build();
        try (var response = client.newCall(request).execute()) {
            IOUtils.write(response.body().string(), new FileOutputStream("src/doc/_res/test.svg"), StandardCharsets.UTF_8);
        }
        
    }

    public static void main(String[] args) throws Exception {
        new TestDrawIO().testExportToSVG();
    }
}
