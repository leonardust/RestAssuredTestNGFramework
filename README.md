# Framework refactor

## Create pojo classes for Playlist and Error objects

## Builder pattern

To create requests using builder pattern modify setters to return object of the class for given properties

from:

```java
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }
```
to:
```java
    @JsonProperty("description")
    public Playlist setDescription(String description) {
        this.description = description;
        return this;
    }
```

#### Spec builder

- create package `api`
- create class `SpecBuilder`
- remove `@beforeClass`
- create `getRequestSpec()` and `getResponseSpec() methods in `SpecBuilder`

The spec builder class should look like below

```java
    public static RequestSpecification getRequestSpec() {
        return new RequestSpecBuilder().
        setBaseUri("https://api.spotify.com").
        setBasePath("/v1").
        addHeader("Authorization", "Bearer " + access_token).
        setContentType(ContentType.JSON).
        log(LogDetail.ALL).
        build();
    }

    public static ResponseSpecification getResponseSpec() {
        return new ResponseSpecBuilder().
        log(LogDetail.ALL).
        build();
    }
```
## Playlist API

- Create reusable methods in `PlaylistApi` class

```java
    public Response post(Playlist requestPlaylist) {
        return given(getRequestSpec()).
            body(requestPlaylist).
        when().
            post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists").
        then().
            spec(getResponseSpec()).
            contentType(ContentType.JSON).
            extract().
            response();
    }
```

- Modify test class from:

```java
    @Test
    void shouldBeAvailableToCreatePlaylist() {
        // Create playlist object
        Playlist requestPlaylist = new Playlist().
            setName("Playlist created by RestAssured no 06").
            setDescription("RestAssured playlist description no 06").
            setPublic(false);

        // make request and get response
        Playlist responsePlaylist = given(getRequestSpec()).
            body(requestPlaylist).
        when().
            post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists").
        then().spec(getResponseSpec()).
            assertThat().
            statusCode(201).
            contentType(ContentType.JSON).
            extract().response().as(Playlist.class);

        // assert response
        assertThat(responsePlaylist.getName(), equalTo(requestPlaylist.getName()));
        assertThat(responsePlaylist.getDescription(), equalTo(requestPlaylist.getDescription()));
        assertThat(responsePlaylist.getPublic(), equalTo(requestPlaylist.getPublic()));
    }
```
to:

```java
    @Test
    void shouldBeAvailableToCreatePlaylist() {
        // Create playlist object
        Playlist requestPlaylist = new Playlist().
            setName("Playlist created by RestAssured no 06").
            setDescription("RestAssured playlist description no 06").
            setPublic(false);

        // Get response and assert status code
        Response response = PlaylistApi.post(requestPlaylist);
        assertThat(response.statusCode(), equalTo(201));

        // Deserialize response to Playlist object
        Playlist responsePlaylist = response.as(Playlist.class);

        // Assert Playlist object
        assertThat(responsePlaylist.getName(), equalTo(requestPlaylist.getName()));
        assertThat(responsePlaylist.getDescription(), equalTo(requestPlaylist.getDescription()));
        assertThat(responsePlaylist.getPublic(), equalTo(requestPlaylist.getPublic()));
    }
```
- move authorization header from `SpecBuilder` to `PlaylistApi`
- move `access_token` from `SpecBuilder` to `PlaylistApi`

```java
public static Response post(Playlist requestPlaylist) {
    return given(getRequestSpec()).
        body(requestPlaylist).
        header("Authorization", "Bearer " + access_token).
    when().
        post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists").
    then().
        spec(getResponseSpec()).
    extract().
    response();
}
```
## Rest Assured reusable api

- Create reusable methods between different api's in `RestResource` class

```java
        public static Response post(String path, String token, Object requestPlaylist) {
        return given(getRequestSpec()).
            body(requestPlaylist).
            header("Authorization", "Bearer " + token).
        when().
            post(path).
        then().
            spec(getResponseSpec()).
            extract().
            response();
    }
```
- modify `PlaylistApi` class to use methods from `RestResource` class

from:
```java
    public static Response post(Playlist requestPlaylist) {
        return given(getRequestSpec()).
            body(requestPlaylist).
            header("Authorization", "Bearer " + access_token).
        when().
            post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists").
        then().
            spec(getResponseSpec()).
            extract().
            response();
    }
```

to:
```java
    public static Response post(Playlist requestPlaylist) {
        return RestResource.post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists",access_token, requestPlaylist);
    }
