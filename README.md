Turnstyle Siteverity Client
===========================
![GitHub](https://img.shields.io/github/license/osslabz/turnstile-siteverify-client)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/turnstile-siteverify-client/build-on-push.yml?branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/net.osslabz/turnstile-siteverify-client?label=Maven%20Central)](https://search.maven.org/artifact/net.osslabz/turnstile-siteverify-client)

# Cloudflare Turnstile Client

This is a Java client library for the Cloudflare Turnstile Siteverify API. It provides a simple and efficient way to verify Turnstile tokens in your Java applications.

## Features

- Easy-to-use API for verifying Turnstile tokens
- Built with OkHttp for efficient HTTP requests
- Uses Jackson for JSON parsing

## Installation

To use this library in your project, add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>net.osslabz</groupId>
    <artifactId>turnstile-siteverify-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Usage

Here's a simple example of how to use the Cloudflare Turnstile Client:

```java
import com.example.cloudflare.turnstile.TurnstileClient;
import com.example.cloudflare.turnstile.TurnstileResponse;

public class Example {
    public static void main(String[] args) {
        String secretKey = "your_secret_key_here";
        TurnstileClient client = new TurnstileClient(secretKey);

        try {
            TurnstileResponse response = client.verify("turnstile_response_token");
            if (response.isSuccess()) {
                System.out.println("Verification successful!");
            } else {
                System.out.println("Verification failed: " + response.getErrorCodes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## Configuration

You can customize the `OkHttpClient` and `ObjectMapper` used by the `TurnstileClient` by using the appropriate constructor:

```java
OkHttpClient customHttpClient = new OkHttpClient.Builder()
    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    .build();
ObjectMapper customObjectMapper = new ObjectMapper();

TurnstileClient client = new TurnstileClient(secretKey, customHttpClient, customObjectMapper);
```

## Building

To build the project, run:

```
mvn clean install
```

## Testing

To run the tests, use:

```
mvn test
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License