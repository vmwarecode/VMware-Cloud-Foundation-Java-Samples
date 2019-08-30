/** Copyright 2019 VMware, Inc. All rights reserved. -- VMware Confidential */
package com.vmware.vcf.rest.examples;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONObject;

/**
 * REST API client class for VMware Cloud Foundation SDDC Manager
 *
 * @author VMware Inc
 */
public class Client {

  Properties properties = new Properties();
  String baseUrl;

  public Client() {
    super();
    try {
      properties.load(ClassLoader.getSystemResourceAsStream("application.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.baseUrl = "https://" + properties.getProperty("sddcManagerIP");
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  /** @return SDDC Manager Basic Authentication Header */
  public Header getAuthHeader() {
    String headerValue =
        "Basic "
            + java.util.Base64.getEncoder()
                .encodeToString(
                    (properties.getProperty("restApiUsername")
                            + ":"
                            + properties.getProperty("restApiPassword"))
                        .getBytes());
    return new BasicHeader("Authorization", headerValue);
  }

  /**
   * Execute a REST call on SDDC Manager
   *
   * @param request
   * @return Response String
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws ClientProtocolException
   * @throws IOException
   */
  public HttpResponse execute(Request request)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
    final SSLContext sslContext =
        new SSLContextBuilder().loadTrustMaterial(null, (x509CertChain, authType) -> true).build();
    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setSSLContext(sslContext)
            .build();
    HttpResponse response =
        Executor.newInstance(httpClient)
            .execute(request.addHeader(getAuthHeader()))
            .returnResponse();
    System.out.println(
        "Request - " + request + " , response code : " + response.getStatusLine().getStatusCode());
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    if (responseContent.contains("errorCode")) {
      JSONObject responseJson = new JSONObject(responseContent);
      StringBuilder message = new StringBuilder();
      message.append(
          responseJson.has("errorCode")
              ? "Error Code : " + responseJson.getString("errorCode")
              : "");
      message.append(
          responseJson.has("remediationMessage")
              ? " , Remediation Message : " + responseJson.getString("remediationMessage")
              : "");
      System.out.println(message);
    }
    return response;
  }
}
