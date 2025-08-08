Для тестирования в postman: 
1) Создание пользователя :
   curl -X POST 'http://localhost:8081/api/v1/users' \
   -H 'Content-Type: application/json' \
   -d '{
   "email": "test@example.com",
   "password": "password123",
   "firstName": "John",
   "lastName": "Doe",
   "address": {
   "countryId": 1,
   "address": "123 Main St",
   "zipCode": "12345",
   "city": "New York",
   "state": "NY"
   },
   "individual": {
   "passportNumber": "AB123456",
   "phoneNumber": "+1234567890"
   }
   }'
2) Получение пользователя по ID
# Замените {userId} на реальный UUID из ответа на создание
curl -X GET 'http://localhost:8081/api/v1/users/{userId}' \
-H 'Accept: application/json'

3) Получение пользователя по email
   curl -X GET 'http://localhost:8081/api/v1/users/by-email/test@example.com' \
   -H 'Accept: application/json'
4) Обновление пользователя
# Обновляем имя и город в адресе
curl -X PUT 'http://localhost:8081/api/v1/users/{userId}' \
-H 'Content-Type: application/json' \
-d '{
"firstName": "John Updated",
"address": {
"city": "Brooklyn"
}
}'
5) Удаление пользователя
   curl -X DELETE 'http://localhost:8081/api/v1/users/{userId}'



