# My Garage Server

This is the backend server for the My Garage application.

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
2.  Place the `aiven-ca.pem` file in the `src/main/resources` directory.
3.  In the `application.properties` file, comment out the local database configuration and uncomment the Aiven database configuration.
