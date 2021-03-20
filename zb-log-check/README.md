# Zeebe Log Check

## Docker

You can use the provided `Makefile` to build a small container which will run the application.

To build and push at once, just run:

```shell
make docker
```

### Build

The image is a multi-stage image, with two stages: `builder` and `application`.

The builder is a simple maven based image which will create the application JAR, and the
application is the actual container than can later be used.

To build, run:

```shell
make docker-build 
```

### Push

As the image is tagged for the Zeebe team repositories, it requires you to be have Docker configured to
use Google Cloud authentication credentials.

To push, simply run:

```shell
make docker-push
```

## Kubernetes

One of the best use cases for this is to use it as an init container for a Zeebe broker. This allows us to run
consistency checks before the broker starts on its own data, making our tests much more robust.

Here is a sample configuration you could use to describe an init container for a Zeebe broker:

```yaml
initContainers:
  - name: zb-log-check
    image: gcr.io/zeebe-io/zb-log-check:0.1.0
    imagePullPolicy: Always
    volumeMounts:
    - name: data
      mountPath: "/usr/local/zb-log-check/zeebe/data"
```

If you want to use a different mount path, you can specify it as the environment variable `ZB_LOG_CHECK_DATA_DIR`
```yaml
initContainers:
  - name: zb-log-check
    image: gcr.io/zeebe-io/zb-log-check:0.1.0
    imagePullPolicy: Always
    env:
      - name: ZB_LOG_CHECK_DATA_DIR
        value: "/my/custom/path"
    volumeMounts:
    - name: data
      mountPath: "/my/custom/path"
```

To use it with the Zeebe Helm charts, you can specify it under the `extraInitContainers` value:

```yaml
extraInitContainers: |
  - name: zb-log-check
    image: gcr.io/zeebe-io/zb-log-check:0.1.0
    imagePullPolicy: Always
    volumeMounts:
    - name: data
      mountPath: "/usr/local/zb-log-check/zeebe/data"
```