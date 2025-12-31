# Avatar Store Backend

Spring Boot backend for the Avatar Store application, connecting to Supabase for database operations.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Supabase account and project

## Setup

1. **Clone and navigate to backend directory:**
   ```bash
   cd backend
   ```

2. **Set up environment variables:**
   
   **Option 1: Environment variables (recommended)**
   ```bash
   export SUPABASE_URL=https://your-project.supabase.co
   export SUPABASE_SERVICE_ROLE_KEY=your_service_role_key
   export SUPABASE_ANON_KEY=your_anon_key
   ```
   
   **Option 2: Update application.properties**
   Edit `src/main/resources/application.properties`:
   ```properties
   supabase.url=https://your-project.supabase.co
   supabase.service-role-key=your_service_role_key
   supabase.anon-key=your_anon_key
   ```
   
   **Important:** The Supabase URL should be the full URL (e.g., `https://xxxxx.supabase.co`) without a trailing slash.

3. **Build the project:**
   ```bash
   mvn clean install
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR:
   ```bash
   java -jar target/avatar-store-backend-1.0.0.jar
   ```

The API will be available at `http://localhost:8080/api`

## API Endpoints

### Avatars
- `GET /api/avatars` - Get all avatars
- `GET /api/avatars?id={id}` - Get avatar by ID
- `GET /api/avatars?slug={slug}` - Get avatar by slug

## Configuration

The application uses `application.properties` for configuration. Key settings:
- Server port: 8080
- Context path: /api
- CORS: Enabled for localhost:3000 and localhost:3001

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/avatarstore/
│   │   │   ├── AvatarStoreApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SupabaseConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── ObjectMapperConfig.java
│   │   │   ├── controller/
│   │   │   │   └── AvatarController.java
│   │   │   ├── service/
│   │   │   │   └── SupabaseService.java
│   │   │   ├── model/
│   │   │   │   └── Avatar.java
│   │   │   └── dto/
│   │   │       └── ApiResponse.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## Development

The application uses Spring Boot DevTools for hot reloading during development.

## Dependencies

- Spring Boot Web
- RestTemplate (for HTTP client)
- Jackson (JSON processing)
- Lombok (reducing boilerplate)
- Supabase REST API integration

## Troubleshooting

### "URI is not absolute" Error
This error means the Supabase URL is not configured. Make sure:
1. `SUPABASE_URL` environment variable is set, OR
2. `supabase.url` is set in `application.properties`
3. The URL is a complete URL (e.g., `https://xxxxx.supabase.co`) without trailing slash

### "Configuration error" on startup
Check that all required environment variables are set:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_ANON_KEY` (optional, but recommended)

### "infinite recursion detected in policy" Error
This error occurs when RLS policies reference the same table they're protecting. To fix:

1. Go to your Supabase dashboard → SQL Editor
2. Run the fix script: `backend/src/main/resources/db/fix_rls_recursion.sql`
3. This creates an `is_admin()` function that bypasses RLS to check admin status
4. All admin policies are updated to use this function instead of directly querying `user_profiles`

Alternatively, if you're setting up a new database, use the updated `schema.sql` which already includes the fix.

