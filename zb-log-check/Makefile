.DEFAULT: docker

GET_VERSION = $(shell mvn org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn | grep -v "^\[")
SET_VERSION = $(eval VERSION=$(GET_VERSION))

# Eventually use the --squash flag when building (currently an experimental feature)
.PHONY: docker-build
docker-build:
	$(SET_VERSION)
	docker build -f Dockerfile --build-arg VERSION=$(VERSION) --target application -t gcr.io/zeebe-io/zb-log-check:$(VERSION) .

.PHONY: docker-push
docker-push:
	$(SET_VERSION)
	docker push gcr.io/zeebe-io/zb-log-check:$(VERSION)

.PHONY: docker
docker: docker-build docker-push