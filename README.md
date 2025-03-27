

Connect to SSE stream
```
curl -v -H "Accept: text/event-stream" "http://localhost:8080/sse"
```

Send messages to /mcp/messages (note, include sessionId from SSE)
```
curl -X POST "http://localhost:8080/mcp/message?sessionId=b64ad193-6bb3-4e2e-b33c-27a23011acb4" -d '{"hi": "mynameis"}' | jq .
```


good example?

https://github.com/youngsu5582/mcp-server-mysql

important migration guide

https://github.com/modelcontextprotocol/java-sdk/blob/main/migration-0.8.0.md