```

## Renew Token

- create class `TokenManager` in `api` package
- create method `renewToken()`
- create `HashMap` to store `key-value` pairs from `x-www-form-urlencoded`
- build request and extract response
- determine calls for refresh token

```java
    public static String renewToken() {
        HashMap<String, String> formParams = new HashMap<>();
        formParams.put("client_id", "c5cd1ae8a7e040069f3e38c75f9015df");
        formParams.put("client_secret", "ba0731e1780e446097cb8823a5203706");
        formParams.put("grant_type", "refresh_token");
        formParams.put("refresh_token", "AQDj-4M9OPmwXMeOAmpBwy56Zf0i2XNR5Kk-wqWSXtQVjW3qH83UkGqWc_nndzzRFJQoS48uh-VV2uhBOS6pOJhgMGj62rkQ76CDtX2ltLzbarRDJtx0go9zH-Lioua6qcU");

        Response response = given().
            baseUri("https://accounts.spotify.com").
            contentType(ContentType.URLENC).
            formParams(formParams).
            log().all().
        when().
            post("/api/token").
        then().
            spec(getResponseSpec()).
            extract().
            response();

        if(response.statusCode() != 200) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return response.path("access_token");
    }
```

- remove access token and adjust `PlaylistApi` methods

```java
    public static Response post(Playlist requestPlaylist) {
        return RestResource.post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists", renewToken(), requestPlaylist);
    }
```

## Check token expiry

- add private static fields `access_token` and `expiry time`
- add method `getToken()` to `TokenManager` class
- modify method `renewToken()` to return `Response` type
- add `getToken()` body and change access modifier of `renewToken()` method to `private`

```java
    public static String getToken() {
        try {
            if(access_token == null || Instant.now().isAfter(expiry_time)) {
                System.out.println("Renewing token ...");
                Response response = renewToken();
                access_token = response.path("access_token");
                int expiryTimeInSeconds = response.path("expires_in");
                expiry_time = Instant.now().plusSeconds(expiryTimeInSeconds - 300);
            } else {
                System.out.println("Token is valid");
            }

        } catch(Exception e) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return access_token;
    }
```

- adjust `PlayListApi`

```java
    public static Response post(Playlist requestPlaylist) {
        return RestResource.post("/users/31xfrt3i3nylwdknakbx7xxbgytm/playlists", getToken(), requestPlaylist);
    }
```
- move rest methods from `TokenManager` to `RestResource`

RestResource
```java
    public static Response postAccount(HashMap<String, String> formParams) {
        return given().
            baseUri("https://accounts.spotify.com").
            contentType(ContentType.URLENC).
            formParams(formParams).
            log().all().
        when().
            post("/api/token").
        then().
            spec(getResponseSpec()).
            extract().
            response();
    }
```

TokenManager
```java
    private static Response renewToken() {
        HashMap<String, String> formParams = new HashMap<>();
        formParams.put("client_id", "c5cd1ae8a7e040069f3e38c75f9015df");
        formParams.put("client_secret", "ba0731e1780e446097cb8823a5203706");
        formParams.put("grant_type", "refresh_token");
        formParams.put("refresh_token", "AQDj-4M9OPmwXMeOAmpBwy56Zf0i2XNR5Kk-wqWSXtQVjW3qH83UkGqWc_nndzzRFJQoS48uh-VV2uhBOS6pOJhgMGj62rkQ76CDtX2ltLzbarRDJtx0go9zH-Lioua6qcU");

        Response response = RestResource.postAccount(formParams);

        if(response.statusCode() != 200) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return response;
    }
```
## Reusable spec

- add `getAccountRequestSpec()` into `SpecBuilder`
- add `postAccount()` into `RestResource`

getAccountRequestSpec()
```java
    public static RequestSpecification getAccountRequestSpec() {
        return new RequestSpecBuilder().
        setBaseUri("https://accounts.spotify.com").
        setContentType(ContentType.URLENC).
        log(LogDetail.ALL).
        build();
    }
```
postAccount()
```java
    public static Response postAccount(HashMap<String, String> formParams) {
        return given(getAccountRequestSpec()).
            formParams(formParams).
        when().
            post("/api/token").
        then().
            spec(getResponseSpec()).
            extract().
            response();
    }
