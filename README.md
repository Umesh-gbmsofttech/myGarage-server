# My Garage Server

This is the backend server for the My Garage application.

## AI Chat (Groq)

The in-app Support Chat uses Groq. Configure the key via:
- `GROQ_API_KEY` environment variable, or
- `api.groq.key` in `src/main/resources/application.properties`.

The assistant is scoped to user features (login, signup, bookings, ratings, feedback, DIY) and intentionally refuses admin guidance.

## Database Configuration

The application is configured to connect to a MySQL database. The configuration can be found in the `src/main/resources/application.properties` file.

### Local Database

By default, the application is configured to connect to a local MySQL database with the following credentials:
-   **URL**: `jdbc:mysql://localhost:3306/user?createDatabaseIfNotExist=true`
-   **Username**: `root`
-   **Password**: `root`

Please ensure you have a local MySQL server running with these credentials.

### Aiven Database (Production)

The production database is hosted on Aiven. To connect to the Aiven database, you need to:
1.  Obtain the `aiven-ca.pem` certificate file.
2.  Place the `aiven-ca.pem` file in the base directory where the Dockerfile is placed.
3.  In the `application.properties` file, comment out the local database configuration and uncomment the Aiven database configuration.


## Using Cron job website to keep alive render hosted server:

1.  https://console.cron-job.org/jobs

## Mobile Build Commands (for frontend app)

1. Generate Android native files (clean)
```
npx expo prebuild --platform android --clean
```

2. Local APK build (Windows)
```
cd android
.\gradlew.bat clean assembleRelease
```

3. EAS Android build (clear cache)
```
eas build -p android --clear-cache
```

4. EAS iOS build (clear cache)
```
eas build -p ios --clear-cache
```
