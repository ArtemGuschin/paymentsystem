# DefaultApi

All URIs are relative to *http://localhost:8083/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**currenciesGet**](DefaultApi.md#currenciesGet) | **GET** /currencies | Получить список поддерживаемых валют |
| [**healthGet**](DefaultApi.md#healthGet) | **GET** /health | Health check |
| [**rateProvidersGet**](DefaultApi.md#rateProvidersGet) | **GET** /rate-providers | Получить список провайдеров курсов валют |
| [**ratesGet**](DefaultApi.md#ratesGet) | **GET** /rates | Получить курс между двумя валютами на указанную дату/время |


<a id="currenciesGet"></a>
# **currenciesGet**
> List&lt;CurrencyResponse&gt; currenciesGet()

Получить список поддерживаемых валют

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8083/api/v1");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    try {
      List<CurrencyResponse> result = apiInstance.currenciesGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#currenciesGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;CurrencyResponse&gt;**](CurrencyResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Список валют |  -  |

<a id="healthGet"></a>
# **healthGet**
> HealthGet200Response healthGet()

Health check

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8083/api/v1");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    try {
      HealthGet200Response result = apiInstance.healthGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#healthGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**HealthGet200Response**](HealthGet200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Сервис работает |  -  |

<a id="rateProvidersGet"></a>
# **rateProvidersGet**
> List&lt;RateProviderResponse&gt; rateProvidersGet()

Получить список провайдеров курсов валют

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8083/api/v1");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    try {
      List<RateProviderResponse> result = apiInstance.rateProvidersGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#rateProvidersGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;RateProviderResponse&gt;**](RateProviderResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Провайдеры |  -  |

<a id="ratesGet"></a>
# **ratesGet**
> RateResponse ratesGet(from, to, timestamp)

Получить курс между двумя валютами на указанную дату/время

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.models.*;
import org.openapitools.client.api.DefaultApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8083/api/v1");

    DefaultApi apiInstance = new DefaultApi(defaultClient);
    String from = "from_example"; // String | Код исходной валюты (например, \"USD\")
    String to = "to_example"; // String | Код целевой валюты (например, \"EUR\")
    OffsetDateTime timestamp = OffsetDateTime.now(); // OffsetDateTime | Дата/время, на которую нужен курс (ISO 8601, UTC). Если не указано, возвращается последний актуальный курс.
    try {
      RateResponse result = apiInstance.ratesGet(from, to, timestamp);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DefaultApi#ratesGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **from** | **String**| Код исходной валюты (например, \&quot;USD\&quot;) | |
| **to** | **String**| Код целевой валюты (например, \&quot;EUR\&quot;) | |
| **timestamp** | **OffsetDateTime**| Дата/время, на которую нужен курс (ISO 8601, UTC). Если не указано, возвращается последний актуальный курс. | [optional] |

### Return type

[**RateResponse**](RateResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Курс найден |  -  |
| **404** | Курс не найден |  -  |
| **400** | Ошибка запроса |  -  |