```
## Routes

- create `Route` class to store information about paths to resources
- adjust `Playlist` api

Route class
```java
    public static final String BASE_PATH = "/v1";
    public static final String API = "/api";
    public static final String TOKEN = "/token";
    public static final String USERS = "/users";
    public static final String PLAYLISTS = "/playlists/";
```
Playlist class
```java
    public static Response post(Playlist requestPlaylist) {
        return RestResource.post(USERS + "/31xfrt3i3nylwdknakbx7xxbgytm" + PLAYLISTS, getToken(), requestPlaylist);
    }
```
## Property Loaders

### Property Utils class

- create package `Utils` in `com.spotify.oauth2` package
- create class `PropertyUtils` in `Utils` package
- create `propertLoader()` method in `PropertyUtils` class

```java
    public static Properties propertyLoader(String filePath) {
        Properties properties = new Properties();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            try {
                properties.load(reader);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load properties file " + filePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Properties file not found at " + filePath);
        }
        return properties;
    }
```

### Config Loader (Singleton design pattern)

- create `resources` package in `src/test`
- create `config.properties` file in `resources`
- copy `formParams` from `TokenManager` class to `config.properties` also add `user_id` 
- create `ConfigLoader` in `utils` package

```java
    private final Properties properties;
    private static ConfigLoader configLoader;

    private ConfigLoader() {
    properties = PropertyUtils.propertyLoader("src/test/resources/config.properties");
    }

    public static ConfigLoader getInstance() {
    if(configLoader == null) {
    configLoader = new ConfigLoader();
    }
    return configLoader;
    }

    public String getClientId() {
    String prop = properties.getProperty("client_id");
    if(prop != null) return prop;
    else throw new RuntimeException("Property client_id is not specified in the config.properties file");
    }

    public String getClientSecret() {
    String prop = properties.getProperty("client_secret");
    if(prop != null) return prop;
    else throw new RuntimeException("Property client_secret is not specified in the config.properties file");
    }

    public String getGrantType() {
    String prop = properties.getProperty("grant_type");
    if(prop != null) return prop;
    else throw new RuntimeException("Property grant_type is not specified in the config.properties file");
    }

    public String getRefreshToken() {
    String prop = properties.getProperty("refresh_token");
    if(prop != null) return prop;
    else throw new RuntimeException("Property refresh_token is not specified in the config.properties file");
    }

    public String getUserId() {
    String prop = properties.getProperty("user_id");
    if(prop != null) return prop;
    else throw new RuntimeException("Property user_id is not specified in the config.properties file");
    }
```

- adjust `renewToken()` method in `TokenManager` class
```java
private static Response renewToken() {
        HashMap<String, String> formParams = new HashMap<>();
        formParams.put("client_id", ConfigLoader.getInstance().getClientId());
        formParams.put("client_secret", ConfigLoader.getInstance().getClientSecret());
        formParams.put("grant_type", ConfigLoader.getInstance().getGrantType());
        formParams.put("refresh_token", ConfigLoader.getInstance().getRefreshToken());
        Response response = RestResource.postAccount(formParams);

        if(response.statusCode() != 200) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return response;
    }
```
- adjust `PlaylistApi` class
```java
    public static Response post(Playlist requestPlaylist) {
        return RestResource.post(USERS + "/" + ConfigLoader.getInstance().getUserId() + PLAYLISTS, getToken(), requestPlaylist);
    }
```

### Data properties

- add `DataLoader` class

```java
    private final Properties properties;
    private static DataLoader dataLoader;

    private DataLoader() {
        properties = PropertyUtils.propertyLoader("src/test/resources/config.properties");
    }

    public static DataLoader getInstance() {
        if(dataLoader == null) {
            dataLoader = new DataLoader();
        }
        return dataLoader;
    }

    public String getPlaylistId() {
        String prop = properties.getProperty("get_playlist_id");
        if(prop != null) return prop;
        else throw new RuntimeException("Property get_playlist_id is not specified in the config.properties file");
    }

    public String getUpdatePlaylistId() {
        String prop = properties.getProperty("update_playlist_id");
        if(prop != null) return prop;
        else throw new RuntimeException("Property update_playlist_id is not specified in the config.properties file");
    }
