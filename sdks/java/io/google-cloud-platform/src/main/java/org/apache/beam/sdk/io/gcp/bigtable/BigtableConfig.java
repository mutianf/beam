/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.io.gcp.bigtable;

import static org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.Preconditions.checkArgument;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.bigtable.config.BigtableOptions;
import com.google.cloud.bigtable.config.CredentialOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.annotations.VisibleForTesting;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.MoreObjects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Configuration for a Cloud Bigtable client. */
@AutoValue
@SuppressWarnings({
  "nullness" // TODO(https://github.com/apache/beam/issues/20497)
})
abstract class BigtableConfig implements Serializable {

  enum CredentialType {
    DEFAULT,
    P12,
    SUPPLIED,
    JSON,
    NONE
  }

  /** Returns the project id being written to. */
  abstract @Nullable ValueProvider<String> getProjectId();

  /** Returns the instance id being written to. */
  abstract @Nullable ValueProvider<String> getInstanceId();

  /** Returns the app profile id of this workload. */
  abstract @Nullable ValueProvider<String> getAppProfileId();

  /**
   * Returns the Google Cloud Bigtable instance being written to, and other parameters.
   *
   * @deprecated will be replaced by bigtable options configurator.
   */
  @Deprecated
  abstract @Nullable BigtableOptions getBigtableOptions();

  /** Configurator of the effective Bigtable Options. */
  abstract @Nullable SerializableFunction<BigtableOptions.Builder, BigtableOptions.Builder>
      getBigtableOptionsConfigurator();

  /** Weather validate that table exists before writing. */
  abstract boolean getValidate();

  /** {@link BigtableService} used only for testing. */
  abstract @Nullable BigtableService getBigtableService();

  /** Bigtable emulator. Used only for testing. */
  abstract @Nullable String getEmulatorHost();

  /** User agent for this job. */
  abstract @Nullable String getUserAgent();

  /** Credentials for running the job. */
  abstract @Nullable Credentials getCredentials();

  abstract Builder toBuilder();

  static BigtableConfig.Builder builder() {
    return new AutoValue_BigtableConfig.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setProjectId(ValueProvider<String> projectId);

    abstract Builder setInstanceId(ValueProvider<String> instanceId);

    abstract Builder setAppProfileId(ValueProvider<String> appProfileId);

    /** @deprecated will be replaced by bigtable options configurator. */
    @Deprecated
    abstract Builder setBigtableOptions(BigtableOptions options);

    abstract Builder setValidate(boolean validate);

    abstract Builder setBigtableOptionsConfigurator(
        SerializableFunction<BigtableOptions.Builder, BigtableOptions.Builder> optionsConfigurator);

    abstract Builder setBigtableService(BigtableService bigtableService);

    abstract Builder setEmulatorHost(String emulatorHost);

    abstract Builder setUserAgent(String userAgent);

    abstract Builder setCredentials(Credentials credentials);

    abstract BigtableConfig build();
  }

  BigtableConfig withProjectId(ValueProvider<String> projectId) {
    checkArgument(projectId != null, "Project Id of BigTable can not be null");
    return toBuilder().setProjectId(projectId).build();
  }

  BigtableConfig withInstanceId(ValueProvider<String> instanceId) {
    checkArgument(instanceId != null, "Instance Id of BigTable can not be null");
    return toBuilder().setInstanceId(instanceId).build();
  }

  BigtableConfig withAppProfileId(ValueProvider<String> appProfileId) {
    checkArgument(appProfileId != null, "App profile id can not be null");
    return toBuilder().setAppProfileId(appProfileId).build();
  }

  /** @deprecated will be replaced by bigtable options configurator. */
  @Deprecated
  BigtableConfig withBigtableOptions(BigtableOptions options) {
    checkArgument(options != null, "Bigtable options can not be null");
    return toBuilder().setBigtableOptions(options).build();
  }

  BigtableConfig withBigtableOptionsConfigurator(
      SerializableFunction<BigtableOptions.Builder, BigtableOptions.Builder> configurator) {
    checkArgument(configurator != null, "configurator can not be null");
    return toBuilder().setBigtableOptionsConfigurator(configurator).build();
  }

  BigtableConfig withValidate(boolean isEnabled) {
    return toBuilder().setValidate(isEnabled).build();
  }

  @VisibleForTesting
  BigtableConfig withBigtableService(BigtableService bigtableService) {
    checkArgument(bigtableService != null, "bigtableService can not be null");
    return toBuilder().setBigtableService(bigtableService).build();
  }

  @VisibleForTesting
  BigtableConfig withEmulator(String emulatorHost) {
    checkArgument(emulatorHost != null, "emulatorHost can not be null");
    return toBuilder().setEmulatorHost(emulatorHost).build();
  }

  BigtableConfig withCredentails(Credentials credentials) {
    return toBuilder().setCredentials(credentials).build();
  }

  void validate() {
    checkArgument(
        (getProjectId() != null
                && (!getProjectId().isAccessible() || !getProjectId().get().isEmpty()))
            || (getBigtableOptions() != null
                && getBigtableOptions().getProjectId() != null
                && !getBigtableOptions().getProjectId().isEmpty()),
        "Could not obtain Bigtable project id");

    checkArgument(
        (getInstanceId() != null
                && (!getInstanceId().isAccessible() || !getInstanceId().get().isEmpty()))
            || (getBigtableOptions() != null
                && getBigtableOptions().getInstanceId() != null
                && !getBigtableOptions().getInstanceId().isEmpty()),
        "Could not obtain Bigtable instance id");
  }

