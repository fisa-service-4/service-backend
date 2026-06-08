package com.service.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class FirebaseConfig {

  public FirebaseConfig(
      @Value("${firebase.credential-path}") String credentialPath, ResourceLoader resourceLoader)
      throws IOException {

    if (FirebaseApp.getApps().isEmpty()) {
      Resource resource = resourceLoader.getResource(credentialPath);

      try (InputStream serviceAccount = resource.getInputStream()) {
        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
      }
    }
  }
}
