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

public class DomainExample {

  static int POLL_INTERVAL_SECONDS = 30;
  static int MAX_DOMAIN_CREATION_POLL_TIME_MINUTES = 180;
  static int MAX_DOMAIN_CREATION_VALIDATION_POLL_TIME_MINUTES = 20;

  Client client;

  public DomainExample() {
    this.client = new Client();
  }

  /**
   * Poll for Domain Creation Validation
   *
   * @param id
   * @return
   * @throws InterruptedException
   * @throws UnsupportedOperationException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  public boolean pollDomainCreationValidation(String id)
      throws InterruptedException, UnsupportedOperationException, IOException,
          KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    boolean isValidationSuccessful = true;
    String executionStatus = "IN_PROGRESS";
    long timeout =
        System.currentTimeMillis()
            + TimeUnit.MINUTES.toMillis(MAX_DOMAIN_CREATION_VALIDATION_POLL_TIME_MINUTES);
    String responseString = "";
    while (System.currentTimeMillis() < timeout && executionStatus.equals("IN_PROGRESS")) {
      System.out.println("Waiting for domain creation validation");
      Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS));
      Request pollRequest =
          Request.Get(client.baseUrl + "/v1/domains/validations/creations/" + id)
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

  /**
   * Poll for Domain Creation
   *
   * @param id
   * @return
   * @throws InterruptedException
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  private String pollDomainCreation(String id)
      throws InterruptedException, KeyManagementException, NoSuchAlgorithmException,
          KeyStoreException, IOException {
    String status = "";
    long timeout =
        System.currentTimeMillis()
            + TimeUnit.MINUTES.toMillis(MAX_DOMAIN_CREATION_POLL_TIME_MINUTES);
    while (System.currentTimeMillis() < timeout
        && (status.equals("Pending") || status.equals("In Progress") || status.equals(""))) {
      System.out.println("Waiting for Domain Creation, status - " + status);
      Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS));
      Request pollRequest = Request.Get(client.baseUrl + "/v1/tasks/" + id);
      HttpResponse pollResponse = client.execute(pollRequest);
      String responseString =
          IOUtils.toString(pollResponse.getEntity().getContent(), Charset.defaultCharset());
      status = new JSONObject(responseString).getString("status");
    }
    System.out.println("Exit pollDomainCreation, status - " + status);
    return status;
  }

  /**
   * Validate a domain spec
   *
   * @param domainSpec
   * @return
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean validateDomainCreation(String domainSpec)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException,
          InterruptedException {
    boolean validated = false;
    Request request =
        Request.Post(client.baseUrl + "/v1/domains/validations/creations")
            .bodyString(domainSpec, ContentType.APPLICATION_JSON);
    HttpResponse response = client.execute(request);
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      System.out.println("Domain Creation Validation Started");
      JSONObject responseJson = new JSONObject(responseContent);
      String executionStatus = responseJson.getString("executionStatus");
      String resultStatus = responseJson.getString("resultStatus");
      System.out.println(
          "Completed validateDomainCreation, executionStatus - "
              + executionStatus
              + " , resultStatus - "
              + resultStatus);
      validated = true;
    } else {
      System.err.println("Domain Creation Validation has not started");
    }
    return validated;
  }

  /**
   * Create a workload domain
   *
   * @param domainSpec
   * @throws UnsupportedOperationException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws InterruptedException
   */
  public void createDomain(String domainSpec)
      throws UnsupportedOperationException, IOException, KeyManagementException,
          NoSuchAlgorithmException, KeyStoreException, InterruptedException {
    boolean validated = validateDomainCreation(domainSpec);
    if (validated) {
      Request request =
          Request.Post(client.baseUrl + "/v1/domains")
              .bodyString(domainSpec, ContentType.APPLICATION_JSON);
      HttpResponse response = client.execute(request);
      String responseContent =
          IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
        System.out.println("Domain Creation Started");
        JSONObject responseJson = new JSONObject(responseContent);
        String id = responseJson.getString("id");
        String status = pollDomainCreation(id);
        System.out.println("Completed domain creation, status - " + status);
      } else {
        System.out.println("Domain creation has not started");
      }
    } else {
      System.err.println("Validation failed, domain creation skipped");
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException,
          KeyStoreException, InterruptedException {
    DomainExample domainEx = new DomainExample();
    String domainSpec =
        new String(
            Files.readAllBytes(
                Paths.get(ClassLoader.getSystemResource("add_domain.json").toURI())));
    domainEx.createDomain(domainSpec);
  }
}
