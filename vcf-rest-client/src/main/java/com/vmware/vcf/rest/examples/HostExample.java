/** Copyright 2019 VMware, Inc. All rights reserved. -- VMware Confidential */
package com.vmware.vcf.rest.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

public class HostExample {

  static int POLL_INTERVAL_SECONDS = 30;
  static int MAX_HOST_VALIDATION_POLL_TIME_MINUTES = 30;
  static int MAX_HOST_COMMISSION_POLL_TIME_MINUTES = 60;

  Client client;

  public HostExample() {
    this.client = new Client();
  }

  public boolean pollHostValidation(String id)
      throws InterruptedException, UnsupportedOperationException, IOException,
          KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    boolean isValidationSuccessful = true;
    String executionStatus = "IN_PROGRESS";
    long timeout =
        System.currentTimeMillis()
            + TimeUnit.MINUTES.toMillis(MAX_HOST_VALIDATION_POLL_TIME_MINUTES);
    String responseString = "";
    while (System.currentTimeMillis() < timeout && executionStatus.equals("IN_PROGRESS")) {
      System.out.println("Waiting for Host validation");
      Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS));
      Request pollRequest =
          Request.Get(client.baseUrl + "/v1/hosts/validations/" + id)
              .addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
      HttpResponse pollResponse = client.execute(pollRequest);
      responseString =
          IOUtils.toString(pollResponse.getEntity().getContent(), Charset.defaultCharset());
      System.out.println(responseString);
      executionStatus = new JSONObject(responseString).getString("executionStatus");
    }
    for (Object s : new JSONObject(responseString).getJSONArray("validationChecks")) {
      if (new JSONObject(s).has("resultStatus")
          && !new JSONObject(s).getString("resultStatus").equals("SUCCEEDED")) {
        isValidationSuccessful = false;
      }
    }
    System.out.println(
        "Exit pollHostValidation, isValidationSuccessful - " + isValidationSuccessful);
    return isValidationSuccessful;
  }

  public String pollHostCommission(String id)
      throws InterruptedException, UnsupportedOperationException, IOException,
          KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    String status = "In Progress";
    long timeout =
        System.currentTimeMillis()
            + TimeUnit.MINUTES.toMillis(MAX_HOST_COMMISSION_POLL_TIME_MINUTES);
    while (System.currentTimeMillis() < timeout && status.equals("In Progress")) {
      System.out.println("Waiting for Host commission, status - " + status);
      Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS));
      Request pollRequest = Request.Get(client.baseUrl + "/v1/tasks/" + id);
      HttpResponse pollResponse = client.execute(pollRequest);
      String responseString =
          IOUtils.toString(pollResponse.getEntity().getContent(), Charset.defaultCharset());
      status = new JSONObject(responseString).getString("status");
    }
    System.out.println("Exit pollHostCommission, status - " + status);
    return status;
  }

  public void validateHosts(String hostsSpec)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException,
          InterruptedException {
    Request request =
        Request.Post(client.baseUrl + "/v1/hosts/validations/commissions")
            .bodyString(hostsSpec, ContentType.APPLICATION_JSON);
    HttpResponse response = client.execute(request);
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
      System.out.println("Host Validation Started");
      JSONObject responseJson = new JSONObject(responseContent);
      String id = responseJson.getString("id");
      boolean validationStatus = pollHostValidation(id);
      System.out.println("Exit validateHosts, validationStatus - " + validationStatus);
    } else {
      System.out.println("Host Validation has not started");
    }
  }

  public void commissionHosts(String hostsSpec)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException,
          UnsupportedOperationException, InterruptedException {
    Request request =
        Request.Post(client.baseUrl + "/v1/hosts")
            .bodyString(hostsSpec, ContentType.APPLICATION_JSON);
    HttpResponse response = client.execute(request);
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
      System.out.println("Host Validation Started");
      JSONObject responseJson = new JSONObject(responseContent);
      String id = responseJson.getString("id");
      String status = pollHostCommission(id);
      System.out.println("Exit host commission, status - " + status);
    } else {
      System.out.println("Host Validation has not started");
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException,
          KeyStoreException, InterruptedException {
    HostExample hostEx = new HostExample();
    String hostsSpec =
        new String(
            Files.readAllBytes(
                Paths.get(ClassLoader.getSystemResource("hostcommission.json").toURI())));
    hostEx.commissionHosts(hostsSpec);
  }
}