  void populateDisplayData(DisplayData.Builder builder) {
    builder
        .addIfNotNull(
            DisplayData.item("projectId", getProjectId()).withLabel("Bigtable Project Id"))
        .addIfNotNull(
            DisplayData.item("instanceId", getInstanceId()).withLabel("Bigtable Instance Id"))
        .add(DisplayData.item("withValidation", getValidate()).withLabel("Check is table exists"));

    if (getBigtableOptions() != null) {
      builder.add(
          DisplayData.item("bigtableOptions", getBigtableOptions().toString())
              .withLabel("Bigtable Options"));
    }
  }

  /**
   * Helper function that either returns the mock Bigtable service supplied by {@link
   * #withBigtableService} or creates and returns an implementation that talks to {@code Cloud
   * Bigtable}.
   *
   * <p>Also populate the credentials option from {@link GcpOptions#getGcpCredential()} if the
   * default credentials are being used on {@link BigtableOptions}.
   */
  @VisibleForTesting
  BigtableService getBigtableService(PipelineOptions pipelineOptions) {
    if (getBigtableService() != null) {
      return getBigtableService();
    }

    BigtableConfig.Builder config = toBuilder();

    if (pipelineOptions instanceof GcpOptions) {
      config.setCredentials(((GcpOptions) pipelineOptions).getGcpCredential());
    }

    try {
      translateBigtableOptions(config);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    config.setUserAgent(pipelineOptions.getUserAgent());

    return new BigtableServiceImpl(config.build());
  }

  boolean isDataAccessible() {
    return (getProjectId() == null || getProjectId().isAccessible())
        && (getInstanceId() == null || getInstanceId().isAccessible());
  }

  private void translateBigtableOptions(BigtableConfig.Builder builder) throws IOException {
    BigtableOptions.Builder effectiveOptionsBuilder = null;

    if (getBigtableOptions() != null) {
      effectiveOptionsBuilder = getBigtableOptions().toBuilder();
    }

    if (getBigtableOptionsConfigurator() != null) {
      effectiveOptionsBuilder = getBigtableOptionsConfigurator().apply(BigtableOptions.builder());
    }

    if (effectiveOptionsBuilder == null) {
      return;
    }

    BigtableOptions effectiveOptions = effectiveOptionsBuilder.build();

    // Todo decided if we should implement cached channel pool

    if (effectiveOptions.getInstanceId() != null && getInstanceId() == null) {
      builder.setInstanceId(ValueProvider.StaticValueProvider.of(effectiveOptions.getInstanceId()));
    }

    if (effectiveOptions.getProjectId() != null && getProjectId() == null) {
      builder.setProjectId(ValueProvider.StaticValueProvider.of(effectiveOptions.getProjectId()));
    }

    if (!effectiveOptions.getDataHost().equals("bigtable.googleapis.com")
        && getEmulatorHost() == null) {
      builder.setEmulatorHost(
          String.format("%s:%s", effectiveOptions.getDataHost(), effectiveOptions.getPort()));
    }

    if (effectiveOptions.getCredentialOptions() != null) {
      CredentialOptions credOptions = effectiveOptions.getCredentialOptions();
      switch (credOptions.getCredentialType()) {
        case DefaultCredentials:
          GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
          builder.setCredentials(credentials);
          break;
        case P12:
          String keyFile = ((CredentialOptions.P12CredentialOptions) credOptions).getKeyFile();
          String serviceAccount =
              ((CredentialOptions.P12CredentialOptions) credOptions).getServiceAccount();
          try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (FileInputStream fin = new FileInputStream(keyFile)) {
              keyStore.load(fin, "notasecret".toCharArray());
            }
            PrivateKey privateKey =
                (PrivateKey) keyStore.getKey("privatekey", "notasecret".toCharArray());

            if (privateKey == null) {
              throw new IllegalStateException("private key cannot be null");
            }
            builder.setCredentials(
                ServiceAccountJwtAccessCredentials.newBuilder()
                    .setClientEmail(serviceAccount)
                    .setPrivateKey(privateKey)
                    .build());
          } catch (GeneralSecurityException exception) {
            throw new RuntimeException("exception while retrieving credentials", exception);
          }
          break;
        case SuppliedCredentials:
          builder.setCredentials(
              ((CredentialOptions.UserSuppliedCredentialOptions) credOptions).getCredential());
          break;
        case SuppliedJson:
          CredentialOptions.JsonCredentialsOptions jsonCredentialsOptions =
              (CredentialOptions.JsonCredentialsOptions) credOptions;
          synchronized (jsonCredentialsOptions) {
            if (jsonCredentialsOptions.getCachedCredentials() == null) {
              jsonCredentialsOptions.setCachedCredentails(
                  GoogleCredentials.fromStream(jsonCredentialsOptions.getInputStream()));
            }
            builder.setCredentials(jsonCredentialsOptions.getCachedCredentials());
          }
          break;
        case None:
          builder.setCredentials(NoCredentialsProvider.create().getCredentials());
          break;
      }
    }
  }

  @Override
  public final String toString() {
    return MoreObjects.toStringHelper(BigtableConfig.class)
        .add("projectId", getProjectId())
        .add("instanceId", getInstanceId())
        .add("appProfileId", getAppProfileId())
        .add("userAgent", getUserAgent())
        .add("emulator", getEmulatorHost())
        .toString();
  }
}
