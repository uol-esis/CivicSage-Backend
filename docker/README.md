# Development Docker Compose

To make use of the object Storage you need to configure the object storage and obtain the credentials. All those steps
have been gathered in individual scripts:

| script              | goal                                                                       |
|---------------------|----------------------------------------------------------------------------|
| createGarageToml.sh | generates a new `garage.toml` file for the object storage to be functional |
| configure.sh        | configures the object storage; will be used by a helper container          |

The steps to get the object storage started are as follows (from within the `docker` directory):

1. Start the object storage with `docker compose -f objectstarage.yaml up`
2. On the first start, the helper script will print a key id and a secret key to the console.
3. Enter the credentials in `civicsage.env` for docker deployment or `application.properties` for local development.
4. Start the rest of the stack with `docker compose up`.

As Garage is just starting with developing a UI for bucket administration, the scripts are required to be able to
configure the stack. However, once the UI is ready, we will have an eye on it to check if it can replace the current
setup.

## Why we chose Garage

For a long time MinIO seemed to be the most viable option when it comes to open source object storage solutions.
However, with recent developments (https://github.com/minio/object-browser/pull/3509) it seems, that the project is
moving into a more business oriented direction. This itself is not a problem, but since we want to provide an easily
manageable solution for our users, we required a UI administration solution that is not behind a paywall. During the
search for alternatives, we stumbled upon Garage (and other object Storages like Apache Ozone). Garage seemed well
documented as well as straight forward to set up and use. That's not to say, that we will definitely keep using Garage -
we implemented the communication based on the S3 protocol, so changing the underlying storage should not be too
hard - but for now, we will stick to it.