package com.yondu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yondu.App;
import com.yondu.model.Token;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** All API calls that will be made going to Rush API should be here / API Module
 *
 *  @author m1d0rf33d
 */
public class ApiService {

    private String baseUrl;
    private String authorizationEndpoint;
    private String appKey;
    private String appSecret;

    public ApiService() {
        try {
            Properties prop = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("api.properties");
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file api.properties not found in the classpath");
            }
            this.baseUrl = prop.getProperty("base_url");
            this.authorizationEndpoint = prop.getProperty("authorization_endpoint");
            this.appKey = prop.getProperty("app_key");
            this.appSecret = prop.getProperty("app_secret");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String call(String url, List<NameValuePair> params, String method) {
        //Validate token
        if (App.appContextHolder.getAuthorizationToken() == null) {
            App.appContextHolder.setAuthorizationToken(getToken());
        }

        HttpResponse response = null;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            //POST request
            if (method.equalsIgnoreCase("post")) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                httpPost.addHeader("Authorization", "Bearer "+ App.appContextHolder.getAuthorizationToken());
                response = httpClient.execute(httpPost);
            }
            //GET request
            if (method.equalsIgnoreCase("get")) {
                HttpGet request = new HttpGet(url);
                request.addHeader("content-type", "application/json");
                request.addHeader("Authorization", "Bearer "+ App.appContextHolder.getAuthorizationToken());
                response = httpClient.execute(request);
            }
            // use httpClient (no need to close it explicitly)
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private String getToken() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost httpPost = new HttpPost((this.baseUrl + this.authorizationEndpoint));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("app_key", this.appKey));
            params.add(new BasicNameValuePair("app_secret", this.appSecret));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpClient.execute(httpPost);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            ObjectMapper mapper = new ObjectMapper();
            Token token = mapper.readValue(result.toString(), Token.class);
            return token.getToken();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
