1 Health
curl --location http://localhost:8083/api/v1/actuator/health
2️⃣ Получить список валют
curl --location http://localhost:8083/api/v1/currencies
3️⃣ Получить список провайдеров курсов
curl -X GET http://localhost:8083/api/v1/rate-providers
4️⃣ Получить курс валют
http://localhost:8083/api/v1/rates?from=EUR&to=USD
5 Get Rate With Timestamp
curl -X GET http://localhost:8083/api/v1/rates?from=EUR&to=USD&timestamp=2026-03-16T09:56:00Z


 
Collection of Postman
{
"info": {
"name": "Currency Rate Service API",
"_postman_id": "c9e1b7a4-9b34-4e7b-9e0f-1f3f3a1c0c01",
"description": "API collection for Currency Rate Service",
"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
},
"item": [
{
"name": "1 Health Check",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8083/api/v1/actuator/health",
"protocol": "http",
"host": ["localhost"],
"port": "8083",
"path": ["api", "v1", "actuator", "health"]
}
}
},
{
"name": "2 Get Currencies",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8083/api/v1/currencies",
"protocol": "http",
"host": ["localhost"],
"port": "8083",
"path": ["api", "v1", "currencies"]
}
}
},
{
"name": "3 Get Rate Providers",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8083/api/v1/rate-providers",
"protocol": "http",
"host": ["localhost"],
"port": "8083",
"path": ["api", "v1", "rate-providers"]
}
}
},
{
"name": "4 Get Currency Rate (with timestamp)",
"request": {
"method": "GET",
"header": [],
"url": {
"raw": "http://localhost:8083/api/v1/rates?from=EUR&to=USD&timestamp=2026-03-16T09:56:00Z",
"protocol": "http",
"host": ["localhost"],
"port": "8083",
"path": ["api", "v1", "rates"],
"query": [
{
"key": "from",
"value": "EUR"
},
{
"key": "to",
"value": "USD"
},
{
"key": "timestamp",
"value": "2026-03-16T09:56:00Z"
}
]
}
}
}
]
}
