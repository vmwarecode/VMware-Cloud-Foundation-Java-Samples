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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;

public class LicenseExample {

  Client client;

  public LicenseExample() {
    this.client = new Client();
  }

  /**
   * Add a license key
   *
   * @param productType
   * @param key
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws ClientProtocolException
   * @throws IOException
   */
  public void addLicense(String licenseSpec)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
    Request request =
        Request.Post(client.baseUrl + "/v1/license-keys")
            .bodyString(licenseSpec, ContentType.APPLICATION_JSON);
    HttpResponse response = client.execute(request);
    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
      System.out.println("License add successful");
    } else {
      System.out.println("License add failed");
    }
  }

  /**
   * Get all license keys
   *
   * @return JSONObject
   * @throws UnsupportedOperationException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  public JSONObject getAllLicenseKeys()
      throws UnsupportedOperationException, IOException, KeyManagementException,
          NoSuchAlgorithmException, KeyStoreException {
    Request request = Request.Get(client.baseUrl + "/v1/license-keys");
    HttpResponse response = client.execute(request);
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    JSONObject licenseKeys = new JSONObject(responseContent);
    return licenseKeys;
  }

  /**
   * Get license key metrics like usage and validity of a license key
   *
   * @param key
   * @return JSONObject
   * @throws UnsupportedOperationException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   */
  public JSONObject getLicenseKey(String key)
      throws UnsupportedOperationException, IOException, KeyManagementException,
          NoSuchAlgorithmException, KeyStoreException {
    Request request = Request.Get(client.baseUrl + "/v1/license-keys/"+key);
    HttpResponse response = client.execute(request);
    String responseContent =
        IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
    JSONObject licenseKeys = new JSONObject(responseContent);
    return licenseKeys;
  }

  /**
   * Delete a license key
   *
   * @param key
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws ClientProtocolException
   * @throws IOException
   */
  public void deleteLicense(String key)
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
    JSONObject licenseKeys = getAllLicenseKeys();
    String licenseKeyUuid = null;
    System.out.println();
    for (Object license : (JSONArray) licenseKeys.get("elements")) {
      JSONObject lic = (JSONObject) license;
      if (lic.keySet().contains("key") && lic.get("key").equals(key)) {
        licenseKeyUuid = (String) lic.get("id");
      }
    }
    if (licenseKeyUuid == null) {
      System.err.println("License key - " + key + " not found");
    } else {
      Request request = Request.Delete(client.baseUrl + "/licensing/licensekeys/" + licenseKeyUuid);
      HttpResponse response = client.execute(request);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        System.out.println("License delete successful");
      }
    }
  }

  public static void main(String[] args)
      throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
          URISyntaxException {
    LicenseExample lic = new LicenseExample();
    String licenseSpec =
        new String(
            Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("license.json").toURI())));
    lic.addLicense(licenseSpec);
    System.out.println(lic.getAllLicenseKeys());
    System.out.println(lic.getLicenseKey("AAAAA-BBBBB-CCCCC-DDDDD-EEEEE"));
    lic.deleteLicense("AAAAA-BBBBB-CCCCC-DDDDD-EEEEE");
  }
}
