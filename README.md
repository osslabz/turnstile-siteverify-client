Turnstile Siteverify Client
===========================
![GitHub](https://img.shields.io/github/license/osslabz/turnstile-siteverify-client)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/turnstile-siteverify-client/build-on-push.yml?branch=dev&label=build&logo=git)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/osslabz/turnstile-siteverify-client/release.yml?branch=dev&label=perform-release&logo=semanticrelease)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/net/osslabz/turnstile-siteverify-client/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/net/osslabz/turnstile-siteverify-client/README.md)
[![Maven Central](https://img.shields.io/maven-central/v/net.osslabz/turnstile-siteverify-client?label=Maven%20Central)](https://search.maven.org/artifact/net.osslabz/turnstile-siteverify-client)

# Cloudflare Turnstile Client

This is a Java client library for the Cloudflare Turnstile Siteverify API. It provides a simple and efficient way to verify Turnstile tokens in your Java applications.

Version 0.4.1 is from 2025-10-29; dependency bumps and an X-Forwarded-For fix have landed since. It is four classes, tested offline against a local HTTP server. One other project of mine depends on it.

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
    <version>0.4.1</version>
</dependency>
```

## Snapshots

Every push to `dev` publishes the next version as a `-SNAPSHOT` to Central's snapshot repository. Maven doesn't
search that repository by default, so a build that wants a snapshot declares it:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Usage

`isValid` returns true only when Cloudflare accepts the token, reports no error codes, and the token was issued for
the given action. It returns false instead of throwing when the call to siteverify fails.

In a servlet application, pass the request. The client reads the token from the `cf-turnstile-response` form field
and the visitor's IP from the usual proxy headers, falling back to the remote address:

```java
import jakarta.servlet.http.HttpServletRequest;
import net.osslabz.turnstile.siteverify.TurnstileSiteverifyClient;

TurnstileSiteverifyClient turnstile = new TurnstileSiteverifyClient("your-secret-key");

boolean human = turnstile.isValid("login", httpServletRequest);
```

Without a servlet request, pass the token and the visitor's IP yourself. The IP is optional; pass `null` to leave
it out:

```java
boolean human = turnstile.isValid("login", token, visitorIp);
```

## Configuration

The default `OkHttpClient` uses 30-second timeouts and logs request and response headers at TRACE. It leaves the
bodies out, because the request body carries the secret key. To use your own, pass it first, and optionally an
`ObjectMapper`, before the secret key:

```java
OkHttpClient httpClient = new OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(5))
        .build();
ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

TurnstileSiteverifyClient turnstile = new TurnstileSiteverifyClient(httpClient, objectMapper, "your-secret-key");
```

A custom `ObjectMapper` needs the `JavaTimeModule` and must ignore unknown properties, because siteverify returns
fields the response class doesn't map.

Contributions are welcome! Please feel free to submit a Pull Request.

## License
