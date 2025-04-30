# App Usage Tutorial


## 2. Postman Testing Guide

### Authentication

1. **Register User**
   - Method: POST
   - URL: http://localhost:8081/api/user/register
   - Headers: Content-Type: application/json
   - Body (raw JSON):
   ```json
   {
       "username": "testuser",
       "email": "test@example.com",
       "password": "secret"
   }
   ```

2. **Login**
   - Method: POST
   - URL: http://localhost:8081/api/user/login
   - Headers: Content-Type: application/json
   - Body (raw JSON):
   ```json
   {
       "username": "testuser",
       "password": "secret"
   }
   ```
   - Save the returned JWT token for subsequent requests

### Friend Requests

3. **Send Friend Request**
   - Method: POST
   - URL: http://localhost:8081/api/friends/send/2
   - Headers: 
     - Authorization: Bearer YOUR_JWT_TOKEN
     - Content-Type: application/json
   - No body required

4. **Accept Friend Request**
   - Method: POST
   - URL: http://localhost:8081/api/friends/accept/1
   - Headers:
     - Authorization: Bearer YOUR_JWT_TOKEN
     - Content-Type: application/json
   - No body required

5. **Reject Friend Request**
   - Method: POST
   - URL: http://localhost:8081/api/friends/reject/1
   - Headers:
     - Authorization: Bearer YOUR_JWT_TOKEN
     - Content-Type: application/json
   - No body required

6. **View Pending Requests**
   - Method: GET
   - URL: http://localhost:8081/api/friends/pending
   - Headers:
     - Authorization: Bearer YOUR_JWT_TOKEN
   - No body required

### User Management

7. **Get All Users**
   - Method: GET
   - URL: http://localhost:8081/api/user/getAll
   - Headers: Authorization: Bearer YOUR_JWT_TOKEN

8. **Get User by ID**
   - Method: GET
   - URL: http://localhost:8081/api/user/getUserById/1
   - Headers: Authorization: Bearer YOUR_JWT_TOKEN

9. **Get User by Email**
   - Method: GET
   - URL: http://localhost:8081/api/user/getUserByEmail/test@example.com
   - Headers: Authorization: Bearer YOUR_JWT_TOKEN

10. **Update User**
    - Method: PUT
    - URL: http://localhost:8081/api/user/update
    - Headers: 
      - Authorization: Bearer YOUR_JWT_TOKEN
      - Content-Type: application/json
    - Body (raw JSON):
    ```json
    {
        "id": 1,
        "username": "updateduser",
        "email": "updated@example.com"
    }
    ```

11. **Delete User**
    - Method: DELETE
    - URL: http://localhost:8081/api/user/1
    - Headers: Authorization: Bearer YOUR_JWT_TOKEN

### Testing Workflow

1. Register a new user
2. Login to get the JWT token
3. Copy the JWT token
4. Use the token in the Authorization header for subsequent requests
5. Test friend request endpoints
6. Test user management endpoints

*Note: Replace YOUR_JWT_TOKEN with the actual token received from the login response.*

// ...rest of existing content...
