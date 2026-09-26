FROM golang:1.26.2-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/resistance-server ./cmd/server

FROM alpine:3.22
RUN addgroup -S resistance && adduser -S -G resistance resistance && mkdir -p /data/assets && chown -R resistance:resistance /data
COPY --from=build /out/resistance-server /usr/local/bin/resistance-server
USER resistance
EXPOSE 8080
VOLUME ["/data"]
ENTRYPOINT ["resistance-server"]