```

- adjust test cases
```java
    @Test
    void shouldBeAvailableToGetPlaylist() {
        Response response = PlaylistApi.get(DataLoader.getInstance().getPlaylistId());
        assertThat(response.statusCode(), equalTo(200));

        Playlist responsePlaylist = response.as(Playlist.class);

        assertThat(responsePlaylist.getName(), equalTo("Updated Playlist Name"));
        assertThat(responsePlaylist.getDescription(), equalTo("Updated playlist description"));
        assertThat(responsePlaylist.getPublic(), equalTo(false));
    }
```

## Optimize tests

## Lombok

- `@Getter` and `@Setter`, `@Jackonized`
- `@Jacksonized` annotation allows to recognize Jackson annotations along with `@Builder`

### Implement Lombok without builder

### Implement Lombok with builder

## Allure Reports

- install
- add dependency
- generate reports `allure serve allure-results`
- add `display name`, `description`, `links`, `behaviors(epic, feature)`, `steps`, `filter`, `custom folder path`

## OAuth 

- replace header method by `auth().oauth2(token)`

```java
    public static Response post(String path, String token, Object requestPlaylist) {
        return given(getRequestSpec()).
            body(requestPlaylist).
            auth().oauth2(token).
        when().
            post(path).
        then().
            spec(getResponseSpec()).
            extract().
            response();
    }
```

## Java Faker API
Allows to create random strings during runtime

- add dependency
```xml
<!-- https://mvnrepository.com/artifact/com.github.javafaker/javafaker -->
<dependency>
    <groupId>com.github.javafaker</groupId>
        <artifactId>javafaker</artifactId>
        <version>1.0.2</version>
</dependency>
```
- create `FakerUtils` class and implement methods
```java
public class FakerUtils {

    public static String generateName() {
        Faker faker = new Faker();
        return "Playlist " + faker.regexify("[A-Za-z0-9 ,_-{10}]");
    }

    public static String generateDescription() {
        Faker faker = new Faker();
        return "Playlist " + faker.regexify("[A-Za-z0-9_@&+ ,_-{50}]");
    }
}
```

## Java Enums to store Status Codes

- create enum `StatusCode` in `api` package
```java
@Getter
public enum StatusCode {

    CODE_200(200, ""),
    CODE_201(201, ""),
    CODE_400(400, "Missing required field: name"),
    CODE_401(401, "Invalid access token");

    private final int code;
    private final String message;

    StatusCode(int code, String message) {
      this.code = code;
      this.message = message;
    }
}
```
- adjust test cases
```java
assertStatusCode(response.statusCode(), StatusCode.CODE_200.getCode());
or
assertError(response.as(Error.class), StatusCode.CODE_400.getCode(), StatusCode.CODE_400.getMessage());
```

## Parallel execution

- access token should be shared between test instances
- add `synchronized` keyword to `getToken()` method to prevent that in one time only one instance can execute method
- set parallel property in `maven surfire` plugin configuration. Default threads is `5`
```xml
<parallel>methods</parallel>
<threadCount>10</threadCount>
```
- add `BaseTest class` and create `beforeMethod()` to store information about current Threads and methods
```java
@BeforeMethod
void beforeMethod(Method m) {
    log.info("STARTING TEST: " + m.getName() + " with THREAD ID: " + Thread.currentThread().getId());
}
```

## Handle multiple environments

- use `System property` in `setBaseUri()`

API_BASE_URI
```java
    public static RequestSpecification getRequestSpec() {
        return new RequestSpecBuilder().
        setBaseUri(System.getProperty("API_BASE_URI")).
        setBasePath(BASE_PATH).
        setContentType(ContentType.JSON).
        addFilter(new AllureRestAssured()).
        log(LogDetail.ALL).
        build();
    }
```
ACCOUNT_BASE_URI
```java
    public static RequestSpecification getAccountRequestSpec() {
        return new RequestSpecBuilder().
        setBaseUri(System.getProperty("ACCOUNT_BASE_URI")).
        setContentType(ContentType.URLENC).
        addFilter(new AllureRestAssured()).
        log(LogDetail.ALL).
        build();
    }
```
- adjust maven command to run test against specific env
```bash
mvn clean test -DAPI_BASE_URI="https://api.spotify.com" -DACCOUNT_BASE_URI="https://accounts.spotify.com"
```
- adjust testng runner in edit configuration with:
```java
-DAPI_BASE_URI="https://api.spotify.com"
-DACCOUNT_BASE_URI="https://accounts.spotify.com"
```

## Github

- create repository in GitHub and execute commands in terminal

```bash
git init
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/leonardust/RestAssuredTestNGFramework.git
git push -u origin main
```